package no.nav.helse.spleis

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.sak.Sak
import javax.sql.DataSource

class SakPostgresRepository(private val dataSource: DataSource,
                            private val probe: PostgresProbe = PostgresProbe) : SakRepository {

    override fun hentSak(aktørId: String): Sak? {
        return using(sessionOf(dataSource)) { session ->
            session.run(queryOf("SELECT data FROM person WHERE aktor_id = ? ORDER BY id DESC LIMIT 1", aktørId).map {
                it.string("data")
            }.asSingle)
        }?.let {
            Sak.restore(Sak.Memento.fromString(it))
        }?.also {
            probe.sakLestFraDb()
        }
    }

}
