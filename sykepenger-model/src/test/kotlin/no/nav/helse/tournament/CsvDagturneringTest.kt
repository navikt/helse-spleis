package no.nav.helse.tournament

import no.nav.helse.sykdomstidslinje.NyDag.*
import no.nav.helse.sykdomstidslinje.NySykdomstidslinje
import no.nav.helse.testhelpers.*
import no.nav.helse.testhelpers.TestEvent.Companion.inntektsmelding
import no.nav.helse.testhelpers.TestEvent.Companion.sykmelding
import no.nav.helse.testhelpers.TestEvent.Companion.søknad
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class CsvDagturneringTest {

    @Test
    internal fun `Arbeidsdag fra søknad vinner over sykedag fra sykmelding`() {
        val sykmeldingSykedag = NySykdomstidslinje.sykedager(1.mandag, 1.mandag, 100.0, sykmelding)
        val søknadArbeidsdag = NySykdomstidslinje.arbeidsdager(1.mandag, 1.mandag, søknad)

        val tidslinje = sykmeldingSykedag.merge(søknadArbeidsdag, dagturnering::beste)

        assertTrue(
            tidslinje[1.mandag] is NyArbeidsdag,
            "Torsdag er en arbeidsdag etter kombinering av sykmelding og søknad"
        )
    }

    @Test
    internal fun `kombinering av tidslinjer fører til at dagsturnering slår sammen dagene`() {
        val søknadSykedager = NySykdomstidslinje.sykedager(1.mandag, 1.fredag, 100.0, søknad)
        val søknadArbeidsdager = NySykdomstidslinje.arbeidsdager(1.torsdag, 1.fredag, søknad)

        val tidslinje = søknadSykedager.merge(søknadArbeidsdager, dagturnering::beste)
        assertTrue(
            tidslinje[1.onsdag] is NySykedag,
            "Onsdag er fortsatt en sykedag etter kombinering av sykmelding og søknad"
        )
        assertTrue(
            tidslinje[1.torsdag] is NyArbeidsdag,
            "Torsdag er en arbeidsdag etter kombinering av sykmelding og søknad"
        )
    }

    @Test
    internal fun `sykedag fra arbeidsgiver taper mot syk helgedag fra sykmeldingen`() {
        val sykmeldingSykHelgedag = NySykdomstidslinje.sykedager(1.søndag, 1.søndag, 100.0, sykmelding)
        val inntektsmeldingArbeidsgiverdag = NySykdomstidslinje.arbeidsgiverdager(1.søndag, 1.søndag, 100.0, inntektsmelding)

        val tidslinje = inntektsmeldingArbeidsgiverdag.merge(sykmeldingSykHelgedag, dagturnering::beste)

        assertTrue(tidslinje[1.søndag] is NySykHelgedag)
    }

    @Test
    internal fun `arbeidsdag fra inntektsmelding vinner over egenmelding fra søknad`() {
        val søknadArbeidsgiverdag = NySykdomstidslinje.arbeidsgiverdager(1.mandag, 1.mandag, 100.0, søknad)
        val inntektsmeldingArbeidsdag = NySykdomstidslinje.arbeidsdager(1.mandag, 1.mandag, inntektsmelding)

        val tidslinje = søknadArbeidsgiverdag.merge(inntektsmeldingArbeidsdag, dagturnering::beste)

        assertTrue(tidslinje[1.mandag] is NyArbeidsdag)
    }

    @Test
    internal fun `arbeidsdag fra inntektsmelding vinner over sykedag fra sykmelding`() {
        val sykmeldingSykedag = NySykdomstidslinje.sykedager(1.mandag, 1.mandag, 100.0, sykmelding)
        val inntektsmeldingArbeidsdag = NySykdomstidslinje.arbeidsdager(1.mandag, 1.mandag, inntektsmelding)

        val tidslinje = sykmeldingSykedag.merge(inntektsmeldingArbeidsdag, dagturnering::beste)

        assertTrue(tidslinje[1.mandag] is NyArbeidsdag)
    }
}

