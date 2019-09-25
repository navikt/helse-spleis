package no.nav.helse.sakskompleks

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.inntektsmelding.InntektsmeldingConsumer.Companion.inntektsmeldingObjectMapper
import no.nav.helse.inntektsmelding.domain.Inntektsmelding
import no.nav.helse.readResource
import no.nav.helse.sakskompleks.domain.*
import no.nav.helse.sykmelding.SykmeldingConsumer.Companion.sykmeldingObjectMapper
import no.nav.helse.sykmelding.domain.SykmeldingMessage
import no.nav.helse.søknad.domain.Sykepengesøknad
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

class SakskompleksTilstandTest {

    companion object {
        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        val enSykmelding = SykmeldingMessage(sykmeldingObjectMapper.readTree("/sykmelding.json".readResource())).sykmelding
        val enSøknad = Sykepengesøknad(objectMapper.readTree("/søknad_arbeidstaker_sendt_nav.json".readResource()))
        val enInntektsmelding = Inntektsmelding(inntektsmeldingObjectMapper.readTree("/inntektsmelding.json".readResource()))
    }

    @Test
    fun `startTilstand er første tilstand`() {
        val etSakskompleks = Sakskompleks(
                id = UUID.randomUUID(),
                aktørId = "123456789"
        )

        assertTrue(etSakskompleks.tilstand is StartTilstand)
    }

    @Test
    fun `vi kan motta en sykmelding`() {
        val etSakskompleks = Sakskompleks(
            id = UUID.randomUUID(),
            aktørId = "123456789"
        )

        etSakskompleks.leggTil(enSykmelding)

        assertTrue(etSakskompleks.tilstand is SykmeldingMottattTilstand)
        assertTrue(etSakskompleks.har(enSykmelding))
    }

    @Test
    fun `vi kan motta en søknad når vi har en sykmelding`() {
        val etSakskompleks = Sakskompleks(
                id = UUID.randomUUID(),
                aktørId = "123456789"
        )

        etSakskompleks.leggTil(enSykmelding)
        etSakskompleks.leggTil(enSøknad)

        assertTrue(etSakskompleks.tilstand is SøknadMottattTilstand)
        assertTrue(etSakskompleks.har(enSykmelding))
        assertTrue(etSakskompleks.har(enSøknad))
    }

    @Test
    fun `vi kan motta en inntektsmelding når vi har en sykmelding`() {
        val etSakskompleks = Sakskompleks(
            id = UUID.randomUUID(),
            aktørId = "123456789"
        )

        etSakskompleks.leggTil(enSykmelding)
        etSakskompleks.leggTil(enInntektsmelding)

        assertTrue(etSakskompleks.tilstand is InntektsmeldingMottattTilstand)
        assertTrue(etSakskompleks.har(enSykmelding))
        assertTrue(etSakskompleks.har(enInntektsmelding))
    }

    @Test
    fun `vi har en komplett sak når vi har mottatt en sykmelding, en inntektsmelding og en søknad (i den rekkefølgen)`() {
        val etSakskompleks = Sakskompleks(
            id = UUID.randomUUID(),
            aktørId = "123456789"
        )

        etSakskompleks.leggTil(enSykmelding)
        etSakskompleks.leggTil(enInntektsmelding)
        etSakskompleks.leggTil(enSøknad)

        assertTrue(etSakskompleks.tilstand is KomplettSakTilstand)
        assertTrue(etSakskompleks.har(enSykmelding))
        assertTrue(etSakskompleks.har(enInntektsmelding))
        assertTrue(etSakskompleks.har(enSøknad))
    }

    @Test
    fun `vi har en komplett sak når vi har mottatt en sykmelding, en søknad og en inntektsmelding (i den rekkefølgen)`() {
        val etSakskompleks = Sakskompleks(
            id = UUID.randomUUID(),
            aktørId = "123456789"
        )

        etSakskompleks.leggTil(enSykmelding)
        etSakskompleks.leggTil(enSøknad)
        etSakskompleks.leggTil(enInntektsmelding)

        assertTrue(etSakskompleks.tilstand is KomplettSakTilstand)
        assertTrue(etSakskompleks.har(enSykmelding))
        assertTrue(etSakskompleks.har(enInntektsmelding))
        assertTrue(etSakskompleks.har(enSøknad))
    }

}
