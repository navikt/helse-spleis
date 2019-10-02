package no.nav.helse.unit.sakskompleks.domain

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.Event
import no.nav.helse.TestConstants.søknad
import no.nav.helse.inntektsmelding.domain.Inntektsmelding
import no.nav.helse.sakskompleks.domain.Sakskompleks
import no.nav.helse.sakskompleks.domain.SakskompleksObserver
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
    fun `motta sykmelding`(){
        val sakskompleks = beInStartTilstand()

        sakskompleks.leggTil(nySøknad)

        assertEquals("StartTilstand", lastEvent.previousState)
        assertEquals("SykmeldingMottattTilstand", lastEvent.currentState)
        assertEquals(Event.Type.Sykmelding, lastEvent.eventName)
    }

    @Test
    fun `motta søknad`() {
        val sakskompleks = beInStartTilstand()

        sakskompleks.leggTil(søknad())

        assertEquals("StartTilstand", lastEvent.previousState)
        assertEquals("TrengerManuellHåndteringTilstand", lastEvent.currentState)
        assertEquals(Event.Type.Sykepengesøknad, lastEvent.eventName)
    }

    @Test
    fun `motta inntektsmelding`() {
        val sakskompleks = beInStartTilstand()

        sakskompleks.leggTil(inntektsmelding())

        assertEquals("StartTilstand", lastEvent.previousState)
        assertEquals("TrengerManuellHåndteringTilstand", lastEvent.currentState)
        assertEquals(Event.Type.Inntektsmelding, lastEvent.eventName)
    }

    @Test
    fun `motta søknad etter sykmelding`(){
        val sakskompleks = beInMottattSykmelding()

        sakskompleks.leggTil(søknad())

        assertEquals("SykmeldingMottattTilstand", lastEvent.previousState)
        assertEquals("SøknadMottattTilstand", lastEvent.currentState)
    }

    @Test
    fun `motta inntektsmelding etter sykmelding`(){
        val sakskompleks = beInMottattSykmelding()

        sakskompleks.leggTil(inntektsmelding())

        assertEquals("SykmeldingMottattTilstand", lastEvent.previousState)
        assertEquals("InntektsmeldingMottattTilstand", lastEvent.currentState)
    }

    @Test
    fun `motta sykmelding etter sykmelding`(){
        val sakskompleks = beInMottattSykmelding()

        sakskompleks.leggTil(nySøknad)

        assertEquals("SykmeldingMottattTilstand", lastEvent.previousState)
        assertEquals("TrengerManuellHåndteringTilstand", lastEvent.currentState)
    }

    @Test
    fun `motta søknad etter søknad`() {
        val sakskompleks = beInMottattSøknad()

        sakskompleks.leggTil(sendtSøknad)

        assertEquals("SøknadMottattTilstand", lastEvent.previousState)
        assertEquals("TrengerManuellHåndteringTilstand", lastEvent.currentState)
    }

    @Test
    fun `motta inntektsmelding etter søknad`() {
        val sakskompleks = beInMottattSøknad()

        sakskompleks.leggTil(inntektsmelding())

        assertEquals("SøknadMottattTilstand", lastEvent.previousState)
        assertEquals("KomplettSakTilstand", lastEvent.currentState)
    }

    @Test
    fun `motta sykmelding etter søknad`() {
        val sakskompleks = beInMottattSøknad()

        sakskompleks.leggTil(nySøknad)

        assertEquals("SøknadMottattTilstand", lastEvent.previousState)
        assertEquals("TrengerManuellHåndteringTilstand", lastEvent.currentState)
    }

    @Test
    fun `motta søknad etter inntektsmelding`() {
        val sakskompleks = beInMottattInntektsmelding()

        sakskompleks.leggTil(sendtSøknad)

        assertEquals("InntektsmeldingMottattTilstand", lastEvent.previousState)
        assertEquals("KomplettSakTilstand", lastEvent.currentState)
    }

    @Test
    fun `motta sykmelding etter inntektsmelding`() {
        val sakskompleks = beInMottattInntektsmelding()

        sakskompleks.leggTil(nySøknad)

        assertEquals("InntektsmeldingMottattTilstand", lastEvent.previousState)
        assertEquals("TrengerManuellHåndteringTilstand", lastEvent.currentState)
    }

    @Test
    fun `motta inntektsmelding etter inntektsmelding`() {
        val sakskompleks = beInMottattInntektsmelding()

        sakskompleks.leggTil(inntektsmelding())

        assertEquals("InntektsmeldingMottattTilstand", lastEvent.previousState)
        assertEquals("TrengerManuellHåndteringTilstand", lastEvent.currentState)
    }

    companion object {
        private val objectMapper = jacksonObjectMapper()
    }

    private fun beInStartTilstand() =
            Sakskompleks(aktørId = aktørId, id = sakskompleksId).apply {
                addObserver(this@SakskompleksStateTest)
            }

    private fun beInMottattSykmelding() =
            beInStartTilstand().apply {
                leggTil(nySøknad)
            }

    private fun beInMottattSøknad() =
            beInMottattSykmelding().apply {
                leggTil(søknad())
            }

    private fun beInMottattInntektsmelding() =
            beInMottattSykmelding().apply {
                leggTil(inntektsmelding())
            }

    private fun inntektsmelding() =
            Inntektsmelding(objectMapper.valueToTree(mapOf(
                    "inntektsmeldingId" to inntektsmeldingId.toString(),
                    "arbeidstakerAktorId" to aktørId,
                    "virksomhetsnummer" to "123456789"
            )))
}
