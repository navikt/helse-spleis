package no.nav.helse.spleis.db

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.spleis.PostgresProbe
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.*
import no.nav.helse.spleis.meldinger.model.*
import org.slf4j.LoggerFactory
import javax.sql.DataSource

internal class HendelseRepository(private val dataSource: DataSource) {

    private companion object {
        private val log = LoggerFactory.getLogger(HendelseRepository::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    fun lagreMelding(melding: HendelseMessage) {
        val type = when (melding) {
            is NySøknadMessage -> NY_SØKNAD
            is SendtSøknadArbeidsgiverMessage -> SENDT_SØKNAD_ARBEIDSGIVER
            is SendtSøknadNavMessage -> SENDT_SØKNAD_NAV
            is InntektsmeldingMessage -> INNTEKTSMELDING
            is UtbetalingpåminnelseMessage -> UTBETALINGPÅMINNELSE
            is YtelserMessage -> YTELSER
            is VilkårsgrunnlagMessage -> VILKÅRSGRUNNLAG
            is SimuleringMessage -> SIMULERING
            is UtbetalingsgodkjenningMessage -> UTBETALINGSGODKJENNING
            is UtbetalingOverførtMessage -> UTBETALING_OVERFØRT
            is UtbetalingMessage -> UTBETALING
            is AnnulleringMessage -> KANSELLER_UTBETALING
            is EtterbetalingMessage -> GRUNNBELØPSREGULERING
            is AvstemmingMessage,
            is OverstyrTidslinjeMessage,
            is PersonPåminnelseMessage,
            is PåminnelseMessage,
            is UtbetalingshistorikkMessage -> return // Disse trenger vi ikke å lagre
            else -> return log.warn("ukjent meldingstype ${melding::class.simpleName}: melding lagres ikke")
        }

        lagreMelding(type, melding)
    }

    internal fun finnInntektsmeldinger(fnr: String): List<JsonNode> =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT data FROM melding WHERE fnr = ? AND melding_type = 'INNTEKTSMELDING' ORDER BY lest_dato ASC",
                    fnr.toLong()
                )
                    .map { objectMapper.readTree(it.string("data")) }
                    .asList
            )
        }

    private fun lagreMelding(meldingstype: Meldingstype, melding: HendelseMessage) {
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO melding (fnr, melding_id, melding_type, data) VALUES (?, ?, ?, (to_json(?::json))) ON CONFLICT(melding_id) DO NOTHING",
                    melding.fødselsnummer.toLong(),
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
        PERSONPÅMINNELSE,
        UTBETALINGPÅMINNELSE,
        YTELSER,
        VILKÅRSGRUNNLAG,
        UTBETALINGSGODKJENNING,
        UTBETALING_OVERFØRT,
        UTBETALING,
        SIMULERING,
        KANSELLER_UTBETALING,
        GRUNNBELØPSREGULERING,
    }
}
