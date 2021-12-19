package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V47BeregnetUtbetalingstidslinjerTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `kopierer utbetalingstidslinjer fra utbetalinger`() {
        val migrated = listOf(V47BeregnetUtbetalingstidslinjer())
            .migrate(objectMapper.readTree(originalJson()))
        val expected = objectMapper.readTree(expectedJson())

        assertEquals(expected, migrated)
    }
}

@Language("JSON")
private fun originalJson() =
    """
{
    "fødselsnummer": "fnr",
    "arbeidsgivere": [
        {
            "organisasjonsnummer": "ag1",
            "utbetalinger": [
                {
                    "tidsstempel": "2020-05-25T00:00:00.000",
                    "utbetalingstidslinje": {
                      "dager": [
                        {
                            "type": "ArbeidsgiverperiodeDag",
                            "dato": "2018-01-01",
                            "grad": 0.0,
                            "arbeidsgiverBetalingProsent": 100.0,
                            "dekningsgrunnlag": 1431.0,
                            "aktuellDagsinntekt": 1431.0,
                            "arbeidsgiverbeløp": 0,
                            "personbeløp": 0,
                            "er6GBegrenset": false
                        },
                        {
                            "type": "NavDag",
                            "dato": "2018-01-17",
                            "grad": 100.0,
                            "arbeidsgiverBetalingProsent": 100.0,
                            "dekningsgrunnlag": 1431.0,
                            "aktuellDagsinntekt": 1431.0,
                            "arbeidsgiverbeløp": 1431,
                            "personbeløp": 0,
                            "er6GBegrenset": false
                        }
                      ]
                    }
                },
                {
                    "tidsstempel": "2020-06-01T00:00:00.000",
                    "utbetalingstidslinje": {
                      "dager": [
                        {
                            "type": "NavDag",
                            "dato": "2020-06-01",
                            "grad": 100.0,
                            "arbeidsgiverBetalingProsent": 100.0,
                            "dekningsgrunnlag": 1431.0,
                            "aktuellDagsinntekt": 1431.0,
                            "arbeidsgiverbeløp": 1431,
                            "personbeløp": 0,
                            "er6GBegrenset": false
                        }
                      ]
                    }
                }
            ]
        }
    ],
    "skjemaVersjon": 46
}
"""

@Language("JSON")
private fun expectedJson() =
    """{
    "fødselsnummer": "fnr",
    "arbeidsgivere": [
        {
            "organisasjonsnummer": "ag1",
            "utbetalinger": [
                {
                    "tidsstempel": "2020-05-25T00:00:00.000",
                    "utbetalingstidslinje": {
                        "dager": [
                            {
                                "type": "ArbeidsgiverperiodeDag",
                                "dato": "2018-01-01",
                                "grad": 0.0,
                                "arbeidsgiverBetalingProsent": 100.0,
                                "dekningsgrunnlag": 1431.0,
                                "aktuellDagsinntekt": 1431.0,
                                "arbeidsgiverbeløp": 0,
                                "personbeløp": 0,
                                "er6GBegrenset": false
                            },
                            {
                                "type": "NavDag",
                                "dato": "2018-01-17",
                                "grad": 100.0,
                                "arbeidsgiverBetalingProsent": 100.0,
                                "dekningsgrunnlag": 1431.0,
                                "aktuellDagsinntekt": 1431.0,
                                "arbeidsgiverbeløp": 1431,
                                "personbeløp": 0,
                                "er6GBegrenset": false
                            }
                        ]
                    }
                },
                {
                    "tidsstempel": "2020-06-01T00:00:00.000",
                    "utbetalingstidslinje": {
                        "dager": [
                            {
                                "type": "NavDag",
                                "dato": "2020-06-01",
                                "grad": 100.0,
                                "arbeidsgiverBetalingProsent": 100.0,
                                "dekningsgrunnlag": 1431.0,
                                "aktuellDagsinntekt": 1431.0,
                                "arbeidsgiverbeløp": 1431,
                                "personbeløp": 0,
                                "er6GBegrenset": false
                            }
                        ]
                    }
                }
            ],
            "beregnetUtbetalingstidslinjer": [
                {
                    "organisasjonsnummer": "ag1",
                    "tidsstempel": "2020-05-25T00:00:00.000",
                    "utbetalingstidslinje": {
                        "dager": [
                            {
                                "type": "ArbeidsgiverperiodeDag",
                                "dato": "2018-01-01",
                                "grad": 0.0,
                                "arbeidsgiverBetalingProsent": 100.0,
                                "dekningsgrunnlag": 1431.0,
                                "aktuellDagsinntekt": 1431.0,
                                "arbeidsgiverbeløp": 0,
                                "personbeløp": 0,
                                "er6GBegrenset": false
                            },
                            {
                                "type": "NavDag",
                                "dato": "2018-01-17",
                                "grad": 100.0,
                                "arbeidsgiverBetalingProsent": 100.0,
                                "dekningsgrunnlag": 1431.0,
                                "aktuellDagsinntekt": 1431.0,
                                "arbeidsgiverbeløp": 1431,
                                "personbeløp": 0,
                                "er6GBegrenset": false
                            }
                        ]
                    }
                },
                {
                    "organisasjonsnummer": "ag1",
                    "tidsstempel": "2020-06-01T00:00:00.000",
                    "utbetalingstidslinje": {
                        "dager": [
                            {
                                "type": "NavDag",
                                "dato": "2020-06-01",
                                "grad": 100.0,
                                "arbeidsgiverBetalingProsent": 100.0,
                                "dekningsgrunnlag": 1431.0,
                                "aktuellDagsinntekt": 1431.0,
                                "arbeidsgiverbeløp": 1431,
                                "personbeløp": 0,
                                "er6GBegrenset": false
                            }
                        ]
                    }
                }
            ]
        }
    ],
    "skjemaVersjon": 47
}
"""
