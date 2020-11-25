package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V62VurderingGodkjentBooleanTest {
    @Test
    fun `setter godkjent-boolean`() {
        val expected = serdeObjectMapper.readTree(expectedJson)
        val migrated = listOf(V62VurderingGodkjentBoolean())
            .migrate(serdeObjectMapper.readTree(originalJson))
        assertEquals(expected, migrated)
    }
}

@Language("JSON")
private val originalJson =
    """
{
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                    "status": "IKKE_UTBETALT"
                },
                {
                    "status": "IKKE_GODKJENT",
                    "vurdering": {}
                },
                {
                    "status": "GODKJENT",
                    "vurdering": {}
                },
                {
                    "status": "SENDT",
                    "vurdering": {}
                },
                {
                    "status": "OVERFØRT",
                    "vurdering": {}
                },
                {
                    "status": "UTBETALT",
                    "vurdering": {}
                },
                {
                    "status": "GODKJENT_UTEN_UTBETALING",
                    "vurdering": {}
                },
                {
                    "status": "UTBETALING_FEILET",
                    "vurdering": {}
                },
                {
                    "status": "ANNULLERT",
                    "vurdering": {}
                }
            ]
        }
    ],
    "skjemaVersjon": 61
}
"""

@Language("JSON")
private val expectedJson =
    """
{
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                    "status": "IKKE_UTBETALT"
                },
                {
                    "status": "IKKE_GODKJENT",
                    "vurdering": {
                      "godkjent": false
                    }
                },
                {
                    "status": "GODKJENT",
                    "vurdering": {
                      "godkjent": true
                    }
                },
                {
                    "status": "SENDT",
                    "vurdering": {
                      "godkjent": true
                    }
                },
                {
                    "status": "OVERFØRT",
                    "vurdering": {
                      "godkjent": true
                    }
                },
                {
                    "status": "UTBETALT",
                    "vurdering": {
                      "godkjent": true
                    }
                },
                {
                    "status": "GODKJENT_UTEN_UTBETALING",
                    "vurdering": {
                      "godkjent": true
                    }
                },
                {
                    "status": "UTBETALING_FEILET",
                    "vurdering": {
                      "godkjent": true
                    }
                },
                {
                    "status": "ANNULLERT",
                    "vurdering": {
                      "godkjent": true
                    }
                }
            ]
        }
    ],
    "skjemaVersjon": 62
}
"""

