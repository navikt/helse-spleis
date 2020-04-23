package no.nav.helse.spleis.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.spleis.PostgresProbe
import no.nav.helse.spleis.db.HendelseRecorder.Meldingstype.*
import no.nav.helse.spleis.meldinger.model.*
import org.slf4j.LoggerFactory
import javax.sql.DataSource

internal class HendelseRecorder(private val dataSource: DataSource) {
    private companion object {
        private val log = LoggerFactory.getLogger(HendelseRecorder::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    fun lagreMelding(melding: HendelseMessage) {
        val type = when (melding) {
            is NySøknadMessage -> NY_SØKNAD
            is SendtSøknadArbeidsgiverMessage -> SENDT_SØKNAD_ARBEIDSGIVER
            is SendtSøknadNavMessage -> SENDT_SØKNAD_NAV
            is InntektsmeldingMessage -> INNTEKTSMELDING
            is PåminnelseMessage -> PÅMINNELSE
            is YtelserMessage -> YTELSER
            is VilkårsgrunnlagMessage -> VILKÅRSGRUNNLAG
            is SimuleringMessage -> SIMULERING
            is ManuellSaksbehandlingMessage -> MANUELL_SAKSBEHANDLING
            is UtbetalingOverførtMessage -> UTBETALING_OVERFØRT
            is UtbetalingMessage -> UTBETALING
            is KansellerUtbetalingMessage -> KANSELLER_UTBETALING
            else -> return log.warn("ukjent meldingstype ${melding::class.simpleName}: melding lagres ikke")
        }

        lagreMelding(type, melding)
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
        YTELSER,
        VILKÅRSGRUNNLAG,
        MANUELL_SAKSBEHANDLING,
        UTBETALING_OVERFØRT,
        UTBETALING,
        SIMULERING,
        KANSELLER_UTBETALING
    }
}
