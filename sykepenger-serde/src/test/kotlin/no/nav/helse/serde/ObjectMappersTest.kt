package no.nav.helse.serde

import java.time.LocalDate
import no.nav.helse.serde.PersonData.FaktaavklartInntektData.PensjonsgivendeInntektData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue

internal class ObjectMappersTest {

    @Test
    fun `felter som starter med æ, ø eller å blir med i serialisering og deserialisering`() {
        val data = PensjonsgivendeInntektData(årstall = 2024, årligBeløp = 600000.0)
        val json = serdeObjectMapper.writeValueAsString(data)
        val node = serdeObjectMapper.readTree(json)
        assertEquals(2024, node.path("årstall").asInt())
        assertEquals(600000.0, node.path("årligBeløp").asDouble())
        assertEquals(data, serdeObjectMapper.readValue<PensjonsgivendeInntektData>(json))
    }

    @Test
    fun `datoer serialiseres som arrays`() {
        val json = serdeObjectMapper.writeValueAsString(mapOf("dato" to LocalDate.of(2018, 1, 1)))
        assertEquals("""{"dato":[2018,1,1]}""", json)
    }
}
