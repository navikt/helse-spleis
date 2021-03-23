package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V61MigrereUtbetaltePerioderITilInfotrygdTest {

    @Test
    fun `migrerer forkastede TIL_INFOTRYGD-perioder til AVSLUTTET`() {
        val expected = serdeObjectMapper.readTree(expectedJson)
        val migrated = listOf(V61MigrereUtbetaltePerioderITilInfotrygd())
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
                    "status": "UTBETALT",
                    "id": "bbbbbbbb-3566-4697-b10f-bf77a36ed3f7"
                },
                {
                    "status": "UTBETALT",
                    "id": "cccccccc-c900-4370-a709-1f300c6bc9ec",
                    "type": "UTBETALING"
                },
                {
                    "status": "UTBETALT",
                    "id": "dddddddd-4e93-463f-9470-ed631304a001",
                    "type": "UTBETALING"
                },
                {
                    "status": "UTBETALT",
                    "id": "eeeeeeee-e722-40cf-9089-da79219a2d53",
                    "type": "UTBETALING"
                },
                {
                    "status": "UTBETALT",
                    "id": "aaaaaaaa-d3c0-4492-970a-469a5a58ff29",
                    "type": "UTBETALING"
                }
            ],
            "forkastede": [
                {
                    "vedtaksperiode": {
                        "fom": "2020-03-23",
                        "tom": "2020-04-05",
                        "tilstand": "TIL_INFOTRYGD"
                    }
                },
                {
                    "vedtaksperiode": {
                        "fom": "2020-04-06",
                        "tom": "2020-04-13",
                        "tilstand": "TIL_INFOTRYGD"
                    }
                },
                {
                    "vedtaksperiode": {
                        "fom": "2020-05-26",
                        "tom": "2020-06-09",
                        "tilstand": "AVSLUTTET_UTEN_UTBETALING"
                    }
                },
                {
                    "vedtaksperiode": {
                        "fom": "2020-05-27",
                        "tom": "2020-06-10",
                        "tilstand": "TIL_INFOTRYGD"
                    }
                },
                {
                    "vedtaksperiode": {
                        "fom": "2020-06-10",
                        "tom": "2020-06-17",
                        "tilstand": "TIL_INFOTRYGD",
                        "utbetalingId": "bbbbbbbb-3566-4697-b10f-bf77a36ed3f7"
                    }
                },
                {
                    "vedtaksperiode": {
                        "fom": "2020-06-18",
                        "tom": "2020-06-26",
                        "tilstand": "TIL_INFOTRYGD"
                    }
                },
                {
                    "vedtaksperiode": {
                        "fom": "2020-06-27",
                        "tom": "2020-07-12",
                        "tilstand": "AVSLUTTET",
                        "utbetalingId": "cccccccc-c900-4370-a709-1f300c6bc9ec"
                    }
                },
                {
                    "vedtaksperiode": {
                        "fom": "2020-07-13",
                        "tom": "2020-07-26",
                        "tilstand": "AVSLUTTET",
                        "utbetalingId": "dddddddd-4e93-463f-9470-ed631304a001"
                    }
                },
                {
                    "vedtaksperiode": {
                        "fom": "2020-07-27",
                        "tom": "2020-08-24",
                        "tilstand": "AVSLUTTET",
                        "utbetalingId": "eeeeeeee-e722-40cf-9089-da79219a2d53"
                    }
                },
                {
                    "vedtaksperiode": {
                        "fom": "2020-08-25",
                        "tom": "2020-09-22",
                        "tilstand": "TIL_INFOTRYGD",
                        "utbetalingId": "aaaaaaaa-d3c0-4492-970a-469a5a58ff29"
                    }
                },
                {
                    "vedtaksperiode": {
                        "fom": "2020-10-05",
                        "tom": "2020-10-26",
                        "tilstand": "TIL_INFOTRYGD"
                    }
                }
            ]
        }
    ],
    "skjemaVersjon": 60
}""".trimIndent()

@Language("JSON")
private val expectedJson =
    """
{
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                    "status": "UTBETALT",
                    "id": "bbbbbbbb-3566-4697-b10f-bf77a36ed3f7"
                },
                {
                    "status": "UTBETALT",
                    "id": "cccccccc-c900-4370-a709-1f300c6bc9ec",
                    "type": "UTBETALING"
                },
                {
                    "status": "UTBETALT",
                    "id": "dddddddd-4e93-463f-9470-ed631304a001",
                    "type": "UTBETALING"
                },
                {
                    "status": "UTBETALT",
                    "id": "eeeeeeee-e722-40cf-9089-da79219a2d53",
                    "type": "UTBETALING"
                },
                {
                    "status": "UTBETALT",
                    "id": "aaaaaaaa-d3c0-4492-970a-469a5a58ff29",
                    "type": "UTBETALING"
                }
            ],
            "forkastede": [
                {
                    "vedtaksperiode": {
                        "fom": "2020-03-23",
                        "tom": "2020-04-05",
                        "tilstand": "TIL_INFOTRYGD"
                    }
                },
                {
                    "vedtaksperiode": {
                        "fom": "2020-04-06",
                        "tom": "2020-04-13",
                        "tilstand": "TIL_INFOTRYGD"
                    }
                },
                {
                    "vedtaksperiode": {
                        "fom": "2020-05-26",
                        "tom": "2020-06-09",
                        "tilstand": "AVSLUTTET_UTEN_UTBETALING"
                    }
                },
                {
                    "vedtaksperiode": {
                        "fom": "2020-05-27",
                        "tom": "2020-06-10",
                        "tilstand": "TIL_INFOTRYGD"
                    }
                },
                {
                    "vedtaksperiode": {
                        "fom": "2020-06-10",
                        "tom": "2020-06-17",
                        "tilstand": "TIL_INFOTRYGD",
                        "utbetalingId": "bbbbbbbb-3566-4697-b10f-bf77a36ed3f7"
                    }
                },
                {
                    "vedtaksperiode": {
                        "fom": "2020-06-18",
                        "tom": "2020-06-26",
                        "tilstand": "TIL_INFOTRYGD"
                    }
                },
                {
                    "vedtaksperiode": {
                        "fom": "2020-06-27",
                        "tom": "2020-07-12",
                        "tilstand": "AVSLUTTET",
                        "utbetalingId": "cccccccc-c900-4370-a709-1f300c6bc9ec"
                    }
                },
                {
                    "vedtaksperiode": {
                        "fom": "2020-07-13",
                        "tom": "2020-07-26",
                        "tilstand": "AVSLUTTET",
                        "utbetalingId": "dddddddd-4e93-463f-9470-ed631304a001"
                    }
                },
                {
                    "vedtaksperiode": {
                        "fom": "2020-07-27",
                        "tom": "2020-08-24",
                        "tilstand": "AVSLUTTET",
                        "utbetalingId": "eeeeeeee-e722-40cf-9089-da79219a2d53"
                    }
                },
                {
                    "vedtaksperiode": {
                        "fom": "2020-08-25",
                        "tom": "2020-09-22",
                        "tilstand": "AVSLUTTET",
                        "utbetalingId": "aaaaaaaa-d3c0-4492-970a-469a5a58ff29"
                    }
                },
                {
                    "vedtaksperiode": {
                        "fom": "2020-10-05",
                        "tom": "2020-10-26",
                        "tilstand": "TIL_INFOTRYGD"
                    }
                }
            ]
        }
    ],
    "skjemaVersjon": 61
} """.trimIndent()

