package no.nav.helse.spleis.dao

import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.firstOrNull
import com.github.navikt.tbd_libs.sql_dsl.mapNotNull
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import com.github.navikt.tbd_libs.sql_dsl.string
import io.micrometer.core.instrument.MeterRegistry
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource
import no.nav.helse.spleis.dto.HendelseDTO
import no.nav.helse.spleis.objectMapper
import org.intellij.lang.annotations.Language

internal class HendelseDao(private val dataSource: () -> DataSource, private val meterRegistry: MeterRegistry) {

    fun hentHendelse(meldingsReferanse: UUID): String? {
        return dataSource().connection {
            prepareStatementWithNamedParameters("SELECT data FROM melding WHERE melding_id = cast(:meldingId as text)") {
                withParameter("meldingId", meldingsReferanse)
            }.use {
                it.executeQuery().use { rs ->
                    rs.firstOrNull { it.string("data") }
                }
            }
        }.also {
            PostgresProbe.hendelseLestFraDb(meterRegistry)
        }
    }

    fun hentHendelser(fødselsnummer: Long): List<HendelseDTO> {
        @Language("PostgreSQL")
        val statement = """
            SELECT melding_type, data FROM melding 
            WHERE fnr=:fnr AND (melding_type = 'NY_SØKNAD' OR melding_type = 'SENDT_SØKNAD_NAV' OR melding_type = 'SENDT_SØKNAD_FRILANS'
                OR melding_type = 'SENDT_SØKNAD_SELVSTENDIG' OR melding_type = 'SENDT_SØKNAD_ARBEIDSGIVER' 
                OR melding_type = 'SENDT_SØKNAD_JORDBRUKER'
                OR melding_type = 'SENDT_SØKNAD_ARBEIDSLEDIG' OR melding_type = 'SENDT_SØKNAD_ARBEIDSLEDIG_TIDLIGERE_ARBEIDSTAKER'
                OR melding_type = 'INNTEKTSMELDING' OR melding_type = 'NAV_NO_SELVBESTEMT_INNTEKTSMELDING' OR melding_type = 'NAV_NO_KORRIGERT_INNTEKTSMELDING' OR melding_type = 'NAV_NO_INNTEKTSMELDING' 
                OR melding_type = 'SYKEPENGEGRUNNLAG_FOR_ARBEIDSGIVER')
        """
        return dataSource().connection {
            prepareStatementWithNamedParameters(statement) {
                withParameter("fnr", fødselsnummer)
            }.use {
                it.executeQuery().use { rs ->
                    rs.mapNotNull { row -> Meldingstype.valueOf(row.string("melding_type")) to row.string("data") }
                }
            }
        }.mapNotNull { (type, data) ->
            objectMapper.readTree(data)?.let { node ->
                when (type) {
                    Meldingstype.NY_SØKNAD -> HendelseDTO.nySøknad(
                        id = node.path("@id").asText(),
                        eksternDokumentId = node.path("id").asText(),
                        fom = LocalDate.parse(node.path("fom").asText()),
                        tom = LocalDate.parse(node.path("tom").asText()),
                        rapportertdato = LocalDateTime.parse(node.path("@opprettet").asText()),
                    )

                    Meldingstype.NY_SØKNAD_FRILANS -> HendelseDTO.nyFrilanssøknad(
                        id = node.path("@id").asText(),
                        eksternDokumentId = node.path("id").asText(),
                        fom = LocalDate.parse(node.path("fom").asText()),
                        tom = LocalDate.parse(node.path("tom").asText()),
                        rapportertdato = LocalDateTime.parse(node.path("@opprettet").asText()),
                    )

                    Meldingstype.NY_SØKNAD_SELVSTENDIG -> HendelseDTO.nySelvstendigsøknad(
                        id = node.path("@id").asText(),
                        eksternDokumentId = node.path("id").asText(),
                        fom = LocalDate.parse(node.path("fom").asText()),
                        tom = LocalDate.parse(node.path("tom").asText()),
                        rapportertdato = LocalDateTime.parse(node.path("@opprettet").asText()),
                    )

                    Meldingstype.NY_SØKNAD_ARBEIDSLEDIG -> HendelseDTO.nyArbeidsledigsøknad(
                        id = node.path("@id").asText(),
                        eksternDokumentId = node.path("id").asText(),
                        fom = LocalDate.parse(node.path("fom").asText()),
                        tom = LocalDate.parse(node.path("tom").asText()),
                        rapportertdato = LocalDateTime.parse(node.path("@opprettet").asText()),
                    )

                    Meldingstype.SENDT_SØKNAD_NAV -> HendelseDTO.sendtSøknadNav(
                        id = node.path("@id").asText(),
                        eksternDokumentId = node.path("id").asText(),
                        fom = LocalDate.parse(node.path("fom").asText()),
                        tom = LocalDate.parse(node.path("tom").asText()),
                        rapportertdato = LocalDateTime.parse(node.path("@opprettet").asText()),
                        sendtNav = LocalDateTime.parse(node.path("sendtNav").asText())
                    )

                    Meldingstype.SENDT_SØKNAD_FRILANS -> HendelseDTO.sendtSøknadFrilans(
                        id = node.path("@id").asText(),
                        eksternDokumentId = node.path("id").asText(),
                        fom = LocalDate.parse(node.path("fom").asText()),
                        tom = LocalDate.parse(node.path("tom").asText()),
                        rapportertdato = LocalDateTime.parse(node.path("@opprettet").asText()),
                        sendtNav = LocalDateTime.parse(node.path("sendtNav").asText())
                    )

                    Meldingstype.SENDT_SØKNAD_SELVSTENDIG -> HendelseDTO.sendtSøknadSelvstendig(
                        id = node.path("@id").asText(),
                        eksternDokumentId = node.path("id").asText(),
                        fom = LocalDate.parse(node.path("fom").asText()),
                        tom = LocalDate.parse(node.path("tom").asText()),
                        rapportertdato = LocalDateTime.parse(node.path("@opprettet").asText()),
                        sendtNav = LocalDateTime.parse(node.path("sendtNav").asText())
                    )

                    Meldingstype.SENDT_SØKNAD_ARBEIDSLEDIG_TIDLIGERE_ARBEIDSTAKER,
                    Meldingstype.SENDT_SØKNAD_ARBEIDSLEDIG -> HendelseDTO.sendtSøknadArbeidsledig(
                        id = node.path("@id").asText(),
                        eksternDokumentId = node.path("id").asText(),
                        fom = LocalDate.parse(node.path("fom").asText()),
                        tom = LocalDate.parse(node.path("tom").asText()),
                        rapportertdato = LocalDateTime.parse(node.path("@opprettet").asText()),
                        sendtNav = LocalDateTime.parse(node.path("sendtNav").asText())
                    )

                    Meldingstype.SENDT_SØKNAD_ARBEIDSGIVER -> HendelseDTO.sendtSøknadArbeidsgiver(
                        id = node.path("@id").asText(),
                        eksternDokumentId = node.path("id").asText(),
                        fom = LocalDate.parse(node.path("fom").asText()),
                        tom = LocalDate.parse(node.path("tom").asText()),
                        rapportertdato = LocalDateTime.parse(node.path("@opprettet").asText()),
                        sendtArbeidsgiver = LocalDateTime.parse(node.path("sendtArbeidsgiver").asText())
                    )

                    Meldingstype.NAV_NO_INNTEKTSMELDING,
                    Meldingstype.NAV_NO_SELVBESTEMT_INNTEKTSMELDING,
                    Meldingstype.NAV_NO_KORRIGERT_INNTEKTSMELDING,
                    Meldingstype.INNTEKTSMELDING -> HendelseDTO.inntektsmelding(
                        id = node.path("@id").asText(),
                        eksternDokumentId = node.path("inntektsmeldingId").asText(),
                        mottattDato = LocalDateTime.parse(node.path("@opprettet").asText()),
                        beregnetInntekt = node.path("beregnetInntekt").asDouble()
                    )

                    Meldingstype.SYKEPENGEGRUNNLAG_FOR_ARBEIDSGIVER -> HendelseDTO.inntektFraAOrdningen(
                        id = node.path("@id").asText(),
                        mottattDato = LocalDateTime.parse(node.path("@opprettet").asText())
                    )
                }
            }
        }.onEach {
            PostgresProbe.hendelseLestFraDb(meterRegistry)
        }
    }

    internal enum class Meldingstype {
        NY_SØKNAD,
        NY_SØKNAD_FRILANS,
        NY_SØKNAD_SELVSTENDIG,
        NY_SØKNAD_ARBEIDSLEDIG,
        SENDT_SØKNAD_NAV,
        SENDT_SØKNAD_FRILANS,
        SENDT_SØKNAD_SELVSTENDIG,
        SENDT_SØKNAD_ARBEIDSGIVER,
        SENDT_SØKNAD_ARBEIDSLEDIG,
        SENDT_SØKNAD_ARBEIDSLEDIG_TIDLIGERE_ARBEIDSTAKER,
        NAV_NO_SELVBESTEMT_INNTEKTSMELDING,
        NAV_NO_KORRIGERT_INNTEKTSMELDING,
        NAV_NO_INNTEKTSMELDING,
        INNTEKTSMELDING,
        SYKEPENGEGRUNNLAG_FOR_ARBEIDSGIVER;
    }
}
