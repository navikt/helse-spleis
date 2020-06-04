package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class V19KlippOverlappendeVedtaksperioderTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `klipper bort dager fra overlappende inntektsmelding`() {
        val json = objectMapper.readTree(overlappJson)
        listOf(V19KlippOverlappendeVedtaksperioder()).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())
        assertEquals(objectMapper.readTree(expectedOverlappJson), migratedJson)
    }

    @Test
    fun `ikke-overlappende vedtaksperioder forblir uendret`() {
        val json = objectMapper.readTree(ingenOverlappJson)
        listOf(V19KlippOverlappendeVedtaksperioder()).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())
        assertEquals(objectMapper.readTree(expectedIngenOverlappJson), migratedJson)
    }

    @Test
    fun `kan migrere tidslinje med tom historisk tidslinje`() {
        val json = objectMapper.readTree(overlappMedGammelTomHistorikkJson)
        listOf(V19KlippOverlappendeVedtaksperioder()).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())
        assertEquals(objectMapper.readTree(expectedOverlappMedTomHistorikkJson), migratedJson)
    }

    @Test
    fun `tryner på migrering med tom historikk`() {
        val json = objectMapper.readTree(vedtaksperiodeMedHeltTomHistorikkJson)
        assertThrows<IllegalStateException> { listOf(V19KlippOverlappendeVedtaksperioder()).migrate(json) }
    }

    @Test
    fun `fjerner ikke perioder som overlapper på søknad`() {
        val json = objectMapper.readTree(overlappSøknadJson)
        listOf(V19KlippOverlappendeVedtaksperioder()).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())
        assertEquals(objectMapper.readTree(expectedOverlappSøknadJson), migratedJson)
    }
}

@Language("JSON")
private val overlappSøknadJson = """{
    "arbeidsgivere": [
        {
            "vedtaksperioder": [
                {
                    "id": "uuid-1",
                    "sykdomshistorikk": [
                        {
                            "hendelseSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-08", "kilde": { "type": "Søknad" } },
                                    { "dato": "2020-04-09", "kilde": { "type": "Søknad" } }
                                ]
                            },
                            "beregnetSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-08", "kilde": { "type": "Søknad" } },
                                    { "dato": "2020-04-09", "kilde": { "type": "Søknad" } }
                                ]
                            }
                        }
                    ]
                }, {
                    "id": "uuid-2",
                    "sykdomshistorikk": [
                        {
                            "hendelseSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-08", "kilde": { "type": "Søknad" }},
                                    { "dato": "2020-04-09", "kilde": { "type": "Søknad" } },
                                    { "dato": "2020-04-10", "kilde": { "type": "Søknad" } }
                                ]
                            },
                            "beregnetSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-08", "kilde": { "type": "Søknad" } },
                                    { "dato": "2020-04-09", "kilde": { "type": "Søknad" } },
                                    { "dato": "2020-04-10", "kilde": { "type": "Søknad" } }
                                ]
                            }
                        }
                    ]
                }
            ]
        }
    ]
}
"""

@Language("JSON")
private val expectedOverlappSøknadJson = """{
    "skjemaVersjon": 19,
    "arbeidsgivere": [
        {
            "vedtaksperioder": [
                {
                    "id": "uuid-1",
                    "sykdomshistorikk": [
                        {
                            "hendelseSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-08", "kilde": { "type": "Søknad" } },
                                    { "dato": "2020-04-09", "kilde": { "type": "Søknad" } }
                                ]
                            },
                            "beregnetSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-08", "kilde": { "type": "Søknad" } },
                                    { "dato": "2020-04-09", "kilde": { "type": "Søknad" } }
                                ]
                            }
                        }
                    ]
                }, {
                    "id": "uuid-2",
                    "sykdomshistorikk": [
                        {
                            "hendelseSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-08", "kilde": { "type": "Søknad" }},
                                    { "dato": "2020-04-09", "kilde": { "type": "Søknad" } },
                                    { "dato": "2020-04-10", "kilde": { "type": "Søknad" } }
                                ]
                            },
                            "beregnetSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-08", "kilde": { "type": "Søknad" } },
                                    { "dato": "2020-04-09", "kilde": { "type": "Søknad" } },
                                    { "dato": "2020-04-10", "kilde": { "type": "Søknad" } }
                                ]
                            }
                        }
                    ]
                }
            ]
        }
    ]
}
"""

