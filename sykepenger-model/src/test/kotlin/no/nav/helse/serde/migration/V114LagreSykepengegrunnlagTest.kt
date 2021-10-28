package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V114LagreSykepengegrunnlagTest {

    @Test
    fun `Sykepengegrunnlag lagres riktig for flere arbeidsgivere, en med inntektsmelding og med inntekter fra skatt`() {
        assertEquals(toNode(expected), migrer(original))
    }

    @Test
    fun `Sykepengegrunnlag lagres riktig for person med IT-historikk`() {
        assertEquals(toNode(personOvergangFraITExpected), migrer(personOvergangFraITOriginal))
    }

    @Test
    fun `Én arbeidsgiver med IT-historikk og én med skatteopplysninger - vilkårsgrunnlaget skal kun legge IT-historikken til grunn ved Infotrygd-vilkårsgrunnlag`() {
        assertEquals(toNode(personOvergangFraITMedSkatteopplysningerExpected), migrer(personOvergangFraITMedSkatteopplysningerOriginal))
    }

    @Test
    fun `Migrerer riktig for overgang fra infotrygd med dato ulikt skjæringstidspunkt og manglende inntektsopplysning for vilkårsgrunnlag`() {
        assertEquals(toNode(personMedRartSkjæringstidspunktFraITExpected), migrer(personMedRartSkjæringstidspunktFraITOriginal))
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V114LagreSykepengegrunnlag()).migrate(toNode(json))

    @Language("JSON")
    private val personMedRartSkjæringstidspunktFraITOriginal = """{
        "fødselsnummer": "04206913337",
        "arbeidsgivere": [
            {
                "organisasjonsnummer": "987654321",
                "inntektshistorikk": [
                    {
                        "id": "4e44b7a8-19bd-4ead-8fda-c05ce643d8f8",
                        "inntektsopplysninger": [
                            {
                                "id": "e7decc12-507b-4b57-ba87-5d5ae012752d",
                                "dato": "2017-12-01",
                                "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99a01",
                                "beløp": 31000.0,
                                "kilde": "INFOTRYGD",
                                "tidsstempel": "2021-08-25T14:50:58.248396"
                            },
                            {
                            "id": "fc8e7179-5d63-46ce-a5bf-ec42313ff123",
                            "skatteopplysninger": [
                                {
                                    "dato": "2016-12-05",
                                    "hendelseId": "ae957631-0f86-4703-907c-320950c96aaa",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.395239",
                                    "måned": "2016-11",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "juicy fordel",
                                    "beskrivelse": "juicy beskrivelse"
                                },
                                {
                                    "dato": "2016-12-05",
                                    "hendelseId": "ae957631-0f86-4703-907c-320950c96aaa",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.395239",
                                    "måned": "2016-10",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "juicy fordel",
                                    "beskrivelse": "juicy beskrivelse"
                                },
                                {
                                    "dato": "2016-12-05",
                                    "hendelseId": "ae957631-0f86-4703-907c-320950c96aaa",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.395239",
                                    "måned": "2016-09",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "juicy fordel",
                                    "beskrivelse": "juicy beskrivelse"
                                }
                            ]
                        }
                        ]
                    }
                ]
            }
        ],
        "vilkårsgrunnlagHistorikk": [
            {
                "id": "dfa125bc-a0f1-454a-90bb-bf4f6d4068c2",
                "opprettet": "2021-08-25T14:50:58.252058",
                "vilkårsgrunnlag": [
                    {
                        "skjæringstidspunkt": "2017-12-01",
                        "type": "Infotrygd"
                    },
                    {
                        "skjæringstidspunkt": "2016-12-01",
                        "type": "Infotrygd"
                    }
                ]
            }
        ],
        "skjemaVersjon": 113
    }
    """

    @Language("JSON")
    private val personMedRartSkjæringstidspunktFraITExpected = """{
    "fødselsnummer": "04206913337",
    "arbeidsgivere": [
        {
            "organisasjonsnummer": "987654321",
            "inntektshistorikk": [
                {
                    "id": "4e44b7a8-19bd-4ead-8fda-c05ce643d8f8",
                    "inntektsopplysninger": [
                        {
                            "id": "e7decc12-507b-4b57-ba87-5d5ae012752d",
                            "dato": "2017-12-01",
                            "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99a01",
                            "beløp": 31000.0,
                            "kilde": "INFOTRYGD",
                            "tidsstempel": "2021-08-25T14:50:58.248396"
                        },
                        {
                            "id": "fc8e7179-5d63-46ce-a5bf-ec42313ff123",
                            "skatteopplysninger": [
                                {
                                    "dato": "2016-12-05",
                                    "hendelseId": "ae957631-0f86-4703-907c-320950c96aaa",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.395239",
                                    "måned": "2016-11",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "juicy fordel",
                                    "beskrivelse": "juicy beskrivelse"
                                },
                                {
                                    "dato": "2016-12-05",
                                    "hendelseId": "ae957631-0f86-4703-907c-320950c96aaa",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.395239",
                                    "måned": "2016-10",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "juicy fordel",
                                    "beskrivelse": "juicy beskrivelse"
                                },
                                {
                                    "dato": "2016-12-05",
                                    "hendelseId": "ae957631-0f86-4703-907c-320950c96aaa",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.395239",
                                    "måned": "2016-09",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "juicy fordel",
                                    "beskrivelse": "juicy beskrivelse"
                                }
                            ]
                        }
                    ]
                }
            ]
        }
    ],
    "vilkårsgrunnlagHistorikk": [
        {
            "id": "dfa125bc-a0f1-454a-90bb-bf4f6d4068c2",
            "opprettet": "2021-08-25T14:50:58.252058",
            "vilkårsgrunnlag": [
                {
                    "skjæringstidspunkt": "2017-12-01",
                    "type": "Infotrygd",
                    "sykepengegrunnlag": {
                        "sykepengegrunnlag": 372000.0,
                        "grunnlagForSykepengegrunnlag": 372000.0,
                        "arbeidsgiverInntektsopplysninger": [
                            {
                                "orgnummer": "987654321",
                                "inntektsopplysning": {
                                    "id": "e7decc12-507b-4b57-ba87-5d5ae012752d",
                                    "dato": "2017-12-01",
                                    "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99a01",
                                    "beløp": 31000.0,
                                    "kilde": "INFOTRYGD",
                                    "tidsstempel": "2021-08-25T14:50:58.248396"
                                }
                            }
                        ]
                    }
                },
                {
                    "skjæringstidspunkt": "2016-12-01",
                    "type": "Infotrygd",
                    "sykepengegrunnlag": {
                        "sykepengegrunnlag": 0.0,
                        "grunnlagForSykepengegrunnlag": 0.0,
                        "arbeidsgiverInntektsopplysninger": []
                    }
                }
            ]
        }
    ],
    "skjemaVersjon": 114
}
    """

    @Language("JSON")
    private val personOvergangFraITOriginal = """{
    "fødselsnummer": "04206913337",
    "arbeidsgivere": [
        {
            "organisasjonsnummer": "987654321",
            "inntektshistorikk": [
                {
                    "id": "4e44b7a8-19bd-4ead-8fda-c05ce643d8f8",
                    "inntektsopplysninger": [
                        {
                            "id": "e7decc12-507b-4b57-ba87-5d5ae012752d",
                            "dato": "2017-12-01",
                            "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99a01",
                            "beløp": 31000.0,
                            "kilde": "INFOTRYGD",
                            "tidsstempel": "2021-08-25T14:50:58.248396"
                        },
                        {
                            "id": "e7decc12-507b-4b57-ba87-5d5ae0127aaa",
                            "dato": "2016-12-17",
                            "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99aaa",
                            "beløp": 32000.0,
                            "kilde": "INFOTRYGD",
                            "tidsstempel": "2021-08-25T14:50:58.248396"
                        }
                    ]
                }
            ]
        }
    ],
    "vilkårsgrunnlagHistorikk": [
        {
            "id": "dfa125bc-a0f1-454a-90bb-bf4f6d4068c2",
            "opprettet": "2021-08-25T14:50:58.252058",
            "vilkårsgrunnlag": [
                {
                    "skjæringstidspunkt": "2017-12-01",
                    "type": "Infotrygd"
                },
                {
                    "skjæringstidspunkt": "2016-12-01",
                    "type": "Infotrygd"
                }
            ]
        }
    ],
    "skjemaVersjon": 113
}
    """

    @Language("JSON")
    private val personOvergangFraITExpected = """{
    "fødselsnummer": "04206913337",
    "arbeidsgivere": [
        {
            "organisasjonsnummer": "987654321",
            "inntektshistorikk": [
                {
                    "id": "4e44b7a8-19bd-4ead-8fda-c05ce643d8f8",
                    "inntektsopplysninger": [
                        {
                            "id": "e7decc12-507b-4b57-ba87-5d5ae012752d",
                            "dato": "2017-12-01",
                            "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99a01",
                            "beløp": 31000.0,
                            "kilde": "INFOTRYGD",
                            "tidsstempel": "2021-08-25T14:50:58.248396"
                        },
                        {
                            "id": "e7decc12-507b-4b57-ba87-5d5ae0127aaa",
                            "dato": "2016-12-17",
                            "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99aaa",
                            "beløp": 32000.0,
                            "kilde": "INFOTRYGD",
                            "tidsstempel": "2021-08-25T14:50:58.248396"
                        }
                    ]
                }
            ]
        }
    ],
    "vilkårsgrunnlagHistorikk": [
        {
            "id": "dfa125bc-a0f1-454a-90bb-bf4f6d4068c2",
            "opprettet": "2021-08-25T14:50:58.252058",
            "vilkårsgrunnlag": [
                {
                    "skjæringstidspunkt": "2017-12-01",
                    "type": "Infotrygd",
                    "sykepengegrunnlag": {
                        "sykepengegrunnlag": 372000.0,
                        "grunnlagForSykepengegrunnlag": 372000.0,
                        "arbeidsgiverInntektsopplysninger": [
                            {
                                "orgnummer": "987654321",
                                "inntektsopplysning": {
                                    "id": "e7decc12-507b-4b57-ba87-5d5ae012752d",
                                    "dato": "2017-12-01",
                                    "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99a01",
                                    "beløp": 31000.0,
                                    "kilde": "INFOTRYGD",
                                    "tidsstempel": "2021-08-25T14:50:58.248396"
                                }
                            }
                        ]
                    }
                },
                {
                    "skjæringstidspunkt": "2016-12-01",
                    "type": "Infotrygd",
                    "sykepengegrunnlag": {
                        "sykepengegrunnlag": 384000.0,
                        "grunnlagForSykepengegrunnlag": 384000.0,
                        "arbeidsgiverInntektsopplysninger": [
                            {
                                "orgnummer": "987654321",
                                "inntektsopplysning": {
                                    "id": "e7decc12-507b-4b57-ba87-5d5ae0127aaa",
                                    "dato": "2016-12-17",
                                    "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99aaa",
                                    "beløp": 32000.0,
                                    "kilde": "INFOTRYGD",
                                    "tidsstempel": "2021-08-25T14:50:58.248396"
                                }
                            }
                        ]
                    }
                }
            ]
        }
    ],
    "skjemaVersjon": 114
}
    """

    @Language("JSON")
    private val personOvergangFraITMedSkatteopplysningerOriginal = """{
    "fødselsnummer": "04206913337",
    "arbeidsgivere": [
        {
            "organisasjonsnummer": "987654321",
            "inntektshistorikk": [
                {
                    "id": "4e44b7a8-19bd-4ead-8fda-c05ce643d8f8",
                    "inntektsopplysninger": [
                        {
                            "id": "e7decc12-507b-4b57-ba87-5d5ae012752d",
                            "dato": "2017-12-01",
                            "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99a01",
                            "beløp": 31000.0,
                            "kilde": "INFOTRYGD",
                            "tidsstempel": "2021-08-25T14:50:58.248396"
                        },
                        {
                            "id": "e7decc12-507b-4b57-ba87-5d5ae0127aaa",
                            "dato": "2016-12-17",
                            "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99aaa",
                            "beløp": 32000.0,
                            "kilde": "INFOTRYGD",
                            "tidsstempel": "2021-08-25T14:50:58.248396"
                        }
                    ]
                }
            ]
        },
        {
            "organisasjonsnummer": "654321987",
            "inntektshistorikk": [
                {
                    "id": "4e44b7a8-19bd-4ead-8fda-c05ce643d8f9",
                    "inntektsopplysninger": [
                        {
                            "id": "fc8e7179-5d63-46ce-a5bf-ec42313ff124",
                            "skatteopplysninger": [
                                {
                                    "dato": "2017-12-01",
                                    "hendelseId": "ae957631-0f86-4703-907c-320950c96aaa",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.395239",
                                    "måned": "2017-11",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "juicy fordel",
                                    "beskrivelse": "juicy beskrivelse"
                                },
                                {
                                    "dato": "2017-12-01",
                                    "hendelseId": "ae957631-0f86-4703-907c-320950c96aaa",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.395239",
                                    "måned": "2017-10",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "juicy fordel",
                                    "beskrivelse": "juicy beskrivelse"
                                },
                                {
                                    "dato": "2017-12-01",
                                    "hendelseId": "ae957631-0f86-4703-907c-320950c96aaa",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.395239",
                                    "måned": "2017-09",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "juicy fordel",
                                    "beskrivelse": "juicy beskrivelse"
                                }
                            ]
                        }
                    ]
                }
            ]
        }
    ],
    "vilkårsgrunnlagHistorikk": [
        {
            "id": "dfa125bc-a0f1-454a-90bb-bf4f6d4068c2",
            "opprettet": "2021-08-25T14:50:58.252058",
            "vilkårsgrunnlag": [
                {
                    "skjæringstidspunkt": "2017-12-01",
                    "type": "Infotrygd"
                },
                {
                    "skjæringstidspunkt": "2016-12-01",
                    "type": "Infotrygd"
                }
            ]
        }
    ],
    "skjemaVersjon": 113
}
    """

    @Language("JSON")
    private val personOvergangFraITMedSkatteopplysningerExpected = """{
    "fødselsnummer": "04206913337",
    "arbeidsgivere": [
        {
            "organisasjonsnummer": "987654321",
            "inntektshistorikk": [
                {
                    "id": "4e44b7a8-19bd-4ead-8fda-c05ce643d8f8",
                    "inntektsopplysninger": [
                        {
                            "id": "e7decc12-507b-4b57-ba87-5d5ae012752d",
                            "dato": "2017-12-01",
                            "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99a01",
                            "beløp": 31000.0,
                            "kilde": "INFOTRYGD",
                            "tidsstempel": "2021-08-25T14:50:58.248396"
                        },
                        {
                            "id": "e7decc12-507b-4b57-ba87-5d5ae0127aaa",
                            "dato": "2016-12-17",
                            "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99aaa",
                            "beløp": 32000.0,
                            "kilde": "INFOTRYGD",
                            "tidsstempel": "2021-08-25T14:50:58.248396"
                        }
                    ]
                }
            ]
        },
        {
            "organisasjonsnummer": "654321987",
            "inntektshistorikk": [
                {
                    "id": "4e44b7a8-19bd-4ead-8fda-c05ce643d8f9",
                    "inntektsopplysninger": [
                        {
                            "id": "fc8e7179-5d63-46ce-a5bf-ec42313ff124",
                            "skatteopplysninger": [
                                {
                                    "dato": "2017-12-01",
                                    "hendelseId": "ae957631-0f86-4703-907c-320950c96aaa",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.395239",
                                    "måned": "2017-11",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "juicy fordel",
                                    "beskrivelse": "juicy beskrivelse"
                                },
                                {
                                    "dato": "2017-12-01",
                                    "hendelseId": "ae957631-0f86-4703-907c-320950c96aaa",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.395239",
                                    "måned": "2017-10",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "juicy fordel",
                                    "beskrivelse": "juicy beskrivelse"
                                },
                                {
                                    "dato": "2017-12-01",
                                    "hendelseId": "ae957631-0f86-4703-907c-320950c96aaa",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.395239",
                                    "måned": "2017-09",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "juicy fordel",
                                    "beskrivelse": "juicy beskrivelse"
                                }
                            ]
                        }
                    ]
                }
            ]
        }
    ],
    "vilkårsgrunnlagHistorikk": [
        {
            "id": "dfa125bc-a0f1-454a-90bb-bf4f6d4068c2",
            "opprettet": "2021-08-25T14:50:58.252058",
            "vilkårsgrunnlag": [
                {
                    "skjæringstidspunkt": "2017-12-01",
                    "type": "Infotrygd",
                    "sykepengegrunnlag": {
                        "sykepengegrunnlag": 372000.0,
                        "grunnlagForSykepengegrunnlag": 372000.0,
                        "arbeidsgiverInntektsopplysninger": [
                            {
                                "orgnummer": "987654321",
                                "inntektsopplysning": {
                                    "id": "e7decc12-507b-4b57-ba87-5d5ae012752d",
                                    "dato": "2017-12-01",
                                    "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99a01",
                                    "beløp": 31000.0,
                                    "kilde": "INFOTRYGD",
                                    "tidsstempel": "2021-08-25T14:50:58.248396"
                                }
                            }
                        ]
                    }
                },
                {
                    "skjæringstidspunkt": "2016-12-01",
                    "type": "Infotrygd",
                    "sykepengegrunnlag": {
                        "sykepengegrunnlag": 384000.0,
                        "grunnlagForSykepengegrunnlag": 384000.0,
                        "arbeidsgiverInntektsopplysninger": [
                            {
                                "orgnummer": "987654321",
                                "inntektsopplysning": {
                                    "id": "e7decc12-507b-4b57-ba87-5d5ae0127aaa",
                                    "dato": "2016-12-17",
                                    "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99aaa",
                                    "beløp": 32000.0,
                                    "kilde": "INFOTRYGD",
                                    "tidsstempel": "2021-08-25T14:50:58.248396"
                                }
                            }
                        ]
                    }
                }
            ]
        }
    ],
    "skjemaVersjon": 114
}
    """

    @Language("JSON")
    private val original = """{
        "aktørId": "12345",
        "fødselsnummer": "12020052345",
        "opprettet": "2021-08-24T13:33:13.058378",
        "dødsdato": null,
        "arbeidsgivere": [
            {
                "organisasjonsnummer": "987654321",
                "inntektshistorikk": [
                    {
                        "id": "e661625f-c9f2-4fe4-a510-03ec7f594567",
                        "inntektsopplysninger": [
                            {
                                "id": "8042d1e5-2524-4c67-9f39-cfbbb6f44d65",
                                "skatteopplysninger": [
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664fff",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2018-06",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664fff",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2018-07",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664fff",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2018-08",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664fff",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2018-09",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664fff",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2018-10",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664fff",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2018-11",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664fff",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2018-12",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664fff",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2019-01",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664fff",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2019-02",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664fff",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2019-03",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664fff",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2019-04",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664fff",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2019-05",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    }
                                ]
                            },
                            {
                                "id": "fc8e7179-5d63-46ce-a5bf-ec42313ff123",
                                "skatteopplysninger": [
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "ae957631-0f86-4703-907c-320950c96aaa",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.395239",
                                        "måned": "2019-05",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "juicy fordel",
                                        "beskrivelse": "juicy beskrivelse"
                                    },
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "ae957631-0f86-4703-907c-320950c96aaa",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.395239",
                                        "måned": "2019-04",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "juicy fordel",
                                        "beskrivelse": "juicy beskrivelse"
                                    },
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "ae957631-0f86-4703-907c-320950c96aaa",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.395239",
                                        "måned": "2019-03",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "juicy fordel",
                                        "beskrivelse": "juicy beskrivelse"
                                    }
                                ]
                            },
                            {
                                "id": "15abfcab-d6ae-428d-9b9f-4d0445cabcde",
                                "dato": "2019-06-01",
                                "hendelseId": "518938a9-a856-4c3a-a238-a5fe4f4abcde",
                                "beløp": 31000.0,
                                "kilde": "INNTEKTSMELDING",
                                "tidsstempel": "2021-08-25T13:33:13.364413"
                            },
                            {
                                "id": "15abfcab-d6ae-428d-9b9f-4d0445cea479",
                                "dato": "2018-01-01",
                                "hendelseId": "518938a9-a856-4c3a-a238-a5fe4f4020d3",
                                "beløp": 31000.0,
                                "kilde": "INNTEKTSMELDING",
                                "tidsstempel": "2021-08-24T13:33:13.364413"
                            },
                            {
                                "id": "fc8e7179-5d63-46ce-a5bf-ec42313ff403",
                                "skatteopplysninger": [
                                    {
                                        "dato": "2018-01-01",
                                        "hendelseId": "ae957631-0f86-4703-907c-320950c965b0",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.395239",
                                        "måned": "2017-10",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "juicy fordel",
                                        "beskrivelse": "juicy beskrivelse"
                                    },
                                    {
                                        "dato": "2018-01-01",
                                        "hendelseId": "ae957631-0f86-4703-907c-320950c965b0",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.395239",
                                        "måned": "2017-11",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "juicy fordel",
                                        "beskrivelse": "juicy beskrivelse"
                                    },
                                    {
                                        "dato": "2018-01-01",
                                        "hendelseId": "ae957631-0f86-4703-907c-320950c965b0",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.395239",
                                        "måned": "2017-12",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "juicy fordel",
                                        "beskrivelse": "juicy beskrivelse"
                                    },
                                    {
                                        "dato": "2018-01-01",
                                        "hendelseId": "ae957631-0f86-4703-907c-320950c965b0",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.395239",
                                        "måned": "2018-01",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "juicy fordel",
                                        "beskrivelse": "juicy beskrivelse"
                                    }
                                ]
                            },
                            {
                                "id": "8042d1e5-2524-4c67-9f39-cf4e06f44d65",
                                "skatteopplysninger": [
                                    {
                                        "dato": "2018-01-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664ff9",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2017-01",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2018-01-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664ff9",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2017-02",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2018-01-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664ff9",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2017-03",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2018-01-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664ff9",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2017-04",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2018-01-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664ff9",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2017-05",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2018-01-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664ff9",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2017-06",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2018-01-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664ff9",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2017-07",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2018-01-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664ff9",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2017-08",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2018-01-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664ff9",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2017-09",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2018-01-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664ff9",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2017-10",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2018-01-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664ff9",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2017-11",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2018-01-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664ff9",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2017-12",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    }
                                ]
                            }
                        ]
                    }
                ]
            },
             {
                "organisasjonsnummer": "999999999",
                "inntektshistorikk": [
                    {
                        "id": "e661625f-c9f2-4fe4-a510-03ec7f594eee",
                        "inntektsopplysninger": [
                            {
                                "id": "8042d1e5-2524-4c67-9f39-cfbbb6f44aba",
                                "skatteopplysninger": [
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664cdc",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2018-06",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664cdc",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2018-07",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664cdc",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2018-08",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664cdc",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2018-09",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664cdc",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2018-10",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664cdc",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2018-11",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664cdc",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2018-12",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664cdc",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2019-01",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664cdc",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2019-02",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664cdc",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2019-03",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664cdc",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2019-04",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    },
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664cdc",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.455787",
                                        "måned": "2019-05",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "kontantytelse",
                                        "beskrivelse": "fastloenn"
                                    }
                                ]
                            },
                            {
                                "id": "15abfcab-d6ae-428d-9b9f-4d0445cab999",
                                "dato": "2019-06-02",
                                "hendelseId": "518938a9-a856-4c3a-a238-a5fe4f4ab999",
                                "beløp": 31000.0,
                                "kilde": "INNTEKTSMELDING",
                                "tidsstempel": "2021-08-25T13:33:13.364413"
                            },
                            {
                                "id": "fc8e7179-5d63-46ce-a5bf-ec42313ff456",
                                "skatteopplysninger": [
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "ae957631-0f86-4703-907c-320950c96bbb",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.395239",
                                        "måned": "2019-05",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "juicy fordel",
                                        "beskrivelse": "juicy beskrivelse"
                                    },
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "ae957631-0f86-4703-907c-320950c96bbb",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.395239",
                                        "måned": "2019-04",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "juicy fordel",
                                        "beskrivelse": "juicy beskrivelse"
                                    },
                                    {
                                        "dato": "2019-06-01",
                                        "hendelseId": "ae957631-0f86-4703-907c-320950c96bbb",
                                        "beløp": 31000.0,
                                        "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                        "tidsstempel": "2021-08-24T13:33:13.395239",
                                        "måned": "2019-03",
                                        "type": "LØNNSINNTEKT",
                                        "fordel": "juicy fordel",
                                        "beskrivelse": "juicy beskrivelse"
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }
        ],
        "vilkårsgrunnlagHistorikk": [
            {
                "opprettet": "2021-08-24T13:33:13.48367",
                "vilkårsgrunnlag": [
                    {
                        "skjæringstidspunkt": "2019-06-01",
                        "type": "Vilkårsprøving"
                    },
                    {
                        "skjæringstidspunkt": "2018-01-01",
                        "type": "Vilkårsprøving"
                    }
                ]
            }
        ],
        "skjemaVersjon": 113
    }
    """

    @Language("JSON")
    private val expected = """{
    "aktørId": "12345",
    "fødselsnummer": "12020052345",
    "opprettet": "2021-08-24T13:33:13.058378",
    "dødsdato": null,
    "arbeidsgivere": [
        {
            "organisasjonsnummer": "987654321",
            "inntektshistorikk": [
                {
                    "id": "e661625f-c9f2-4fe4-a510-03ec7f594567",
                    "inntektsopplysninger": [
                        {
                            "id": "8042d1e5-2524-4c67-9f39-cfbbb6f44d65",
                            "skatteopplysninger": [
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664fff",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2018-06",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664fff",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2018-07",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664fff",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2018-08",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664fff",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2018-09",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664fff",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2018-10",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664fff",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2018-11",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664fff",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2018-12",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664fff",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2019-01",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664fff",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2019-02",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664fff",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2019-03",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664fff",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2019-04",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664fff",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2019-05",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                }
                            ]
                        },
                        {
                            "id": "fc8e7179-5d63-46ce-a5bf-ec42313ff123",
                            "skatteopplysninger": [
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "ae957631-0f86-4703-907c-320950c96aaa",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.395239",
                                    "måned": "2019-05",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "juicy fordel",
                                    "beskrivelse": "juicy beskrivelse"
                                },
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "ae957631-0f86-4703-907c-320950c96aaa",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.395239",
                                    "måned": "2019-04",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "juicy fordel",
                                    "beskrivelse": "juicy beskrivelse"
                                },
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "ae957631-0f86-4703-907c-320950c96aaa",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.395239",
                                    "måned": "2019-03",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "juicy fordel",
                                    "beskrivelse": "juicy beskrivelse"
                                }
                            ]
                        },
                        {
                            "id": "15abfcab-d6ae-428d-9b9f-4d0445cabcde",
                            "dato": "2019-06-01",
                            "hendelseId": "518938a9-a856-4c3a-a238-a5fe4f4abcde",
                            "beløp": 31000.0,
                            "kilde": "INNTEKTSMELDING",
                            "tidsstempel": "2021-08-25T13:33:13.364413"
                        },
                        {
                            "id": "15abfcab-d6ae-428d-9b9f-4d0445cea479",
                            "dato": "2018-01-01",
                            "hendelseId": "518938a9-a856-4c3a-a238-a5fe4f4020d3",
                            "beløp": 31000.0,
                            "kilde": "INNTEKTSMELDING",
                            "tidsstempel": "2021-08-24T13:33:13.364413"
                        },
                        {
                            "id": "fc8e7179-5d63-46ce-a5bf-ec42313ff403",
                            "skatteopplysninger": [
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "ae957631-0f86-4703-907c-320950c965b0",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.395239",
                                    "måned": "2017-10",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "juicy fordel",
                                    "beskrivelse": "juicy beskrivelse"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "ae957631-0f86-4703-907c-320950c965b0",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.395239",
                                    "måned": "2017-11",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "juicy fordel",
                                    "beskrivelse": "juicy beskrivelse"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "ae957631-0f86-4703-907c-320950c965b0",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.395239",
                                    "måned": "2017-12",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "juicy fordel",
                                    "beskrivelse": "juicy beskrivelse"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "ae957631-0f86-4703-907c-320950c965b0",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.395239",
                                    "måned": "2018-01",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "juicy fordel",
                                    "beskrivelse": "juicy beskrivelse"
                                }
                            ]
                        },
                        {
                            "id": "8042d1e5-2524-4c67-9f39-cf4e06f44d65",
                            "skatteopplysninger": [
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664ff9",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2017-01",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664ff9",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2017-02",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664ff9",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2017-03",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664ff9",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2017-04",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664ff9",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2017-05",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664ff9",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2017-06",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664ff9",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2017-07",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664ff9",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2017-08",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664ff9",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2017-09",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664ff9",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2017-10",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664ff9",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2017-11",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2018-01-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664ff9",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2017-12",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                }
                            ]
                        }
                    ]
                }
            ]
        },
        {
            "organisasjonsnummer": "999999999",
            "inntektshistorikk": [
                {
                    "id": "e661625f-c9f2-4fe4-a510-03ec7f594eee",
                    "inntektsopplysninger": [
                        {
                            "id": "8042d1e5-2524-4c67-9f39-cfbbb6f44aba",
                            "skatteopplysninger": [
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664cdc",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2018-06",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664cdc",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2018-07",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664cdc",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2018-08",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664cdc",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2018-09",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664cdc",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2018-10",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664cdc",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2018-11",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664cdc",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2018-12",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664cdc",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2019-01",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664cdc",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2019-02",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664cdc",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2019-03",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664cdc",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2019-04",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                },
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "2e2df08f-1661-4ae3-ac55-54e18b664cdc",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SAMMENLIGNINGSGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.455787",
                                    "måned": "2019-05",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "kontantytelse",
                                    "beskrivelse": "fastloenn"
                                }
                            ]
                        },
                        {
                            "id": "15abfcab-d6ae-428d-9b9f-4d0445cab999",
                            "dato": "2019-06-02",
                            "hendelseId": "518938a9-a856-4c3a-a238-a5fe4f4ab999",
                            "beløp": 31000.0,
                            "kilde": "INNTEKTSMELDING",
                            "tidsstempel": "2021-08-25T13:33:13.364413"
                        },
                        {
                            "id": "fc8e7179-5d63-46ce-a5bf-ec42313ff456",
                            "skatteopplysninger": [
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "ae957631-0f86-4703-907c-320950c96bbb",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.395239",
                                    "måned": "2019-05",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "juicy fordel",
                                    "beskrivelse": "juicy beskrivelse"
                                },
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "ae957631-0f86-4703-907c-320950c96bbb",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.395239",
                                    "måned": "2019-04",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "juicy fordel",
                                    "beskrivelse": "juicy beskrivelse"
                                },
                                {
                                    "dato": "2019-06-01",
                                    "hendelseId": "ae957631-0f86-4703-907c-320950c96bbb",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "tidsstempel": "2021-08-24T13:33:13.395239",
                                    "måned": "2019-03",
                                    "type": "LØNNSINNTEKT",
                                    "fordel": "juicy fordel",
                                    "beskrivelse": "juicy beskrivelse"
                                }
                            ]
                        }
                    ]
                }
            ]
        }
    ],
    "vilkårsgrunnlagHistorikk": [
        {
            "opprettet": "2021-08-24T13:33:13.48367",
            "vilkårsgrunnlag": [
                {
                    "skjæringstidspunkt": "2019-06-01",
                    "type": "Vilkårsprøving",
                    "sykepengegrunnlag": {
                        "sykepengegrunnlag": 599148.0,
                        "grunnlagForSykepengegrunnlag": 744000.0,
                        "arbeidsgiverInntektsopplysninger": [
                            {
                                "orgnummer": "987654321",
                                "inntektsopplysning": {
                                    "id": "15abfcab-d6ae-428d-9b9f-4d0445cabcde",
                                    "dato": "2019-06-01",
                                    "hendelseId": "518938a9-a856-4c3a-a238-a5fe4f4abcde",
                                    "beløp": 31000.0,
                                    "kilde": "INNTEKTSMELDING",
                                    "tidsstempel": "2021-08-25T13:33:13.364413"
                                }
                            },
                            {
                                "orgnummer": "999999999",
                                "inntektsopplysning": {
                                    "id": "fc8e7179-5d63-46ce-a5bf-ec42313ff456",
                                    "skatteopplysninger": [
                                            {
                                                "dato": "2019-06-01",
                                                "hendelseId": "ae957631-0f86-4703-907c-320950c96bbb",
                                                "beløp": 31000.0,
                                                "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                                "tidsstempel": "2021-08-24T13:33:13.395239",
                                                "måned": "2019-05",
                                                "type": "LØNNSINNTEKT",
                                                "fordel": "juicy fordel",
                                                "beskrivelse": "juicy beskrivelse"
                                            },
                                            {
                                                "dato": "2019-06-01",
                                                "hendelseId": "ae957631-0f86-4703-907c-320950c96bbb",
                                                "beløp": 31000.0,
                                                "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                                "tidsstempel": "2021-08-24T13:33:13.395239",
                                                "måned": "2019-04",
                                                "type": "LØNNSINNTEKT",
                                                "fordel": "juicy fordel",
                                                "beskrivelse": "juicy beskrivelse"
                                            },
                                            {
                                                "dato": "2019-06-01",
                                                "hendelseId": "ae957631-0f86-4703-907c-320950c96bbb",
                                                "beløp": 31000.0,
                                                "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                                "tidsstempel": "2021-08-24T13:33:13.395239",
                                                "måned": "2019-03",
                                                "type": "LØNNSINNTEKT",
                                                "fordel": "juicy fordel",
                                                "beskrivelse": "juicy beskrivelse"
                                            }
                                        ]
                                }
                            }
                        ]
                    }
                },
                {
                    "skjæringstidspunkt": "2018-01-01",
                    "type": "Vilkårsprøving",
                    "sykepengegrunnlag": {
                        "sykepengegrunnlag": 372000.0,
                        "grunnlagForSykepengegrunnlag": 372000.0,
                        "arbeidsgiverInntektsopplysninger": [
                            {
                                "orgnummer": "987654321",
                                "inntektsopplysning": {
                                    "id": "15abfcab-d6ae-428d-9b9f-4d0445cea479",
                                    "dato": "2018-01-01",
                                    "hendelseId": "518938a9-a856-4c3a-a238-a5fe4f4020d3",
                                    "beløp": 31000.0,
                                    "kilde": "INNTEKTSMELDING",
                                    "tidsstempel": "2021-08-24T13:33:13.364413"
                                }
                            }
                        ]
                    }
                }
            ]
        }
    ],
    "skjemaVersjon": 114
}"""
}


