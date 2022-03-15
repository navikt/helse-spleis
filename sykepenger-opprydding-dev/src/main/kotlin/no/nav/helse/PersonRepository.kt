package no.nav.helse

import javax.sql.DataSource
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf

internal class PersonRepository(private val dataSource: DataSource) {
    internal fun slett(fødselsnummer: String) {
        sessionOf(dataSource).transaction {
            it.slettPerson(fødselsnummer)
            it.slettMeldinger(fødselsnummer)
            it.slettUnikePerson(fødselsnummer)
        }
    }

    private fun TransactionalSession.slettPerson(fødselsnummer: String) {
        val query = "DELETE FROM person WHERE fnr = ?"
        run(queryOf(query, fødselsnummer.toLong()).asExecute)
    }

    private fun TransactionalSession.slettMeldinger(fødselsnummer: String) {
        val query = "DELETE FROM melding WHERE fnr = ?"
        run(queryOf(query, fødselsnummer.toLong()).asExecute)
    }

    private fun TransactionalSession.slettUnikePerson(fødselsnummer: String) {
        val query = "DELETE FROM unike_person WHERE fnr = ?"
        run(queryOf(query, fødselsnummer.toLong()).asExecute)
    }
}