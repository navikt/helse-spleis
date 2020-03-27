package no.nav.helse.spleis.dao

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import java.util.*
import javax.sql.DataSource

internal class HendelseDao(private val dataSource: DataSource) {
    fun hentHendelser(referanser: Set<UUID>): List<Pair<Meldingstype, String>> {
        if (referanser.isEmpty()) return emptyList()
        return using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT * FROM melding WHERE melding_id in (${referanser.joinToString(",") { "?" }})",
                    *referanser.map { it.toString() }.toTypedArray()
                ).map {
                    Meldingstype.valueOf(it.string("melding_type")) to it.string("data")
                }.asList)
        }.onEach {
            PostgresProbe.hendelseLestFraDb()
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
        SIMULERING
    }
}