@Language("JSON")
private val overlappJson = """{
    "arbeidsgivere": [
        {
            "vedtaksperioder": [
                {
                    "id": "uuid-1",
                    "sykdomshistorikk": [
                        {
                            "hendelseSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-08", "kilde": { "type": "Søknad" } },
                                    { "dato": "2020-04-09", "kilde": { "type": "Søknad" } }
                                ]
                            },
                            "beregnetSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-08", "kilde": { "type": "Søknad" } },
                                    { "dato": "2020-04-09", "kilde": { "type": "Søknad" } }
                                ]
                            }
                        }
                    ]
                }, {
                    "id": "uuid-2",
                    "sykdomshistorikk": [
                        {
                            "hendelseSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-08", "kilde": { "type": "Inntektsmelding" } },
                                    { "dato": "2020-04-09", "kilde": { "type": "Inntektsmelding" } },
                                    { "dato": "2020-04-10", "kilde": { "type": "Inntektsmelding" } }
                                ]
                            },
                            "beregnetSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-08", "kilde": { "type": "Inntektsmelding" } },
                                    { "dato": "2020-04-09", "kilde": { "type": "Inntektsmelding" } },
                                    { "dato": "2020-04-10", "kilde": { "type": "Inntektsmelding" } }
                                ]
                            }
                        }
                    ]
                }
            ]
        }
    ]
}
"""

@Language("JSON")
private val expectedOverlappJson = """{
    "skjemaVersjon": 19,
    "arbeidsgivere": [
        {
            "vedtaksperioder": [
                {
                    "id": "uuid-1",
                    "sykdomshistorikk": [
                        {
                            "hendelseSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-08", "kilde": { "type": "Søknad" } },
                                    { "dato": "2020-04-09", "kilde": { "type": "Søknad" } }
                                ]
                            },
                            "beregnetSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-08", "kilde": { "type": "Søknad" } },
                                    { "dato": "2020-04-09", "kilde": { "type": "Søknad" } }
                                ]
                            }
                        }
                    ]
                }, {
                    "id": "uuid-2",
                    "sykdomshistorikk": [
                        {
                            "hendelseSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-10", "kilde": { "type": "Inntektsmelding" } }
                                ]
                            },
                            "beregnetSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-10", "kilde": { "type": "Inntektsmelding" } }
                                ]
                            }
                        }
                    ]
                }
            ]
        }
    ]
}
"""

@Language("JSON")
private val ingenOverlappJson = """{
    "arbeidsgivere": [
        {
            "vedtaksperioder": [
                {
                    "id": "uuid-1",
                    "sykdomshistorikk": [
                        {
                            "hendelseSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-07", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-08", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-09", "kilde": { "type":  "Inntektsmelding" } }
                                ]
                            },
                            "beregnetSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-07", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-08", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-09", "kilde": { "type":  "Inntektsmelding" } }
                                ]
                            }
                        }
                    ]
                },
                {
                    "id": "uuid-2",
                    "sykdomshistorikk": [
                        {
                            "hendelseSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-10", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-11", "kilde": { "type":  "Inntektsmelding" } }
                                ]
                            },
                            "beregnetSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-10", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-11", "kilde": { "type":  "Inntektsmelding" } }
                                ]
                            }
                        }
                    ]
                }
            ]
        }
    ]
}
"""

@Language("JSON")
private val expectedIngenOverlappJson = """{
    "skjemaVersjon": 19,
    "arbeidsgivere": [
        {
            "vedtaksperioder": [
                {
                    "id": "uuid-1",
                    "sykdomshistorikk": [
                        {
                            "hendelseSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-07", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-08", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-09", "kilde": { "type":  "Inntektsmelding" } }
                                ]
                            },
                            "beregnetSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-07", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-08", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-09", "kilde": { "type":  "Inntektsmelding" } }
                                ]
                            }
                        }
                    ]
                },
                {
                    "id": "uuid-2",
                    "sykdomshistorikk": [
                        {
                            "hendelseSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-10", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-11", "kilde": { "type":  "Inntektsmelding" } }
                                ]
                            },
                            "beregnetSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-10", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-11", "kilde": { "type":  "Inntektsmelding" } }
                                ]
                            }
                        }
                    ]
                }
            ]
        }
    ]
}
"""

