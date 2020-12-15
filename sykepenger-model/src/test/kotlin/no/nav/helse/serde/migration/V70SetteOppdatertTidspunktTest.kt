package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V70SetteOppdatertTidspunktTest {
    @Test
    fun `legger på oppdaterttidspunkt`() {
        val expected = serdeObjectMapper.readTree(expectedJson)
        val migrated = listOf(V70SetteOppdatertTidspunkt())
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
                    "id": "399645a5-283c-4a3f-9b2a-f360b8170d98",
                    "tidsstempel": "2020-12-12T15:10:10.100"
                },
                {
                    "id": "3264ed40-ca5b-4af0-8591-5ee74df8df89",
                    "tidsstempel": "2020-12-12T15:10:10.100",
                    "vurdering": {
                      "tidspunkt": "2020-12-12T16:10:10.100"
                    }
                },
                {
                    "id": "f22e490a-978a-4852-bc83-7fa7f3df5733",
                    "tidsstempel": "2020-12-12T15:10:10.100",
                    "vurdering": {
                      "tidspunkt": "2020-12-12T16:10:10.100"
                    },
                    "overføringstidspunkt": "2020-12-13T15:10:10.100"
                },
                {
                    "id": "dda722c8-9d07-4df2-995f-a13e5ea8af90",
                    "tidsstempel": "2020-12-12T15:10:10.100",
                    "vurdering": {
                      "tidspunkt": "2020-12-12T16:10:10.100"
                    },
                    "overføringstidspunkt": "2020-12-13T15:10:10.100",
                    "avsluttet": "2020-12-14T15:10:10.100"
                }
            ]
        }
    ],
    "skjemaVersjon": 69
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
                    "id": "399645a5-283c-4a3f-9b2a-f360b8170d98",
                    "tidsstempel": "2020-12-12T15:10:10.100",
                    "oppdatert": "2020-12-12T15:10:10.100"
                },
                {
                    "id": "3264ed40-ca5b-4af0-8591-5ee74df8df89",
                    "tidsstempel": "2020-12-12T15:10:10.100",
                    "vurdering": {
                      "tidspunkt": "2020-12-12T16:10:10.100"
                    },
                    "oppdatert": "2020-12-12T16:10:10.100"
                },
                {
                    "id": "f22e490a-978a-4852-bc83-7fa7f3df5733",
                    "tidsstempel": "2020-12-12T15:10:10.100",
                    "vurdering": {
                      "tidspunkt": "2020-12-12T16:10:10.100"
                    },
                    "overføringstidspunkt": "2020-12-13T15:10:10.100",
                    "oppdatert": "2020-12-13T15:10:10.100"
                },
                {
                    "id": "dda722c8-9d07-4df2-995f-a13e5ea8af90",
                    "tidsstempel": "2020-12-12T15:10:10.100",
                    "vurdering": {
                      "tidspunkt": "2020-12-12T16:10:10.100"
                    },
                    "overføringstidspunkt": "2020-12-13T15:10:10.100",
                    "avsluttet": "2020-12-14T15:10:10.100",
                    "oppdatert": "2020-12-14T15:10:10.100"
                }
            ]
        }
    ],
    "skjemaVersjon": 70
}
"""
