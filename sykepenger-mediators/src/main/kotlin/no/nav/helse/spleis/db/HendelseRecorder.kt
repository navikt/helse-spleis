package no.nav.helse.spleis.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.spleis.PostgresProbe
import no.nav.helse.spleis.hendelser.MessageProcessor
import no.nav.helse.spleis.hendelser.model.*
import javax.sql.DataSource

internal class HendelseRecorder(private val dataSource: DataSource): MessageProcessor {

    override fun process(message: NySøknadMessage) {
        lagreMelding(Meldingstype.NY_SØKNAD, message)
    }

    override fun process(message: SendtSøknadArbeidsgiverMessage) {
        lagreMelding(Meldingstype.SENDT_SØKNAD_ARBEIDSGIVER, message)
    }

    override fun process(message: SendtSøknadNavMessage) {
        lagreMelding(Meldingstype.SENDT_SØKNAD_NAV, message)
    }

    override fun process(message: InntektsmeldingMessage) {
        lagreMelding(Meldingstype.INNTEKTSMELDING, message)
    }

    override fun process(message: PåminnelseMessage) {
        lagreMelding(Meldingstype.PÅMINNELSE, message)
    }

    override fun process(message: YtelserMessage) {
        lagreMelding(Meldingstype.YTELSER, message)
    }

    override fun process(message: VilkårsgrunnlagMessage) {
        lagreMelding(Meldingstype.VILKÅRSGRUNNLAG, message)
    }

    override fun process(message: SimuleringMessage) {
        lagreMelding(Meldingstype.SIMULERING, message)
    }

    override fun process(message: ManuellSaksbehandlingMessage) {
        lagreMelding(Meldingstype.MANUELL_SAKSBEHANDLING, message)
    }

    override fun process(message: UtbetalingMessage) {
        lagreMelding(Meldingstype.UTBETALING, message)
    }

    override fun process(message: KansellerUtbetalingMessage) {
        lagreMelding(Meldingstype.KANSELLER_UTBETALING, message)
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
        UTBETALING,
        SIMULERING,
        KANSELLER_UTBETALING
    }
}
