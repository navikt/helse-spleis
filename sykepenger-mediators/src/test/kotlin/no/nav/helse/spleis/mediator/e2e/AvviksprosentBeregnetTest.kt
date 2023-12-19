package no.nav.helse.spleis.mediator.e2e

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

internal class AvviksprosentBeregnetTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `Sender ut forventet avviksprosent_beregnet_event`() {
        nyttVedtak()
        assertEquals(1, testRapid.inspektør.meldinger("avviksprosent_beregnet_event").size)
        val event = testRapid.inspektør.meldinger("avviksprosent_beregnet_event").single()
        val faktiskResultat = event.json(
            "@event_name",
            "skjæringstidspunkt",
            "beregningsgrunnlagTotalbeløp",
            "sammenligningsgrunnlagTotalbeløp",
            "avviksprosent",
            "omregnedeÅrsinntekter",
            "sammenligningsgrunnlag",
            "aktørId",
            "fødselsnummer"
        )

        JSONAssert.assertEquals(forventetResultatAvviksprosentBeregnet, faktiskResultat, JSONCompareMode.STRICT)
    }

    private companion object {
        private fun JsonNode.json(vararg behold: String) = (this as ObjectNode).let { json ->
            json.remove(json.fieldNames().asSequence().minus(behold.toSet()).toList())
        }.toString()
    }
}

@Language("json")
val forventetResultatAvviksprosentBeregnet = """
        {
          "@event_name": "avviksprosent_beregnet_event",
          "skjæringstidspunkt": "2018-01-01",
          "beregningsgrunnlagTotalbeløp": 372000.0,
          "sammenligningsgrunnlagTotalbeløp": 372000.0,
          "avviksprosent": 0.0,
          "omregnedeÅrsinntekter": [
            {
              "orgnummer": "987654321",
              "beløp": 372000.0
            }
          ],
          "sammenligningsgrunnlag": [
            {
              "orgnummer": "987654321",
              "skatteopplysninger": [
                  {
                      "beløp": 31000.0,
                      "måned": "2017-01",
                      "type": "LØNNSINNTEKT",
                      "fordel": "kontantytelse",
                      "beskrivelse": "fastloenn"
                  },
                  {
                      "beløp": 31000.0,
                      "måned": "2017-02",
                      "type": "LØNNSINNTEKT",
                      "fordel": "kontantytelse",
                      "beskrivelse": "fastloenn"
                  },
                  {
                      "beløp": 31000.0,
                      "måned": "2017-03",
                      "type": "LØNNSINNTEKT",
                      "fordel": "kontantytelse",
                      "beskrivelse": "fastloenn"
                  },
                  {
                      "beløp": 31000.0,
                      "måned": "2017-04",
                      "type": "LØNNSINNTEKT",
                      "fordel": "kontantytelse",
                      "beskrivelse": "fastloenn"
                  },
                  {
                      "beløp": 31000.0,
                      "måned": "2017-05",
                      "type": "LØNNSINNTEKT",
                      "fordel": "kontantytelse",
                      "beskrivelse": "fastloenn"
                  },
                  {
                      "beløp": 31000.0,
                      "måned": "2017-06",
                      "type": "LØNNSINNTEKT",
                      "fordel": "kontantytelse",
                      "beskrivelse": "fastloenn"
                  },
                  {
                      "beløp": 31000.0,
                      "måned": "2017-07",
                      "type": "LØNNSINNTEKT",
                      "fordel": "kontantytelse",
                      "beskrivelse": "fastloenn"
                  },
                  {
                      "beløp": 31000.0,
                      "måned": "2017-08",
                      "type": "LØNNSINNTEKT",
                      "fordel": "kontantytelse",
                      "beskrivelse": "fastloenn"
                  },
                  {
                      "beløp": 31000.0,
                      "måned": "2017-09",
                      "type": "LØNNSINNTEKT",
                      "fordel": "kontantytelse",
                      "beskrivelse": "fastloenn"
                  },
                  {
                      "beløp": 31000.0,
                      "måned": "2017-10",
                      "type": "LØNNSINNTEKT",
                      "fordel": "kontantytelse",
                      "beskrivelse": "fastloenn"
                  },
                  {
                      "beløp": 31000.0,
                      "måned": "2017-11",
                      "type": "LØNNSINNTEKT",
                      "fordel": "kontantytelse",
                      "beskrivelse": "fastloenn"
                  },
                  {
                      "beløp": 31000.0,
                      "måned": "2017-12",
                      "type": "LØNNSINNTEKT",
                      "fordel": "kontantytelse",
                      "beskrivelse": "fastloenn"
                  }
              ]
            }
          ],
          "aktørId": "42",
          "fødselsnummer": "12029240045"
        }"""