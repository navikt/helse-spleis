package no.nav.helse.spleis.dao

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import java.util.*
import javax.sql.DataSource

internal class HendelseDao(private val dataSource: DataSource) {

    fun hentHendelse(meldingsReferanse: UUID): String? {
        return using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT data FROM melding WHERE melding_id = ?",
                    meldingsReferanse.toString()
                ).map {
                    it.string("data")
                }.asSingle
            )
        }.also {
            PostgresProbe.hendelseLestFraDb()
        }
    }

    fun hentHendelser(referanser: Set<UUID>): List<Pair<Meldingstype, String>> {
        if (referanser.isEmpty()) return emptyList()
        return using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT * FROM melding WHERE " +
                        "melding_type IN (${Meldingstype.values().joinToString { "?" }}) " +
                        "AND melding_id IN (${referanser.joinToString { "?" }})",
                    *Meldingstype.values().map(Enum<*>::name).toTypedArray(),
                    *referanser.map { it.toString() }.toTypedArray()
                ).map {
                    Meldingstype.valueOf(it.string("melding_type")) to it.string("data")
                }.asList
            )
        }.onEach {
            PostgresProbe.hendelseLestFraDb()
        }
    }

    fun hentAlleHendelser(fødselsnummer: Long): Map<UUID, String> {
        return using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT melding_id,data FROM melding WHERE fnr = ? AND melding_type IN (${Meldingstype.values().joinToString { "?" }})",
                    fødselsnummer, *Meldingstype.values().map(Enum<*>::name).toTypedArray()
                ).map {
                    UUID.fromString(it.string("melding_id")) to it.string("data")
                }.asList
            ).toMap()
        }
    }

    internal enum class Meldingstype {
        NY_SØKNAD,
        SENDT_SØKNAD_NAV,
        SENDT_SØKNAD_ARBEIDSGIVER,
        INNTEKTSMELDING
    }
}
