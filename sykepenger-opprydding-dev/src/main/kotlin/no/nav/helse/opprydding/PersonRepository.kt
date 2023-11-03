package no.nav.helse.opprydding

import javax.sql.DataSource
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf

internal class PersonRepository(private val dataSource: DataSource) {
    internal fun slett(fødselsnummer: String) {
        sessionOf(dataSource).use { session ->
            session.transaction {
                it.slettPersonAlias(fødselsnummer)
                it.slettPerson(fødselsnummer)
                it.slettMeldinger(fødselsnummer)
            }
        }
    }

    private fun TransactionalSession.slettPersonAlias(fødselsnummer: String) {
        val query = "DELETE FROM person_alias WHERE fnr = ?"
        run(queryOf(query, fødselsnummer.toLong()).asExecute)
    }

    private fun TransactionalSession.slettPerson(fødselsnummer: String) {
        val query = "DELETE FROM person WHERE fnr = ?"
        run(queryOf(query, fødselsnummer.toLong()).asExecute)
    }

    private fun TransactionalSession.slettMeldinger(fødselsnummer: String) {
        val query = "DELETE FROM melding WHERE fnr = ?"
        run(queryOf(query, fødselsnummer.toLong()).asExecute)
    }
}