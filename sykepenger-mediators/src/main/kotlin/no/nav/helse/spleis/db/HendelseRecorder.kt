package no.nav.helse.spleis.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.spleis.PostgresProbe
import no.nav.helse.spleis.hendelser.MessageProcessor
import no.nav.helse.spleis.hendelser.model.*
import java.util.*
import javax.sql.DataSource

internal class HendelseRecorder(
    private val dataSource: DataSource,
    private val probe: PostgresProbe = PostgresProbe
): MessageProcessor {

    override fun process(message: NySøknadMessage) {
        lagreMelding(Meldingstype.NY_SØKNAD, message.id, message.toJson())
    }

    override fun process(message: SendtSøknadMessage) {
        lagreMelding(Meldingstype.SENDT_SØKNAD, message.id, message.toJson())
    }

    override fun process(message: InntektsmeldingMessage) {
        lagreMelding(Meldingstype.INNTEKTSMELDING, message.id, message.toJson())
    }

    override fun process(message: PåminnelseMessage) {
        lagreMelding(Meldingstype.PÅMINNELSE, message.id, message.toJson())
    }

    override fun process(message: YtelserMessage) {
        lagreMelding(Meldingstype.YTELSER, message.id, message.toJson())
    }

    override fun process(message: VilkårsgrunnlagMessage) {
        lagreMelding(Meldingstype.VILKÅRSGRUNNLAG, message.id, message.toJson())
    }

    override fun process(message: ManuellSaksbehandlingMessage) {
        lagreMelding(Meldingstype.MANUELL_SAKSBEHANDLING, message.id, message.toJson())
    }

    override fun process(message: UtbetalingMessage) {
        lagreMelding(Meldingstype.UTBETALING, message.id, message.toJson())
    }

    fun hentHendelser(hendelseIder: Set<UUID>) =
        using(sessionOf(dataSource)) { session ->
            session.run(queryOf(
                "SELECT * FROM melding WHERE melding_id in (${hendelseIder.joinToString(",") { "?" }})",
                *hendelseIder.map { it.toString() }.toTypedArray()
            ).map {
                Meldingstype.valueOf(it.string("melding_type")) to it.string("data")
            }.asList)
        }

    fun lagreMelding(meldingstype: Meldingstype, meldingsId: UUID, orginalmelding: String) {
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO melding (melding_id, melding_type, data) VALUES (?, ?, (to_json(?::json)))",
                    meldingsId.toString(),
                    meldingstype.name,
                    orginalmelding
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
    UTBETALING,
    UKJENT
}
