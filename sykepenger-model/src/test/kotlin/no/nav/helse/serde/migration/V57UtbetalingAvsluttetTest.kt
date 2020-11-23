package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V57UtbetalingAvsluttetTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `lager avstemmingsnøkkel på utbetalinger`() {
        val migrated = listOf(V57UtbetalingAvsluttet())
            .migrate(objectMapper.readTree(originalJson()))
        val expected = objectMapper.readTree(expectedJson())

        assertEquals(expected, migrated)
    }
}

@Language("JSON")
private fun originalJson() =
    """
{
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                  "id": "c11cdfbc-dcc4-4f29-9ef9-e0db7b197487"
                },
                {
                  "id": "9b842ebe-699c-4a57-9265-453ca1ace142",
                  "status": "IKKE_GODKJENT",
                  "vurdering": {
                      "ident": "Z999999",
                      "epost": "ukjent@nav.no",
                      "tidspunkt": "2020-11-21T18:39:57.014",
                      "automatiskBehandling": false
                  }
                },
                {
                  "id": "db8c2c11-e6f6-4731-a184-b3f0393570fc",
                  "vurdering": {
                      "ident": "Z999999",
                      "epost": "ukjent@nav.no",
                      "tidspunkt": "2020-11-21T18:39:57.014",
                      "automatiskBehandling": false
                  }
                },
                {
                  "id": "db8c2c11-e6f6-4731-a184-b3f0393570fc",
                  "vurdering": {
                      "ident": "Z999999",
                      "epost": "ukjent@nav.no",
                      "tidspunkt": "2020-11-21T18:39:57.014",
                      "automatiskBehandling": false
                  },
                  "overføringstidspunkt": "2020-12-01T17:39:57.014"
                }
            ]
        }
    ],
    "skjemaVersjon": 56
}
"""

@Language("JSON")
private fun expectedJson() =
    """
{
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                  "id": "c11cdfbc-dcc4-4f29-9ef9-e0db7b197487"
                },
                {
                  "id": "9b842ebe-699c-4a57-9265-453ca1ace142",
                  "status": "IKKE_GODKJENT",
                  "vurdering": {
                      "ident": "Z999999",
                      "epost": "ukjent@nav.no",
                      "tidspunkt": "2020-11-21T18:39:57.014",
                      "automatiskBehandling": false
                  }
                },
                {
                  "id": "db8c2c11-e6f6-4731-a184-b3f0393570fc",
                  "vurdering": {
                      "ident": "Z999999",
                      "epost": "ukjent@nav.no",
                      "tidspunkt": "2020-11-21T18:39:57.014",
                      "automatiskBehandling": false
                  },
                  "avsluttet": "2020-11-21T18:39:57.014"
                },
                {
                  "id": "db8c2c11-e6f6-4731-a184-b3f0393570fc",
                  "vurdering": {
                      "ident": "Z999999",
                      "epost": "ukjent@nav.no",
                      "tidspunkt": "2020-11-21T18:39:57.014",
                      "automatiskBehandling": false
                  },
                  "overføringstidspunkt": "2020-12-01T17:39:57.014",
                  "avsluttet": "2020-12-01T17:39:57.014"
                }
            ]
        }
    ],
    "skjemaVersjon": 57
}
"""
