package no.nav.helse.spleis.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.InntektsmeldingerReplayRiver
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class InntektsmeldingerReplayRiverTest : RiverTest() {
    override fun river(rapidsConnection: RapidsConnection, mediator: IMessageMediator) {
        InntektsmeldingerReplayRiver(rapidsConnection, mediator)
    }

    @Test
    fun `gjenkjenner replays`() {
        assertNoErrors(replaymelding)
    }
}

@Language("JSON")
private val replaymelding = """{
  "@event_name": "inntektsmeldinger_replay",
  "fødselsnummer": "fnr",
  "organisasjonsnummer": "a1",
  "vedtaksperiodeId": "ee655d8c-6d3e-4d1b-843d-8cf54380ed36",
  "replayId": 123456,
  "inntektsmeldinger": [
    {
      "internDokumentId": "2286f619-4b4d-4384-900b-7d612996eb3c",
      "inntektsmelding": {
        "inntektsmeldingId": "2e27f5bd-9e92-4456-ade6-a50af3832807",
        "vedtaksperiodeId": null,
        "matcherSpleis": true,
        "arbeidstakerFnr": "fnr",
        "arbeidstakerAktorId": "aktor",
        "virksomhetsnummer": "a1",
        "arbeidsgiverFnr": null,
        "arbeidsgiverAktorId": null,
        "innsenderFulltNavn": "Kari Nordmann",
        "innsenderTelefon": "12345678",
        "begrunnelseForReduksjonEllerIkkeUtbetalt": "",
        "bruttoUtbetalt": "31000.00",
        "arbeidsgivertype": "VIRKSOMHET",
        "arbeidsforholdId": "v1234",
        "beregnetInntekt": "31000.00",
        "inntektsdato": null,
        "refusjon": {
          "beloepPrMnd": "31000.00",
          "opphoersdato": null
        },
        "endringIRefusjoner": [],
        "opphoerAvNaturalytelser": [],
        "gjenopptakelseNaturalytelser": [],
        "arbeidsgiverperioder": [
          {
            "fom": "2025-01-01",
            "tom": "2025-01-01"
          },
          {
            "fom": "2025-01-03",
            "tom": "2025-01-17"
          }
        ],
        "status": "GYLDIG",
        "arkivreferanse": "ar12345",
        "ferieperioder": [],
        "foersteFravaersdag": "2025-01-01",
        "mottattDato": "2025-01-01T00:00:00",
        "naerRelasjon": false,
        "avsenderSystem": {
          "navn": "LPS",
          "versjon": "v1.0.0"
        },
        "inntektEndringAarsak": null,
        "arsakTilInnsending": "Ny"
      }
    }
  ],
  "@id": "09c5150d-c307-4839-b89b-c9e858a30182",
  "@opprettet": "2025-01-01T00:00:00.000"
}"""
