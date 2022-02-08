package no.nav.helse.serde.migration

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.serde.serdeObjectMapper
import no.nav.helse.januar
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class V116LeggTilRefusjonshistorikkTest {

    private companion object {
        private val meldingsreferanseId1 = UUID.fromString("a7bed8ec-c54e-4477-aad1-c385d4fbd32d")
        private val meldingsreferanseId2 = UUID.randomUUID()
        private val meldingsreferanseId3 = UUID.randomUUID()
        private const val organisasjonsnummer1 = "987654321"
        private const val organisasjonsnummer2 = "987654322"
        private val førsteFraværsdag = 1.januar
        private val arbeidsgiverperioder = listOf(1.januar til 16.januar)
        private const val beløp = 31000.0
        private val sisteRefusjonsdag = 31.januar
        private val endringerIRefusjon = listOf(
            EndringIRefusjon(
                endringsdato = 22.januar,
                beløp = 30000.0
            )
        )

        private class EndringIRefusjon(
            private val endringsdato: LocalDate,
            private val beløp: Double
        ) {
            override fun toString() = """{"endringsdato":"$endringsdato", "beløp": $beløp}"""
            fun kontraktformat() = """{"endringsdato":"$endringsdato", "beloep": "$beløp"}"""
        }

        private fun Periode.toJson() = """{"fom":"$start", "tom":"$endInclusive"}"""
    }

    @Test
    fun `to arbeidsgivere med inntektsmelding på en`() {
        val migrert = migrer(originalToArbeidsgivereEnInntektsmelding) {
            mapOf(meldingsreferanseId1 to inntektsmelding(organisasjonsnummer1, meldingsreferanseId1))
        }

        val expected = toNode(expectedToArbeidsgivereEnInntektsmelding)

        assertEquals(expected, migrert)
    }

    @Test
    fun `har allererde refusjonshistorikk lagret`() {
        val migrert = migrer(originalHarAlleredeRefunsjonshistorikk) {
            mapOf(meldingsreferanseId1 to inntektsmelding(organisasjonsnummer2, meldingsreferanseId1))
        }

        val expected = toNode(expectedHarAlleredeRefunsjonshistorikk)

        assertEquals(expected, migrert)
    }

    @Test
    fun `flere inntektsmeldinger`() {
        val migrert = migrer(originalFlereInnteksmeldinger) {
            mapOf(
                meldingsreferanseId1 to inntektsmelding(organisasjonsnummer1, meldingsreferanseId1),
                meldingsreferanseId2 to inntektsmelding(organisasjonsnummer1, meldingsreferanseId2),
                meldingsreferanseId3 to inntektsmelding(organisasjonsnummer2, meldingsreferanseId3)
            )
        }

        val expected = toNode(expectedFlereInnektsmeldinger)

        assertEquals(expected, migrert)
    }

    @Test
    fun `filtrerer bort hendelser som ikke mer inntektsmeldinger`() {
        val migrert = migrer(originalIkkeRelevanteHendelser) {
            mapOf(
                meldingsreferanseId1 to Pair(
                    "test",
                    """{"@event_name":"test"}"""
                )
            )
        }

        val expected = toNode(expectedIkkeRelevanteHendelser)

        assertEquals(expected, migrert)
    }

    @Test
    fun `filtrerer bort inntektsmelding uten virksomhetsnummer`() {
        val migrert = migrer(originalIkkeRelevanteHendelser) {
            mapOf(meldingsreferanseId1 to inntektsmelding(null, meldingsreferanseId1))
        }

        val expected = toNode(expectedIkkeRelevanteHendelser)

        assertEquals(expected, migrert)
    }

    @Test
    fun `første fraværsdag, opphørsdato og beløp ikke satt`() {
        val migrert = migrer(originalOptionalFelt) {
            mapOf(
                meldingsreferanseId1 to inntektsmelding(
                    virksomhetsnummer = organisasjonsnummer1,
                    meldingsreferanseId = meldingsreferanseId1,
                    refusjonsbeløp = null,
                    opphørsdato = null,
                    førsteFraværsdag = null
                )
            )
        }

        val expected = toNode(expectedOptionalFelt)

        assertEquals(expected, migrert)
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String, meldingSupplier: MeldingerSupplier) = listOf(V116LeggTilRefusjonshistorikk()).migrate(toNode(json), meldingSupplier)


    private fun inntektsmelding(
        virksomhetsnummer: String?,
        meldingsreferanseId: UUID,
        refusjonsbeløp: Double? = beløp,
        opphørsdato: LocalDate? = sisteRefusjonsdag,
        førsteFraværsdag: LocalDate? = Companion.førsteFraværsdag
    ) = Pair(
        "inntektsmelding", """
        {
            "inntektsmeldingId": "innteksmeldingId",
            "arbeidstakerFnr": "20046913337",
            "arbeidstakerAktorId": "aktørId",
            "virksomhetsnummer": ${virksomhetsnummer?.let { "\"$it\"" }},
            "arbeidsgiverFnr": null,
            "arbeidsgiverAktorId": null,
            "begrunnelseForReduksjonEllerIkkeUtbetalt": null,
            "arbeidsgivertype": "VIRKSOMHET",
            "arbeidsforholdId": null,
            "beregnetInntekt": "1.00",
            "refusjon": {
                "beloepPrMnd": ${refusjonsbeløp?.let { "\"$it\"" }},
                "opphoersdato": ${opphørsdato?.let { "\"$it\"" }}
            },
            "endringIRefusjoner": ${endringerIRefusjon.map { it.kontraktformat() }},
            "opphoerAvNaturalytelser": [],
            "gjenopptakelseNaturalytelser": [],
            "arbeidsgiverperioder": ${arbeidsgiverperioder.map { it.toJson() }},
            "status": "GYLDIG",
            "arkivreferanse": "",
            "ferieperioder": [
                {
                    "fom": "2021-10-15",
                    "tom": "2021-10-15"
                }
            ],
            ${førsteFraværsdag?.let { """"foersteFravaersdag": "$førsteFraværsdag",""" } ?: ""}
            "mottattDato": "2021-10-15T15:10:58.961689",
            "@id": "$meldingsreferanseId",
            "@event_name": "inntektsmelding",
            "@opprettet": "2021-10-15T15:11:01.262131"
        }
    """
    )

    @Language("JSON")
    private val originalToArbeidsgivereEnInntektsmelding = """{
        "fødselsnummer": "20046913337",
        "arbeidsgivere": [
            {
                "organisasjonsnummer": "$organisasjonsnummer1"
            },
            {
                "organisasjonsnummer": "$organisasjonsnummer2"
            }
        ],
        "skjemaVersjon": 115
    }
    """

    @Language("JSON")
    private val expectedToArbeidsgivereEnInntektsmelding = """{
    "fødselsnummer": "20046913337",
    "arbeidsgivere": [
        {
            "organisasjonsnummer": "987654321",
            "refusjonshistorikk": [
                {
                    "meldingsreferanseId": "a7bed8ec-c54e-4477-aad1-c385d4fbd32d",
                    "førsteFraværsdag": "2018-01-01",
                    "arbeidsgiverperioder": [
                        {
                            "fom": "2018-01-01",
                            "tom": "2018-01-16"
                        }
                    ],
                    "beløp": 31000.0,
                    "sisteRefusjonsdag": "2018-01-31",
                    "endringerIRefusjon": [
                        {
                            "endringsdato": "2018-01-22",
                            "beløp": 30000.0
                        }
                    ],
                    "tidsstempel": "2021-10-15T15:11:01.262131"
                }
            ]
        },
        {
            "organisasjonsnummer": "987654322",
            "refusjonshistorikk": []
        }
    ],
    "skjemaVersjon": 116
}
    """

    @Language("JSON")
    private val originalHarAlleredeRefunsjonshistorikk = """{
        "fødselsnummer": "20046913337",
        "arbeidsgivere": [
            {
                "organisasjonsnummer": "$organisasjonsnummer1",
                "refusjonshistorikk": []
            },
            {
                "organisasjonsnummer": "$organisasjonsnummer2",
                "refusjonshistorikk": [
                    {
                        "meldingsreferanseId": "$meldingsreferanseId1",
                        "førsteFraværsdag": "$førsteFraværsdag",
                        "arbeidsgiverperioder": ${arbeidsgiverperioder.map { it.toJson() }},
                        "beløp": $beløp,
                        "opphørsdato": "$sisteRefusjonsdag",
                        "endringerIRefusjon": $endringerIRefusjon,
                        "tidsstempel": "2021-10-15T15:11:01.262131"
                    }
                ]
            }
        ],
        "skjemaVersjon": 115
    }
    """

    @Language("JSON")
    private val expectedHarAlleredeRefunsjonshistorikk = """{
        "fødselsnummer": "20046913337",
        "arbeidsgivere": [
            {
                "organisasjonsnummer": "$organisasjonsnummer1",
                "refusjonshistorikk": []
            },
            {
                "organisasjonsnummer": "$organisasjonsnummer2",
                "refusjonshistorikk": [
                    {
                        "meldingsreferanseId": "$meldingsreferanseId1",
                        "førsteFraværsdag": "$førsteFraværsdag",
                        "arbeidsgiverperioder": ${arbeidsgiverperioder.map { it.toJson() }},
                        "beløp": $beløp,
                        "sisteRefusjonsdag": "$sisteRefusjonsdag",
                        "endringerIRefusjon": $endringerIRefusjon,
                        "tidsstempel": "2021-10-15T15:11:01.262131"
                    }
                ]
            }
        ],
        "skjemaVersjon": 116
    }
    """

    @Language("JSON")
    private val originalFlereInnteksmeldinger = """{
        "fødselsnummer": "20046913337",
        "arbeidsgivere": [
            {
                "organisasjonsnummer": "$organisasjonsnummer1"
            },
            {
                "organisasjonsnummer": "$organisasjonsnummer2"
            }
        ],
        "skjemaVersjon": 115
    }
    """

    @Language("JSON")
    private val expectedFlereInnektsmeldinger = """{
    "fødselsnummer": "20046913337",
    "arbeidsgivere": [
        {
            "organisasjonsnummer": "$organisasjonsnummer1",
            "refusjonshistorikk": [
                {
                    "meldingsreferanseId": "$meldingsreferanseId1",
                    "førsteFraværsdag": "$førsteFraværsdag",
                    "arbeidsgiverperioder": ${arbeidsgiverperioder.map { it.toJson() }},
                    "beløp": $beløp,
                    "sisteRefusjonsdag": "$sisteRefusjonsdag",
                    "endringerIRefusjon": $endringerIRefusjon,
                    "tidsstempel": "2021-10-15T15:11:01.262131"
                },
                {
                    "meldingsreferanseId": "$meldingsreferanseId2",
                    "førsteFraværsdag": "$førsteFraværsdag",
                    "arbeidsgiverperioder": ${arbeidsgiverperioder.map { it.toJson() }},
                    "beløp": $beløp,
                    "sisteRefusjonsdag": "$sisteRefusjonsdag",
                    "endringerIRefusjon": $endringerIRefusjon,
                    "tidsstempel": "2021-10-15T15:11:01.262131"
                }
            ]
        },
        {
            "organisasjonsnummer": "$organisasjonsnummer2",
            "refusjonshistorikk": [
                {
                    "meldingsreferanseId": "$meldingsreferanseId3",
                    "førsteFraværsdag": "$førsteFraværsdag",
                    "arbeidsgiverperioder": ${arbeidsgiverperioder.map { it.toJson() }},
                    "beløp": $beløp,
                    "sisteRefusjonsdag": "$sisteRefusjonsdag",
                    "endringerIRefusjon": $endringerIRefusjon,
                    "tidsstempel": "2021-10-15T15:11:01.262131"
                }
            ]
        }
    ],
    "skjemaVersjon": 116
}
    """

    @Language("JSON")
    private val originalIkkeRelevanteHendelser = """{
        "fødselsnummer": "20046913337",
        "arbeidsgivere": [
            {
                "organisasjonsnummer": "$organisasjonsnummer1"
            },
            {
                "organisasjonsnummer": "$organisasjonsnummer2"
            }
        ],
        "skjemaVersjon": 115
    }
    """

    @Language("JSON")
    private val expectedIkkeRelevanteHendelser = """{
        "fødselsnummer": "20046913337",
        "arbeidsgivere": [
            {
                "organisasjonsnummer": "$organisasjonsnummer1",
                "refusjonshistorikk": []
            },
            {
                "organisasjonsnummer": "$organisasjonsnummer2",
                "refusjonshistorikk": []
            }
        ],
        "skjemaVersjon": 116
    }
    """

    @Language("JSON")
    private val originalOptionalFelt = """{
        "fødselsnummer": "20046913337",
        "arbeidsgivere": [
            {
                "organisasjonsnummer": "$organisasjonsnummer1"
            },
            {
                "organisasjonsnummer": "$organisasjonsnummer2"
            }
        ],
        "skjemaVersjon": 115
    }
    """

    @Language("JSON")
    private val expectedOptionalFelt = """{
    "fødselsnummer": "20046913337",
    "arbeidsgivere": [
        {
            "organisasjonsnummer": "987654321",
            "refusjonshistorikk": [
                {
                    "meldingsreferanseId": "a7bed8ec-c54e-4477-aad1-c385d4fbd32d",
                    "førsteFraværsdag": null,
                    "arbeidsgiverperioder": [
                        {
                            "fom": "2018-01-01",
                            "tom": "2018-01-16"
                        }
                    ],
                    "beløp": null,
                    "sisteRefusjonsdag": null,
                    "endringerIRefusjon": [
                        {
                            "endringsdato": "2018-01-22",
                            "beløp": 30000.0
                        }
                    ],
                    "tidsstempel": "2021-10-15T15:11:01.262131"
                }
            ]
        },
        {
            "organisasjonsnummer": "987654322",
            "refusjonshistorikk": []
        }
    ],
    "skjemaVersjon": 116
}
    """
}
