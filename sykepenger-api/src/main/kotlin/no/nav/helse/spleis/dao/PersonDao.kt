package no.nav.helse.spleis.dao

import com.github.navikt.tbd_libs.sql_dsl.*
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.serde.SerialisertPerson
import org.intellij.lang.annotations.Language
import javax.sql.DataSource

internal class PersonDao(
    private val dataSource: () -> DataSource,
    private val meterRegistry: MeterRegistry,
) {
    fun hentPersonFraFnr(fødselsnummer: Long): SerialisertPerson? {
        @Language("PostgreSQL")
        val sql = "SELECT skjema_versjon, data FROM person WHERE id = (SELECT person_id FROM person_alias WHERE fnr=:fnr);"
        return dataSource().connection {
            this
                .prepareStatementWithNamedParameters(sql) {
                    withParameter("fnr", fødselsnummer)
                }.use {
                    it.executeQuery().use { rs ->
                        rs.mapNotNull { rs ->
                            SerialisertPerson(
                                json = rs.string("data"),
                                skjemaVersjon = rs.int("skjema_versjon"),
                            )
                        }
                    }
                }.singleOrNullOrThrow()
                ?.also { PostgresProbe.personLestFraDb(meterRegistry) }
        }
    }

    private fun <R> Collection<R>.singleOrNullOrThrow() =
        if (size < 2) {
            this.firstOrNull()
        } else {
            error("Listen inneholder mer enn ett element!")
        }
}
