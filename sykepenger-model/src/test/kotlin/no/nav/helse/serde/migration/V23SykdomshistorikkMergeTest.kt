package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class V23SykdomshistorikkMergeTest {

    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `enkel vedtaksperiode`() {
        val sykmelding = DagKilde.Sykmelding()
        val inntektsmelding = DagKilde.Inntektsmelding()
        val søknad = DagKilde.Søknad()

        val sykmeldingElement = 2.februar.element(
            kilde = sykmelding,
            hendelseSykdomstidslinje = 2.januar.dager("SSSSHH SSSSSHH SSSSSHH", sykmelding),
            beregnetSykdomstidslinje = 2.januar.dager("SSSSHH SSSSSHH SSSSSHH", sykmelding)
        )
        val inntektsmeldingElement = 3.februar.element(
            kilde = inntektsmelding,
            hendelseSykdomstidslinje = 1.januar.dager("S", inntektsmelding),
            beregnetSykdomstidslinje = 1.januar.dager("S", inntektsmelding) + 2.januar.dager(
                "SSSSHH SSSSSHH SSSSSHH",
                sykmelding
            )
        )
        val søknadElement = 4.februar.element(
            kilde = søknad,
            hendelseSykdomstidslinje = 1.januar.dager("SSSSSHH SSSSSHH SSSSARR", søknad),
            beregnetSykdomstidslinje = 1.januar.dager("SSSSSHH SSSSSHH SSSSARR", søknad)
        )

        val historikk = historikk(sykmeldingElement, inntektsmeldingElement, søknadElement)
        val person = person(listOf(vedtaksperiode(historikk)))
        val expected = person(listOf(vedtaksperiode(historikk)), arbeidsgiverHistorikk = historikk, skjemaVersjon = 22)
        val migrated = listOf(V23SykdomshistorikkMerge()).migrate(person)

        assertEquals(expected, migrated)
    }

    @Test
    fun `to sykmeldinger fører til en kombinert beregnet sykdomstidslinje`() {
        val sykmelding1 = DagKilde.Sykmelding()
        val sykmelding2 = DagKilde.Sykmelding()

        val elementVedtaksperiode1 = 1.februar.element(
            kilde = sykmelding1,
            hendelseSykdomstidslinje = 1.januar.dager("SSSSSHH", sykmelding1),
            beregnetSykdomstidslinje = 1.januar.dager("SSSSSHH", sykmelding1)
        )
        val vedtaksperiode1 = vedtaksperiode(
            historikk(elementVedtaksperiode1)
        )
        val vedtaksperiode2 = vedtaksperiode(
            historikk(
                2.februar.element(
                    kilde = sykmelding2,
                    hendelseSykdomstidslinje = 8.januar.dager("SSSSSHH", sykmelding2),
                    beregnetSykdomstidslinje = 8.januar.dager("SSSSSHH", sykmelding2)
                )
            )
        )
        val expectedArbeidsgiverHistory = historikk(
            elementVedtaksperiode1,
            2.februar.element(
                kilde = sykmelding2,
                hendelseSykdomstidslinje = 8.januar.dager("SSSSSHH", sykmelding2),
                beregnetSykdomstidslinje =
                1.januar.dager("SSSSSHH", sykmelding1) +
                    8.januar.dager(
                        "SSSSSHH", sykmelding2
                    )
            )
        )
        val person = person(listOf(vedtaksperiode1, vedtaksperiode2), listOf())
        val migrated = listOf(V23SykdomshistorikkMerge()).migrate(person)
        val expected = person(
            listOf(vedtaksperiode1, vedtaksperiode2),
            arbeidsgiverHistorikk = expectedArbeidsgiverHistory,
            skjemaVersjon = 22
        )
        assertEquals(expected, migrated)
    }

    @Test
    fun `inntektsmelding som går over flere vedtaksperioder`() {
        val inntektsmelding = DagKilde.Inntektsmelding()

        val vedtaksperiode1 = vedtaksperiode(
            historikk(
                1.februar.element(
                    kilde = inntektsmelding,
                    beregnetSykdomstidslinje = 1.januar.dager("SSSSSHH", inntektsmelding),
                    hendelseSykdomstidslinje = 1.januar.dager("SSSSSHH", inntektsmelding)
                )
            )
        )
        val vedtaksperiode2 = vedtaksperiode(
            historikk(
                1.februar.element(
                    kilde = inntektsmelding,
                    beregnetSykdomstidslinje = 8.januar.dager("SSSSSHH", inntektsmelding),
                    hendelseSykdomstidslinje = 8.januar.dager("SSSSSHH", inntektsmelding)
                )
            )
        )

        val expectedHistorikk = historikk(
            1.februar.element(
                kilde = inntektsmelding,
                beregnetSykdomstidslinje = 1.januar.dager("SSSSSHH SSSSSHH", inntektsmelding),
                hendelseSykdomstidslinje = 1.januar.dager("SSSSSHH SSSSSHH", inntektsmelding)
            )
        )
        val person = person(listOf(vedtaksperiode1, vedtaksperiode2), listOf())
        val migrated = listOf(V23SykdomshistorikkMerge()).migrate(person)
        val expected = person(
            listOf(vedtaksperiode1, vedtaksperiode2),
            arbeidsgiverHistorikk = expectedHistorikk,
            skjemaVersjon = 22
        )
        println(person)
        assertEquals(expected, migrated)
    }

    @Test
    fun `inntektsmelding går over to perioder`() {
        val sykmelding1 = DagKilde.Sykmelding()
        val sykmelding2 = DagKilde.Sykmelding()
        val inntektsmelding = DagKilde.Inntektsmelding()
        val søknad2 = DagKilde.Søknad()
        val søknad1 = DagKilde.Søknad()
        val historikk1 = historikk(
            1.februar.element(
                kilde = sykmelding1,
                hendelseSykdomstidslinje = 2.januar.dager("SSSSHH", sykmelding1),
                beregnetSykdomstidslinje = 2.januar.dager("SSSSHH", sykmelding1)
            ),
            3.februar.element(
                kilde = inntektsmelding,
                hendelseSykdomstidslinje = 1.januar.dager("S", inntektsmelding),
                beregnetSykdomstidslinje = 1.januar.dager("S", inntektsmelding) + 2.januar.dager("SSSSHH", sykmelding1)
            ),
            5.februar.element(
                kilde = søknad1,
                hendelseSykdomstidslinje = 2.januar.dager("SSSARR", søknad1),
                beregnetSykdomstidslinje = 1.januar.dager("S", inntektsmelding) + 2.januar.dager(
                    "SSS",
                    sykmelding1
                ) + 5.januar.dager("ARR", søknad1)
            )
        )
        val historikk2 = historikk(
            2.februar.element(
                kilde = sykmelding2,
                hendelseSykdomstidslinje = 8.januar.dager("SSSSSHH", sykmelding2),
                beregnetSykdomstidslinje = 8.januar.dager("SSSSSHH", sykmelding2)
            ),
            3.februar.element(
                kilde = inntektsmelding,
                hendelseSykdomstidslinje = 11.januar.dager("A", inntektsmelding),
                beregnetSykdomstidslinje = 8.januar.dager("SSS", sykmelding2)
                    + 11.januar.dager("A", inntektsmelding)
                    + 12.januar.dager("SHH", sykmelding2)
            ),
            4.februar.element(
                kilde = søknad2,
                hendelseSykdomstidslinje = 8.januar.dager("SSSSSHH", søknad2),
                beregnetSykdomstidslinje = 8.januar.dager("SSS", sykmelding2)
                    + 11.januar.dager("A", inntektsmelding)
                    + 12.januar.dager("SHH", sykmelding2)
            )
        )

        val expectedHistory = historikk(
            1.februar.element(
                kilde = sykmelding1,
                hendelseSykdomstidslinje = 2.januar.dager("SSSSHH", sykmelding1),
                beregnetSykdomstidslinje = 2.januar.dager("SSSSHH", sykmelding1)
            ),
            2.februar.element(
                kilde = sykmelding2,
                hendelseSykdomstidslinje = 8.januar.dager("SSSSSHH", sykmelding2),
                beregnetSykdomstidslinje = 2.januar.dager("SSSSHH", sykmelding1) + 8.januar.dager(
                    "SSSSSHH",
                    sykmelding2
                )
            ),
            3.februar.element(
                kilde = inntektsmelding,
                hendelseSykdomstidslinje = 1.januar.dager("S", inntektsmelding) + 11.januar.dager("A", inntektsmelding),
                beregnetSykdomstidslinje = 1.januar.dager("S", inntektsmelding)
                    + 2.januar.dager("SSSSHH", sykmelding1)
                    + 8.januar.dager("SSS", sykmelding2)
                    + 11.januar.dager("A", inntektsmelding)
                    + 12.januar.dager("SHH", sykmelding2)
            ),
            4.februar.element(
                kilde = søknad2,
                hendelseSykdomstidslinje = 8.januar.dager("SSSSSHH", søknad2),
                beregnetSykdomstidslinje = 1.januar.dager("S", inntektsmelding)
                    + 2.januar.dager("SSSSHH", sykmelding1)
                    + 8.januar.dager("SSS", sykmelding2)
                    + 11.januar.dager("A", inntektsmelding)
                    + 12.januar.dager("SHH", sykmelding2)
            ),
            5.februar.element(
                kilde = søknad1,
                hendelseSykdomstidslinje = 2.januar.dager("SSSARR", søknad1),
                beregnetSykdomstidslinje = 1.januar.dager("S", inntektsmelding)
                    + 2.januar.dager("SSS", sykmelding1)
                    + 5.januar.dager("ARR", søknad1)
                    + 8.januar.dager("SSS", sykmelding2)
                    + 11.januar.dager("A", inntektsmelding)
                    + 12.januar.dager("SHH", sykmelding2)
            )
        )

        val person = person(
            vedtaksperioder = listOf(
                vedtaksperiode(historikk1),
                vedtaksperiode(historikk2)
            )
        )
        val merged = listOf(V23SykdomshistorikkMerge()).migrate(person)
        val expected = person(
            vedtaksperioder = listOf(
                vedtaksperiode(historikk1),
                vedtaksperiode(historikk2)
            ),
            arbeidsgiverHistorikk = expectedHistory,
            skjemaVersjon = 22
        )
        assertEquals(expected, merged)
    }

    @Disabled
    @Test
    fun `merge med en forkastet periode`() {
        val sykmelding1 = DagKilde.Sykmelding()
        val sykmelding2 = DagKilde.Sykmelding()
        val søknad1 = DagKilde.Søknad()
        val søknad2 = DagKilde.Søknad()

        val historikk1 = historikk(
            1.februar.element(
                kilde = sykmelding1,
                hendelseSykdomstidslinje = 2.januar.dager("SSSSHH", sykmelding1),
                beregnetSykdomstidslinje = 2.januar.dager("SSSSHH", sykmelding1)
            ),
            3.februar.element(
                kilde = søknad1,
                hendelseSykdomstidslinje = 2.januar.dager("SSSARR", søknad1),
                beregnetSykdomstidslinje = 2.januar.dager("SSS", sykmelding1)
                    + 5.januar.dager("ARR", søknad1)
            )
        )
        val historikk2 = historikk(
            2.februar.element(
                kilde = sykmelding2,
                hendelseSykdomstidslinje = 8.januar.dager("SSSSSHH", sykmelding2),
                beregnetSykdomstidslinje = 8.januar.dager("SSSSSHH", sykmelding2)
            ),
            4.februar.element(
                kilde = søknad2,
                hendelseSykdomstidslinje = 8.januar.dager("SSSSARR", søknad2),
                beregnetSykdomstidslinje = 8.januar.dager("SSSS", sykmelding2)
                    + 12.januar.dager("ARR", søknad2)
            )
        )

        val expectedHistory = historikk(
            1.februar.element(
                kilde = sykmelding1,
                hendelseSykdomstidslinje = 2.januar.dager("SSSSHH", sykmelding1),
                beregnetSykdomstidslinje = 2.januar.dager("SSSSHH", sykmelding1)
            ),
            2.februar.element(
                kilde = sykmelding2,
                hendelseSykdomstidslinje = 8.januar.dager("SSSSSHH", sykmelding2),
                beregnetSykdomstidslinje = 2.januar.dager("SSSSHH", sykmelding1) +
                    8.januar.dager("SSSSSHH", sykmelding2)
            ),
            3.februar.element( // nullable entry
                kilde = søknad1,
                hendelseSykdomstidslinje = 2.januar.dager("SSSARR", søknad1),
                beregnetSykdomstidslinje = 2.januar.dager("SSS", sykmelding1)
                    + 5.januar.dager("ARR", søknad1)
                    + 8.januar.dager("SSSSSHH", sykmelding2)
            ),
            3.februar.nullElement(
                beregnetSykdomstidslinje = 8.januar.dager("SSSSSHH", sykmelding2)
            ),
            4.februar.element(
                kilde = søknad2,
                hendelseSykdomstidslinje = 8.januar.dager("SSSSARR", søknad2),
                beregnetSykdomstidslinje = 8.januar.dager("SSSS", sykmelding2)
                    + 12.januar.dager("ARR", søknad2)
            )
        )

        val person = person(
            vedtaksperioder = listOf(
                vedtaksperiode(historikk1),
                vedtaksperiode(historikk2)
            )
        )
        val merged = listOf(V23SykdomshistorikkMerge()).migrate(person)
        val expected = person(
            vedtaksperioder = listOf(
                vedtaksperiode(historikk1),
                vedtaksperiode(historikk2)
            ),
            arbeidsgiverHistorikk = expectedHistory,
            skjemaVersjon = 23
        )
        assertEquals(expected, merged)
    }

    fun historikk(
        vararg historikkElementer: String
    ) = historikkElementer.reversed().joinToJsonArray()

    @Language("JSON")
    private fun LocalDate.element(
        kilde: DagKilde,
        hendelseSykdomstidslinje: List<String>,
        beregnetSykdomstidslinje: List<String>
    ) =
        """
            {
            "hendelseId" : "${kilde.uuid}",
            "tidsstempel" : "${this.atStartOfDay()}",
            "hendelseSykdomstidslinje" : ${hendelseSykdomstidslinje.joinToJsonArray()},
            "beregnetSykdomstidslinje" : ${beregnetSykdomstidslinje.joinToJsonArray()}
            }
        """


    @Language("JSON")
    private fun LocalDate.nullElement(
        beregnetSykdomstidslinje: List<String>
    ) =
        """
            {
            "tidsstempel" : "${this.atStartOfDay()}",
            "hendelseSykdomstidslinje" : [],
            "beregnetSykdomstidslinje" : ${beregnetSykdomstidslinje.joinToJsonArray()}
            }
        """

    private fun LocalDate.dager(dagString: String, kilde: DagKilde): List<String> {
        var dato = this
        return dagString.toCharArray().filterNot { it == ' ' }.map {
            when (it) {
                'S' -> dagString("Sykedag", dato, kilde)
                'H' -> dagString("SykHelgedag", dato, kilde)
                'A' -> dagString("Arbeidsdag", dato, kilde)
                'U' -> dagString("Arbeidsgiverdag", dato, kilde)
                'G' -> dagString("ArbeidsgiverHelgedag", dato, kilde)
                'F' -> dagString("Feriedag", dato, kilde)
                'R' -> dagString("FriskHelgedag", dato, kilde)
                else -> throw IllegalArgumentException("Don't recognize character $it")
            }.also { dato = dato.plusDays(1) }
        }
    }

    fun Iterable<String>.joinToJsonArray() = joinToString(",", "[", "]")

    @Language("JSON")
    private fun dagString(type: String, dato: LocalDate, kilde: DagKilde) = """
        {
          "dato": "$dato",
          "type": "$type",
          "kilde": {
            "type": "${kilde.type}",
            "id": "${kilde.uuid}"
          },
          "grad": 100.0,
          "arbeidsgiverBetalingProsent": 100.0
        }"""

    sealed class DagKilde(internal val uuid: UUID, internal val type: String) {
        class Sykmelding : DagKilde(UUID.randomUUID(), "SYKMELDING")
        class Søknad : DagKilde(UUID.randomUUID(), "SØKNAD")
        class Inntektsmelding : DagKilde(UUID.randomUUID(), "INNTEKTSMELDING")
    }

    fun vedtaksperiode(
        sykdomshistorikk: String
    ) = """{"sykdomshistorikk": $sykdomshistorikk}"""

    @Language("JSON")
    fun person(
        vedtaksperioder: List<String>,
        forkastedeVedtaksperioder: List<String> = listOf(),
        arbeidsgiverHistorikk: String = "[]",
        skjemaVersjon: Int = 20
    ) = objectMapper.readTree(
        """
{
    "arbeidsgivere": [
        {
            "sykdomshistorikk": $arbeidsgiverHistorikk,
            "vedtaksperioder": ${vedtaksperioder.joinToJsonArray()},
            "forkastede": ${forkastedeVedtaksperioder.joinToJsonArray()}
        }
    ],
    "skjemaVersjon": $skjemaVersjon
}
"""
    )
}
