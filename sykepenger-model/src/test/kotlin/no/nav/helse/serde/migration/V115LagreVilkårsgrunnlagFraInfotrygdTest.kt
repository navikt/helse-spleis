package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V115LagreVilkårsgrunnlagFraInfotrygdTest {
    @Test
    fun `Lagrer vilkårsgrunnlag fra infotrygd`() {
        val migrering = migrer(original)
        val forventetResultat = toNode(expected)
        (forventetResultat["vilkårsgrunnlagHistorikk"][0] as ObjectNode).set<TextNode>("id", migrering["vilkårsgrunnlagHistorikk"][0]["id"])
        (forventetResultat["vilkårsgrunnlagHistorikk"][0] as ObjectNode).set<TextNode>("opprettet", migrering["vilkårsgrunnlagHistorikk"][0]["opprettet"])
        assertEquals(forventetResultat, migrering)
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V115LagreVilkårsgrunnlagFraInfotrygd()).migrate(toNode(json))

    @Language("JSON")
    private val original = """{
        "fødselsnummer": "20046913337",
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
                                "dato": "2016-12-01",
                                "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99aaa",
                                "beløp": 32000.0,
                                "kilde": "INFOTRYGD",
                                "tidsstempel": "2021-08-25T14:50:58.248396"
                            },
                            {
                                "id": "e7decc12-507b-4b57-ba87-5d5ae0127bbb",
                                "dato": "2015-12-01",
                                "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99aaa",
                                "beløp": 33000.0,
                                "kilde": "INFOTRYGD",
                                "tidsstempel": "2021-08-25T14:50:58.248396"
                            },
                            {
                                "id": "e7decc12-507b-4b57-ba87-5d5ae0127ccc",
                                "dato": "2015-12-01",
                                "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99aaa",
                                "beløp": 34000.0,
                                "kilde": "INNTEKTSMELDING",
                                "tidsstempel": "2021-08-25T14:50:58.248396"
                            },
                            {
                                "id": "e7decc12-507b-4b57-ba87-5d5ae0127ddd",
                                "dato": "2014-12-01",
                                "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99aaa",
                                "beløp": 35000.0,
                                "kilde": "INNTEKTSMELDING",
                                "tidsstempel": "2021-08-25T14:50:58.248396"
                            }
                        ]
                    }
                ],
                "vedtaksperioder": [],
                "forkastede": []
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
                        "skjæringstidspunkt": "2015-12-01",
                        "type": "Vilkårsprøving",
                        "sykepengegrunnlag": {
                            "sykepengegrunnlag": 408000.0,
                            "grunnlagForSykepengegrunnlag": 408000.0,
                            "arbeidsgiverInntektsopplysninger": [
                                {
                                    "orgnummer": "987654321",
                                    "inntektsopplysning": {
                                        "id": "e7decc12-507b-4b57-ba87-5d5ae0127ccc",
                                        "dato": "2015-12-01",
                                        "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99aaa",
                                        "beløp": 34000.0,
                                        "kilde": "INNTEKTSMELDING",
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
    private val expected = """{
        "fødselsnummer": "20046913337",
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
                                "dato": "2016-12-01",
                                "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99aaa",
                                "beløp": 32000.0,
                                "kilde": "INFOTRYGD",
                                "tidsstempel": "2021-08-25T14:50:58.248396"
                            },
                            {
                                "id": "e7decc12-507b-4b57-ba87-5d5ae0127bbb",
                                "dato": "2015-12-01",
                                "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99aaa",
                                "beløp": 33000.0,
                                "kilde": "INFOTRYGD",
                                "tidsstempel": "2021-08-25T14:50:58.248396"
                            },
                            {
                                "id": "e7decc12-507b-4b57-ba87-5d5ae0127ccc",
                                "dato": "2015-12-01",
                                "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99aaa",
                                "beløp": 34000.0,
                                "kilde": "INNTEKTSMELDING",
                                "tidsstempel": "2021-08-25T14:50:58.248396"
                            },
                            {
                                "id": "e7decc12-507b-4b57-ba87-5d5ae0127ddd",
                                "dato": "2014-12-01",
                                "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99aaa",
                                "beløp": 35000.0,
                                "kilde": "INNTEKTSMELDING",
                                "tidsstempel": "2021-08-25T14:50:58.248396"
                            }
                        ]
                    }
                ],
                "vedtaksperioder": [],
                "forkastede": []
            }
        ],
        "vilkårsgrunnlagHistorikk": [
            {
                "id": "blir overskrevet i testen",
                "opprettet": "blir også overskrevet i testen",
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
                        "skjæringstidspunkt": "2015-12-01",
                        "type": "Vilkårsprøving",
                        "sykepengegrunnlag": {
                            "sykepengegrunnlag": 408000.0,
                            "grunnlagForSykepengegrunnlag": 408000.0,
                            "arbeidsgiverInntektsopplysninger": [
                                {
                                    "orgnummer": "987654321",
                                    "inntektsopplysning": {
                                        "id": "e7decc12-507b-4b57-ba87-5d5ae0127ccc",
                                        "dato": "2015-12-01",
                                        "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99aaa",
                                        "beløp": 34000.0,
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
                            "sykepengegrunnlag": 384000.0,
                            "grunnlagForSykepengegrunnlag": 384000.0,
                            "arbeidsgiverInntektsopplysninger": [
                                {
                                    "orgnummer": "987654321",
                                    "inntektsopplysning": {
                                        "id": "e7decc12-507b-4b57-ba87-5d5ae0127aaa",
                                        "dato": "2016-12-01",
                                        "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99aaa",
                                        "beløp": 32000.0,
                                        "kilde": "INFOTRYGD",
                                        "tidsstempel": "2021-08-25T14:50:58.248396"
                                    }
                                }
                            ]
                        }
                    },
                    {
                        "skjæringstidspunkt": "2015-12-01",
                        "type": "Infotrygd",
                        "sykepengegrunnlag": {
                            "sykepengegrunnlag": 396000.0,
                            "grunnlagForSykepengegrunnlag": 396000.0,
                            "arbeidsgiverInntektsopplysninger": [
                                {
                                    "orgnummer": "987654321",
                                    "inntektsopplysning": {
                                        "id": "e7decc12-507b-4b57-ba87-5d5ae0127bbb",
                                        "dato": "2015-12-01",
                                        "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99aaa",
                                        "beløp": 33000.0,
                                        "kilde": "INFOTRYGD",
                                        "tidsstempel": "2021-08-25T14:50:58.248396"
                                    }
                                }
                            ]
                        }
                    }
                ]
            },
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
                        "skjæringstidspunkt": "2015-12-01",
                        "type": "Vilkårsprøving",
                        "sykepengegrunnlag": {
                            "sykepengegrunnlag": 408000.0,
                            "grunnlagForSykepengegrunnlag": 408000.0,
                            "arbeidsgiverInntektsopplysninger": [
                                {
                                    "orgnummer": "987654321",
                                    "inntektsopplysning": {
                                        "id": "e7decc12-507b-4b57-ba87-5d5ae0127ccc",
                                        "dato": "2015-12-01",
                                        "hendelseId": "c9b89436-cc6e-4f85-900e-d72527a99aaa",
                                        "beløp": 34000.0,
                                        "kilde": "INNTEKTSMELDING",
                                        "tidsstempel": "2021-08-25T14:50:58.248396"
                                    }
                                }
                            ]
                        }
                    }
                ]
            }
        ],
        "skjemaVersjon": 115
    }
    """
}
