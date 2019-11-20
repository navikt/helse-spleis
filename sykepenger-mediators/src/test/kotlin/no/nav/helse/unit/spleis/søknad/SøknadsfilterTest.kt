package no.nav.helse.unit.spleis.søknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.mockk
import no.nav.helse.spleis.søknad.SøknadProbe
import no.nav.helse.spleis.søknad.skalTaInnSøknad
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class SøknadsfilterTest {

    val probe = mockk<SøknadProbe>(relaxed = true)

    companion object {
        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    @ParameterizedTest
    @ValueSource(strings = ["SENDT", "NY", "FREMTIDIG"])
    fun `skal ta inn alle typer arbeidstaker søknader`(status: String) {
        val søknad = søknadJson("ARBEIDSTAKERE", status)

        assertTrue(skalTaInnSøknad(søknad))
    }

    @ParameterizedTest
    @ValueSource(strings = ["SENDT", "NY", "FREMTIDIG"])
    fun `skal ta inn alle typer selvstendig og frilans søknader`(status: String) {
        val søknad = søknadJson("SELVSTENDIGE_OG_FRILANSERE", status)

        assertTrue(skalTaInnSøknad(søknad))
    }

    private fun søknadJson(type: String, status: String): JsonNode {
        return objectMapper.readTree("{\n" +
                "  \"id\": \"68da259c-ff7f-47cf-8fa0-c348ae95e220\",\n" +
                "  \"type\": \"$type\",\n" +
                "  \"status\": \"$status\"}")
    }
}
