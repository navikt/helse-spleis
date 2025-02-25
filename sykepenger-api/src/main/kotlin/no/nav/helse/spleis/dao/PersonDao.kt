package no.nav.helse.spleis.dao

import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.mapNotNull
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import com.github.navikt.tbd_libs.sql_dsl.string
import io.micrometer.core.instrument.MeterRegistry
import javax.sql.DataSource
import no.nav.helse.serde.SerialisertPerson
import org.intellij.lang.annotations.Language

internal class PersonDao(private val dataSource: () -> DataSource, private val meterRegistry: MeterRegistry) {
    fun hentPersonFraFnr(fødselsnummer: Long): SerialisertPerson? {
        @Language("PostgreSQL")
        val sql = "SELECT data FROM person WHERE id = (SELECT person_id FROM person_alias WHERE fnr=:fnr);"
        return dataSource().connection {
            this
                .prepareStatementWithNamedParameters(sql) {
                    withParameter("fnr", fødselsnummer)
                }
                .use {
                    it.executeQuery().use { rs ->
                        rs.mapNotNull { SerialisertPerson(it.string("data"))}
                    }
                }
                .singleOrNullOrThrow()
                ?.also { PostgresProbe.personLestFraDb(meterRegistry) }
        }
    }

    private fun <R> Collection<R>.singleOrNullOrThrow() =
        if (size < 2) this.firstOrNull()
        else error("Listen inneholder mer enn ett element!")

}
