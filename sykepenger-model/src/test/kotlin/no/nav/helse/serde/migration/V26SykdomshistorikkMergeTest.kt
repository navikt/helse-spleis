package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class V26SykdomshistorikkMergeTest {

    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `ferdigbygd json blir migrert riktig`() {
        val original = objectMapper.readTree(testPerson)
        val expected = objectMapper.readTree(expectedPerson)

        assertEquals(expected, listOf(V26SykdomshistorikkMerge()).migrate(original))
    }

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
        val person = person(arbeidsgiver(listOf(vedtaksperiode(historikk))))
        val expected = person(
            arbeidsgiver(listOf(vedtaksperiode(historikk)), arbeidsgiverHistorikk = historikk),
            skjemaVersjon = 26
        )
        val migrated = listOf(V26SykdomshistorikkMerge()).migrate(person)

        assertEquals(expected, migrated)
    }

    @Test
    fun `overskriver sykdomshistorikk på arbeidsgiver`() {
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
        val person = person(arbeidsgiver(listOf(vedtaksperiode(historikk)), arbeidsgiverHistorikk = historikk))
        val expected = person(
            arbeidsgiver(listOf(vedtaksperiode(historikk)), arbeidsgiverHistorikk = historikk),
            skjemaVersjon = 26
        )
        val migrated = listOf(V26SykdomshistorikkMerge()).migrate(person)

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
        val person = person(
            arbeidsgiver(
                vedtaksperioder = listOf(vedtaksperiode1, vedtaksperiode2),
                forkastedeVedtaksperioder = listOf()
            )
        )
        val migrated = listOf(V26SykdomshistorikkMerge()).migrate(person)
        val expected = person(
            arbeidsgiver(
                listOf(vedtaksperiode1, vedtaksperiode2),
                arbeidsgiverHistorikk = expectedArbeidsgiverHistory
            ),
            skjemaVersjon = 26
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
        val person = person(
            arbeidsgiver(
                vedtaksperioder = listOf(vedtaksperiode1, vedtaksperiode2),
                forkastedeVedtaksperioder = listOf()
            )
        )
        val migrated = listOf(V26SykdomshistorikkMerge()).migrate(person)
        val expected = person(
            arbeidsgiver(
                listOf(vedtaksperiode1, vedtaksperiode2),
                arbeidsgiverHistorikk = expectedHistorikk
            ),
            skjemaVersjon = 26
        )
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
            arbeidsgiver(
                vedtaksperioder = listOf(
                    vedtaksperiode(historikk1),
                    vedtaksperiode(historikk2)
                )
            )
        )
        val merged = listOf(V26SykdomshistorikkMerge()).migrate(person)
        val expected = person(
            arbeidsgiver(
                vedtaksperioder = listOf(
                    vedtaksperiode(historikk1),
                    vedtaksperiode(historikk2)
                ),
                arbeidsgiverHistorikk = expectedHistory
            ),
            skjemaVersjon = 26
        )
        assertEquals(expected, merged)
    }

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
            arbeidsgiver(
                vedtaksperioder = listOf(vedtaksperiode(historikk2)),
                forkastedeVedtaksperioder = listOf(vedtaksperiode(historikk1))
            )
        )
        val merged = listOf(V26SykdomshistorikkMerge()).migrate(person)
        val expected = person(
            arbeidsgiver(
                vedtaksperioder = listOf(vedtaksperiode(historikk2)),
                forkastedeVedtaksperioder = listOf(vedtaksperiode(historikk1)),
                arbeidsgiverHistorikk = expectedHistory
            ),
            skjemaVersjon = 26
        )
        assertEquals(expected, merged)
    }

    @Test
    fun `merge med to forkastede perioder`() {
        val sykmelding1 = DagKilde.Sykmelding()
        val sykmelding2 = DagKilde.Sykmelding()
        val sykmelding3 = DagKilde.Sykmelding()
        val søknad3 = DagKilde.Søknad()

        val historikk1 = historikk(
            1.februar.element(
                kilde = sykmelding1,
                hendelseSykdomstidslinje = 2.januar.dager("SSSSHH", sykmelding1),
                beregnetSykdomstidslinje = 2.januar.dager("SSSSHH", sykmelding1)
            )
        )
        val historikk2 = historikk(
            2.februar.element(
                kilde = sykmelding2,
                hendelseSykdomstidslinje = 8.januar.dager("SSSSSHH", sykmelding2),
                beregnetSykdomstidslinje = 8.januar.dager("SSSSSHH", sykmelding2)
            )
        )
        val historikk3 = historikk(
            3.februar.element(
                kilde = sykmelding3,
                hendelseSykdomstidslinje = 15.januar.dager("SSSSSHH", sykmelding3),
                beregnetSykdomstidslinje = 15.januar.dager("SSSSSHH", sykmelding3)
            ),
            4.februar.element(
                kilde = søknad3,
                hendelseSykdomstidslinje = 15.januar.dager("SSSSARR", søknad3),
                beregnetSykdomstidslinje = 15.januar.dager("SSSS", sykmelding3)
                    + 19.januar.dager("ARR", søknad3)
            )
        )

        val expectedHistory = historikk(
            1.februar.element(
                kilde = sykmelding1,
                hendelseSykdomstidslinje = 2.januar.dager("SSSSHH", sykmelding1),
                beregnetSykdomstidslinje = 2.januar.dager("SSSSHH", sykmelding1)
            ),
            1.februar.nullElement(
                beregnetSykdomstidslinje = 2.januar.dager("", sykmelding1)
            ),
            2.februar.element(
                kilde = sykmelding2,
                hendelseSykdomstidslinje = 8.januar.dager("SSSSSHH", sykmelding2),
                beregnetSykdomstidslinje = 8.januar.dager("SSSSSHH", sykmelding2)
            ),
            2.februar.nullElement(
                beregnetSykdomstidslinje = 8.januar.dager("", sykmelding2)
            ),
            3.februar.element(
                kilde = sykmelding3,
                hendelseSykdomstidslinje = 15.januar.dager("SSSSSHH", sykmelding3),
                beregnetSykdomstidslinje = 15.januar.dager("SSSSSHH", sykmelding3)
            ),
            4.februar.element(
                kilde = søknad3,
                hendelseSykdomstidslinje = 15.januar.dager("SSSSARR", søknad3),
                beregnetSykdomstidslinje = 15.januar.dager("SSSS", sykmelding3)
                    + 19.januar.dager("ARR", søknad3)
            )
        )

        val person = person(
            arbeidsgiver(
                vedtaksperioder = listOf(vedtaksperiode(historikk3)),
                forkastedeVedtaksperioder = listOf(vedtaksperiode(historikk1), vedtaksperiode(historikk2))
            )
        )
        val merged = listOf(V26SykdomshistorikkMerge()).migrate(person)
        val expected = person(
            arbeidsgiver(
                vedtaksperioder = listOf(vedtaksperiode(historikk3)),
                forkastedeVedtaksperioder = listOf(vedtaksperiode(historikk1), vedtaksperiode(historikk2)),
                arbeidsgiverHistorikk = expectedHistory
            ),
            skjemaVersjon = 26
        )
        assertEquals(expected, merged)
    }

    @Test
    fun `hendelser i feil rekkefølge`() {
        val sykmelding1 = DagKilde.Sykmelding()
        val sykmelding2 = DagKilde.Sykmelding()
        val sykmelding3 = DagKilde.Sykmelding()
        val søknad2 = DagKilde.Søknad()
        val søknad1 = DagKilde.Søknad()
        val søknad3 = DagKilde.Søknad()

        val historikk1 = historikk(
            1.februar.element(
                kilde = sykmelding1,
                hendelseSykdomstidslinje = 2.januar.dager("SSSSHH", sykmelding1),
                beregnetSykdomstidslinje = 2.januar.dager("SSSSHH", sykmelding1)
            ),
            5.februar.element(
                kilde = søknad1,
                hendelseSykdomstidslinje = 2.januar.dager("SSSSHH", søknad1),
                beregnetSykdomstidslinje = 2.januar.dager("SSSSHH", sykmelding1)
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
                hendelseSykdomstidslinje = 8.januar.dager("SSSSSHH", søknad2),
                beregnetSykdomstidslinje = 8.januar.dager("SSSSSHH", sykmelding2)
            )
        )
        val historikk3 = historikk(
            3.februar.element(
                kilde = sykmelding3,
                hendelseSykdomstidslinje = 15.januar.dager("SSSSSHH", sykmelding3),
                beregnetSykdomstidslinje = 15.januar.dager("SSSSSHH", sykmelding3)
            ),
            6.februar.element(
                kilde = søknad3,
                hendelseSykdomstidslinje = 15.januar.dager("SSSSARR", søknad3),
                beregnetSykdomstidslinje = 15.januar.dager("SSSS", sykmelding3)
                    + 19.januar.dager("ARR", søknad3)
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
            3.februar.element(
                kilde = sykmelding3,
                hendelseSykdomstidslinje = 15.januar.dager("SSSSSHH", sykmelding3),
                beregnetSykdomstidslinje = 2.januar.dager("SSSSHH", sykmelding1) +
                    8.januar.dager("SSSSSHH", sykmelding2) +
                    15.januar.dager("SSSSSHH", sykmelding3)
            ),
            4.februar.element(
                kilde = søknad2,
                hendelseSykdomstidslinje = 8.januar.dager("SSSSSHH", søknad2),
                beregnetSykdomstidslinje = 2.januar.dager("SSSSHH", sykmelding1) +
                    8.januar.dager("SSSSSHH", sykmelding2) +
                    15.januar.dager("SSSSSHH", sykmelding3)
            ),
            4.februar.nullElement(
                beregnetSykdomstidslinje = 2.januar.dager("SSSSHH", sykmelding1) +
                    15.januar.dager("SSSSSHH", sykmelding3)
            ),
            5.februar.element(
                kilde = søknad1,
                hendelseSykdomstidslinje = 2.januar.dager("SSSSHH", søknad1),
                beregnetSykdomstidslinje = 2.januar.dager("SSSSHH", sykmelding1) +
                    15.januar.dager("SSSSSHH", sykmelding3)
            ),
            5.februar.nullElement(
                beregnetSykdomstidslinje = 15.januar.dager("SSSSSHH", sykmelding3)
            ),
            6.februar.element(
                kilde = søknad3,
                hendelseSykdomstidslinje = 15.januar.dager("SSSSARR", søknad3),
                beregnetSykdomstidslinje = 15.januar.dager("SSSS", sykmelding3)
                    + 19.januar.dager("ARR", søknad3)
            )
        )

        val person = person(
            arbeidsgiver(
                vedtaksperioder = listOf(vedtaksperiode(historikk3)),
                forkastedeVedtaksperioder = listOf(vedtaksperiode(historikk1), vedtaksperiode(historikk2))
            )
        )
        val merged = listOf(V26SykdomshistorikkMerge()).migrate(person)
        val expected = person(
            arbeidsgiver(
                vedtaksperioder = listOf(vedtaksperiode(historikk3)),
                forkastedeVedtaksperioder = listOf(vedtaksperiode(historikk1), vedtaksperiode(historikk2)),
                arbeidsgiverHistorikk = expectedHistory
            ),
            skjemaVersjon = 26
        )
        assertEquals(expected, merged)
    }

    @Test
    fun `person med to arbeidsgivere`() {
        val sykmelding = DagKilde.Sykmelding()
        val element = 1.februar.element(
            sykmelding,
            hendelseSykdomstidslinje = 1.januar.dager("SSSSSHH SSSSSHH SSSSSHH SSSSSHH", sykmelding),
            beregnetSykdomstidslinje = 1.januar.dager("SSSSSHH SSSSSHH SSSSSHH SSSSSHH", sykmelding)
        )
        val historikk = historikk(
            element
        )
        val person = person(
            arbeidsgiver(
                vedtaksperioder = emptyList(),
                forkastedeVedtaksperioder = listOf(vedtaksperiode(sykdomshistorikk = historikk))
            ),
            arbeidsgiver(
                vedtaksperioder = emptyList(),
                forkastedeVedtaksperioder = emptyList()
            )
        )

        val expectedPerson = person(
            arbeidsgiver(
                vedtaksperioder = emptyList(),
                forkastedeVedtaksperioder = listOf(
                    vedtaksperiode(sykdomshistorikk = historikk)
                ),
                arbeidsgiverHistorikk = historikk(
                    element,
                    1.februar.nullElement(listOf())
                )
            ),
            arbeidsgiver(
                vedtaksperioder = emptyList(),
                forkastedeVedtaksperioder = emptyList(),
                arbeidsgiverHistorikk = "[]"
            ),
            skjemaVersjon = 26
        )

        val merged = listOf(V26SykdomshistorikkMerge()).migrate(person)
        assertEquals(expectedPerson, merged)
    }

    @Test
    fun `person med arbeidsgiver uten vedtaksperioder`() {
        // Dette caset kan oppstå om en person fikk søknad før sykmelding i en gammel versjon
        val person = person(
            arbeidsgiver(
                vedtaksperioder = emptyList(),
                forkastedeVedtaksperioder = emptyList()
            )
        )

        val expectedPerson = person(
            arbeidsgiver(
                vedtaksperioder = emptyList(),
                forkastedeVedtaksperioder = emptyList(),
                arbeidsgiverHistorikk = "[]"
            ),
            skjemaVersjon = 26
        )

        val merged = listOf(V26SykdomshistorikkMerge()).migrate(person)
        assertEquals(expectedPerson, merged)
    }

    @Test
    fun `kan migrere tom forkastet periode`() {
        val original = objectMapper.readTree(personMedTomForkastetPeriode)
        val expected = objectMapper.readTree(expectedpersonMedTomForkastetPeriode)

        assertEquals(expected, listOf(V26SykdomshistorikkMerge()).migrate(original))
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
            "hendelseSykdomstidslinje" : { "låstePerioder": [], "dager": ${hendelseSykdomstidslinje.joinToJsonArray()} },
            "beregnetSykdomstidslinje" : { "låstePerioder": [], "dager": ${beregnetSykdomstidslinje.joinToJsonArray()} }
            }
        """


    @Language("JSON")
    private fun LocalDate.nullElement(
        beregnetSykdomstidslinje: List<String>
    ) =
        """
            {
            "tidsstempel" : "${this.atStartOfDay().plusNanos(1)}",
            "hendelseSykdomstidslinje" : { "låstePerioder": [], "dager": [] },
            "beregnetSykdomstidslinje" : { "låstePerioder": [], "dager": ${beregnetSykdomstidslinje.joinToJsonArray()} }
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

    data class ArbeidsgiverWrapper(val json: String)

    @Language("JSON")
    fun arbeidsgiver(
        vedtaksperioder: List<String>,
        forkastedeVedtaksperioder: List<String> = listOf(),
        arbeidsgiverHistorikk: String = "[]"
    ): ArbeidsgiverWrapper = ArbeidsgiverWrapper(
        """
        {
            "sykdomshistorikk": $arbeidsgiverHistorikk,
            "vedtaksperioder": ${vedtaksperioder.joinToJsonArray()},
            "forkastede": ${forkastedeVedtaksperioder.joinToJsonArray()}
        }
    """
    )

    @Language("JSON")
    fun person(
        vararg arbeidsgivere: ArbeidsgiverWrapper,
        skjemaVersjon: Int = 20
    ) = objectMapper.readTree(
        """
{
    "arbeidsgivere": ${arbeidsgivere.map { it.json }.joinToJsonArray()},
    "skjemaVersjon": $skjemaVersjon
}
"""
    )
}

@Language("JSON")
private val testPerson = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "sykdomshistorikk": [
            {
              "hendelseId": "dc11a2d4-274f-439d-b9ea-b74dd4b6b91a",
              "tidsstempel": "2020-06-22T13:37:50.16249",
              "hendelseSykdomstidslinje": {
                "låstePerioder": [],
                "dager": [
                  {
                    "dato": "2018-01-01",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Sykmelding",
                      "id": "dc11a2d4-274f-439d-b9ea-b74dd4b6b91a"
                    },
                    "grad": 100.0,
                    "arbeidsgiverBetalingProsent": 100.0
                  },
                  {
                    "dato": "2018-01-02",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Sykmelding",
                      "id": "dc11a2d4-274f-439d-b9ea-b74dd4b6b91a"
                    },
                    "grad": 100.0,
                    "arbeidsgiverBetalingProsent": 100.0
                  },
                  {
                    "dato": "2018-01-16",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Sykmelding",
                      "id": "dc11a2d4-274f-439d-b9ea-b74dd4b6b91a"
                    },
                    "grad": 100.0,
                    "arbeidsgiverBetalingProsent": 100.0
                  }
                ]
              },
              "beregnetSykdomstidslinje": {
                "låstePerioder": [],
                "dager": [
                  {
                    "dato": "2018-01-01",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Sykmelding",
                      "id": "dc11a2d4-274f-439d-b9ea-b74dd4b6b91a"
                    },
                    "grad": 100.0,
                    "arbeidsgiverBetalingProsent": 100.0
                  },
                  {
                    "dato": "2018-01-02",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Sykmelding",
                      "id": "dc11a2d4-274f-439d-b9ea-b74dd4b6b91a"
                    },
                    "grad": 100.0,
                    "arbeidsgiverBetalingProsent": 100.0
                  },
                  {
                    "dato": "2018-01-16",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Sykmelding",
                      "id": "dc11a2d4-274f-439d-b9ea-b74dd4b6b91a"
                    },
                    "grad": 100.0,
                    "arbeidsgiverBetalingProsent": 100.0
                  }
                ]
              }
            }
          ]
        }
      ],
      "forkastede": []
    }
  ]
}
"""

@Language("JSON")
private val expectedPerson = """
{
  "arbeidsgivere": [
    {
      "sykdomshistorikk": [
        {
          "hendelseId": "dc11a2d4-274f-439d-b9ea-b74dd4b6b91a",
          "tidsstempel": "2020-06-22T13:37:50.16249",
          "hendelseSykdomstidslinje": {
            "låstePerioder": [],
            "dager": [
              {
                "dato": "2018-01-01",
                "type": "SYKEDAG",
                "kilde": {
                  "type": "Sykmelding",
                  "id": "dc11a2d4-274f-439d-b9ea-b74dd4b6b91a"
                },
                "grad": 100.0,
                "arbeidsgiverBetalingProsent": 100.0
              },
              {
                "dato": "2018-01-02",
                "type": "SYKEDAG",
                "kilde": {
                  "type": "Sykmelding",
                  "id": "dc11a2d4-274f-439d-b9ea-b74dd4b6b91a"
                },
                "grad": 100.0,
                "arbeidsgiverBetalingProsent": 100.0
              },
              {
                "dato": "2018-01-16",
                "type": "SYKEDAG",
                "kilde": {
                  "type": "Sykmelding",
                  "id": "dc11a2d4-274f-439d-b9ea-b74dd4b6b91a"
                },
                "grad": 100.0,
                "arbeidsgiverBetalingProsent": 100.0
              }
            ]
          },
          "beregnetSykdomstidslinje": {
            "låstePerioder": [],
            "dager": [
              {
                "dato": "2018-01-01",
                "type": "SYKEDAG",
                "kilde": {
                  "type": "Sykmelding",
                  "id": "dc11a2d4-274f-439d-b9ea-b74dd4b6b91a"
                },
                "grad": 100.0,
                "arbeidsgiverBetalingProsent": 100.0
              },
              {
                "dato": "2018-01-02",
                "type": "SYKEDAG",
                "kilde": {
                  "type": "Sykmelding",
                  "id": "dc11a2d4-274f-439d-b9ea-b74dd4b6b91a"
                },
                "grad": 100.0,
                "arbeidsgiverBetalingProsent": 100.0
              },
              {
                "dato": "2018-01-16",
                "type": "SYKEDAG",
                "kilde": {
                  "type": "Sykmelding",
                  "id": "dc11a2d4-274f-439d-b9ea-b74dd4b6b91a"
                },
                "grad": 100.0,
                "arbeidsgiverBetalingProsent": 100.0
              }
            ]
          }
        }
      ],
      "vedtaksperioder": [
        {
          "sykdomshistorikk": [
            {
              "hendelseId": "dc11a2d4-274f-439d-b9ea-b74dd4b6b91a",
              "tidsstempel": "2020-06-22T13:37:50.16249",
              "hendelseSykdomstidslinje": {
                "låstePerioder": [],
                "dager": [
                  {
                    "dato": "2018-01-01",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Sykmelding",
                      "id": "dc11a2d4-274f-439d-b9ea-b74dd4b6b91a"
                    },
                    "grad": 100.0,
                    "arbeidsgiverBetalingProsent": 100.0
                  },
                  {
                    "dato": "2018-01-02",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Sykmelding",
                      "id": "dc11a2d4-274f-439d-b9ea-b74dd4b6b91a"
                    },
                    "grad": 100.0,
                    "arbeidsgiverBetalingProsent": 100.0
                  },
                  {
                    "dato": "2018-01-16",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Sykmelding",
                      "id": "dc11a2d4-274f-439d-b9ea-b74dd4b6b91a"
                    },
                    "grad": 100.0,
                    "arbeidsgiverBetalingProsent": 100.0
                  }
                ]
              },
              "beregnetSykdomstidslinje": {
                "låstePerioder": [],
                "dager": [
                  {
                    "dato": "2018-01-01",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Sykmelding",
                      "id": "dc11a2d4-274f-439d-b9ea-b74dd4b6b91a"
                    },
                    "grad": 100.0,
                    "arbeidsgiverBetalingProsent": 100.0
                  },
                  {
                    "dato": "2018-01-02",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Sykmelding",
                      "id": "dc11a2d4-274f-439d-b9ea-b74dd4b6b91a"
                    },
                    "grad": 100.0,
                    "arbeidsgiverBetalingProsent": 100.0
                  },
                  {
                    "dato": "2018-01-16",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Sykmelding",
                      "id": "dc11a2d4-274f-439d-b9ea-b74dd4b6b91a"
                    },
                    "grad": 100.0,
                    "arbeidsgiverBetalingProsent": 100.0
                  }
                ]
              }
            }
          ]
        }
      ],
      "forkastede": []
    }
  ],
  "skjemaVersjon": 26
}
"""

@Language("JSON")
private val personMedTomForkastetPeriode = """
    {
      "aktørId": "aktørId",
      "fødselsnummer": "fnr",
      "arbeidsgivere": [
        {
          "organisasjonsnummer": "orgnummer",
          "id": "a8d9144d-7911-47b3-ac81-bf49867a5b4d",
          "sykdomshistorikk": [],
          "forkastede": [
            {
              "sykdomshistorikk": []
            }
          ]
        }
      ],
      "skjemaVersjon": 25
    }
"""

@Language("JSON")
private val expectedpersonMedTomForkastetPeriode = """
    {
      "aktørId": "aktørId",
      "fødselsnummer": "fnr",
      "arbeidsgivere": [
        {
          "organisasjonsnummer": "orgnummer",
          "id": "a8d9144d-7911-47b3-ac81-bf49867a5b4d",
          "sykdomshistorikk": [],
          "forkastede": []
        }
      ],
      "skjemaVersjon": 26
    }
"""
