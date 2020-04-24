package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class V2MedlemskapstatusTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `putter på medlemskapstatus`() {
        val json = objectMapper.readTree(personJson)
        listOf(V2Medlemskapstatus()).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())
        assertTrue(migratedJson.toString().contains("medlemskapstatus")) { "Forventet å finne medlemskapstatus i JSON:\n${migratedJson.toPrettyString()}" }
    }
}

@Language("JSON")
private const val personJson = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "dataForVilkårsvurdering": {
            "erEgenAnsatt": false,
            "beregnetÅrsinntektFraInntektskomponenten": 466253.76,
            "avviksprosent": 0.1635209118742549,
            "antallOpptjeningsdagerErMinst": 602,
            "harOpptjening": true
          }
        },
        {
          "dataForVilkårsvurdering": null
        }
      ]
    }
  ]
}
"""
