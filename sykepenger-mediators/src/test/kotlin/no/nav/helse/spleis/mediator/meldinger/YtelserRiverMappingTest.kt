package no.nav.helse.spleis.mediator.meldinger

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import java.util.UUID
import no.nav.helse.hendelser.SelvstendigForsikring
import no.nav.helse.januar
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.YtelserRiver
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import no.nav.helse.spleis.meldinger.model.YtelserMessage
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull

internal class YtelserRiverMappingTest {

    private var forrigeHendelseMessage: HendelseMessage? = null

    private val testMessageMediator = object: IMessageMediator {
        override fun onRecognizedMessage(message: HendelseMessage, context: MessageContext) {
            forrigeHendelseMessage = message
        }
        override fun onRiverError(riverName: String, problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
            forrigeHendelseMessage = null
        }
    }

    private val rapid = TestRapid().apply {
        YtelserRiver(this, testMessageMediator)
    }

    @Test
    fun `Mapping ved manglende løsning`() {
        rapid.sendTestMessage(json)
        checkNotNull(forrigeHendelseMessage) { "Forrige hendelse er null!" }
        val ytelser = checkNotNull(forrigeHendelseMessage as? YtelserMessage) { "Forrige hendelse er ikke ytelser!" }
        assertNull(ytelser.selvstendigForsikring) { "Forventet ingen forsikring her" }
    }

    @Test
    fun `Mapping ved ingen forsikring - array edition`() {
        rapid.sendTestMessage(medSelvstendigForsikringLøsning("[]"))
        checkNotNull(forrigeHendelseMessage) { "Forrige hendelse er null!" }
        val ytelser = checkNotNull(forrigeHendelseMessage as? YtelserMessage) { "Forrige hendelse er ikke ytelser!" }
        assertNull(ytelser.selvstendigForsikring) { "Forventet ingen forsikring her" }
    }

    @Test
    fun `Mapping ved ingen forsikring - object edition`() {
        @Language("JSON")
        val selvstendigForsikring = """
        {
          "forsikringer": []
        }
        """
        rapid.sendTestMessage(medSelvstendigForsikringLøsning(selvstendigForsikring))
        checkNotNull(forrigeHendelseMessage) { "Forrige hendelse er null!" }
        val ytelser = checkNotNull(forrigeHendelseMessage as? YtelserMessage) { "Forrige hendelse er ikke ytelser!" }
        assertNull(ytelser.selvstendigForsikring) { "Forventet ingen forsikring her" }
    }

    @Test
    fun `Mapping ved forsikring - array edition`() {
        @Language("JSON")
        val selvstendigForsikring = """[{
          "startdato": "2019-01-01",
          "forsikringstype": "ÅttiProsentFraDagEn",
          "premiegrunnlag": 500000,
          "sluttdato": null
        }]"""

        rapid.sendTestMessage(medSelvstendigForsikringLøsning(selvstendigForsikring))
        checkNotNull(forrigeHendelseMessage) { "Forrige hendelse er null!" }
        val ytelser = checkNotNull(forrigeHendelseMessage as? YtelserMessage) { "Forrige hendelse er ikke ytelser!" }
        val forsikring = checkNotNull(ytelser.selvstendigForsikring) { "Selvstendig forikring er null!" }

        val forventetForsikring = SelvstendigForsikring(
            virkningsdato = 1.januar(2019),
            opphørsdato = null,
            type = SelvstendigForsikring.Forsikringstype.ÅttiProsentFraDagEn,
            premiegrunnlag = 500_000.årlig
        )

        assertEquals(forventetForsikring, forsikring)
    }

    @Test
    fun `Mapping ved forsikring - object edition`() {
        @Language("JSON")
        val selvstendigForsikring = """
           {
              "forsikringer": [{
                  "startdato": "2019-01-01",
                  "forsikringstype": "HundreProsentFraDagSytten",
                  "premiegrunnlag": 505000,
                  "sluttdato": "2020-01-01"
              }]
           } 
        """

        rapid.sendTestMessage(medSelvstendigForsikringLøsning(selvstendigForsikring))
        checkNotNull(forrigeHendelseMessage) { "Forrige hendelse er null!" }
        val ytelser = checkNotNull(forrigeHendelseMessage as? YtelserMessage) { "Forrige hendelse er ikke ytelser!" }
        val forsikring = checkNotNull(ytelser.selvstendigForsikring) { "Selvstendig forikring er null!" }

        val forventetForsikring = SelvstendigForsikring(
            virkningsdato = 1.januar(2019),
            opphørsdato = 1.januar(2020),
            type = SelvstendigForsikring.Forsikringstype.HundreProsentFraDagSytten,
            premiegrunnlag = 505_000.årlig
        )

        assertEquals(forventetForsikring, forsikring)
    }

