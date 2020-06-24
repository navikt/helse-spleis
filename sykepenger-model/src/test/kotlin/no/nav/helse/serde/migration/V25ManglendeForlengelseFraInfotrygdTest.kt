package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.ForlengelseFraInfotrygd
import no.nav.helse.testhelpers.april
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class V25ManglendeForlengelseFraInfotrygdTest {

    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `ferdigbygd json blir migrert riktig`() {
        val original = objectMapper.readTree(testperson(
            Pair(Periode(1.januar, 31.januar), ForlengelseFraInfotrygd.JA),
            Pair(Periode(1.februar, 10.februar), ForlengelseFraInfotrygd.IKKE_ETTERSPURT),
            Pair(Periode(11.februar, 25.februar), ForlengelseFraInfotrygd.IKKE_ETTERSPURT),
            skjemaversjon = 24
        ))

        val expected = objectMapper.readTree(testperson(
            Pair(Periode(1.januar, 31.januar), ForlengelseFraInfotrygd.JA),
            Pair(Periode(1.februar, 10.februar), ForlengelseFraInfotrygd.JA),
            Pair(Periode(11.februar, 25.februar), ForlengelseFraInfotrygd.JA),
            skjemaversjon = 25
        ))

        Assertions.assertEquals(expected, listOf(V25ManglendeForlengelseFraInfotrygd()).migrate(original))
    }

    @Test
    fun `forlengelseFraInfotrygd overføres kun til forlengelser`() {
        val original = objectMapper.readTree(testperson(
            Pair(Periode(1.januar, 31.januar), ForlengelseFraInfotrygd.JA),
            Pair(Periode(1.februar, 10.februar), ForlengelseFraInfotrygd.IKKE_ETTERSPURT),
            Pair(Periode(11.februar, 25.februar), ForlengelseFraInfotrygd.IKKE_ETTERSPURT),
            Pair(Periode(1.april, 20.april), ForlengelseFraInfotrygd.NEI),
            skjemaversjon = 24
        ))

        val expected = objectMapper.readTree(testperson(
            Pair(Periode(1.januar, 31.januar), ForlengelseFraInfotrygd.JA),
            Pair(Periode(1.februar, 10.februar), ForlengelseFraInfotrygd.JA),
            Pair(Periode(11.februar, 25.februar), ForlengelseFraInfotrygd.JA),
            Pair(Periode(1.april, 20.april), ForlengelseFraInfotrygd.NEI),
            skjemaversjon = 25
        ))

        Assertions.assertEquals(expected, listOf(V25ManglendeForlengelseFraInfotrygd()).migrate(original))
    }

}

@Language("JSON")
private fun testperson(vararg perioder: Pair<Periode, ForlengelseFraInfotrygd>, skjemaversjon: Int) =
    """
    {
      "arbeidsgivere": [
        {
          "vedtaksperioder": [
            ${perioder.joinToString { (periode, forlengelseFraInfotrygd) ->
                """
                {
                    "fom": "${periode.start}",
                    "tom": "${periode.endInclusive}",
                    "forlengelseFraInfotrygd": "${forlengelseFraInfotrygd.name}"
                }
                """.trimIndent()
            }}
          ]
        }
      ],
      "skjemaVersjon": $skjemaversjon
    }
    """.trimIndent()
