package no.nav.helse.spleis.db

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Personidentifikator
import no.nav.helse.serde.migration.Json
import no.nav.helse.serde.migration.Navn
import no.nav.helse.spleis.PostgresProbe
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.GRUNNBELØPSREGULERING
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.INNTEKTSMELDING
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.KANSELLER_UTBETALING
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.NY_SØKNAD
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.OVERSTYRARBEIDSFORHOLD
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.OVERSTYRARBEIDSGIVEROPPLYSNINGER
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.OVERSTYRINNTEKT
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.OVERSTYRTIDSLINJE
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.SENDT_SØKNAD_ARBEIDSGIVER
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.SENDT_SØKNAD_NAV
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.SIMULERING
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.UTBETALING
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.UTBETALINGPÅMINNELSE
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.UTBETALINGSGODKJENNING
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.UTBETALINGSHISTORIKK_FOR_FERIEPENGER
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.VILKÅRSGRUNNLAG
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.YTELSER
import no.nav.helse.spleis.meldinger.model.AnnulleringMessage
import no.nav.helse.spleis.meldinger.model.AvstemmingMessage
import no.nav.helse.spleis.meldinger.model.EtterbetalingMessage
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import no.nav.helse.spleis.meldinger.model.InfotrygdendringMessage
import no.nav.helse.spleis.meldinger.model.InntektsmeldingMessage
import no.nav.helse.spleis.meldinger.model.InntektsmeldingReplayUtførtMessage
import no.nav.helse.spleis.meldinger.model.MigrateMessage
import no.nav.helse.spleis.meldinger.model.NySøknadMessage
import no.nav.helse.spleis.meldinger.model.OverstyrArbeidsforholdMessage
import no.nav.helse.spleis.meldinger.model.OverstyrArbeidsgiveropplysningerMessage
import no.nav.helse.spleis.meldinger.model.OverstyrTidslinjeMessage
import no.nav.helse.spleis.meldinger.model.PersonPåminnelseMessage
import no.nav.helse.spleis.meldinger.model.PåminnelseMessage
import no.nav.helse.spleis.meldinger.model.SendtSøknadArbeidsgiverMessage
import no.nav.helse.spleis.meldinger.model.SendtSøknadNavMessage
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.helse.spleis.meldinger.model.UtbetalingMessage
import no.nav.helse.spleis.meldinger.model.UtbetalingpåminnelseMessage
import no.nav.helse.spleis.meldinger.model.UtbetalingsgodkjenningMessage
import no.nav.helse.spleis.meldinger.model.UtbetalingshistorikkEtterInfotrygdendringMessage
import no.nav.helse.spleis.meldinger.model.UtbetalingshistorikkForFeriepengerMessage
import no.nav.helse.spleis.meldinger.model.UtbetalingshistorikkMessage
import no.nav.helse.spleis.meldinger.model.VilkårsgrunnlagMessage
import no.nav.helse.spleis.meldinger.model.YtelserMessage
import org.slf4j.LoggerFactory

internal class HendelseRepository(private val dataSource: DataSource) {

    private companion object {
        private val log = LoggerFactory.getLogger(HendelseRepository::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    fun lagreMelding(melding: HendelseMessage) {
        melding.lagreMelding(this)
    }

    internal fun finnInntektsmeldinger(fnr: Personidentifikator): List<JsonNode> =
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    "SELECT data FROM melding WHERE fnr = ? AND melding_type = 'INNTEKTSMELDING' ORDER BY lest_dato ASC",
                    fnr.toLong()
                )
                    .map { objectMapper.readTree(it.string("data")) }
                    .asList
            )
        }

    internal fun lagreMelding(melding: HendelseMessage, personidentifikator: Personidentifikator, meldingId: UUID, json: String) {
        val meldingtype = meldingstype(melding) ?: return
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    "INSERT INTO melding (fnr, melding_id, melding_type, data) VALUES (?, ?, ?, (to_json(?::json))) ON CONFLICT(melding_id) DO NOTHING",
                    personidentifikator.toLong(),
                    meldingId.toString(),
                    meldingtype.name,
                    json
                ).asExecute
            )
        }.also {
            PostgresProbe.hendelseSkrevetTilDb()
        }
    }

    fun markerSomBehandlet(meldingId: UUID) = sessionOf(dataSource).use { session ->
        session.run(queryOf("UPDATE melding SET behandlet_tidspunkt=now() WHERE melding_id = ? AND behandlet_tidspunkt IS NULL",
            meldingId.toString()
        ).asUpdate)
    }

    fun erBehandlet(meldingId: UUID) = sessionOf(dataSource).use { session ->
        session.run(
            queryOf("SELECT behandlet_tidspunkt FROM melding WHERE melding_id = ?", meldingId.toString())
                .map { it.localDateTimeOrNull("behandlet_tidspunkt") }.asSingle
        ) != null
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
        is UtbetalingMessage -> UTBETALING
        is AnnulleringMessage -> KANSELLER_UTBETALING
        is EtterbetalingMessage -> GRUNNBELØPSREGULERING
        is OverstyrTidslinjeMessage -> OVERSTYRTIDSLINJE
        is OverstyrArbeidsforholdMessage -> OVERSTYRARBEIDSFORHOLD
        is OverstyrArbeidsgiveropplysningerMessage -> OVERSTYRARBEIDSGIVEROPPLYSNINGER
        is UtbetalingshistorikkForFeriepengerMessage -> UTBETALINGSHISTORIKK_FOR_FERIEPENGER
        is MigrateMessage,
        is AvstemmingMessage,
        is PersonPåminnelseMessage,
        is PåminnelseMessage,
        is UtbetalingshistorikkMessage,
        is InfotrygdendringMessage,
        is UtbetalingshistorikkEtterInfotrygdendringMessage,
        is InntektsmeldingReplayUtførtMessage -> null // Disse trenger vi ikke å lagre
        else -> null.also { log.warn("ukjent meldingstype ${melding::class.simpleName}: melding lagres ikke") }
    }

    internal fun hentAlleHendelser(personidentifikator: Personidentifikator): Map<UUID, Pair<Navn, Json>> {
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    "SELECT melding_id, melding_type, data FROM melding WHERE fnr = ? AND melding_type IN (?, ?, ?, ?, ?, ?, ?)",
                    personidentifikator.toLong(), NY_SØKNAD.name, SENDT_SØKNAD_ARBEIDSGIVER.name, SENDT_SØKNAD_NAV.name, INNTEKTSMELDING.name, OVERSTYRTIDSLINJE.name,
                    OVERSTYRINNTEKT.name, VILKÅRSGRUNNLAG.name
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
        UTBETALING,
        SIMULERING,
        KANSELLER_UTBETALING,
        GRUNNBELØPSREGULERING,
        UTBETALINGSHISTORIKK_FOR_FERIEPENGER,
        OVERSTYRARBEIDSGIVEROPPLYSNINGER
    }
}
