package no.nav.helse.sykmelding.domain

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.readResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SykmeldingTest {

    companion object {

        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private val testSykmelding = Sykmelding(objectMapper.readTree("/sykmelding.json".readResource())["sykmelding"])
    }

    @Test
    fun `skal svare med id`() {
        assertEquals("71bd853d-36a1-49df-a34c-6e02cf727cfa", testSykmelding.id)
    }
}
