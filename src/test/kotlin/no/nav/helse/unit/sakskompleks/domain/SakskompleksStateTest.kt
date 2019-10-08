package no.nav.helse.unit.sakskompleks.domain

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.Event
import no.nav.helse.TestConstants.søknad
import no.nav.helse.inntektsmelding.domain.Inntektsmelding
import no.nav.helse.person.domain.Sakskompleks
import no.nav.helse.person.domain.SakskompleksObserver
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsstatusDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

internal class SakskompleksStateTest : SakskompleksObserver {
    private lateinit var lastEvent: SakskompleksObserver.StateChangeEvent

    override fun sakskompleksChanged(event: SakskompleksObserver.StateChangeEvent) {
        lastEvent = event
    }

    private val nySøknad = søknad(status = SoknadsstatusDTO.NY)
    private val sendtSøknad = søknad(status = SoknadsstatusDTO.SENDT)
    private val aktørId = "1234567891011"
    private val sakskompleksId = UUID.randomUUID()
    private val sykmeldingId = UUID.randomUUID()
    private val søknadId = UUID.randomUUID()
    private val inntektsmeldingId = UUID.randomUUID()

    @Test
    fun `motta ny søknad`(){
        val sakskompleks = beInStartTilstand()

        sakskompleks.håndterNySøknad(nySøknad)

        assertEquals(Sakskompleks.TilstandType.START, lastEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, lastEvent.currentState)
        assertEquals(Event.Type.NySykepengesøknad, lastEvent.eventType)
    }

    @Test
    fun `motta søknad`() {
        val sakskompleks = beInStartTilstand()

        sakskompleks.håndterSendtSøknad(søknad(
                status = SoknadsstatusDTO.SENDT
        ))

        assertEquals(Sakskompleks.TilstandType.START, lastEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, lastEvent.currentState)
        assertEquals(Event.Type.SendtSykepengesøknad, lastEvent.eventType)
    }

    @Test
    fun `motta inntektsmelding`() {
        val sakskompleks = beInStartTilstand()

        sakskompleks.håndterInntektsmelding(inntektsmelding())

        assertEquals(Sakskompleks.TilstandType.START, lastEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, lastEvent.currentState)
        assertEquals(Event.Type.Inntektsmelding, lastEvent.eventType)
    }

    @Test
    fun `motta sendt søknad etter ny søknad`(){
        val sakskompleks = beInNySøknad()

        sakskompleks.håndterSendtSøknad(søknad())

        assertEquals(Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, lastEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.SENDT_SØKNAD_MOTTATT, lastEvent.currentState)
    }

    @Test
    fun `motta inntektsmelding etter ny søknad`(){
        val sakskompleks = beInNySøknad()

        sakskompleks.håndterInntektsmelding(inntektsmelding())

        assertEquals(Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, lastEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.INNTEKTSMELDING_MOTTATT, lastEvent.currentState)
    }

    @Test
    fun `motta ny søknad etter ny søknad`(){
        val sakskompleks = beInNySøknad()

        sakskompleks.håndterNySøknad(nySøknad)

        assertEquals(Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, lastEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, lastEvent.currentState)
    }

    @Test
    fun `motta ny søknad etter sendt søknad`() {
        val sakskompleks = beInSendtSøknad()

        sakskompleks.håndterNySøknad(nySøknad)

        assertEquals(Sakskompleks.TilstandType.SENDT_SØKNAD_MOTTATT, lastEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, lastEvent.currentState)
    }

    @Test
    fun `motta inntektsmelding etter sendt søknad`() {
        val sakskompleks = beInSendtSøknad()

        sakskompleks.håndterInntektsmelding(inntektsmelding())

        assertEquals(Sakskompleks.TilstandType.SENDT_SØKNAD_MOTTATT, lastEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.KOMPLETT_SAK, lastEvent.currentState)
    }

    @Test
    fun `motta ny søknad etter søknad`() {
        val sakskompleks = beInSendtSøknad()

        sakskompleks.håndterNySøknad(nySøknad)

        assertEquals(Sakskompleks.TilstandType.SENDT_SØKNAD_MOTTATT, lastEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, lastEvent.currentState)
    }

    @Test
    fun `motta sendt søknad etter inntektsmelding`() {
        val sakskompleks = beInMottattInntektsmelding()

        sakskompleks.håndterSendtSøknad(sendtSøknad)

        assertEquals(Sakskompleks.TilstandType.INNTEKTSMELDING_MOTTATT, lastEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.KOMPLETT_SAK, lastEvent.currentState)
    }

    @Test
    fun `motta ny søknad etter inntektsmelding`() {
        val sakskompleks = beInMottattInntektsmelding()

        sakskompleks.håndterNySøknad(nySøknad)

        assertEquals(Sakskompleks.TilstandType.INNTEKTSMELDING_MOTTATT, lastEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, lastEvent.currentState)
    }

    @Test
    fun `motta inntektsmelding etter inntektsmelding`() {
        val sakskompleks = beInMottattInntektsmelding()

        sakskompleks.håndterInntektsmelding(inntektsmelding())

        assertEquals(Sakskompleks.TilstandType.INNTEKTSMELDING_MOTTATT, lastEvent.previousState)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, lastEvent.currentState)
    }

    companion object {
        private val objectMapper = jacksonObjectMapper()
    }

    private fun beInStartTilstand() =
            Sakskompleks(aktørId = aktørId, id = sakskompleksId).apply {
                addObserver(this@SakskompleksStateTest)
            }

    private fun beInNySøknad() =
            beInStartTilstand().apply {
                håndterNySøknad(nySøknad)
            }

    private fun beInSendtSøknad() =
            beInNySøknad().apply {
                håndterSendtSøknad(søknad())
            }

    private fun beInMottattInntektsmelding() =
            beInNySøknad().apply {
                håndterInntektsmelding(inntektsmelding())
            }

    private fun inntektsmelding() =
            Inntektsmelding(objectMapper.valueToTree(mapOf(
                    "inntektsmeldingId" to inntektsmeldingId.toString(),
                    "arbeidstakerAktorId" to aktørId,
                    "virksomhetsnummer" to "123456789"
            )))
}
