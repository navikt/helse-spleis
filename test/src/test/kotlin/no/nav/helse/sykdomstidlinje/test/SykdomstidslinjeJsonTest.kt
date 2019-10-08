package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SykdomstidslinjeJsonTest {
    @Test
    fun `lagring og restoring av en sykdomstidslinje med kun arbeidsdag returnerer like objekter`() {
        val tidslinjeA = Sykdomstidslinje.ikkeSykedager(
            LocalDate.of(2019, 10, 1),
            LocalDate.of(2019, 10, 3), Testhendelse())
        val tidslinjeB = Sykdomstidslinje.ikkeSykedager(
            LocalDate.of(2019, 10, 7),
            LocalDate.of(2019, 10, 10), Testhendelse())
        val tidslinjeC = Sykdomstidslinje.sykedager(
            LocalDate.of(2019, 10, 7),
            LocalDate.of(2019, 10, 10), Testhendelse())

        val json = (tidslinjeA + tidslinjeB + tidslinjeC).toJson()
        println(json)

        assertEquals("[{\"type\":\"ARBEIDSDAG\",\"dato\":\"2019-10-01\",\"hendelse\":{\"type\":\"Inntektsmelding\",\"json\":{}},\"erstatter\":[]},{\"type\":\"ARBEIDSDAG\",\"dato\":\"2019-10-02\",\"hendelse\":{\"type\":\"Inntektsmelding\",\"json\":{}},\"erstatter\":[]},{\"type\":\"ARBEIDSDAG\",\"dato\":\"2019-10-03\",\"hendelse\":{\"type\":\"Inntektsmelding\",\"json\":{}},\"erstatter\":[]},{\"type\":\"ARBEIDSDAG\",\"dato\":\"2019-10-04\",\"hendelse\":{\"type\":\"Inntektsmelding\",\"json\":{}},\"erstatter\":[]},{\"type\":\"HELGEDAG\",\"dato\":\"2019-10-05\",\"hendelse\":{\"type\":\"Inntektsmelding\",\"json\":{}},\"erstatter\":[]},{\"type\":\"HELGEDAG\",\"dato\":\"2019-10-06\",\"hendelse\":{\"type\":\"Inntektsmelding\",\"json\":{}},\"erstatter\":[]},{\"type\":\"ARBEIDSDAG\",\"dato\":\"2019-10-07\",\"hendelse\":{\"type\":\"Inntektsmelding\",\"json\":{}},\"erstatter\":[{\"type\":\"SYKEDAG\",\"dato\":\"2019-10-07\",\"hendelse\":{\"type\":\"Inntektsmelding\",\"json\":{}},\"erstatter\":[]}]},{\"type\":\"ARBEIDSDAG\",\"dato\":\"2019-10-08\",\"hendelse\":{\"type\":\"Inntektsmelding\",\"json\":{}},\"erstatter\":[{\"type\":\"SYKEDAG\",\"dato\":\"2019-10-08\",\"hendelse\":{\"type\":\"Inntektsmelding\",\"json\":{}},\"erstatter\":[]}]},{\"type\":\"ARBEIDSDAG\",\"dato\":\"2019-10-09\",\"hendelse\":{\"type\":\"Inntektsmelding\",\"json\":{}},\"erstatter\":[{\"type\":\"SYKEDAG\",\"dato\":\"2019-10-09\",\"hendelse\":{\"type\":\"Inntektsmelding\",\"json\":{}},\"erstatter\":[]}]},{\"type\":\"ARBEIDSDAG\",\"dato\":\"2019-10-10\",\"hendelse\":{\"type\":\"Inntektsmelding\",\"json\":{}},\"erstatter\":[{\"type\":\"SYKEDAG\",\"dato\":\"2019-10-10\",\"hendelse\":{\"type\":\"Inntektsmelding\",\"json\":{}},\"erstatter\":[]}]}]", json)
    }
}