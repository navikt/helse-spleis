package no.nav.helse.unit.sykmelding.domain

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.readResource
import no.nav.helse.sykmelding.domain.SykmeldingMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SykmeldingMessageTest {

    companion object {

        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private val testSykmelding = SykmeldingMessage(objectMapper.readTree("/sykmelding.json".readResource()))
    }

    @Test
    fun `skal svare med id`() {
        assertEquals("71bd853d-36a1-49df-a34c-6e02cf727cfa", testSykmelding.sykmelding.id)
    }
}
