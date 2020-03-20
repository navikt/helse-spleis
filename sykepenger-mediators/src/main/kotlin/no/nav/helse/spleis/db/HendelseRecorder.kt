package no.nav.helse.spleis.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.spleis.PostgresProbe
import no.nav.helse.spleis.hendelser.MessageProcessor
import no.nav.helse.spleis.hendelser.model.*
import java.util.*
import javax.sql.DataSource

internal class HendelseRecorder(private val dataSource: DataSource): MessageProcessor {

    override fun process(message: NySøknadMessage) {
        lagreMelding(Meldingstype.NY_SØKNAD, message)
    }

    override fun process(message: SendtSøknadMessage) {
        lagreMelding(Meldingstype.SENDT_SØKNAD, message)
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

    override fun process(message: ManuellSaksbehandlingMessage) {
        lagreMelding(Meldingstype.MANUELL_SAKSBEHANDLING, message)
    }

    override fun process(message: UtbetalingMessage) {
        lagreMelding(Meldingstype.UTBETALING, message)
    }

    fun hentHendelser(hendelseIder: Set<UUID>): List<Pair<Meldingstype, String>> {
        if (hendelseIder.isEmpty()) return emptyList()
        return using(sessionOf(dataSource)) { session ->
            session.run(queryOf(
                "SELECT * FROM melding WHERE melding_id in (${hendelseIder.joinToString(",") { "?" }})",
                *hendelseIder.map { it.toString() }.toTypedArray()
            ).map {
                Meldingstype.valueOf(it.string("melding_type")) to it.string("data")
            }.asList)
        }
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
}

internal enum class Meldingstype {
    NY_SØKNAD,
    SENDT_SØKNAD,
    INNTEKTSMELDING,
    PÅMINNELSE,
    YTELSER,
    VILKÅRSGRUNNLAG,
    MANUELL_SAKSBEHANDLING,
    UTBETALING
}