@Language("JSON")
private val overlappMedGammelTomHistorikkJson = """{
    "arbeidsgivere": [
        {
            "vedtaksperioder": [
                {
                    "id": "uuid-1",
                    "sykdomshistorikk": [
                        {
                            "hendelseSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-08", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-09", "kilde": { "type":  "Inntektsmelding" } }
                                ]
                            },
                            "beregnetSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-08", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-09", "kilde": { "type":  "Inntektsmelding" } }
                                ]
                            }
                        }
                    ]
                }, {
                    "id": "uuid-2",
                    "sykdomshistorikk": [
                        {
                            "hendelseSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-08", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-09", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-10", "kilde": { "type":  "Inntektsmelding" } }
                                ]
                            },
                            "beregnetSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-08", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-09", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-10", "kilde": { "type":  "Inntektsmelding" } }
                                ]
                            }
                        },
                        {
                            "hendelseSykdomstidslinje": {
                                "dager": []
                            },
                            "beregnetSykdomstidslinje": {
                                "dager": []
                            }
                        }
                    ]
                },
                {
                    "id": "uuid-3",
                    "sykdomshistorikk": [
                        {
                            "hendelseSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-08", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-09", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-10", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-11", "kilde": { "type":  "Inntektsmelding" } }
                                ]
                            },
                            "beregnetSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-08", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-09", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-10", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-11", "kilde": { "type":  "Inntektsmelding" } }
                                ]
                            }
                        }
                    ]
                }
            ]
        }
    ]
}
"""

@Language("JSON")
private val expectedOverlappMedTomHistorikkJson = """{
    "skjemaVersjon": 19,
    "arbeidsgivere": [
        {
            "vedtaksperioder": [
                {
                    "id": "uuid-1",
                    "sykdomshistorikk": [
                        {
                            "hendelseSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-08", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-09", "kilde": { "type":  "Inntektsmelding" } }
                                ]
                            },
                            "beregnetSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-08", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-09", "kilde": { "type":  "Inntektsmelding" } }
                                ]
                            }
                        }
                    ]
                }, {
                    "id": "uuid-2",
                    "sykdomshistorikk": [
                        {
                            "hendelseSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-10", "kilde": { "type":  "Inntektsmelding" } }
                                ]
                            },
                            "beregnetSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-10", "kilde": { "type":  "Inntektsmelding" } }
                                ]
                            }
                        },
                        {
                            "hendelseSykdomstidslinje": {
                                "dager": []
                            },
                            "beregnetSykdomstidslinje": {
                                "dager": []
                            }
                        }
                    ]
                },
                {
                    "id": "uuid-3",
                    "sykdomshistorikk": [
                        {
                            "hendelseSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-11", "kilde": { "type":  "Inntektsmelding" } }
                                ]
                            },
                            "beregnetSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-11", "kilde": { "type":  "Inntektsmelding" } }
                                ]
                            }
                        }
                    ]
                }
            ]
        }
    ]
}
"""

@Language("JSON")
private val vedtaksperiodeMedHeltTomHistorikkJson = """{
    "arbeidsgivere": [
        {
            "vedtaksperioder": [
                {
                    "id": "uuid-1",
                    "sykdomshistorikk": [
                        {
                            "hendelseSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-08", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-09", "kilde": { "type":  "Inntektsmelding" } }
                                ]
                            },
                            "beregnetSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-08", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-09", "kilde": { "type":  "Inntektsmelding" } }
                                ]
                            }
                        }
                    ]
                }, {
                    "id": "uuid-2",
                    "sykdomshistorikk": [
                        {
                            "hendelseSykdomstidslinje": {
                                "dager": []
                            },
                            "beregnetSykdomstidslinje": {
                                "dager": []
                            }
                        }
                    ]
                },
                {
                    "id": "uuid-3",
                    "sykdomshistorikk": [
                        {
                            "hendelseSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-08", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-09", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-10", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-11", "kilde": { "type":  "Inntektsmelding" } }
                                ]
                            },
                            "beregnetSykdomstidslinje": {
                                "dager": [
                                    { "dato": "2020-04-08", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-09", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-10", "kilde": { "type":  "Inntektsmelding" } },
                                    { "dato": "2020-04-11", "kilde": { "type":  "Inntektsmelding" } }
                                ]
                            }
                        }
                    ]
                }
            ]
        }
    ]
}
"""
