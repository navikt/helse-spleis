package no.nav.helse.søknad.domain

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.readResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SykepengesøknadTest {

    companion object {

        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private val testSøknadJson = objectMapper.readTree("/søknad_arbeidstaker_sendt_nav.json".readResource())
        private val testSøknad = Sykepengesøknad(testSøknadJson)
        private val testSøknadSerialisert = objectMapper.readTree(objectMapper.writeValueAsString(testSøknad))
    }

    @Test
    fun `skal svare med id`() {
        assertEquals("68da259c-ff7f-47cf-8fa0-c348ae95e220", testSøknad.id)
    }

    @Test
    fun `skal svare med sykmeldingId`() {
        assertEquals("71bd853d-36a1-49df-a34c-6e02cf727cfa", testSøknad.sykmeldingId)
    }

    @Test
    fun `serialisering av søknaden skal være lik den deserialiserte json`() {
        assertEquals(testSøknadJson, testSøknadSerialisert)
    }

    @Test
    fun `serialisering av en annen søknad skal ikke være lik den deserialiserte json`() {
        val expectedJson = objectMapper.readTree("/sykmelding.json".readResource())["sykmelding"]
        assertNotEquals(expectedJson, testSøknadSerialisert)
    }

    @Test
    fun `kan parse søknad uten arbeidGjenopptatt`() {
        val json = objectMapper.readTree("/søknad_arbeidstaker_sendt_nav.json".readResource()) as ObjectNode
        json.remove("arbeidGjenopptatt")

        val søknad = Sykepengesøknad(json)
        assertNull(søknad.arbeidGjenopptatt)
    }
}
