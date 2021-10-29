package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class V119SletteSkatteopplydsningFraITVilkårsgrunnlagTest {

    @Test
    fun `Sletter skatteopplysninger fra Infotrygd-vilkårsgrunnlag`() {
        Assertions.assertEquals(toNode(personOvergangFraITMedSkatteopplysningerExpected), migrer(personOvergangFraITMedSkatteopplysningerOriginal))
    }

    @Test
    fun `Sletter skatteopplysninger fra Infotrygd-vilkårsgrunnlag når infotrygdinntektsopplysningen er feilmerket som inntektsmelding`() {
        Assertions.assertEquals(toNode(personMedITMerketSomIMExpected), migrer(personMedITMerketSomIMOriginal))
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V119SletteSkatteopplydsningFraITVilkårsgrunnlag()).migrate(toNode(json))

    @Language("JSON")
    private val personOvergangFraITMedSkatteopplysningerOriginal = """{
    "fødselsnummer": "04206913337",
    "vilkårsgrunnlagHistorikk": [
        {
            "id": "dfa125bc-a0f1-454a-90bb-bf4f6d4068c2",
            "opprettet": "2021-08-25T14:50:58.252058",
            "vilkårsgrunnlag": [
                {
                    "skjæringstidspunkt": "2017-12-01",
                    "type": "Infotrygd",
                    "sykepengegrunnlag": {
                        "sykepengegrunnlag": 561804.0,
                        "grunnlagForSykepengegrunnlag": 744000.0,
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
                            },
                            {
                                "orgnummer": "987654321",
                                "inntektsopplysning": {
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
                            }
                        ]
                    }
                },
                {
                    "skjæringstidspunkt": "2016-12-01",
                    "type": "Infotrygd",
                    "sykepengegrunnlag": {
                        "sykepengegrunnlag": 372000.0,
                        "grunnlagForSykepengegrunnlag": 372000.0,
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
    "skjemaVersjon": 118
}
    """

    @Language("JSON")
    private val personOvergangFraITMedSkatteopplysningerExpected = """{
    "fødselsnummer": "04206913337",
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
                        "sykepengegrunnlag": 372000.0,
                        "grunnlagForSykepengegrunnlag": 372000.0,
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
    "skjemaVersjon": 119
}
    """

    @Language("JSON")
    private val personMedITMerketSomIMOriginal = """{
    "fødselsnummer": "04206913337",
    "vilkårsgrunnlagHistorikk": [
        {
            "id": "dfa125bc-a0f1-454a-90bb-bf4f6d4068c2",
            "opprettet": "2021-08-25T14:50:58.252058",
            "vilkårsgrunnlag": [
                {
                    "skjæringstidspunkt": "2017-12-01",
                    "type": "Infotrygd",
                    "sykepengegrunnlag": {
                        "sykepengegrunnlag": 561804.0,
                        "grunnlagForSykepengegrunnlag": 744000.0,
                        "arbeidsgiverInntektsopplysninger": [
                            {
                                "orgnummer": "987654321",
                                "inntektsopplysning": {
                                    "id": "e7decc12-507b-4b57-ba87-5d5ae012752d",
                                    "dato": "2017-12-01",
                                    "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99a01",
                                    "beløp": 31000.0,
                                    "kilde": "INNTEKTSMELDING",
                                    "tidsstempel": "2021-08-25T14:50:58.248396"
                                }
                            },
                            {
                                "orgnummer": "987654321",
                                "inntektsopplysning": {
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
                            }
                        ]
                    }
                },
                {
                    "skjæringstidspunkt": "2016-12-01",
                    "type": "Infotrygd",
                    "sykepengegrunnlag": {
                        "sykepengegrunnlag": 372000.0,
                        "grunnlagForSykepengegrunnlag": 372000.0,
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
    "skjemaVersjon": 118
}
    """

    @Language("JSON")
    private val personMedITMerketSomIMExpected = """{
    "fødselsnummer": "04206913337",
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
                                    "kilde": "INNTEKTSMELDING",
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
                        "sykepengegrunnlag": 372000.0,
                        "grunnlagForSykepengegrunnlag": 372000.0,
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
    "skjemaVersjon": 119
}
    """
}
