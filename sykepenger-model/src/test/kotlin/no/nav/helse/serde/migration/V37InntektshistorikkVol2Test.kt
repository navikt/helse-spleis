package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V37InntektshistorikkVol2Test {

    @Test
    fun `migrerer inntekt til inntektshistorikk`() {
        val resultat = listOf(V37InntektshistorikkVol2())
            .migrate(serdeObjectMapper.readTree(before))
        val expected = serdeObjectMapper.readTree(expected)
        assertEquals(expected, resultat)
    }

    @Test
    fun `kan migrere tom inntekt`() {
        val resultat = listOf(V37InntektshistorikkVol2())
            .migrate(serdeObjectMapper.readTree(tomInntekt))
        val expected = serdeObjectMapper.readTree(expectedTomInntekt)
        assertEquals(expected, resultat)
    }

    @Test
    fun `kan migrere ingen inntekt`() {
        val resultat = listOf(V37InntektshistorikkVol2())
            .migrate(serdeObjectMapper.readTree(ingenting))
        val expected = serdeObjectMapper.readTree(expectedIngenInntekt)
        assertEquals(expected, resultat)
    }
}

@Language("JSON")
private val before = """{
    "fnr": "fnr",
    "arbeidsgivere": [
        {
            "orgnummer": "orgnummer",
            "inntekter": [
                {
                    "fom": "2017-12-31",
                    "hendelseId": "6c4ff2a8-f634-47b8-a8d5-143708a2c40e",
                    "beløp": 31000.0,
                    "kilde": "INNTEKTSMELDING",
                    "tidsstempel": "2020-09-13T08:51:08.552656"
                },
                {
                    "fom": "2017-12-31",
                    "hendelseId": "6c4ff2a8-f634-47b8-a8d5-143708a2c40f",
                    "beløp": 30000.0,
                    "kilde": "INFOTRYGD",
                    "tidsstempel": "2020-09-15T08:51:08.552656"
                }
            ],
            "inntektshistorikk": [
                {
                    "inntektsopplysninger": [
                        {
                            "dato": "2018-01-01",
                            "hendelseId": "6c4ff2a8-f634-47b8-a8d5-143708a2c40e",
                            "beløp": 31000.0,
                            "kilde": "INNTEKTSMELDING",
                            "tidsstempel": "2020-09-15T08:51:08.554908"
                        },
                        {
                            "skatteopplysninger": [
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "daa2d0fa-0652-416b-91c1-4de83b800218",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2018-01",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn",
                                    "tidsstempel": "2020-09-15T08:51:08.569939"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "daa2d0fa-0652-416b-91c1-4de83b800218",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2018-02",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn",
                                    "tidsstempel": "2020-09-15T08:51:08.569939"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "daa2d0fa-0652-416b-91c1-4de83b800218",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2018-03",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn",
                                    "tidsstempel": "2020-09-15T08:51:08.569939"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "daa2d0fa-0652-416b-91c1-4de83b800218",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2018-04",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn",
                                    "tidsstempel": "2020-09-15T08:51:08.569939"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "daa2d0fa-0652-416b-91c1-4de83b800218",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2018-05",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn",
                                    "tidsstempel": "2020-09-15T08:51:08.569939"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "daa2d0fa-0652-416b-91c1-4de83b800218",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2018-06",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn",
                                    "tidsstempel": "2020-09-15T08:51:08.569939"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "daa2d0fa-0652-416b-91c1-4de83b800218",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2018-07",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn",
                                    "tidsstempel": "2020-09-15T08:51:08.569939"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "daa2d0fa-0652-416b-91c1-4de83b800218",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2018-08",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn",
                                    "tidsstempel": "2020-09-15T08:51:08.569939"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "daa2d0fa-0652-416b-91c1-4de83b800218",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2018-09",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn",
                                    "tidsstempel": "2020-09-15T08:51:08.569939"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "daa2d0fa-0652-416b-91c1-4de83b800218",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2018-10",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn",
                                    "tidsstempel": "2020-09-15T08:51:08.569939"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "daa2d0fa-0652-416b-91c1-4de83b800218",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2018-11",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn",
                                    "tidsstempel": "2020-09-15T08:51:08.569939"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "daa2d0fa-0652-416b-91c1-4de83b800218",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2018-12",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn",
                                    "tidsstempel": "2020-09-15T08:51:08.569939"
                                }
                            ]
                        },
                        {
                            "dato": "2016-01-01",
                            "hendelseId": "38dbdee3-183f-4358-a7a1-ee3bcc26a9c7",
                            "beløp": 25000.0,
                            "kilde": "INFOTRYGD",
                            "tidsstempel": "2020-09-15T08:51:08.586209"
                        }
                    ]
                },
                {
                    "inntektsopplysninger": [
                        {
                            "dato": "2018-01-01",
                            "hendelseId": "6c4ff2a8-f634-47b8-a8d5-143708a2c40e",
                            "beløp": 31000.0,
                            "kilde": "INNTEKTSMELDING",
                            "tidsstempel": "2020-09-15T08:51:08.554908"
                        },
                        {
                            "skatteopplysninger": [
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "daa2d0fa-0652-416b-91c1-4de83b800218",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2018-01",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn",
                                    "tidsstempel": "2020-09-15T08:51:08.569939"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "daa2d0fa-0652-416b-91c1-4de83b800218",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2018-02",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn",
                                    "tidsstempel": "2020-09-15T08:51:08.569939"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "daa2d0fa-0652-416b-91c1-4de83b800218",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2018-03",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn",
                                    "tidsstempel": "2020-09-15T08:51:08.569939"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "daa2d0fa-0652-416b-91c1-4de83b800218",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2018-04",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn",
                                    "tidsstempel": "2020-09-15T08:51:08.569939"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "daa2d0fa-0652-416b-91c1-4de83b800218",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2018-05",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn",
                                    "tidsstempel": "2020-09-15T08:51:08.569939"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "daa2d0fa-0652-416b-91c1-4de83b800218",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2018-06",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn",
                                    "tidsstempel": "2020-09-15T08:51:08.569939"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "daa2d0fa-0652-416b-91c1-4de83b800218",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2018-07",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn",
                                    "tidsstempel": "2020-09-15T08:51:08.569939"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "daa2d0fa-0652-416b-91c1-4de83b800218",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2018-08",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn",
                                    "tidsstempel": "2020-09-15T08:51:08.569939"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "daa2d0fa-0652-416b-91c1-4de83b800218",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2018-09",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn",
                                    "tidsstempel": "2020-09-15T08:51:08.569939"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "daa2d0fa-0652-416b-91c1-4de83b800218",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2018-10",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn",
                                    "tidsstempel": "2020-09-15T08:51:08.569939"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "daa2d0fa-0652-416b-91c1-4de83b800218",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2018-11",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn",
                                    "tidsstempel": "2020-09-15T08:51:08.569939"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "daa2d0fa-0652-416b-91c1-4de83b800218",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2018-12",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn",
                                    "tidsstempel": "2020-09-15T08:51:08.569939"
                                }
                            ]
                        }
                    ]
                },
                {
                    "inntektsopplysninger": [
                        {
                            "dato": "2018-01-01",
                            "hendelseId": "6c4ff2a8-f634-47b8-a8d5-143708a2c40e",
                            "beløp": 31000.0,
                            "kilde": "INNTEKTSMELDING",
                            "tidsstempel": "2020-09-15T08:51:08.554908"
                        }
                    ]
                }
            ]
        }
    ],
    "skjemaVersjon": 36
}
"""

