package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V106SletteArbeidsforholdTest {

    @Test
    fun `Sletter arbeidsforhold`() {
        assertEquals(toNode(expected), migrer(original))
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V106SletteArbeidsforhold()).migrate(toNode(json))

    @Language("JSON")
    private val original = """
    {
      "arbeidsgivere": [
        {
          "arbeidsforholdhistorikk": [
            {
              "id": "f02eef12-7140-49f7-a1f5-ec88b1ea8faf",
              "arbeidsforhold": [
                {
                  "orgnummer": "arbeidsgiver 1",
                  "fom": "1970-01-01",
                  "tom": null
                }
              ]
            }
          ]
        },
        {
          "arbeidsforholdhistorikk": [
            {
              "id": "381ed08e-f261-4c64-a9df-545328b20ced",
              "arbeidsforhold": [
                {
                  "orgnummer": "arbeidsgiver 2",
                  "fom": "1970-01-01",
                  "tom": null
                }
              ]
            }
          ]
        }
      ],
      "skjemaVersjon": 105
    }
    """

    @Language("JSON")
    private val expected = """
    {
      "arbeidsgivere": [
        {
          "arbeidsforholdhistorikk": []
        },
        {
          "arbeidsforholdhistorikk": []
        }
      ],
      "skjemaVersjon": 106
    }
    """
}
