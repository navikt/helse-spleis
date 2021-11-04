package no.nav.helse.serde.migration

import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class V114LagreSykepengegrunnlagTest : MigrationTest(V114LagreSykepengegrunnlag()) {

    @Test
    fun `Sykepengegrunnlag lagres riktig for flere arbeidsgivere, en med inntektsmelding og med inntekter fra skatt`() {
        assertMigrationRaw(expected, original)
    }

    @Test
    fun `Sykepengegrunnlag lagres riktig for person med IT-historikk`() {
        assertMigrationRaw(personOvergangFraITExpected, personOvergangFraITOriginal)
    }

    @Test
    fun `Én arbeidsgiver med IT-historikk og én med skatteopplysninger - vilkårsgrunnlaget skal kun legge IT-historikken til grunn ved Infotrygd-vilkårsgrunnlag`() {
        assertMigrationRaw(personOvergangFraITMedSkatteopplysningerExpected, personOvergangFraITMedSkatteopplysningerOriginal)
    }

    @Test
    fun `Migrerer riktig for overgang fra infotrygd med dato ulikt skjæringstidspunkt og manglende inntektsopplysning for vilkårsgrunnlag`() {
        assertMigrationRaw(personMedRartSkjæringstidspunktFraITExpected, personMedRartSkjæringstidspunktFraITOriginal)
    }

    @Test
    fun `Inntektsopplysning fra inntektsmelding med dato ulik fra skjæringstidspunkt prioriteres over skatteopplysning`() {
        assertMigrationRaw(personMedRartSkjæringstidspunktFraIMExpected, personMedRartSkjæringstidspunktFraIMOriginal)
    }

    @Test
    fun `Tre inntektsmeldinger, IM som er lagt til grunn på vedtaksperioden er den midterste og ligger ikke på skjæringstidspunkt - velger riktig`() {
        assertMigrationRaw(personMedTreIMExpected, personMedTreIMOriginal)
    }

    @Test
    fun `Flere arbeidsgivere med ulik fom skal bruke IM fra tidligste fom og skatteopplysninger fra seneste fom`() {
        assertMigrationRaw(personMedFlereAGUlikFomExpected, personMedFlereAGUlikFomOriginal)
    }

    @Test
    fun `Forkastede vedtaksperioder`() {
        assertMigration("/migrations/114/forkastedeVedtaksperioderExpected.json", "/migrations/114/forkastedeVedtaksperioderOriginal.json")
    }

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
                ],
                "forkastede": [],
                "vedtaksperioder": [
                    {
                        "inntektsmeldingInfo": null,
                        "tilstand": "AVSLUTTET",
                        "skjæringstidspunkt": "2017-12-01",
                        "fom": "2017-12-01"
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
            ],
            "forkastede": [],
            "vedtaksperioder": [
                    {
                        "inntektsmeldingInfo": null,
                        "tilstand": "AVSLUTTET",
                        "skjæringstidspunkt": "2017-12-01",
                        "fom": "2017-12-01"
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
            ],
            "forkastede": [],
            "vedtaksperioder": [
                {
                    "inntektsmeldingInfo": null,
                    "tilstand": "AVSLUTTET",
                    "skjæringstidspunkt": "2017-12-01",
                    "fom": "2017-12-01"
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
            ],
            "forkastede":[],
            "vedtaksperioder": [
                {
                    "inntektsmeldingInfo": null,
                    "tilstand": "AVSLUTTET",
                    "skjæringstidspunkt": "2017-12-01",
                    "fom": "2017-12-01"
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
            ],
            "forkastede": [],
            "vedtaksperioder": [
                {
                    "inntektsmeldingInfo": null,
                    "tilstand": "AVSLUTTET",
                    "skjæringstidspunkt": "2017-12-01",
                    "fom": "2017-12-01"
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
            ],
            "forkastede": [],
            "vedtaksperioder": []
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
            ],
            "forkastede": [],
            "vedtaksperioder": [
                    {
                        "inntektsmeldingInfo": null,
                        "tilstand": "AVSLUTTET",
                        "skjæringstidspunkt": "2017-12-01",
                        "fom": "2017-12-01"
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
            ],
            "forkastede": [],
            "vedtaksperioder": []
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
                ],
                "forkastede": [],
                "vedtaksperioder": [
                    {
                        "inntektsmeldingInfo": {
                            "id": "518938a9-a856-4c3a-a238-a5fe4f4abcde"
                        },
                        "tilstand": "AVSLUTTET",
                        "skjæringstidspunkt": "2019-06-01",
                        "fom": "2019-06-01"
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
                ],
                "forkastede": [],
                "vedtaksperioder": []
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
            ],
            "forkastede": [],
            "vedtaksperioder": [
                    {
                        "inntektsmeldingInfo": {
                            "id": "518938a9-a856-4c3a-a238-a5fe4f4abcde"
                        },
                        "tilstand": "AVSLUTTET",
                        "skjæringstidspunkt": "2019-06-01",
                        "fom": "2019-06-01"
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
            ],
            "forkastede": [],
            "vedtaksperioder": []
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

    @Language("JSON")
    private val personMedRartSkjæringstidspunktFraIMOriginal = """{
    "aktørId": "42",
    "fødselsnummer": "12020052345",
    "arbeidsgivere": [
        {
            "organisasjonsnummer": "987654321",
            "id": "04bd638d-6bb6-4d19-addc-c02572dec4de",
            "inntektshistorikk": [
                {
                    "id": "72a147a2-ca02-4616-9c08-b3e9c0fc2670",
                    "inntektsopplysninger": [
                        {
                            "id": "04c3395e-c70c-4e58-9e99-b985ab4138b7",
                            "hendelseId": "abf3b3d9-e3ae-4bd3-a685-8b0575961006",
                            "dato": "2018-01-02",
                            "beløp": 31000.0,
                            "kilde": "INNTEKTSMELDING"
                        },
                        {
                            "id": "3efad503-7a37-4a07-bdfc-a1a35d988334",
                            "skatteopplysninger": [
                                {
                                    "hendelseId": "e1f84076-c00b-4403-9357-c726c12e5477",
                                    "dato": "2018-01-01",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2017-10",
                                    "type": "LØNNSINNTEKT"
                                },
                                {
                                    "hendelseId": "e1f84076-c00b-4403-9357-c726c12e5477",
                                    "dato": "2018-01-01",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2017-11",
                                    "type": "LØNNSINNTEKT"
                                },
                                {
                                    "hendelseId": "e1f84076-c00b-4403-9357-c726c12e5477",
                                    "dato": "2018-01-01",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2017-12",
                                    "type": "LØNNSINNTEKT"
                                }
                            ]
                        }
                    ]
                }
            ],
            "forkastede": [],
            "vedtaksperioder": [
                {
                    "inntektsmeldingInfo": {
                        "id": "abf3b3d9-e3ae-4bd3-a685-8b0575961006"
                    },
                    "tilstand": "AVSLUTTET",
                    "skjæringstidspunkt": "2018-01-01",
                    "fom": "2018-01-01"
                },
                {
                    "inntektsmeldingInfo": {
                        "id": "518938a9-a856-4c3a-a238-a5fe4f4020d7"
                    },
                    "tilstand": "AVVENTER_SØKNAD_FERDIG_FORLENGELSE",
                    "skjæringstidspunkt": "2018-01-01",
                    "fom": "2018-01-01"
                }
            ]
        }
    ],
    "vilkårsgrunnlagHistorikk": [
        {
            "id": "0354f216-fa7b-4fcd-9ebd-d9732354f8a7",
            "vilkårsgrunnlag": [
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
    private val personMedRartSkjæringstidspunktFraIMExpected = """
{
    "aktørId": "42",
    "fødselsnummer": "12020052345",
    "arbeidsgivere": [
        {
            "organisasjonsnummer": "987654321",
            "id": "04bd638d-6bb6-4d19-addc-c02572dec4de",
            "inntektshistorikk": [
                {
                    "id": "72a147a2-ca02-4616-9c08-b3e9c0fc2670",
                    "inntektsopplysninger": [
                        {
                            "id": "04c3395e-c70c-4e58-9e99-b985ab4138b7",
                            "hendelseId": "abf3b3d9-e3ae-4bd3-a685-8b0575961006",
                            "dato": "2018-01-02",
                            "beløp": 31000.0,
                            "kilde": "INNTEKTSMELDING"
                        },
                        {
                            "id": "3efad503-7a37-4a07-bdfc-a1a35d988334",
                            "skatteopplysninger": [
                                {
                                    "hendelseId": "e1f84076-c00b-4403-9357-c726c12e5477",
                                    "dato": "2018-01-01",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2017-10",
                                    "type": "LØNNSINNTEKT"
                                },
                                {
                                    "hendelseId": "e1f84076-c00b-4403-9357-c726c12e5477",
                                    "dato": "2018-01-01",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2017-11",
                                    "type": "LØNNSINNTEKT"
                                },
                                {
                                    "hendelseId": "e1f84076-c00b-4403-9357-c726c12e5477",
                                    "dato": "2018-01-01",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG",
                                    "måned": "2017-12",
                                    "type": "LØNNSINNTEKT"
                                }
                            ]
                        }
                    ]
                }
            ],
            "forkastede": [],
            "vedtaksperioder": [
                {
                    "inntektsmeldingInfo": {
                        "id": "abf3b3d9-e3ae-4bd3-a685-8b0575961006"
                    },
                    "tilstand": "AVSLUTTET",
                    "skjæringstidspunkt": "2018-01-01",
                    "fom": "2018-01-01"
                },
                {
                    "inntektsmeldingInfo": {
                        "id": "518938a9-a856-4c3a-a238-a5fe4f4020d7"
                    },
                    "tilstand": "AVVENTER_SØKNAD_FERDIG_FORLENGELSE",
                    "skjæringstidspunkt": "2018-01-01",
                    "fom":  "2018-01-01"
                }
            ]
        }
    ],
    "vilkårsgrunnlagHistorikk": [
        {
            "id": "0354f216-fa7b-4fcd-9ebd-d9732354f8a7",
            "vilkårsgrunnlag": [
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
                                    "id": "04c3395e-c70c-4e58-9e99-b985ab4138b7",
                                    "hendelseId": "abf3b3d9-e3ae-4bd3-a685-8b0575961006",
                                    "dato": "2018-01-02",
                                    "beløp": 31000.0,
                                    "kilde": "INNTEKTSMELDING"
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
    private val personMedTreIMOriginal = """
    {
    "aktørId": "42",
    "fødselsnummer": "12020052345",
    "arbeidsgivere": [
        {
            "organisasjonsnummer": "987654321",
            "id": "04bd638d-6bb6-4d19-addc-c02572dec4de",
            "inntektshistorikk": [
                {
                    "id": "72a147a2-ca02-4616-9c08-b3e9c0fc2670",
                    "inntektsopplysninger": [
                        {
                            "id": "2fd33523-c4ad-4a0b-9e4a-3efff12cf31a",
                            "hendelseId": "2608e59e-95bf-446b-ada5-afd863e4edb6",
                            "dato": "2018-01-01",
                            "beløp": 31000.0,
                            "kilde": "INNTEKTSMELDING"
                        },
                        {
                            "id": "04c3395e-c70c-4e58-9e99-b985ab4138b7",
                            "hendelseId": "abf3b3d9-e3ae-4bd3-a685-8b0575961006",
                            "dato": "2018-01-02",
                            "beløp": 31000.0,
                            "kilde": "INNTEKTSMELDING"
                        },
                        {
                            "id": "216fa7f8-ff50-45e4-8f62-178082c15305",
                            "hendelseId": "802e44e9-97b8-42aa-8207-2e88e3208321",
                            "dato": "2018-01-01",
                            "beløp": 31000.0,
                            "kilde": "INNTEKTSMELDING"
                        }
                    ]
                }
            ],
            "forkastede": [],
            "vedtaksperioder": [
                {
                    "inntektsmeldingInfo": {
                        "id": "abf3b3d9-e3ae-4bd3-a685-8b0575961006"
                    },
                    "tilstand": "AVSLUTTET",
                    "skjæringstidspunkt": "2018-01-01",
                    "fom": "2018-01-01"
                },
                {
                    "inntektsmeldingInfo": {
                        "id": "518938a9-a856-4c3a-a238-a5fe4f4020d7"
                    },
                    "tilstand": "AVVENTER_SØKNAD_FERDIG_FORLENGELSE",
                    "skjæringstidspunkt": "2018-01-01",
                    "fom": "2018-01-01"
                }
            ]
        }
    ],
    "vilkårsgrunnlagHistorikk": [
        {
            "id": "0354f216-fa7b-4fcd-9ebd-d9732354f8a7",
            "vilkårsgrunnlag": [
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
    private val personMedTreIMExpected = """
{
    "aktørId": "42",
    "fødselsnummer": "12020052345",
    "arbeidsgivere": [
        {
            "organisasjonsnummer": "987654321",
            "id": "04bd638d-6bb6-4d19-addc-c02572dec4de",
            "inntektshistorikk": [
                {
                    "id": "72a147a2-ca02-4616-9c08-b3e9c0fc2670",
                    "inntektsopplysninger": [
                        {
                            "id": "2fd33523-c4ad-4a0b-9e4a-3efff12cf31a",
                            "hendelseId": "2608e59e-95bf-446b-ada5-afd863e4edb6",
                            "dato": "2018-01-01",
                            "beløp": 31000.0,
                            "kilde": "INNTEKTSMELDING"
                        },
                        {
                            "id": "04c3395e-c70c-4e58-9e99-b985ab4138b7",
                            "hendelseId": "abf3b3d9-e3ae-4bd3-a685-8b0575961006",
                            "dato": "2018-01-02",
                            "beløp": 31000.0,
                            "kilde": "INNTEKTSMELDING"
                        },
                        {
                            "id": "216fa7f8-ff50-45e4-8f62-178082c15305",
                            "hendelseId": "802e44e9-97b8-42aa-8207-2e88e3208321",
                            "dato": "2018-01-01",
                            "beløp": 31000.0,
                            "kilde": "INNTEKTSMELDING"
                        }
                    ]
                }
            ],
            "forkastede": [],
            "vedtaksperioder": [
                {
                    "inntektsmeldingInfo": {
                        "id": "abf3b3d9-e3ae-4bd3-a685-8b0575961006"
                    },
                    "tilstand": "AVSLUTTET",
                    "skjæringstidspunkt": "2018-01-01",
                    "fom": "2018-01-01"
                },
                {
                    "inntektsmeldingInfo": {
                        "id": "518938a9-a856-4c3a-a238-a5fe4f4020d7"
                    },
                    "tilstand": "AVVENTER_SØKNAD_FERDIG_FORLENGELSE",
                    "skjæringstidspunkt": "2018-01-01",
                    "fom": "2018-01-01"
                }
            ]
        }
    ],
    "vilkårsgrunnlagHistorikk": [
        {
            "id": "0354f216-fa7b-4fcd-9ebd-d9732354f8a7",
            "vilkårsgrunnlag": [
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
                                    "id": "04c3395e-c70c-4e58-9e99-b985ab4138b7",
                                    "hendelseId": "abf3b3d9-e3ae-4bd3-a685-8b0575961006",
                                    "dato": "2018-01-02",
                                    "beløp": 31000.0,
                                    "kilde": "INNTEKTSMELDING"
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
    private val personMedFlereAGUlikFomOriginal = """
    {
    "aktørId": "42",
    "fødselsnummer": "12020052345",
    "arbeidsgivere": [
        {
            "organisasjonsnummer": "arbeidsgiver 1",
            "id": "760c9862-effb-4118-9c4f-bbf8c14d82f1",
            "inntektshistorikk": [
                {
                    "id": "c528ae2d-290e-4288-b1f0-48334bff2364",
                    "inntektsopplysninger": [
                        {
                            "id": "11136c9c-eb2f-4375-9fa7-b0660d51f43a",
                            "dato": "2018-03-01",
                            "hendelseId": "42820dc5-f5dc-4b8d-87a4-d79af3f05fb7",
                            "beløp": 31000.0,
                            "kilde": "INNTEKTSMELDING"
                        },
                        {
                            "id": "713e06cc-0ced-4007-864a-6d17ae6ead04",
                            "skatteopplysninger": [
                                {
                                    "dato": "2018-03-01",
                                    "hendelseId": "2ab35a04-3d59-4d20-964d-03cb8aef1f44",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG"
                                },
                                {
                                    "dato": "2018-03-01",
                                    "hendelseId": "2ab35a04-3d59-4d20-964d-03cb8aef1f44",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG"
                                },
                                {
                                    "dato": "2018-03-01",
                                    "hendelseId": "2ab35a04-3d59-4d20-964d-03cb8aef1f44",
                                    "beløp": 31000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG"
                                }
                            ]
                        }
                    ]
                }
            ],
            "forkastede": [],
            "vedtaksperioder": [
                {
                    "fom": "2018-03-01",
                    "tom": "2018-03-31",
                    "inntektsmeldingInfo": {
                        "id": "42820dc5-f5dc-4b8d-87a4-d79af3f05fb7",
                        "arbeidsforholdId": null
                    },
                    "skjæringstidspunkt": "2018-03-01",
                    "tilstand": "AVVENTER_SIMULERING"
                }
            ]
        },
        {
            "organisasjonsnummer": "arbeidsgiver 2",
            "id": "4a92d598-76e6-4e5d-bbb5-7158cb48ed01",
            "inntektshistorikk": [
                {
                    "id": "7233eb3b-6e48-4c38-9cb2-6c05cde5df9a",
                    "inntektsopplysninger": [
                        {
                            "id": "ada6c855-411f-46b3-ba38-53821a290967",
                            "dato": "2018-03-05",
                            "hendelseId": "9cd523e1-7d82-46ad-92d2-534b68850b7a",
                            "beløp": 31000.0,
                            "kilde": "INNTEKTSMELDING"
                        },
                        {
                            "id": "3c8c8fb2-1334-480b-af72-c1bc942fc140",
                            "skatteopplysninger": [
                                {
                                    "dato": "2018-03-01",
                                    "hendelseId": "2ab35a04-3d59-4d20-964d-03cb8aef1f44",
                                    "beløp": 20000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG"
                                },
                                {
                                    "dato": "2018-03-01",
                                    "hendelseId": "2ab35a04-3d59-4d20-964d-03cb8aef1f44",
                                    "beløp": 20000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG"
                                },
                                {
                                    "dato": "2018-03-01",
                                    "hendelseId": "2ab35a04-3d59-4d20-964d-03cb8aef1f44",
                                    "beløp": 20000.0,
                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG"
                                }
                            ]
                        }
                    ]
                }
            ],
            "forkastede": [],
            "vedtaksperioder": [
                {
                    "fom": "2018-03-05",
                    "tom": "2018-03-31",
                    "id": "b0c9ec99-a0b1-45eb-bfcc-79d2ff12f24e",
                    "inntektsmeldingInfo": {
                        "id": "9cd523e1-7d82-46ad-92d2-534b68850b7a",
                        "arbeidsforholdId": null
                    },
                    "skjæringstidspunkt": "2018-03-01",
                    "tilstand": "AVVENTER_SIMULERING"
                }
            ]
        }
    ],
    "vilkårsgrunnlagHistorikk": [
        {
            "id": "16778676-e315-45ea-aaec-122e59914447",
            "vilkårsgrunnlag": [
                {
                    "skjæringstidspunkt": "2018-03-01",
                    "type": "Vilkårsprøving",
                    "meldingsreferanseId": "2ab35a04-3d59-4d20-964d-03cb8aef1f44"
                }
            ]
        }
    ],
    "skjemaVersjon": 113
}
    """
    @Language("JSON")
    private val personMedFlereAGUlikFomExpected = """
        {
            "aktørId": "42",
            "fødselsnummer": "12020052345",
            "arbeidsgivere": [
                {
                    "organisasjonsnummer": "arbeidsgiver 1",
                    "id": "760c9862-effb-4118-9c4f-bbf8c14d82f1",
                    "inntektshistorikk": [
                        {
                            "id": "c528ae2d-290e-4288-b1f0-48334bff2364",
                            "inntektsopplysninger": [
                                {
                                    "id": "11136c9c-eb2f-4375-9fa7-b0660d51f43a",
                                    "dato": "2018-03-01",
                                    "hendelseId": "42820dc5-f5dc-4b8d-87a4-d79af3f05fb7",
                                    "beløp": 31000.0,
                                    "kilde": "INNTEKTSMELDING"
                                },
                                {
                                    "id": "713e06cc-0ced-4007-864a-6d17ae6ead04",
                                    "skatteopplysninger": [
                                        {
                                            "dato": "2018-03-01",
                                            "hendelseId": "2ab35a04-3d59-4d20-964d-03cb8aef1f44",
                                            "beløp": 31000.0,
                                            "kilde": "SKATT_SYKEPENGEGRUNNLAG"
                                        },
                                        {
                                            "dato": "2018-03-01",
                                            "hendelseId": "2ab35a04-3d59-4d20-964d-03cb8aef1f44",
                                            "beløp": 31000.0,
                                            "kilde": "SKATT_SYKEPENGEGRUNNLAG"
                                        },
                                        {
                                            "dato": "2018-03-01",
                                            "hendelseId": "2ab35a04-3d59-4d20-964d-03cb8aef1f44",
                                            "beløp": 31000.0,
                                            "kilde": "SKATT_SYKEPENGEGRUNNLAG"
                                        }
                                    ]
                                }
                            ]
                        }
                    ],
                    "forkastede": [],
                    "vedtaksperioder": [
                        {
                            "fom": "2018-03-01",
                            "tom": "2018-03-31",
                            "inntektsmeldingInfo": {
                                "id": "42820dc5-f5dc-4b8d-87a4-d79af3f05fb7",
                                "arbeidsforholdId": null
                            },
                            "skjæringstidspunkt": "2018-03-01",
                            "tilstand": "AVVENTER_SIMULERING"
                        }
                    ]
                },
                {
                    "organisasjonsnummer": "arbeidsgiver 2",
                    "id": "4a92d598-76e6-4e5d-bbb5-7158cb48ed01",
                    "inntektshistorikk": [
                        {
                            "id": "7233eb3b-6e48-4c38-9cb2-6c05cde5df9a",
                            "inntektsopplysninger": [
                                {
                                    "id": "ada6c855-411f-46b3-ba38-53821a290967",
                                    "dato": "2018-03-05",
                                    "hendelseId": "9cd523e1-7d82-46ad-92d2-534b68850b7a",
                                    "beløp": 31000.0,
                                    "kilde": "INNTEKTSMELDING"
                                },
                                {
                                    "id": "3c8c8fb2-1334-480b-af72-c1bc942fc140",
                                    "skatteopplysninger": [
                                        {
                                            "dato": "2018-03-01",
                                            "hendelseId": "2ab35a04-3d59-4d20-964d-03cb8aef1f44",
                                            "beløp": 20000.0,
                                            "kilde": "SKATT_SYKEPENGEGRUNNLAG"
                                        },
                                        {
                                            "dato": "2018-03-01",
                                            "hendelseId": "2ab35a04-3d59-4d20-964d-03cb8aef1f44",
                                            "beløp": 20000.0,
                                            "kilde": "SKATT_SYKEPENGEGRUNNLAG"
                                        },
                                        {
                                            "dato": "2018-03-01",
                                            "hendelseId": "2ab35a04-3d59-4d20-964d-03cb8aef1f44",
                                            "beløp": 20000.0,
                                            "kilde": "SKATT_SYKEPENGEGRUNNLAG"
                                        }
                                    ]
                                }
                            ]
                        }
                    ],
                    "forkastede": [],
                    "vedtaksperioder": [
                        {
                            "fom": "2018-03-05",
                            "tom": "2018-03-31",
                            "id": "b0c9ec99-a0b1-45eb-bfcc-79d2ff12f24e",
                            "inntektsmeldingInfo": {
                                "id": "9cd523e1-7d82-46ad-92d2-534b68850b7a",
                                "arbeidsforholdId": null
                            },
                            "skjæringstidspunkt": "2018-03-01",
                            "tilstand": "AVVENTER_SIMULERING"
                        }
                    ]
                }
            ],
            "vilkårsgrunnlagHistorikk": [
                {
                    "id": "16778676-e315-45ea-aaec-122e59914447",
                    "vilkårsgrunnlag": [
                        {
                            "skjæringstidspunkt": "2018-03-01",
                            "type": "Vilkårsprøving",
                            "sykepengegrunnlag": {
                                "sykepengegrunnlag": 561804.0,
                                "grunnlagForSykepengegrunnlag": 612000.0,
                                "arbeidsgiverInntektsopplysninger": [
                                    {
                                        "orgnummer": "arbeidsgiver 1",
                                        "inntektsopplysning": {
                                            "id": "11136c9c-eb2f-4375-9fa7-b0660d51f43a",
                                            "dato": "2018-03-01",
                                            "hendelseId": "42820dc5-f5dc-4b8d-87a4-d79af3f05fb7",
                                            "beløp": 31000.0,
                                            "kilde": "INNTEKTSMELDING"
                                        }
                                    },
                                    {
                                        "orgnummer": "arbeidsgiver 2",
                                        "inntektsopplysning": {
                                            "id": "3c8c8fb2-1334-480b-af72-c1bc942fc140",
                                            "skatteopplysninger": [
                                                {
                                                    "dato": "2018-03-01",
                                                    "hendelseId": "2ab35a04-3d59-4d20-964d-03cb8aef1f44",
                                                    "beløp": 20000.0,
                                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG"
                                                },
                                                {
                                                    "dato": "2018-03-01",
                                                    "hendelseId": "2ab35a04-3d59-4d20-964d-03cb8aef1f44",
                                                    "beløp": 20000.0,
                                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG"
                                                },
                                                {
                                                    "dato": "2018-03-01",
                                                    "hendelseId": "2ab35a04-3d59-4d20-964d-03cb8aef1f44",
                                                    "beløp": 20000.0,
                                                    "kilde": "SKATT_SYKEPENGEGRUNNLAG"
                                                }
                                            ]
                                        }
                                    }
                                ]
                            },
                            "meldingsreferanseId": "2ab35a04-3d59-4d20-964d-03cb8aef1f44"
                        }
                    ]
                }
            ],
            "skjemaVersjon": 114
        }
    """
}


