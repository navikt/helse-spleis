package no.nav.helse.spleis.db

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.spleis.JsonNodeDelegate
import no.nav.helse.spleis.PostgresProbe
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.*
import no.nav.helse.spleis.meldinger.model.*
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource

internal class HendelseRepository(private val dataSource: DataSource) {

    private companion object {
        private val log = LoggerFactory.getLogger(HendelseRepository::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    fun gjennopprettMelding(hendelseId: UUID): HendelseMessage? = finnMelding(hendelseId)?.let {(_, type, data) ->
        when (type) {
            NY_SØKNAD -> NySøknadMessage(JsonNodeDelegate(data))
            SENDT_SØKNAD_NAV -> SendtSøknadNavMessage(JsonNodeDelegate(data))
            INNTEKTSMELDING -> InntektsmeldingMessage(JsonNodeDelegate(data))
            SENDT_SØKNAD_ARBEIDSGIVER -> SendtSøknadArbeidsgiverMessage(JsonNodeDelegate(data))
            else -> null
        }
    }

    fun lagreMelding(melding: HendelseMessage) {
        val type = when (melding) {
            is NySøknadMessage -> NY_SØKNAD
            is SendtSøknadArbeidsgiverMessage -> SENDT_SØKNAD_ARBEIDSGIVER
            is SendtSøknadNavMessage -> SENDT_SØKNAD_NAV
            is InntektsmeldingMessage -> INNTEKTSMELDING
            is PåminnelseMessage -> PÅMINNELSE
            is UtbetalingpåminnelseMessage -> UTBETALINGPÅMINNELSE
            is YtelserMessage -> YTELSER
            is VilkårsgrunnlagMessage -> VILKÅRSGRUNNLAG
            is SimuleringMessage -> SIMULERING
            is UtbetalingsgodkjenningMessage -> UTBETALINGSGODKJENNING
            is UtbetalingOverførtMessage -> UTBETALING_OVERFØRT
            is UtbetalingMessage -> UTBETALING
            is AnnulleringMessage -> KANSELLER_UTBETALING
            is UtbetalingshistorikkMessage -> return // ignore UtbetalingshistorikkMessage
            is RollbackMessage -> ROLLBACK
            is EtterbetalingMessage -> GRUNNBELØPSREGULERING
            else -> return log.warn("ukjent meldingstype ${melding::class.simpleName}: melding lagres ikke")
        }

        lagreMelding(type, melding)
    }

    private fun finnMelding(meldingId: UUID): Triple<UUID, Meldingstype, JsonNode>? =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT melding_type, data FROM melding WHERE melding_id = ? ORDER BY lest_dato ASC",
                    meldingId.toString()
                )
                    .map {
                        Triple(
                            meldingId,
                            valueOf(it.string("melding_type")),
                            objectMapper.readTree(it.string("data"))
                        )
                    }.asSingle
            )

        }

    private fun lagreMelding(meldingstype: Meldingstype, melding: HendelseMessage) {
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO melding (fnr, melding_id, melding_type, data) VALUES (?, ?, ?, (to_json(?::json))) ON CONFLICT(melding_id) DO NOTHING",
                    melding.fødselsnummer,
                    melding.id.toString(),
                    meldingstype.name,
                    melding.toJson()
                ).asExecute
            )
        }.also {
            PostgresProbe.hendelseSkrevetTilDb()
        }
    }

    private enum class Meldingstype {
        NY_SØKNAD,
        SENDT_SØKNAD_ARBEIDSGIVER,
        SENDT_SØKNAD_NAV,
        INNTEKTSMELDING,
        PÅMINNELSE,
        UTBETALINGPÅMINNELSE,
        YTELSER,
        VILKÅRSGRUNNLAG,
        UTBETALINGSGODKJENNING,
        UTBETALING_OVERFØRT,
        UTBETALING,
        SIMULERING,
        KANSELLER_UTBETALING,
        ROLLBACK,
        GRUNNBELØPSREGULERING,
    }
}
