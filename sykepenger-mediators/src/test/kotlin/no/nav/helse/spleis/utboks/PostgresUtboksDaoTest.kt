package no.nav.helse.spleis.utboks

import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.mapNotNull
import com.github.navikt.tbd_libs.sql_dsl.transaction
import com.github.navikt.tbd_libs.test_support.TestDataSource
import java.time.Instant
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.spleis.mediator.databaseContainer
import no.nav.helse.spleis.utboks.PostgresUtboksDao.Companion.somUtgåendeMelding
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
        assertEquals(listOf(meldingUtenKey), håndterOgFåTilbakeUsendte(personidentifikator, sendOgFåTilbakeSendtOk = { meldingerTilSykmeldt }))
        assertEquals(emptySet<Personidentifikator>(), dao.personerMedUsendteMeldinger())
        assertEquals(emptyList<UtgåendeMelding>(), håndterOgFåTilbakeUsendte(personidentifikator, sendOgFåTilbakeSendtOk = { listOf(meldingUtenKey) }))
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

        håndterOgFåTilbakeUsendte(personidentifikator2, sendOgFåTilbakeSendtOk = { listOf(melding2 )})
        assertEquals(setOf(personidentifikator1, personidentifikator3), dao.personerMedUsendteMeldinger())

        håndterOgFåTilbakeUsendte(personidentifikator1, sendOgFåTilbakeSendtOk = { listOf(melding1) })
        håndterOgFåTilbakeUsendte(personidentifikator3, sendOgFåTilbakeSendtOk = { listOf(melding3) })
        assertEquals(emptySet<Personidentifikator>(), dao.personerMedUsendteMeldinger())
    }


    private fun lagre(meldinger: List<UtgåendeMelding>, forårsaketAv: UUID = UUID.randomUUID()) {
        dataSource.ds.connection {
            transaction {
                dao.lagre(this, meldinger.map { Utboksmelding.BeholdEtterSending(it) }, forårsaketAv)
            }
        }
    }

    private fun håndterOgFåTilbakeUsendte(
        person: Personidentifikator = personidentifikator,
        sendOgFåTilbakeSendtOk: (meldinger: List<UtgåendeMelding>) -> List<UtgåendeMelding> = { meldinger -> meldinger }, // Default går sending av alle meldinger bra
        skalVæreLagretISendt: (sendtOk: List<UtgåendeMelding>) -> List<UtgåendeMelding> = { sendtOk -> sendtOk } // Default skal alle meldinger lagres i send-tabellen
    ): List<UtgåendeMelding> {
        lateinit var sendtOk: List<UtgåendeMelding>
        lateinit var sendingFeilet: List<UtgåendeMelding>
        dao.usendte(person) { funnedeMeldinger ->
            sendtOk = sendOgFåTilbakeSendtOk(funnedeMeldinger)
            sendingFeilet = funnedeMeldinger - sendtOk.toSet()
            sendtOk.somKvittering(feilet = sendingFeilet)
        }
        val skalVæreLagretISendt = skalVæreLagretISendt(sendtOk)
        sjekkAtErLagretISendt(skalVæreLagretISendt)
        sjekkAtIkkeErLagretISendt(sendtOk - skalVæreLagretISendt.toSet())

        sjekkAtErLagretIUtboks(sendingFeilet)
        sjekkAtIkkeErLagretIUtboks(sendtOk)
        return usendte(person)
    }

    private fun usendte(
        person: Personidentifikator = personidentifikator,
    ): List<UtgåendeMelding> {
        lateinit var usendteMeldinger: List<UtgåendeMelding>
        dao.usendte(person, { funnedeMeldinger ->
            usendteMeldinger = funnedeMeldinger
            ikkeKvitterUtNoenMeldinger
        })
        return usendteMeldinger
    }


    private fun erLagretISendt(utvalg: List<UtgåendeMelding>) = dataSource.ds.connection {
        prepareStatement("SELECT * from sendt").mapNotNull { row -> row.somUtgåendeMelding() }.intersect(utvalg.toSet())
    }

    private fun erLagretIUtboks(utvalg: List<UtgåendeMelding>) = dataSource.ds.connection {
        prepareStatement("SELECT * from utboks").mapNotNull { row -> row.somUtgåendeMelding() }.intersect(utvalg.toSet())
    }

    private fun sjekkAtErLagretISendt(burdeVæreLagret: List<UtgåendeMelding>) {
        val erLagret = erLagretISendt(burdeVæreLagret)
        assertEquals(burdeVæreLagret.toSet(), erLagret) { "Ikke alle meldinger som var forventet lagret i sendt-tabellen var i sendt-tabellen! Forventet ${burdeVæreLagret.size} men var bare ${erLagret.size}" }
    }

    private fun sjekkAtIkkeErLagretISendt(burdeIkkeVæreLagret: List<UtgåendeMelding>) {
        val erLagret = erLagretISendt(burdeIkkeVæreLagret)
        assertEquals(emptySet<UtgåendeMelding>(), erLagret) { "Ingenting burde vært i sendt-tabellen, men her var det ${erLagret.size} stykk!" }
    }

    private fun sjekkAtErLagretIUtboks(burdeVæreLagret: List<UtgåendeMelding>) {
        val erLagret = erLagretIUtboks(burdeVæreLagret)
        assertEquals(burdeVæreLagret.toSet(), erLagret) { "Ikke alle meldinger som var forventet lagret i utboks-tabellen var i sendt-tabellen! Forventet ${burdeVæreLagret.size} men var bare ${erLagret.size}" }
    }

    private fun sjekkAtIkkeErLagretIUtboks(burdeIkkeVæreLagret: List<UtgåendeMelding>) {
        val erLagret = erLagretIUtboks(burdeIkkeVæreLagret)
        assertEquals(emptySet<UtgåendeMelding>(), erLagret) { "Ingen av disse burde være lagret utboks-tabellen, men her var det ${erLagret.size} stykk!" }
    }

    private companion object {
        private val personidentifikator = Personidentifikator("12345678910")
        private fun nyMelding(key: Personidentifikator? = personidentifikator, mottaker: UtgåendeMelding.Mottaker = UtgåendeMelding.Mottaker.RAPID) = UtgåendeMelding(key?.toString(), """{"@id": "${nyUuidv7()}", "@even_name": "test", "@opprettetUTC":"${Instant.now()}"}""", mottaker)
        private fun List<UtgåendeMelding>.somKvittering(sendingsTidspunkt: Instant = Instant.now(), feilet: List<UtgåendeMelding> = emptyList()) = Kvittering(
            sendt = sendingsTidspunkt,
            ok = this,
            feilet = feilet
        )
        private val ikkeKvitterUtNoenMeldinger get() = emptyList<UtgåendeMelding>().somKvittering()
    }
}
