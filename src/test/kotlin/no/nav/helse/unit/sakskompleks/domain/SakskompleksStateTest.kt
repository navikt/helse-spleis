package no.nav.helse.unit.sakskompleks.domain

import no.nav.helse.TestConstants.inntektsmelding
import no.nav.helse.TestConstants.nySøknad
import no.nav.helse.TestConstants.sendtSøknad
import no.nav.helse.TestConstants.sykepengehistorikk
import no.nav.helse.behov.Behov
import no.nav.helse.hendelse.InntektsmeldingMottatt
import no.nav.helse.hendelse.NySøknadOpprettet
import no.nav.helse.hendelse.SendtSøknadMottatt
import no.nav.helse.hendelse.Sykepengehistorikk
import no.nav.helse.person.domain.Sakskompleks
import no.nav.helse.person.domain.Sakskompleks.TilstandType.*
import no.nav.helse.person.domain.SakskompleksObserver
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class SakskompleksStateTest : SakskompleksObserver {
    private lateinit var lastStateEvent: SakskompleksObserver.StateChangeEvent
    private lateinit var lastNeedEvent: Behov

    override fun sakskompleksEndret(event: SakskompleksObserver.StateChangeEvent) {
        lastStateEvent = event
    }

    override fun sakskompleksTrengerLøsning(event: Behov) {
        lastNeedEvent = event
    }

    private val aktørId = "1234567891011"
    private val organisasjonsnummer = "123456789"
    private val sakskompleksId = UUID.randomUUID()

    @Test
    fun `motta ny søknad`() {
        val sakskompleks = beInStartTilstand()

        sakskompleks.håndterNySøknad(nySøknad())

        assertEquals(START, lastStateEvent.previousState)
        assertEquals(NY_SØKNAD_MOTTATT, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is NySøknadOpprettet)
    }

    @Test
    fun `motta sendt søknad på feil tidspunkt`() {
        val sakskompleks = beInStartTilstand()

        sakskompleks.håndterSendtSøknad(sendtSøknad())

        assertEquals(START, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is SendtSøknadMottatt)
    }

    @Test
    fun `motta inntektsmelding på feil tidspunkt`() {
        val sakskompleks = beInStartTilstand()

        sakskompleks.håndterInntektsmelding(inntektsmelding())

        assertEquals(START, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is InntektsmeldingMottatt)
    }

    @Test
    fun `motta sykdomshistorikk på feil tidspunkt`() {
        val sakskompleks = beInStartTilstand()

        sakskompleks.håndterSykepengehistorikk(sykepengehistorikk(sisteHistoriskeSykedag = LocalDate.now(), sakskompleksId = sakskompleksId))

        assertEquals(START, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is Sykepengehistorikk)
    }

    @Test
    fun `motta sendt søknad etter ny søknad`() {
        val sakskompleks = beInNySøknad()

        sakskompleks.håndterSendtSøknad(sendtSøknad())

        assertEquals(NY_SØKNAD_MOTTATT, lastStateEvent.previousState)
        assertEquals(SENDT_SØKNAD_MOTTATT, lastStateEvent.currentState)
    }

    @Test
    fun `motta inntektsmelding etter ny søknad`() {
        val sakskompleks = beInNySøknad()

        sakskompleks.håndterInntektsmelding(inntektsmelding())

        assertEquals(NY_SØKNAD_MOTTATT, lastStateEvent.previousState)
        assertEquals(INNTEKTSMELDING_MOTTATT, lastStateEvent.currentState)
    }

    @Test
    fun `motta ny søknad etter ny søknad`() {
        val sakskompleks = beInNySøknad()

        sakskompleks.håndterNySøknad(nySøknad())

        assertEquals(NY_SØKNAD_MOTTATT, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
    }

    @Test
    fun `motta ny søknad etter sendt søknad`() {
        val sakskompleks = beInSendtSøknad()

        sakskompleks.håndterNySøknad(nySøknad())

        assertEquals(SENDT_SØKNAD_MOTTATT, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
    }

    @Test
    fun `motta inntektsmelding etter sendt søknad`() {
        val sakskompleks = beInSendtSøknad()

        sakskompleks.håndterInntektsmelding(inntektsmelding())

        assertEquals(SENDT_SØKNAD_MOTTATT, lastStateEvent.previousState)
        assertEquals(KOMPLETT_SAK, lastStateEvent.currentState)
    }

    @Test
    fun `motta ny søknad etter søknad`() {
        val sakskompleks = beInSendtSøknad()

        sakskompleks.håndterNySøknad(nySøknad())

        assertEquals(SENDT_SØKNAD_MOTTATT, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
    }

    @Test
    fun `motta sendt søknad etter inntektsmelding`() {
        val sakskompleks = beInMottattInntektsmelding()

        sakskompleks.håndterSendtSøknad(sendtSøknad())

        assertEquals(INNTEKTSMELDING_MOTTATT, lastStateEvent.previousState)
        assertEquals(KOMPLETT_SAK, lastStateEvent.currentState)
    }

    @Test
    fun `motta ny søknad etter inntektsmelding`() {
        val sakskompleks = beInMottattInntektsmelding()

        sakskompleks.håndterNySøknad(nySøknad())

        assertEquals(INNTEKTSMELDING_MOTTATT, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
    }

    @Test
    fun `motta inntektsmelding etter inntektsmelding`() {
        val sakskompleks = beInMottattInntektsmelding()

        sakskompleks.håndterInntektsmelding(inntektsmelding())

        assertEquals(INNTEKTSMELDING_MOTTATT, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
    }

    @Test
    fun `når saken er komplett, ber vi om sykepengehistorikk`() {
        val sakskompleks = beInMottattInntektsmelding()

        sakskompleks.håndterSendtSøknad(sendtSøknad())

        assertEquals(INNTEKTSMELDING_MOTTATT, lastStateEvent.previousState)
        assertEquals(KOMPLETT_SAK, lastStateEvent.currentState)
        assertNotNull(lastNeedEvent)
    }

    @Test
    fun `motta sykepengehistorikk når saken er komplett men historikken er utenfor seks måneder`() {
        val sakskompleks = beInKomplettTidslinje()
        val sisteHistoriskeSykedag = nySøknad().egenmeldinger.sortedBy { it.fom }.first().fom.minusMonths(7)

        sakskompleks.håndterSykepengehistorikk(sykepengehistorikk(sisteHistoriskeSykedag = sisteHistoriskeSykedag, sakskompleksId = sakskompleksId))

        assertEquals(KOMPLETT_SAK, lastStateEvent.previousState)
        assertEquals(SYKEPENGEHISTORIKK_MOTTATT, lastStateEvent.currentState)
    }

    @Test
    fun `motta sykepengehistorikk med siste sykedag innenfor seks måneder av denne sakens første sykedag`() {
        val sakskompleks = beInKomplettTidslinje()
        val sisteHistoriskeSykedag = nySøknad().egenmeldinger.sortedBy { it.fom }.first().fom.minusMonths(5)

        sakskompleks.håndterSykepengehistorikk(sykepengehistorikk(sisteHistoriskeSykedag = sisteHistoriskeSykedag, sakskompleksId = sakskompleksId))

        assertEquals(KOMPLETT_SAK, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
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
