package no.nav.helse.spleis.utboks

import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.transaction
import com.github.navikt.tbd_libs.test_support.TestDataSource
import java.time.Instant
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.spleis.mediator.databaseContainer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import no.nav.helse.spleis.utboks.UtgåendeMeldingTest.Companion.nyUuidv7
import org.junit.jupiter.api.Assertions.assertEquals

internal class UtboksDaoTest {

    private lateinit var dataSource: TestDataSource
    private lateinit var dao: UtboksDao

    @BeforeEach
    fun setup() {
        dataSource = databaseContainer.nyTilkobling()
        dao = UtboksDao(dataSource.ds)
    }

    @AfterEach
    fun tearDown() {
        databaseContainer.droppTilkobling(dataSource)
    }

    @Test
    fun `lagrer, sender og henter opp meldinger`() {
        val meldingerTilSykmeldt = listOf(nyMelding(), nyMelding(), nyMelding())
        val meldingUtenKey =  nyMelding(key = null)
        val meldinger = meldingerTilSykmeldt + meldingUtenKey
        lagre(meldinger)
        assertEquals(meldinger, dao.usendte(personidentifikator))
        sendt(meldingerTilSykmeldt)
        assertEquals(listOf(meldingUtenKey), dao.usendte(personidentifikator))
        assertEquals(emptySet<Personidentifikator>(), dao.personerMedUsendteMeldinger())
        sendt(listOf(meldingUtenKey))
        assertEquals(emptyList<UtgåendeMelding>(), dao.usendte(personidentifikator))

    }

    @Test
    fun `henter personer med usendte meldinger`() {
        assertEquals(emptySet<Personidentifikator>(), dao.personerMedUsendteMeldinger())
        val personidentifikator1 = Personidentifikator("12345678911")
        val personidentifikator2 = Personidentifikator("12345678912")
        val personidentifikator3 = Personidentifikator("12345678913")
        val melding1 = nyMelding(personidentifikator1)
        val melding2 = nyMelding(personidentifikator2)
        val melding3 = nyMelding(personidentifikator3)
        lagre(listOf(melding1, melding2, melding3))
        assertEquals(setOf(personidentifikator1, personidentifikator2, personidentifikator3), dao.personerMedUsendteMeldinger())
        assertEquals(listOf(melding1), dao.usendte(personidentifikator1))
        assertEquals(listOf(melding2), dao.usendte(personidentifikator2))
        assertEquals(listOf(melding3), dao.usendte(personidentifikator3))
        sendt(listOf(melding2))
        assertEquals(setOf(personidentifikator1, personidentifikator3), dao.personerMedUsendteMeldinger())
        sendt(listOf(melding1, melding3))
        assertEquals(emptySet<Personidentifikator>(), dao.personerMedUsendteMeldinger())
    }


    private fun lagre(meldinger: List<UtgåendeMelding>, forårsaketAv: UUID = UUID.randomUUID()) {
        dataSource.ds.connection {
            transaction {
                dao.lagre(this, meldinger, forårsaketAv)
            }
        }
    }

    private fun sendt(meldinger: List<UtgåendeMelding>) {
        dao.sendt(Kvittering(
            sendt = Instant.now(),
            ok = meldinger,
            feilet = emptyList()
        ))
    }

    private companion object {
        private val personidentifikator = Personidentifikator("12345678910")
        private fun nyMelding(key: Personidentifikator? = personidentifikator, mottaker: UtgåendeMelding.Mottaker = UtgåendeMelding.Mottaker.RAPID) = UtgåendeMelding(key?.toString(), """{"@id": "${nyUuidv7()}", "@even_name": "test"}""", mottaker)
    }
}
