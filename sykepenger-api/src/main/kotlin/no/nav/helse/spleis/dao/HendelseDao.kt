package no.nav.helse.spleis.dao

import io.micrometer.core.instrument.MeterRegistry
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spleis.dto.HendelseDTO
import no.nav.helse.spleis.objectMapper
import org.intellij.lang.annotations.Language

internal class HendelseDao(private val dataSource: () -> DataSource, private val meterRegistry: MeterRegistry) {

    fun hentHendelse(meldingsReferanse: UUID): String? {
        return sessionOf(dataSource()).use { session ->
            session.run(
                queryOf(
                    "SELECT data FROM melding WHERE melding_id = ?",
                    meldingsReferanse.toString()
                ).map {
                    it.string("data")
                }.asSingle
            )
        }.also {
            PostgresProbe.hendelseLestFraDb(meterRegistry)
        }
    }

    fun hentHendelser(fødselsnummer: Long): List<HendelseDTO> {
        @Language("PostgreSQL")
        val statement = """
            SELECT melding_type, data FROM melding 
            WHERE fnr=? AND (melding_type = 'NY_SØKNAD' OR melding_type = 'SENDT_SØKNAD_NAV' OR melding_type = 'SENDT_SØKNAD_FRILANS'
                OR melding_type = 'SENDT_SØKNAD_SELVSTENDIG' OR melding_type = 'SENDT_SØKNAD_ARBEIDSGIVER' OR melding_type = 'SENDT_SØKNAD_ARBEIDSLEDIG' 
                OR melding_type = 'INNTEKTSMELDING' OR melding_type = 'SYKEPENGEGRUNNLAG_FOR_ARBEIDSGIVER')
        """
        return sessionOf(dataSource()).use { session ->
            session.run(queryOf(statement, fødselsnummer).map { row ->
                Meldingstype.valueOf(row.string("melding_type")) to row.string("data")
            }.asList)
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
        INNTEKTSMELDING,
        SYKEPENGEGRUNNLAG_FOR_ARBEIDSGIVER
    }
}
