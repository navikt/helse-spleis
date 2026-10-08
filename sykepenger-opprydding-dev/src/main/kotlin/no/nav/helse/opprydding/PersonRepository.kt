package no.nav.helse.opprydding

import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import com.github.navikt.tbd_libs.sql_dsl.transaction
import org.intellij.lang.annotations.Language
import java.sql.Connection
import javax.sql.DataSource

internal class PersonRepository(
    private val dataSource: DataSource,
) {
    internal fun slett(fødselsnummer: String) {
        dataSource.connection {
            transaction {
                slettPersonAlias(fødselsnummer)
                slettPerson(fødselsnummer)
                slettMeldinger(fødselsnummer)
            }
        }
    }

    private fun Connection.slettPersonAlias(fødselsnummer: String) {
        @Language("PostgreSQL")
        val query = "DELETE FROM person_alias WHERE fnr = :fnr"
        prepareStatementWithNamedParameters(query) { withParameter("fnr", fødselsnummer.toLong()) }.use { it.execute() }
    }

    private fun Connection.slettPerson(fødselsnummer: String) {
        @Language("PostgreSQL")
        val query = "DELETE FROM person WHERE fnr = :fnr"
        prepareStatementWithNamedParameters(query) { withParameter("fnr", fødselsnummer.toLong()) }.use { it.execute() }
    }

    private fun Connection.slettMeldinger(fødselsnummer: String) {
        @Language("PostgreSQL")
        val query = "DELETE FROM melding WHERE fnr = :fnr"
        prepareStatementWithNamedParameters(query) { withParameter("fnr", fødselsnummer.toLong()) }.use { it.execute() }
    }
}
