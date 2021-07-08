package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.math.exp

internal class V105VilkårsgrunnlagMedGenerasjonerTest {
    private val ID = UUID.randomUUID()
    private val OPPRETTET = LocalDateTime.now()

    @Test
    fun `Generasjonsbasert vilkårsprøving med vilkårsprøving gjort i Spleis`() {

        assertEquals(toNode(expected), migrer(original))
    }

    @Test
    fun `Generasjonsbasert vilkårsprøving med vilkårsprøving gjort i Infotrygd`() {
        assertEquals(toNode(expectedMedInfotrygd), migrer(originalMedInfotrygd))
    }

    @Test
    fun `Generasjonsbasert vilkårsprøving med flere vilkårsprøvinger`() {
        assertEquals(toNode(expectedMedFlereVilkårsprøvinger), migrer(originalMedFlereVilkårsprøvinger))
    }

    @Test
    fun `vilkårsgrunnlagHistorikk mangler`() {
        @Language("JSON")
        val originalJson = """{
            "skjemaVersjon": 104
        }"""

        @Language("JSON")
        val expectedJson = """{
            "skjemaVersjon": 105
        }"""

        assertEquals(toNode(expectedJson), migrer(originalJson))
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V105VilkårsgrunnlagMedGenerasjoner(ID, OPPRETTET)).migrate(toNode(json))

    @Language("JSON")
    private val original = """{
  "vilkårsgrunnlagHistorikk": [
    {
      "skjæringstidspunkt": "2018-01-01",
      "type": "Vilkårsprøving",
      "antallOpptjeningsdagerErMinst": 365,
      "avviksprosent": 0.0,
      "sammenligningsgrunnlag": 372000.0,
      "harOpptjening": true,
      "medlemskapstatus": "JA",
      "harMinimumInntekt": true,
      "vurdertOk": true,
      "meldingsreferanseId": "67fffaa8-fbd6-412f-81bc-7613cd44ff46"
    }
  ],
  "skjemaVersjon": 104
}"""

    @Language("JSON")
    private val originalMedInfotrygd = """{
  "vilkårsgrunnlagHistorikk": [
    {
      "skjæringstidspunkt": "2017-12-01",
      "type": "Infotrygd"
    }
  ],
  "skjemaVersjon": 104
}
"""

    @Language("JSON")
    private val originalMedFlereVilkårsprøvinger = """{
  "vilkårsgrunnlagHistorikk": [
    {
      "skjæringstidspunkt": "2017-12-01",
      "type": "Infotrygd"
    },
    {
      "skjæringstidspunkt": "2018-01-01",
      "type": "Vilkårsprøving",
      "antallOpptjeningsdagerErMinst": 365,
      "avviksprosent": 0.0,
      "sammenligningsgrunnlag": 372000.0,
      "harOpptjening": true,
      "medlemskapstatus": "JA",
      "harMinimumInntekt": true,
      "vurdertOk": true,
      "meldingsreferanseId": "67fffaa8-fbd6-412f-81bc-7613cd44ff46"
    }
  ],
  "skjemaVersjon": 104
}
"""

    @Language("JSON")
    private val expected = """
{
  "vilkårsgrunnlagHistorikk": [
    {
      "id": "$ID",
      "opprettet": "$OPPRETTET",
      "vilkårsgrunnlag": [
        {
          "skjæringstidspunkt": "2018-01-01",
          "type": "Vilkårsprøving",
          "antallOpptjeningsdagerErMinst": 365,
          "avviksprosent": 0.0,
          "sammenligningsgrunnlag": 372000.0,
          "harOpptjening": true,
          "medlemskapstatus": "JA",
          "harMinimumInntekt": true,
          "vurdertOk": true,
          "meldingsreferanseId": "67fffaa8-fbd6-412f-81bc-7613cd44ff46"
        }
      ]
    }
  ],
  "skjemaVersjon": 105
}
"""

    @Language("JSON")
    private val expectedMedInfotrygd = """
{
  "vilkårsgrunnlagHistorikk": [
    {
      "id": "$ID",
      "opprettet": "$OPPRETTET",
      "vilkårsgrunnlag": [
        {
          "skjæringstidspunkt": "2017-12-01",
          "type": "Infotrygd"
        }
      ]
    }
  ],
  "skjemaVersjon": 105
}
"""

    @Language("JSON")
    private val expectedMedFlereVilkårsprøvinger = """
{
  "vilkårsgrunnlagHistorikk": [
    {
      "id": "$ID",
      "opprettet": "$OPPRETTET",
      "vilkårsgrunnlag": [
        {
          "skjæringstidspunkt": "2017-12-01",
          "type": "Infotrygd"
        },
        {
          "skjæringstidspunkt": "2018-01-01",
          "type": "Vilkårsprøving",
          "antallOpptjeningsdagerErMinst": 365,
          "avviksprosent": 0.0,
          "sammenligningsgrunnlag": 372000.0,
          "harOpptjening": true,
          "medlemskapstatus": "JA",
          "harMinimumInntekt": true,
          "vurdertOk": true,
          "meldingsreferanseId": "67fffaa8-fbd6-412f-81bc-7613cd44ff46"
        }
      ]
    }
  ],
  "skjemaVersjon": 105
}
"""
}
