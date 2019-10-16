package no.nav.helse.unit.sakskompleks.domain

import no.nav.helse.TestConstants.inntektsmelding
import no.nav.helse.TestConstants.nySøknad
import no.nav.helse.TestConstants.sendtSøknad
import no.nav.helse.TestConstants.sykepengehistorikk
import no.nav.helse.hendelse.Inntektsmelding
import no.nav.helse.hendelse.NySykepengesøknad
import no.nav.helse.hendelse.SendtSykepengesøknad
import no.nav.helse.hendelse.Sykepengehistorikk
import no.nav.helse.person.domain.Sakskompleks
import no.nav.helse.person.domain.SakskompleksObserver
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class SakskompleksStateTest : SakskompleksObserver {
    private lateinit var lastStateEvent: SakskompleksObserver.StateChangeEvent
    private lateinit var lastNeedEvent: SakskompleksObserver.NeedEvent

    override fun sakskompleksChanged(event: SakskompleksObserver.StateChangeEvent) {
        lastStateEvent = event
    }

    override fun sakskompleksHasNeed(event: SakskompleksObserver.NeedEvent) {
        lastNeedEvent = event
    }

    private val aktørId = "1234567891011"
    private val organisasjonsnummer = "123456789"
    private val sakskompleksId = UUID.randomUUID()

    @Test
    fun `motta ny søknad`() {
        val sakskompleks = beInStartTilstand()

        sakskompleks.håndterNySøknad(nySøknad())

        assertEquals(Sakskompleks.TilstandType.START, lastStateEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is NySykepengesøknad)
    }

    @Test
    fun `motta sendt søknad på feil tidspunkt`() {
        val sakskompleks = beInStartTilstand()

        sakskompleks.håndterSendtSøknad(sendtSøknad())

        assertEquals(Sakskompleks.TilstandType.START, lastStateEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is SendtSykepengesøknad)
    }

    @Test
    fun `motta inntektsmelding på feil tidspunkt`() {
        val sakskompleks = beInStartTilstand()

        sakskompleks.håndterInntektsmelding(inntektsmelding())

        assertEquals(Sakskompleks.TilstandType.START, lastStateEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is Inntektsmelding)
    }

    @Test
    fun `motta sykdomshistorikk på feil tidspunkt`() {
        val sakskompleks = beInStartTilstand()

        sakskompleks.håndterSykepengehistorikk(sykepengehistorikk(sisteHistoriskeSykedag = LocalDate.now(), sakskompleksId = sakskompleksId))

        assertEquals(Sakskompleks.TilstandType.START, lastStateEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is Sykepengehistorikk)
    }

    @Test
    fun `motta sendt søknad etter ny søknad`() {
        val sakskompleks = beInNySøknad()

        sakskompleks.håndterSendtSøknad(sendtSøknad())

        assertEquals(Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, lastStateEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.SENDT_SØKNAD_MOTTATT, lastStateEvent.currentState)
    }

    @Test
    fun `motta inntektsmelding etter ny søknad`() {
        val sakskompleks = beInNySøknad()

        sakskompleks.håndterInntektsmelding(inntektsmelding())

        assertEquals(Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, lastStateEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.INNTEKTSMELDING_MOTTATT, lastStateEvent.currentState)
    }

    @Test
    fun `motta ny søknad etter ny søknad`() {
        val sakskompleks = beInNySøknad()

        sakskompleks.håndterNySøknad(nySøknad())

        assertEquals(Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, lastStateEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, lastStateEvent.currentState)
    }

    @Test
    fun `motta ny søknad etter sendt søknad`() {
        val sakskompleks = beInSendtSøknad()

        sakskompleks.håndterNySøknad(nySøknad())

        assertEquals(Sakskompleks.TilstandType.SENDT_SØKNAD_MOTTATT, lastStateEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, lastStateEvent.currentState)
    }

    @Test
    fun `motta inntektsmelding etter sendt søknad`() {
        val sakskompleks = beInSendtSøknad()

        sakskompleks.håndterInntektsmelding(inntektsmelding())

        assertEquals(Sakskompleks.TilstandType.SENDT_SØKNAD_MOTTATT, lastStateEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.KOMPLETT_SAK, lastStateEvent.currentState)
    }

    @Test
    fun `motta ny søknad etter søknad`() {
        val sakskompleks = beInSendtSøknad()

        sakskompleks.håndterNySøknad(nySøknad())

        assertEquals(Sakskompleks.TilstandType.SENDT_SØKNAD_MOTTATT, lastStateEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, lastStateEvent.currentState)
    }

    @Test
    fun `motta sendt søknad etter inntektsmelding`() {
        val sakskompleks = beInMottattInntektsmelding()

        sakskompleks.håndterSendtSøknad(sendtSøknad())

        assertEquals(Sakskompleks.TilstandType.INNTEKTSMELDING_MOTTATT, lastStateEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.KOMPLETT_SAK, lastStateEvent.currentState)
    }

    @Test
    fun `motta ny søknad etter inntektsmelding`() {
        val sakskompleks = beInMottattInntektsmelding()

        sakskompleks.håndterNySøknad(nySøknad())

        assertEquals(Sakskompleks.TilstandType.INNTEKTSMELDING_MOTTATT, lastStateEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, lastStateEvent.currentState)
    }

    @Test
    fun `motta inntektsmelding etter inntektsmelding`() {
        val sakskompleks = beInMottattInntektsmelding()

        sakskompleks.håndterInntektsmelding(inntektsmelding())

        assertEquals(Sakskompleks.TilstandType.INNTEKTSMELDING_MOTTATT, lastStateEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, lastStateEvent.currentState)
    }

    @Test
    fun `når saken er komplett, ber vi om sykepengehistorikk`() {
        val sakskompleks = beInMottattInntektsmelding()

        sakskompleks.håndterSendtSøknad(sendtSøknad())

        assertEquals(Sakskompleks.TilstandType.INNTEKTSMELDING_MOTTATT, lastStateEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.KOMPLETT_SAK, lastStateEvent.currentState)
        assertNotNull(lastNeedEvent)
    }

    @Test
    fun `motta sykepengehistorikk når saken er komplett men historikken er utenfor seks måneder`() {
        val sakskompleks = beInKomplettTidslinje()
        val sisteHistoriskeSykedag = nySøknad().egenmeldinger.sortedBy { it.fom }.first().fom.minusMonths(7)

        sakskompleks.håndterSykepengehistorikk(sykepengehistorikk(sisteHistoriskeSykedag = sisteHistoriskeSykedag, sakskompleksId = sakskompleksId))

        assertEquals(Sakskompleks.TilstandType.KOMPLETT_SAK, lastStateEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.SYKEPENGEHISTORIKK_MOTTATT, lastStateEvent.currentState)
    }

    @Test
    fun `motta sykepengehistorikk med siste sykedag innenfor seks måneder av denne sakens første sykedag`() {
        val sakskompleks = beInKomplettTidslinje()
        val sisteHistoriskeSykedag = nySøknad().egenmeldinger.sortedBy { it.fom }.first().fom.minusMonths(5)

        sakskompleks.håndterSykepengehistorikk(sykepengehistorikk(sisteHistoriskeSykedag = sisteHistoriskeSykedag, sakskompleksId = sakskompleksId))

        assertEquals(Sakskompleks.TilstandType.KOMPLETT_SAK, lastStateEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, lastStateEvent.currentState)
    }

    private fun beInStartTilstand(): Sakskompleks {
        return Sakskompleks(
            aktørId = aktørId,
            id = sakskompleksId,
            organisasjonsnummer = organisasjonsnummer
        ).apply {
            addSakskompleksObserver(this@SakskompleksStateTest)
        }
    }

    private fun beInNySøknad() =
        beInStartTilstand().apply {
            håndterNySøknad(nySøknad())
        }

    private fun beInSendtSøknad() =
        beInNySøknad().apply {
            håndterSendtSøknad(sendtSøknad())
        }

    private fun beInMottattInntektsmelding() =
        beInNySøknad().apply {
            håndterInntektsmelding(inntektsmelding())
        }

    private fun beInKomplettTidslinje() =
        beInMottattInntektsmelding().apply {
            håndterSendtSøknad(sendtSøknad())
        }
}
