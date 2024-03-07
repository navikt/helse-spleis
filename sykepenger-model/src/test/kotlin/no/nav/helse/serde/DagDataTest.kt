package no.nav.helse.serde

import com.fasterxml.jackson.module.kotlin.readValue
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.serde.PersonData.ArbeidsgiverData.SykdomstidslinjeData.DagData
import no.nav.helse.serde.PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType
import no.nav.helse.serde.PersonData.ArbeidsgiverData.SykdomstidslinjeData.KildeData
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

internal class DagDataTest {

    @Test
    fun `deserialisere og serialisere enkeltdag`() {
        @Language("JSON")
        val json = """{
          "dato": "2018-01-01",
          "type": "SYKEDAG",
          "kilde": {
            "type": "Søknad",
            "id": "9d46a9e9-1e40-4f0f-be22-f6d7ef8f427e",
            "tidsstempel": "2018-01-01T01:00:00.123"
          },
          "grad": 100.0,
          "other": null,
          "melding": null
        }"""

        val actual = serdeObjectMapper.readValue<DagData>(json)
        val expected = DagData(
            type = JsonDagType.SYKEDAG,
            kilde = KildeData(
                type = "Søknad",
                id = UUID.fromString("9d46a9e9-1e40-4f0f-be22-f6d7ef8f427e"),
                tidsstempel = LocalDateTime.parse("2018-01-01T01:00:00.123")
            ),
            grad = 100.0,
            other = null,
            melding = null
        ).apply {
            datoer = DateRange.Single(1.januar)
        }

        assertEquals(expected, actual)
        JSONAssert.assertEquals(json, serdeObjectMapper.writeValueAsString(actual), JSONCompareMode.NON_EXTENSIBLE)
    }
    @Test
    fun `deserialisere og serialisere flere dager`() {
        @Language("JSON")
        val json = """{
          "fom": "2018-01-01",
          "tom": "2018-01-10",
          "type": "SYKEDAG",
          "kilde": {
            "type": "Søknad",
            "id": "9d46a9e9-1e40-4f0f-be22-f6d7ef8f427e",
            "tidsstempel": "2018-01-01T01:00:00.123"
          },
          "grad": 100.0,
          "other": null,
          "melding": null
        }"""

        val actual = serdeObjectMapper.readValue<DagData>(json)
        val expected = DagData(
            type = JsonDagType.SYKEDAG,
            kilde = KildeData(
                type = "Søknad",
                id = UUID.fromString("9d46a9e9-1e40-4f0f-be22-f6d7ef8f427e"),
                tidsstempel = LocalDateTime.parse("2018-01-01T01:00:00.123")
            ),
            grad = 100.0,
            other = null,
            melding = null
        ).apply {
            datoer = DateRange.Range(1.januar, 10.januar)
        }

        assertEquals(expected, actual)
        JSONAssert.assertEquals(json, serdeObjectMapper.writeValueAsString(actual), JSONCompareMode.NON_EXTENSIBLE)
    }
}