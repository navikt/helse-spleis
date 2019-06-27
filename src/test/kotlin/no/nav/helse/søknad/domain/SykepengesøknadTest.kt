package no.nav.helse.søknad.domain

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.readResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SykepengesøknadTest {

    companion object {

        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private val testSøknad = Sykepengesøknad(objectMapper.readTree("/søknad_arbeidstaker_sendt_nav.json".readResource()))
    }

    @Test
    fun `skal svare med id`() {
        assertEquals("68da259c-ff7f-47cf-8fa0-c348ae95e220", testSøknad.id)
    }

    @Test
    fun `skal svare med sykmeldingId`() {
        assertEquals("71bd853d-36a1-49df-a34c-6e02cf727cfa", testSøknad.sykmeldingId)
    }
}
