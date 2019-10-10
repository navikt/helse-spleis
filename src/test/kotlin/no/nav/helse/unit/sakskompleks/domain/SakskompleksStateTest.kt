package no.nav.helse.unit.sakskompleks.domain

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.TestConstants.inntektsmelding
import no.nav.helse.TestConstants.nySøknad
import no.nav.helse.TestConstants.sendtSøknad
import no.nav.helse.hendelse.Event
import no.nav.helse.person.domain.Sakskompleks
import no.nav.helse.person.domain.SakskompleksObserver
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
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
        assertEquals(Event.Type.NySykepengesøknad, lastStateEvent.eventType)
    }

    @Test
    fun `motta søknad`() {
        val sakskompleks = beInStartTilstand()

        sakskompleks.håndterSendtSøknad(sendtSøknad())

        assertEquals(Sakskompleks.TilstandType.START, lastStateEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, lastStateEvent.currentState)
        assertEquals(Event.Type.SendtSykepengesøknad, lastStateEvent.eventType)
    }

    @Test
    fun `motta inntektsmelding`() {
        val sakskompleks = beInStartTilstand()

        sakskompleks.håndterInntektsmelding(inntektsmelding())

        assertEquals(Sakskompleks.TilstandType.START, lastStateEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, lastStateEvent.currentState)
        assertEquals(Event.Type.Inntektsmelding, lastStateEvent.eventType)
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

    companion object {
        private val objectMapper = jacksonObjectMapper()
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

}
