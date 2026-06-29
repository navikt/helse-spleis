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

internal class PostgresUtboksDaoTest {

    private lateinit var dataSource: TestDataSource
    private lateinit var dao: PostgresUtboksDao

    @BeforeEach
    fun setup() {
        dataSource = databaseContainer.nyTilkobling()
        dao = PostgresUtboksDao(dataSource.ds)
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
        assertEquals(meldinger, usendte(personidentifikator))
        assertEquals(listOf(meldingUtenKey), håndterOgFåTilbakeUsendte(personidentifikator) { meldingerTilSykmeldt })
        assertEquals(emptySet<Personidentifikator>(), dao.personerMedUsendteMeldinger())
        assertEquals(emptyList<UtgåendeMelding>(), håndterOgFåTilbakeUsendte(personidentifikator) { listOf(meldingUtenKey) })
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
        assertEquals(listOf(melding1), usendte(personidentifikator1))
        assertEquals(listOf(melding2), usendte(personidentifikator2))
        assertEquals(listOf(melding3), usendte(personidentifikator3))

        håndterOgFåTilbakeUsendte(personidentifikator2) { listOf(melding2) }
        assertEquals(setOf(personidentifikator1, personidentifikator3), dao.personerMedUsendteMeldinger())

        håndterOgFåTilbakeUsendte(personidentifikator1) { listOf(melding1) }
        håndterOgFåTilbakeUsendte(personidentifikator3) { listOf(melding3) }
        assertEquals(emptySet<Personidentifikator>(), dao.personerMedUsendteMeldinger())
    }


    private fun lagre(meldinger: List<UtgåendeMelding>, forårsaketAv: UUID = UUID.randomUUID()) {
        dataSource.ds.connection {
            transaction {
                dao.lagre(this, meldinger, forårsaketAv)
            }
        }
    }

    private fun håndterOgFåTilbakeUsendte(
        person: Personidentifikator = personidentifikator,
        sendOgFåTilbakeSendtOk: (meldinger: List<UtgåendeMelding>) -> List<UtgåendeMelding> = { meldinger -> meldinger }
    ): List<UtgåendeMelding> {
        dao.usendte(person) { funnedeMeldinger ->
            val ok = sendOgFåTilbakeSendtOk(funnedeMeldinger)
            ok.somKvittering()
        }
        return usendte(person)
    }

    private fun usendte(
        person: Personidentifikator = personidentifikator,
    ): List<UtgåendeMelding> {
        lateinit var meldingerEtter: List<UtgåendeMelding>
        dao.usendte(person, { funnedeMeldinger ->
            meldingerEtter = funnedeMeldinger
            ikkeKvitterUtNoenMeldinger
        })
        return meldingerEtter
    }

    private companion object {
        private val personidentifikator = Personidentifikator("12345678910")
        private fun nyMelding(key: Personidentifikator? = personidentifikator, mottaker: UtgåendeMelding.Mottaker = UtgåendeMelding.Mottaker.RAPID) = UtgåendeMelding(key?.toString(), """{"@id": "${nyUuidv7()}", "@even_name": "test"}""", mottaker)
        private fun List<UtgåendeMelding>.somKvittering(sendingsTidspunkt: Instant = Instant.now()) = Kvittering(
            sendt = sendingsTidspunkt,
            ok = this,
            feilet = emptyList()
        )
        private val ikkeKvitterUtNoenMeldinger get() = emptyList<UtgåendeMelding>().somKvittering()
    }
}
