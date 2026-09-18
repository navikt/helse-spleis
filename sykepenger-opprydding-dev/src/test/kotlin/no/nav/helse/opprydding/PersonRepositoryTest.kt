package no.nav.helse.opprydding

import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.int
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import com.github.navikt.tbd_libs.sql_dsl.single
import com.github.navikt.tbd_libs.sql_dsl.transaction
import java.util.*
import no.nav.helse.nyttFødselsnummer
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PersonRepositoryTest : DBTest() {

    private lateinit var personRepository: PersonRepository
    private val fødselsnummer = nyttFødselsnummer()

    @BeforeEach
    fun `start postgres`() {
        personRepository = PersonRepository(dataSource.ds)
    }

    @Test
    fun `Kan slette person`() {
        opprettDummyPerson(fødselsnummer)
        assertEquals(1, finnPerson(fødselsnummer))
        assertEquals(1, finnMelding(fødselsnummer))
        personRepository.slett(fødselsnummer)
        assertEquals(0, finnPerson(fødselsnummer))
        assertEquals(0, finnMelding(fødselsnummer))
    }

    private fun finnPerson(fødselsnummer: String): Int {
        return dataSource.ds.connection {
            prepareStatementWithNamedParameters("SELECT COUNT(1) FROM person WHERE fnr = :fnr") {
                withParameter("fnr", fødselsnummer.toLong())
            }.use {
                it.executeQuery().use { rs ->
                    rs.single { it.int(1) }
                }
            }
        }
    }

    private fun finnMelding(fødselsnummer: String): Int {
        return dataSource.ds.connection {
            prepareStatementWithNamedParameters("SELECT COUNT(1) FROM melding WHERE fnr = :fnr") {
                withParameter("fnr", fødselsnummer.toLong())
            }.use {
                it.executeQuery().use { rs ->
                    rs.single { it.int(1) }
                }
            }
        }
    }

    private fun opprettDummyPerson(fødselsnummer: String) {
        dataSource.ds.connection {
            transaction {
                @Language("PostgreSQL")
                val opprettMelding = "INSERT INTO melding(fnr, melding_id, melding_type, data, behandlet_tidspunkt) VALUES(:fnr, :meldingId, :meldingType, cast(:data as json), now())"
                prepareStatementWithNamedParameters(opprettMelding) {
                    withParameter("fnr", fødselsnummer.toLong())
                    withParameter("meldingId", UUID.randomUUID())
                    withParameter("meldingType", "melding")
                    withParameter("data", "{}")
                }.use { it.execute() }
                @Language("PostgreSQL")
                val opprettPerson = "INSERT INTO person(skjema_versjon, fnr, data) VALUES(0, :fnr, '{}')"
                prepareStatementWithNamedParameters(opprettPerson) {
                    withParameter("fnr", fødselsnummer.toLong())
                }.use {
                    it.execute()
                }
            }

        }
    }
}
