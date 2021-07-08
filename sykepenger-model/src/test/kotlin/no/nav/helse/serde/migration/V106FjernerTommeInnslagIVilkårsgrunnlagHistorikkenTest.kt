package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class V106FjernerTommeInnslagIVilkårsgrunnlagHistorikkenTest {
    private val ID = UUID.randomUUID()
    private val OPPRETTET = LocalDateTime.now()

    @Test
    fun `Fjerner innslag uten vilkårsgrunnlag`() {

        assertEquals(toNode(expectedUtenVilkårsgrunnlag), migrer(originalUtenVilkårsgrunnlag))
    }

    @Test
    fun `Fjerner ikke innslag med vilkårsgrunnlag`() {

        assertEquals(toNode(expectedMedVilkårsgrunnlag), migrer(originalMedVilkårsgrunnlag))
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V106FjernerTommeInnslagIVilkårsgrunnlagHistorikken()).migrate(toNode(json))

    @Language("JSON")
    private val originalUtenVilkårsgrunnlag = """{
  "vilkårsgrunnlagHistorikk": [
    {
      "id": "$ID",
      "opprettet": "$OPPRETTET",
      "vilkårsgrunnlag": []
    }
  ],
  "skjemaVersjon": 105
}"""

    @Language("JSON")
    private val expectedUtenVilkårsgrunnlag = """
{
  "vilkårsgrunnlagHistorikk": [],
  "skjemaVersjon": 106
}
"""

    @Language("JSON")
    private val originalMedVilkårsgrunnlag = """{
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
}"""

    @Language("JSON")
    private val expectedMedVilkårsgrunnlag = """{
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
  "skjemaVersjon": 106
}"""

}
