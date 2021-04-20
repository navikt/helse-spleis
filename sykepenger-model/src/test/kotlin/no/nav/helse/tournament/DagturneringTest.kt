package no.nav.helse.tournament

import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.*
import no.nav.helse.testhelpers.TestEvent.Companion.inntektsmelding
import no.nav.helse.testhelpers.TestEvent.Companion.sykmelding
import no.nav.helse.testhelpers.TestEvent.Companion.søknad
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DagturneringTest {

    @Test
    fun `ny sykmelding vinner over gammel sykmelding`() {
        val nyere = Sykdomstidslinje.sykedager(1.mandag, 1.mandag, 100.prosent, TestEvent.Sykmelding(8.januar.atStartOfDay()).kilde)
        val eldre = Sykdomstidslinje.sykedager(1.mandag, 1.mandag, 50.prosent, TestEvent.Sykmelding(1.januar.atStartOfDay()).kilde)
        val tidslinje = nyere.merge(eldre, dagturnering::beste)
        assertTrue(tidslinje[1.mandag] is Sykedag, "Dagen må være sykedag")
        assertEquals(100, SykdomstidslinjeInspektør(tidslinje).grader[1.mandag]) { "Nyere sykmelding skal vinne" }
    }
    @Test
    fun `ny sykmelding vinner over gammel sykmelding - inverse`() {
        val nyere = Sykdomstidslinje.sykedager(1.mandag, 1.mandag, 100.prosent, TestEvent.Sykmelding(8.januar.atStartOfDay()).kilde)
        val eldre = Sykdomstidslinje.sykedager(1.mandag, 1.mandag, 50.prosent, TestEvent.Sykmelding(1.januar.atStartOfDay()).kilde)
        val tidslinje = eldre.merge(nyere, dagturnering::beste)
        assertTrue(tidslinje[1.mandag] is Sykedag, "Dagen må være sykedag")
        assertEquals(100, SykdomstidslinjeInspektør(tidslinje).grader[1.mandag]) { "Nyere sykmelding skal vinne" }
    }

    @Test
    fun `ny sykmelding vinner over gammel søknad`() {
        val nyere = Sykdomstidslinje.sykedager(1.mandag, 1.mandag, 100.prosent, TestEvent.Sykmelding(8.januar.atStartOfDay()).kilde)
        val eldre = Sykdomstidslinje.sykedager(1.mandag, 1.mandag, 50.prosent, TestEvent.Søknad(1.januar.atStartOfDay()).kilde)
        val tidslinje = nyere.merge(eldre, dagturnering::beste)
        assertTrue(tidslinje[1.mandag] is Sykedag, "Dagen må være sykedag")
        assertEquals(100, SykdomstidslinjeInspektør(tidslinje).grader[1.mandag]) { "Nyere sykmelding skal vinne" }
    }
    @Test
    fun `ny sykmelding vinner over gammel søknad - inverse`() {
        val nyere = Sykdomstidslinje.sykedager(1.mandag, 1.mandag, 100.prosent, TestEvent.Sykmelding(8.januar.atStartOfDay()).kilde)
        val eldre = Sykdomstidslinje.sykedager(1.mandag, 1.mandag, 50.prosent, TestEvent.Søknad(1.januar.atStartOfDay()).kilde)
        val tidslinje = eldre.merge(nyere, dagturnering::beste)
        assertTrue(tidslinje[1.mandag] is Sykedag, "Dagen må være sykedag")
        assertEquals(100, SykdomstidslinjeInspektør(tidslinje).grader[1.mandag]) { "Nyere sykmelding skal vinne" }
    }

    @Test
    fun `Arbeidsdag fra søknad vinner over sykedag fra sykmelding når tidspunkt er likt`() {
        val tidspunkt = 1.januar.atStartOfDay()
        val sykmeldingSykedag = Sykdomstidslinje.sykedager(1.mandag, 1.mandag, 100.prosent, TestEvent.Sykmelding(tidspunkt).kilde)
        val søknadArbeidsdag = Sykdomstidslinje.arbeidsdager(1.mandag, 1.mandag, TestEvent.Søknad(tidspunkt).kilde)
        val tidslinje = sykmeldingSykedag.merge(søknadArbeidsdag, dagturnering::beste)
        assertEquals(Arbeidsdag::class, tidslinje[1.mandag]::class, "Torsdag er en arbeidsdag etter kombinering av sykmelding og søknad")
    }

    @Test
    fun `kombinering av tidslinjer fører til at dagsturnering slår sammen dagene`() {
        val søknadSykedager = Sykdomstidslinje.sykedager(1.mandag, 1.fredag, 100.prosent, søknad)
        val søknadArbeidsdager = Sykdomstidslinje.arbeidsdager(1.torsdag, 1.fredag, søknad)

        val tidslinje = søknadSykedager.merge(søknadArbeidsdager, dagturnering::beste)
        assertTrue(
            tidslinje[1.onsdag] is Sykedag,
            "Onsdag er fortsatt en sykedag etter kombinering av sykmelding og søknad"
        )
        assertTrue(
            tidslinje[1.torsdag] is Arbeidsdag,
            "Torsdag er en arbeidsdag etter kombinering av sykmelding og søknad"
        )
    }

    @Test
    fun `syk helgedag fra arbeidsgiver vinner mot syk helgedag fra sykmeldingen`() {
        val sykmeldingSykHelgedag = Sykdomstidslinje.sykedager(1.søndag, 1.søndag, 100.prosent, sykmelding)
        val inntektsmeldingArbeidsgiverdag = Sykdomstidslinje.arbeidsgiverdager(1.søndag, 1.søndag, 100.prosent, inntektsmelding)

        val tidslinje = inntektsmeldingArbeidsgiverdag.merge(sykmeldingSykHelgedag, dagturnering::beste)

        assertTrue(tidslinje[1.søndag] is ArbeidsgiverHelgedag)
    }

    @Test
    fun `arbeidsdag fra inntektsmelding vinner over egenmelding fra søknad`() {
        val søknadArbeidsgiverdag = Sykdomstidslinje.arbeidsgiverdager(1.mandag, 1.mandag, 100.prosent, søknad)
        val inntektsmeldingArbeidsdag = Sykdomstidslinje.arbeidsdager(1.mandag, 1.mandag, inntektsmelding)

        val tidslinje = søknadArbeidsgiverdag.merge(inntektsmeldingArbeidsdag, dagturnering::beste)

        assertTrue(tidslinje[1.mandag] is Arbeidsdag)
    }

    @Test
    fun `arbeidsdag fra inntektsmelding vinner over sykedag fra sykmelding`() {
        val sykmeldingSykedag = Sykdomstidslinje.sykedager(1.mandag, 1.mandag, 100.prosent, sykmelding)
        val inntektsmeldingArbeidsdag = Sykdomstidslinje.arbeidsdager(1.mandag, 1.mandag, inntektsmelding)

        val tidslinje = sykmeldingSykedag.merge(inntektsmeldingArbeidsdag, dagturnering::beste)

        assertTrue(tidslinje[1.mandag] is Arbeidsdag)
    }

    @Test
    fun `arbeidsdag fra inntektsmelding vinner over sykedag fra søknad`() {
        val søknadSykedag = Sykdomstidslinje.sykedager(1.mandag, 1.mandag, 100.prosent, søknad)
        val inntektsmeldingArbeidsdag = Sykdomstidslinje.arbeidsdager(1.mandag, 1.mandag, inntektsmelding)

        val tidslinje = søknadSykedag.merge(inntektsmeldingArbeidsdag, dagturnering::beste)

        assertTrue(tidslinje[1.mandag] is Arbeidsdag)
    }
}