    private fun medSelvstendigForsikringLøsning(løsning: String): String {
        val json = jacksonObjectMapper().readTree(json)
        val løsninger = json.path("@løsning") as ObjectNode
        løsninger.replace("SelvstendigForsikring", jacksonObjectMapper().readTree(løsning))
        return json.toString()
    }
}

@Language("JSON")
private val json = """
  {
      "@event_name": "behov",
      "tilstand": "AVVENTER_HISTORIKK",
      "@behov": [
        "Sykepengehistorikk",
        "Foreldrepenger",
        "Pleiepenger",
        "Omsorgspenger",
        "Opplæringspenger",
        "Institusjonsopphold",
        "ArbeidsavklaringspengerV2",
        "InntekterForBeregning",
        "DagpengerV2"
      ],
      "@id": "${UUID.randomUUID()}",
      "@opprettet": "2020-01-24T11:25:00",
      "hendelse": "Ytelser",
      "fødselsnummer": "08127411111",
      "organisasjonsnummer": "SELVSTENDIG",
      "yrkesaktivitetstype": "SELVSTENDIG",
      "vedtaksperiodeId": "${UUID.randomUUID()}",
      "@løsning": {
        "Foreldrepenger": {
          "Foreldrepengeytelse": {
              "fom": "2019-03-13",
              "tom": "2019-04-21",
              "vedtatt": "2023-03-13T06:49:01.570",
              "perioder": [
                {
                  "fom": "2019-03-13",
                  "tom": "2019-04-21",
                  "grad": "90"
                }
              ]
            },
          "Svangerskapsytelse": null
        },
        "Sykepengehistorikk": [
          {
            "statslønn":  true,
            "inntektsopplysninger": [
              {
                "sykepengerFom": "2019-03-27",
                "inntekt": 36000,
                "orgnummer": "orgnummer",
                "refusjonTom": null,
                "refusjonTilArbeidsgiver": true
              }
            ],
            "utbetalteSykeperioder": [
              {
                "fom": "2019-03-28",
                "tom": "2019-04-12",
                "utbetalingsGrad": "100",
                "oppgjorsType": "",
                "utbetalt": "2019-04-23",
                "dagsats": 1400.0,
                "typeKode": "5",
                "typeTekst": "ArbRef",
                "orgnummer": "orgnummer",
                "inntektPerMåned": 36000
              }
            ],
            "arbeidsKategoriKode": "01"
          }
        ],
        "Pleiepenger": [
          {
            "fom": "2019-03-11",
            "tom": "2019-04-12",
            "grad": "100"
          }
        ],
        "Omsorgspenger": [
          {
            "fom": "2019-03-11",
            "tom": "2019-04-12",
            "grad": "100"
          }
        ],
        "Opplæringspenger": [
          {
            "fom": "2019-03-11",
            "tom": "2019-04-12",
            "grad": "100"
          }
        ],
        "Institusjonsopphold": [
          {
            "startdato": "2019-03-11",
            "faktiskSluttdato": "2019-04-12",
            "institusjonstype": "FO",
            "kategori": "S"
          }
        ],
        "ArbeidsavklaringspengerV2": {
          "utbetalingsperioder": [
            {
              "fom": "2019-03-11",
              "tom": "2019-04-12"
            }
          ]
        },
        "InntekterForBeregning": {
          "inntekter": [
            {
              "fom": "2019-03-11",
              "tom": "2019-04-12",
              "inntektskilde": "Operahuset",
              "daglig": 220.123
            },
            {
              "fom": "2019-03-11",
              "tom": "2019-04-12",
              "inntektskilde": "Nationalteatret",
              "måndelig": 6920.123
            },
            {
              "fom": "2019-03-11",
              "tom": "2019-04-12",
              "inntektskilde": "Deichmanske",
              "årlig": 220213.123
            }
          ]
        },
        "Dagpenger": {
          "meldekortperioder": [
            {
              "fom": "2019-03-11",
              "tom": "2019-04-12"
            }
          ]
        },
        "DagpengerV2": {
          "meldekortperioder": [
            {
              "fom": "2019-03-11",
              "tom": "2019-04-12"
            }
          ]
        }

      },
      "@final": true,
      "@besvart": "2020-01-24T11:25:00"
    }
"""
