package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V12AktivitetsloggTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `legger til forlengelseFraInfotrygd`() {
        val json = objectMapper.readTree(json)
        listOf(V12Aktivitetslogg()).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())

        assertEquals(8, migratedJson["aktivitetslogg"]["kontekster"].size())
        assertEquals(4, migratedJson["aktivitetslogg"]["aktiviteter"].size())
        assertEquals(
            listOf(6, 1, 2, 3, 5, 7),
            migratedJson["aktivitetslogg"]["aktiviteter"].last()["kontekster"].map { it.intValue() })
    }
}

@Language("JSON")
private const val json = """{
    "aktivitetslogg": {
        "aktiviteter": [
            {
                "kontekster": [
                    {
                        "kontekstType": "Sykmelding",
                        "kontekstMap": {
                            "aktørId": "12345",
                            "fødselsnummer": "12020052345",
                            "organisasjonsnummer": "987654321",
                            "id": "15a35623-af26-4eb4-8d4e-443a1dd29b38"
                        }
                    },
                    {
                        "kontekstType": "Person",
                        "kontekstMap": {
                            "fødselsnummer": "12020052345",
                            "aktørId": "12345"
                        }
                    }
                ],
                "alvorlighetsgrad": "INFO",
                "melding": "Behandler sykmelding",
                "detaljer": {},
                "tidsstempel": "2020-05-15 11:21:58.023"
            },
            {
                "kontekster": [
                    {
                        "kontekstType": "Sykmelding",
                        "kontekstMap": {
                            "aktørId": "12345",
                            "fødselsnummer": "12020052345",
                            "organisasjonsnummer": "987654321",
                            "id": "15a35623-af26-4eb4-8d4e-443a1dd29b38"
                        }
                    },
                    {
                        "kontekstType": "Person",
                        "kontekstMap": {
                            "fødselsnummer": "12020052345",
                            "aktørId": "12345"
                        }
                    },
                    {
                        "kontekstType": "Arbeidsgiver",
                        "kontekstMap": {
                            "organisasjonsnummer": "987654321"
                        }
                    }
                ],
                "alvorlighetsgrad": "INFO",
                "melding": "Lager ny vedtaksperiode",
                "detaljer": {},
                "tidsstempel": "2020-05-15 11:21:58.028"
            },
            {
                "kontekster": [
                    {
                        "kontekstType": "Sykmelding",
                        "kontekstMap": {
                            "aktørId": "12345",
                            "fødselsnummer": "12020052345",
                            "organisasjonsnummer": "987654321",
                            "id": "15a35623-af26-4eb4-8d4e-443a1dd29b38"
                        }
                    },
                    {
                        "kontekstType": "Person",
                        "kontekstMap": {
                            "fødselsnummer": "12020052345",
                            "aktørId": "12345"
                        }
                    },
                    {
                        "kontekstType": "Arbeidsgiver",
                        "kontekstMap": {
                            "organisasjonsnummer": "987654321"
                        }
                    },
                    {
                        "kontekstType": "Vedtaksperiode",
                        "kontekstMap": {
                            "vedtaksperiodeId": "0cc3c20d-447a-4c08-a852-a25c3ea46599"
                        }
                    },
                    {
                        "kontekstType": "Tilstand",
                        "kontekstMap": {
                            "tilstand": "START"
                        }
                    },
                    {
                        "kontekstType": "Tilstand",
                        "kontekstMap": {
                            "tilstand": "MOTTATT_SYKMELDING_FERDIG_GAP"
                        }
                    }
                ],
                "alvorlighetsgrad": "INFO",
                "melding": "Fullført behandling av sykmelding",
                "detaljer": {},
                "tidsstempel": "2020-05-15 11:21:58.050"
            },
            {
                "kontekster": [
                    {
                        "kontekstType": "Søknad",
                        "kontekstMap": {
                            "aktørId": "12345",
                            "fødselsnummer": "12020052345",
                            "organisasjonsnummer": "987654321",
                            "id": "37e1bc4f-c552-4ea9-ae95-bf3942483b21"
                        }
                    },
                    {
                        "kontekstType": "Person",
                        "kontekstMap": {
                            "fødselsnummer": "12020052345",
                            "aktørId": "12345"
                        }
                    },
                    {
                        "kontekstType": "Arbeidsgiver",
                        "kontekstMap": {
                            "organisasjonsnummer": "987654321"
                        }
                    },
                    {
                        "kontekstType": "Vedtaksperiode",
                        "kontekstMap": {
                            "vedtaksperiodeId": "0cc3c20d-447a-4c08-a852-a25c3ea46599"
                        }
                    },
                    {
                        "kontekstType": "Tilstand",
                        "kontekstMap": {
                            "tilstand": "MOTTATT_SYKMELDING_FERDIG_GAP"
                        }
                    },
                    {
                        "kontekstType": "Tilstand",
                        "kontekstMap": {
                            "tilstand": "AVVENTER_GAP"
                        }
                    }
                ],
                "alvorlighetsgrad": "BEHOV",
                "behovtype": "Sykepengehistorikk",
                "melding": "Trenger sykepengehistorikk fra Infotrygd",
                "detaljer": {
                    "historikkFom": "2014-01-01",
                    "historikkTom": "2018-01-31"
                },
                "tidsstempel": "2020-05-15 11:21:58.121"
            }
        ]
    }
}
"""
