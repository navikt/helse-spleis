package no.nav.helse.unit.spleis.serde

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.spleis.serde.safelyUnwrapDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class JsonNodeUtilsKtTest {

    companion object {
        val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    val json = """
        {
            "felt1": null,
            "felt2": "2019-01-01"
        }
    """.trimIndent()

    @Test
    fun safelyUnwrapDate() {
        val jsonNode = objectMapper.readTree(json)

        val dato1 = jsonNode["felt1"].safelyUnwrapDate()
        val dato2 = jsonNode["felt2"].safelyUnwrapDate()
        val dato3 = jsonNode["felt3"]?.safelyUnwrapDate()

        assertEquals(null, dato1)
        assertEquals(LocalDate.of(2019, 1, 1), dato2)
        assertEquals(null, dato3)
    }
}