@Language("JSON")
private val tomInntekt = """{
    "arbeidsgivere": [
        {
            "inntekter": [],
            "inntektshistorikk": [
                {
                    "inntektsopplysninger": [
                        {
                            "dato": "2018-01-01",
                            "hendelseId": "6c4ff2a8-f634-47b8-a8d5-143708a2c40e",
                            "beløp": 31000.0,
                            "kilde": "INNTEKTSMELDING",
                            "tidsstempel": "2020-09-15T08:51:08.554908"
                        }
                    ]
                }
            ]
        }
    ],
    "skjemaVersjon": 36
}
"""

@Language("JSON")
private val ingenting = """{
    "arbeidsgivere": [
        {}
    ],
    "skjemaVersjon": 36
}
"""

@Language("JSON")
private val expected = """{
    "fnr": "fnr",
    "arbeidsgivere": [
        {
            "orgnummer": "orgnummer",
            "inntekter": [
                {
                    "fom": "2017-12-31",
                    "hendelseId": "6c4ff2a8-f634-47b8-a8d5-143708a2c40e",
                    "beløp": 31000.0,
                    "kilde": "INNTEKTSMELDING",
                    "tidsstempel": "2020-09-13T08:51:08.552656"
                },
                {
                    "fom": "2017-12-31",
                    "hendelseId": "6c4ff2a8-f634-47b8-a8d5-143708a2c40f",
                    "beløp": 30000.0,
                    "kilde": "INFOTRYGD",
                    "tidsstempel": "2020-09-15T08:51:08.552656"
                }
            ],
            "inntektshistorikk": [
                {
                    "inntektsopplysninger": [
                        {
                            "hendelseId": "6c4ff2a8-f634-47b8-a8d5-143708a2c40e",
                            "beløp": 31000.0,
                            "kilde": "INNTEKTSMELDING",
                            "tidsstempel": "2020-09-13T08:51:08.552656",
                            "dato": "2017-12-31"
                        },
                        {
                            "hendelseId": "6c4ff2a8-f634-47b8-a8d5-143708a2c40f",
                            "beløp": 30000.0,
                            "kilde": "INFOTRYGD",
                            "tidsstempel": "2020-09-15T08:51:08.552656",
                            "dato": "2017-12-31"
                        }
                    ]
                },
                {
                    "inntektsopplysninger": [
                        {
                            "hendelseId": "6c4ff2a8-f634-47b8-a8d5-143708a2c40e",
                            "beløp": 31000.0,
                            "kilde": "INNTEKTSMELDING",
                            "tidsstempel": "2020-09-13T08:51:08.552656",
                            "dato": "2017-12-31"
                        }
                    ]
                }
            ]
        }
    ],
    "skjemaVersjon": 37
}
"""

@Language("JSON")
private val expectedTomInntekt = """{
    "arbeidsgivere": [
        {
            "inntekter": [],
            "inntektshistorikk": []
        }
    ],
    "skjemaVersjon": 37
}
"""

@Language("JSON")
private val expectedIngenInntekt = """{
    "arbeidsgivere": [
        {
            "inntektshistorikk": []
        }
    ],
    "skjemaVersjon": 37
}
"""
