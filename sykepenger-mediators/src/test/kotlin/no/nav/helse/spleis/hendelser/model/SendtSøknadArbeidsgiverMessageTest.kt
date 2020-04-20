package no.nav.helse.spleis.hendelser.model

import io.mockk.ConstantAnswer
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.spleis.MessageMediator
import no.nav.helse.spleis.hendelser.SendtArbeidsgiverSøknader
import no.nav.helse.unit.spleis.hendelser.TestRapid
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class SendtSøknadArbeidsgiverMessageTest {

    @Test
    fun `valid json`() {
        rapid.sendTestMessage(validJson)
        assertTrue(recognizedMessage)
    }

    private var riverError = false
    private var riverSevere = false
    private var recognizedMessage = false
    @BeforeEach
    fun reset() {
        recognizedMessage = false
        riverError = false
        riverSevere = false
        rapid.reset()
    }

    private val messageMediator = mockk<MessageMediator>()
    private val rapid = TestRapid().apply {
        SendtArbeidsgiverSøknader(this, messageMediator)
    }
    init {
        every {
            messageMediator.onRecognizedMessage(any(), any())
        } answers {
            recognizedMessage = true
            ConstantAnswer(Unit)
        }
        every {
            messageMediator.onRiverError(any(), any(), any())
        } answers {
            riverError = true
            ConstantAnswer(Unit)
        }
        every {
            messageMediator.onRiverSevere(any(), any(), any())
        } answers {
            riverSevere = true
            ConstantAnswer(Unit)
        }
    }

    private val validJson = """
{
  "@event_name": "sendt_søknad_arbeidsgiver",
  "@id": "${UUID.randomUUID()}",
  "@opprettet": "2020-01-01T00:00:00.000",
  "id": "id",
  "fnr": "fnr",
  "fom": "2020-01-01",
  "tom": "2020-01-01",
  "type": "ARBEIDSTAKERE",
  "fravar": [],
  "status": "SENDT",
  "aktorId": "aktørid",
  "mottaker": "ARBEIDSGIVER",
  "sendtNav": null,
  "opprettet": "2020-01-01T00:00:00.000",
  "korrigerer": null,
  "korrigertAv": null,
  "arbeidsgiver": {
    "navn": "ARBEIDSGIVERNAVN",
    "orgnummer": "orgnr"
  },
  "avsendertype": "BRUKER",
  "ettersending": false,
  "sykmeldingId": "sykmeldingid",
  "egenmeldinger": [
    {
      "fom": "2020-01-01",
      "tom": "2020-01-01"
    }
  ],
  "soknadsperioder": [
    {
      "fom": "2020-01-01",
      "tom": "2020-01-01",
      "avtaltTimer": null,
      "faktiskGrad": null,
      "faktiskTimer": null,
      "sykmeldingsgrad": 100,
      "sykmeldingstype": "AKTIVITET_IKKE_MULIG"
    }
  ],
  "arbeidssituasjon": "ARBEIDSTAKER",
  "arbeidGjenopptatt": null,
  "papirsykmeldinger": [],
  "sendtArbeidsgiver": "2020-01-01T00:00:00.000",
  "startSyketilfelle": "2020-01-01",
  "sykmeldingSkrevet": "2020-01-01T00:00:00",
  "andreInntektskilder": [],
  "soktUtenlandsopphold": null,
  "arbeidsgiverForskutterer": null
}""".trimIndent()
}
