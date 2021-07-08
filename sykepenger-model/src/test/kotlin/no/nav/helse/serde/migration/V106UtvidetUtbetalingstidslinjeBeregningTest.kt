package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class V106UtvidetUtbetalingstidslinjeBeregningTest {
    private val ID = UUID.randomUUID()
    private val OPPRETTET = LocalDateTime.now()

    @Test
    fun `Generasjonsbasert vilkårsprøving med vilkårsprøving gjort i Spleis`() {

        assertEquals(toNode(expected), migrer(original))
    }

    @Test
    fun `Generasjonsbasert vilkårsprøving med vilkårsprøving gjort i Infotrygd`() {
        assertEquals(toNode(expectedMedFlereArbeidsgivereOgFlereBeregninger), migrer(originalMedFlereArbeidsgivereOgFlereBeregninger))
    }

//    @Test
//    fun `Generasjonsbasert vilkårsprøving med flere vilkårsprøvinger`() {
//        assertEquals(toNode(expectedMedFlereVilkårsprøvinger), migrer(originalMedFlereVilkårsprøvinger))
//    }

    @Test
    fun `vilkårsgrunnlagHistorikk mangler`() {
        @Language("JSON")
        val originalJson = """{
            "skjemaVersjon": 105
        }"""

        @Language("JSON")
        val expectedJson = """{
            "skjemaVersjon": 106
        }"""

        assertEquals(toNode(expectedJson), migrer(originalJson))
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V106UtvidetUtbetalingstidslinjeBeregning()).migrate(toNode(json))

    @Language("JSON")
    private val original = """{
    "arbeidsgivere": [
      {
        "beregnetUtbetalingstidslinjer": [
          {
            "sykdomshistorikkElementId": "11ef5393-5fab-40a5-82a1-ad234011e619"
          }
        ],
        "inntektshistorikk": [
          {
            "id": "472a1c71-df9e-488b-8aa8-f98ac97c5702"
          }
        ]
      }
    ],
    "vilkårsgrunnlagHistorikk": [
      {
        "id": "bb85447d-83c2-460a-a646-ffb85b58b7c4"
      }
    ],
    "skjemaVersjon": 105
}"""

    @Language("JSON")
    private val expected = """{
    "arbeidsgivere": [
        {
            "beregnetUtbetalingstidslinjer": [
                {
                    "id": "6bb3e89a-18f3-42fb-abb5-1ee61ef08f30",
                    "sykdomshistorikkElementId": "fa8f92b0-5d22-456a-b7b8-6e7596a5bc80",
                    "vilkårsgrunnlagHistorikkInnslagId": "bb85447d-83c2-460a-a646-ffb85b58b7c4",
                    "inntektshistorikkInnslagId": "472a1c71-df9e-488b-8aa8-f98ac97c5702"
                }
            ],
            "inntektshistorikk": [
                {
                    "id": "472a1c71-df9e-488b-8aa8-f98ac97c5702"
                }
            ]
        }
    ],
    "vilkårsgrunnlagHistorikk": [
        {
            "id": "bb85447d-83c2-460a-a646-ffb85b58b7c4"
        }
    ],
    "skjemaVersjon": 106
}"""

    @Language("JSON")
    private val originalMedFlereArbeidsgivereOgFlereBeregninger = """{
    "arbeidsgivere": [
        {
            "beregnetUtbetalingstidslinjer": [
                {
                    "sykdomshistorikkElementId": "11ef5393-5fab-40a5-82a1-ad234011e619"
                },
                {
                    "sykdomshistorikkElementId": "11ef5393-5fab-40a5-82a1-ad234011e620"
                }
            ],
            "inntektshistorikk": [
                {
                    "id": "472a1c71-df9e-488b-8aa8-f98ac97c5702"
                }
            ]
        },
        {
            "beregnetUtbetalingstidslinjer": [
                {
                    "sykdomshistorikkElementId": "11ef5393-5fab-40a5-82a1-ad234011e619"
                }
            ],
            "inntektshistorikk": [
                {
                    "id": "472a1c71-df9e-488b-8aa8-f98ac97c5702"
                }
            ]
        }
    ],
    "vilkårsgrunnlagHistorikk": [
        {
            "id": "bb85447d-83c2-460a-a646-ffb85b58b7c4"
        }
    ],
    "skjemaVersjon": 105
}"""


    @Language("JSON")
    private val expectedMedFlereArbeidsgivereOgFlereBeregninger = """{
    "arbeidsgivere": [
        {
            "beregnetUtbetalingstidslinjer": [
                {
                    "id": "6bb3e89a-18f3-42fb-abb5-1ee61ef08f30",
                    "sykdomshistorikkElementId": "fa8f92b0-5d22-456a-b7b8-6e7596a5bc80",
                    "vilkårsgrunnlagHistorikkInnslagId": "36f4adad-8a69-4200-bf8c-c75c49f504f2",
                    "inntektshistorikkInnslagId": "8344a17a-3948-4419-b644-e832ec2cbbac",
                }
            ],
            "inntektshistorikk": [
                {
                    "id": "8344a17a-3948-4419-b644-e832ec2cbbac"
                }
            ]
        }
    ],
    "vilkårsgrunnlagHistorikk": [
        {
            "id": "36f4adad-8a69-4200-bf8c-c75c49f504f2"
        }
    ],
    "skjemaVersjon": 106
}"""
}
