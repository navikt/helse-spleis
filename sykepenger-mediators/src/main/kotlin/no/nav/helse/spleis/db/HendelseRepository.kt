package no.nav.helse.spleis.db

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.Fødselsnummer
import no.nav.helse.serde.migration.Json
import no.nav.helse.serde.migration.Navn
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

    fun lagreMelding(melding: HendelseMessage) {
        if (!skalLagres(melding)) return
        melding.lagreMelding(this)
    }

    internal fun finnInntektsmeldinger(fnr: Fødselsnummer): List<JsonNode> =
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

    internal fun finnSøknader(fnr: Fødselsnummer): List<JsonNode> =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT data FROM melding WHERE fnr = ? AND melding_type = 'SENDT_SØKNAD_NAV' ORDER BY lest_dato ASC",
                    fnr.toLong()
                )
                    .map { objectMapper.readTree(it.string("data")) }
                    .asList
            )
        }

    internal fun lagreMelding(melding: HendelseMessage, fødselsnummer: Fødselsnummer, meldingId: UUID, json: String) {
        val meldingtype = meldingstype(melding) ?: return
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO melding (fnr, melding_id, melding_type, data) VALUES (?, ?, ?, (to_json(?::json))) ON CONFLICT(melding_id) DO NOTHING",
                    fødselsnummer.toLong(),
                    meldingId.toString(),
                    meldingtype.name,
                    json
                ).asExecute
            )
        }.also {
            PostgresProbe.hendelseSkrevetTilDb()
        }
    }

    fun markerSomBehandlet(meldingId: UUID) = using(sessionOf(dataSource)) { session ->
        session.run(queryOf("UPDATE melding SET behandlet_tidspunkt=now() WHERE melding_id = ? AND behandlet_tidspunkt IS NULL",
            meldingId.toString()
        ).asUpdate)
    }

    fun erBehandlet(meldingId: UUID) = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf("SELECT behandlet_tidspunkt FROM melding WHERE melding_id = ?", meldingId.toString())
                .map { it.localDateTimeOrNull("behandlet_tidspunkt") }.asSingle
        ) != null
    }

    private fun skalLagres(melding: HendelseMessage): Boolean {
        return meldingstype(melding) != null
    }

    private fun meldingstype(melding: HendelseMessage) = when (melding) {
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
        is OverstyrTidslinjeMessage -> OVERSTYRTIDSLINJE
        is OverstyrInntektMessage -> OVERSTYRINNTEKT
        is OverstyrArbeidsforholdMessage -> OVERSTYRARBEIDSFORHOLD
        is UtbetalingsgrunnlagMessage -> UTBETALINGSGRUNNLAG
        is UtbetalingshistorikkForFeriepengerMessage -> null //TODO: Skal lagres som UTBETALINGSHISTORIKK_FOR_FERIEPENGER
        is AvstemmingMessage,
        is PersonPåminnelseMessage,
        is PåminnelseMessage,
        is UtbetalingshistorikkMessage -> null // Disse trenger vi ikke å lagre
        else -> null.also { log.warn("ukjent meldingstype ${melding::class.simpleName}: melding lagres ikke") }
    }

    internal fun hentAlleHendelser(fødselsnummer: Fødselsnummer): Map<UUID, Pair<Navn, Json>> {
        return using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT melding_id, melding_type, data FROM melding WHERE fnr = ? AND melding_type IN (?, ?, ?, ?, ?, ?)",
                    fødselsnummer.toLong(), NY_SØKNAD.name, SENDT_SØKNAD_ARBEIDSGIVER.name, SENDT_SØKNAD_NAV.name, INNTEKTSMELDING.name, OVERSTYRTIDSLINJE.name, OVERSTYRINNTEKT.name
                ).map {
                    UUID.fromString(it.string("melding_id")) to Pair<Navn, Json>(
                        it.string("melding_type"),
                        it.string("data")
                    )
                }.asList).toMap()
        }
    }

    private enum class Meldingstype {
        NY_SØKNAD,
        SENDT_SØKNAD_ARBEIDSGIVER,
        SENDT_SØKNAD_NAV,
        INNTEKTSMELDING,
        PÅMINNELSE,
        PERSONPÅMINNELSE,
        OVERSTYRTIDSLINJE,
        OVERSTYRINNTEKT,
        OVERSTYRARBEIDSFORHOLD,
        UTBETALINGPÅMINNELSE,
        YTELSER,
        UTBETALINGSGRUNNLAG,
        VILKÅRSGRUNNLAG,
        UTBETALINGSGODKJENNING,
        UTBETALING_OVERFØRT,
        UTBETALING,
        SIMULERING,
        KANSELLER_UTBETALING,
        GRUNNBELØPSREGULERING,
        UTBETALINGSHISTORIKK_FOR_FERIEPENGER
    }
}
