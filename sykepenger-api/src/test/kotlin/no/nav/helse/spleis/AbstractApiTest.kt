package no.nav.helse.spleis

import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.long
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import com.github.navikt.tbd_libs.sql_dsl.single
import com.github.navikt.tbd_libs.sql_dsl.transaction
import com.github.navikt.tbd_libs.test_support.TestDataSource
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.person.Person
import no.nav.helse.serde.tilPersonData
import no.nav.helse.serde.tilSerialisertPerson
import no.nav.helse.spleis.Applikasjonsservere.BlackboxTestContext
import no.nav.helse.spleis.dao.HendelseDao
import org.intellij.lang.annotations.Language

abstract class AbstractApiTest {

    internal fun blackboxTestApplication(
        testdata: (TestDataSource) -> Unit,
        testblokk: suspend BlackboxTestContext.() -> Unit) {
        val appservere = Applikasjonsservere()
        appservere.kjørTest(testdata, testblokk)
        appservere.ryddOpp()
    }


    protected fun DataSource.lagrePerson(fødselsnummer: String, person: Person) {
        val serialisertPerson = person.dto().tilPersonData().tilSerialisertPerson()
        connection {
            transaction {
                @Language("PostgreSQL")
                val opprettPerson = "INSERT INTO person(skjema_versjon, fnr, data) VALUES(:skjemaVersjon, :fnr, :data) RETURNING id"
                val personId = prepareStatementWithNamedParameters(opprettPerson) {
                    withParameter("fnr", fødselsnummer.toLong())
                    withParameter("skjemaVersjon", serialisertPerson.skjemaVersjon)
                    withParameter("data", serialisertPerson.json)
                }.use {
                    it.executeQuery().use { rs ->
                        rs.single { it.long(1) }
                    }
                }

                @Language("PostgreSQL")
                val opprettPersonAlias = "INSERT INTO person_alias (fnr, person_id) VALUES (:fnr, :personId)"
                prepareStatementWithNamedParameters(opprettPersonAlias) {
                    withParameter("fnr", fødselsnummer.toLong())
                    withParameter("personId", personId)
                }.use {
                    it.execute()
                }
            }

        }
    }

    internal fun DataSource.lagreHendelse(
        meldingsReferanse: UUID,
        meldingstype: HendelseDao.Meldingstype = HendelseDao.Meldingstype.INNTEKTSMELDING,
        fødselsnummer: String,
        data: String = """{ "@opprettet": "${LocalDateTime.now()}" }"""
    ) {
        @Language("PostgreSQL")
        val sql = "INSERT INTO melding (fnr, melding_id, melding_type, data) VALUES (:fnr, :meldingId, :meldingType, cast(:data as json))"
        connection {
            prepareStatementWithNamedParameters(sql) {
                withParameter("fnr", fødselsnummer.toLong())
                withParameter("meldingId", meldingsReferanse)
                withParameter("meldingType", meldingstype.name)
                withParameter("data", data)
            }.use { it.execute() }
        }
    }



}
