package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V88InfotrygdhistorikkInntekterLagretTest {
    @Test
    fun `setter lagretInntekter når inntekt har blitt lagret tidligere`() {
        assertEquals(migrer(person), toNode(expectedPerson))
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V88InfotrygdhistorikkInntekterLagret())
        .migrate(toNode(json))

    @Language("JSON")
    private val person   = """
{
  "arbeidsgivere": [
    {
      "inntektshistorikk": [
        {
          "inntektsopplysninger": [
            {
              "hendelseId": "4a47352d-eb25-4d45-9478-e5fb7d43ceb8",
              "tidsstempel": "2021-02-03T11:02:35.079302"
            },
            {
              "id": "b3614ebd-8b14-4cf9-99ac-08ad80e556f9"
            },
            {
              "hendelseId": "bbafa06d-ae91-4d55-b84b-0594d9fa91a9",
              "tidsstempel": "2021-03-24T18:59:02.205767"
            }
          ]
        }
      ]
    },
    {
      "inntektshistorikk": [
        {
          "inntektsopplysninger": [
            {
              "hendelseId": "d76d15f3-5302-4c56-b73a-76473cf7374c",
              "tidsstempel": "2021-01-01T12:00:00.000101"
            }
          ]
        }
      ]
    }
  ],
  "infotrygdhistorikk": [
    {
      "id": "bbafa06d-ae91-4d55-b84b-0594d9fa91a9",
      "inntekter": [
        {
          "orgnr": "123456789",
          "sykepengerFom": "2021-01-01",
          "inntekt": 25000,
          "refusjonTilArbeidsgiver": true,
          "refusjonTom": null
        }
      ]
    },
    {
      "id": "d76d15f3-5302-4c56-b73a-76473cf7374c",
      "inntekter": [
        {
          "orgnr": "987654321",
          "sykepengerFom": "2021-01-01",
          "inntekt": 25000,
          "refusjonTilArbeidsgiver": true,
          "refusjonTom": null
        }
      ]
    },
    {
      "id": "fb706e23-bd78-418b-8db5-d94db3b18ce4",
      "inntekter": [
        {
          "orgnr": "112233445",
          "sykepengerFom": "2021-01-01",
          "inntekt": 25000,
          "refusjonTilArbeidsgiver": true,
          "refusjonTom": null
        }
      ]
    }
  ],
  "skjemaVersjon": 87
}
"""

    @Language("JSON")
    val expectedPerson = """
{
  "arbeidsgivere": [
    {
      "inntektshistorikk": [
        {
          "inntektsopplysninger": [
            {
              "hendelseId": "4a47352d-eb25-4d45-9478-e5fb7d43ceb8",
              "tidsstempel": "2021-02-03T11:02:35.079302"
            },
            {
              "id": "b3614ebd-8b14-4cf9-99ac-08ad80e556f9"
            },
            {
              "hendelseId": "bbafa06d-ae91-4d55-b84b-0594d9fa91a9",
              "tidsstempel": "2021-03-24T18:59:02.205767"
            }
          ]
        }
      ]
    },
    {
      "inntektshistorikk": [
        {
          "inntektsopplysninger": [
            {
              "hendelseId": "d76d15f3-5302-4c56-b73a-76473cf7374c",
              "tidsstempel": "2021-01-01T12:00:00.000101"
            }
          ]
        }
      ]
    }
  ],
  "infotrygdhistorikk": [
    {
      "id": "bbafa06d-ae91-4d55-b84b-0594d9fa91a9",
      "lagretInntekter": true,
      "lagretVilkårsgrunnlag": true,
      "inntekter": [
        {
          "orgnr": "123456789",
          "sykepengerFom": "2021-01-01",
          "inntekt": 25000,
          "refusjonTilArbeidsgiver": true,
          "refusjonTom": null,
          "lagret": "2021-03-24T18:59:02.205767"
        }
      ]
    },
    {
      "id": "d76d15f3-5302-4c56-b73a-76473cf7374c",
      "lagretInntekter": true,
      "lagretVilkårsgrunnlag": true,
      "inntekter": [
        {
          "orgnr": "987654321",
          "sykepengerFom": "2021-01-01",
          "inntekt": 25000,
          "refusjonTilArbeidsgiver": true,
          "refusjonTom": null,
          "lagret": "2021-01-01T12:00:00.000101"
        }
      ]
    },
    {
      "id": "fb706e23-bd78-418b-8db5-d94db3b18ce4",
      "lagretInntekter": false,
      "lagretVilkårsgrunnlag": false,
      "inntekter": [
        {
          "orgnr": "112233445",
          "sykepengerFom": "2021-01-01",
          "inntekt": 25000,
          "refusjonTilArbeidsgiver": true,
          "refusjonTom": null
        }
      ]
    }
  ],
  "skjemaVersjon": 88
}
    """
}
