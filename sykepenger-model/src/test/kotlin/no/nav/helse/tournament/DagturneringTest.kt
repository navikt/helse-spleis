package no.nav.helse.tournament

import no.nav.helse.fredag
import no.nav.helse.lørdag
import no.nav.helse.mandag
import no.nav.helse.onsdag
import no.nav.helse.sykdomstidslinje.Dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.Dag.Sykedag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.TestEvent.Companion.inntektsmelding
import no.nav.helse.testhelpers.TestEvent.Companion.søknad
import no.nav.helse.testhelpers.TestEvent.Inntektsmelding
import no.nav.helse.torsdag
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DagturneringTest {

    @Test
    fun `kombinering av tidslinjer fører til at dagsturnering slår sammen dagene`() {
        val søknadSykedager = Sykdomstidslinje.sykedager(1.mandag, 1.fredag, 100.prosent, søknad)
        val søknadArbeidsdager = Sykdomstidslinje.arbeidsdager(1.torsdag, 1.fredag, søknad)

        val tidslinje = søknadSykedager.merge(søknadArbeidsdager, Dagturnering.TURNERING::beste)
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
    fun `arbeidsdag fra inntektsmelding vinner over egenmelding fra søknad`() {
        val søknadArbeidsgiverdag = Sykdomstidslinje.arbeidsgiverdager(1.mandag, 1.mandag, 100.prosent, søknad)
        val inntektsmeldingArbeidsdag = Sykdomstidslinje.arbeidsdager(1.mandag, 1.mandag, inntektsmelding)

        val tidslinje = søknadArbeidsgiverdag.merge(inntektsmeldingArbeidsdag, Dagturnering.TURNERING::beste)

        assertTrue(tidslinje[1.mandag] is Arbeidsdag)
    }

    @Test
    fun `arbeidsdag fra inntektsmelding vinner over sykedag fra søknad`() {
        val søknadSykedag = Sykdomstidslinje.sykedager(1.mandag, 1.mandag, 100.prosent, søknad)
        val inntektsmeldingArbeidsdag = Sykdomstidslinje.arbeidsdager(1.mandag, 1.mandag, inntektsmelding)

        val tidslinje = søknadSykedag.merge(inntektsmeldingArbeidsdag, Dagturnering.TURNERING::beste)

        assertTrue(tidslinje[1.mandag] is Arbeidsdag)
    }

    @Test
    fun `arbeidsgiverdag fra inntektsmelding vinner over arbeidsgiverdag fra søknad`() {
        val søknadArbeidsgiverdag = Sykdomstidslinje.arbeidsgiverdager(1.mandag, 1.mandag, 100.prosent, søknad)
        val inntektsmeldingArbeidsgiverdag = Sykdomstidslinje.arbeidsgiverdager(1.mandag, 1.mandag, 100.prosent, inntektsmelding)

        val tidslinje = søknadArbeidsgiverdag.merge(inntektsmeldingArbeidsgiverdag, Dagturnering.TURNERING::beste)

        assertTrue(tidslinje[1.mandag].kommerFra(hendelse = Inntektsmelding::class))
    }

    @Test
    fun `arbeidsgiverdag fra søknad taper for arbeidsgiverdag fra inntektsmelding`() {
        val søknadArbeidsgiverdag = Sykdomstidslinje.arbeidsgiverdager(1.mandag, 1.mandag, 100.prosent, søknad)
        val inntektsmeldingArbeidsgiverdag = Sykdomstidslinje.arbeidsgiverdager(1.mandag, 1.mandag, 100.prosent, inntektsmelding)

        val tidslinje = inntektsmeldingArbeidsgiverdag.merge(søknadArbeidsgiverdag, Dagturnering.TURNERING::beste)

        assertTrue(tidslinje[1.mandag].kommerFra(hendelse = Inntektsmelding::class))
    }

    @Test
    fun `arbeidsgiverdag_helgedag fra inntektsmelding vinner over arbeidsgiverdag fra søknad`() {
        val søknadArbeidsgiverdag = Sykdomstidslinje.arbeidsgiverdager(1.lørdag, 1.lørdag, 100.prosent, søknad)
        val inntektsmeldingArbeidsgiverHelgedag = Sykdomstidslinje.arbeidsgiverdager(1.lørdag, 1.lørdag, 100.prosent, inntektsmelding)

        val tidslinje = søknadArbeidsgiverdag.merge(inntektsmeldingArbeidsgiverHelgedag, Dagturnering.TURNERING::beste)

        assertTrue(tidslinje[1.lørdag].kommerFra(hendelse = Inntektsmelding::class))
    }

    @Test
    fun `arbeidsgiverdag_helgedag fra søknad taper for arbeidsgiverdag_helgedag fra inntektsmelding`() {
        val søknadArbeidsgiverHelgedag = Sykdomstidslinje.arbeidsgiverdager(1.lørdag, 1.lørdag, 100.prosent, søknad)
        val inntektsmeldingArbeidsgiverHelgedag = Sykdomstidslinje.arbeidsgiverdager(1.lørdag, 1.lørdag, 100.prosent, inntektsmelding)

        val tidslinje = inntektsmeldingArbeidsgiverHelgedag.merge(søknadArbeidsgiverHelgedag, Dagturnering.TURNERING::beste)

        assertTrue(tidslinje[1.lørdag].kommerFra(hendelse = Inntektsmelding::class))
    }

    @Test
    fun `arbeidsgiverdag fra søknad taper over arbeidsgiverdag fra inntektsmelding selvom søknad kom sist`() {
        val inntektsmeldingArbeidsgiverdag = Sykdomstidslinje.arbeidsgiverdager(1.mandag, 1.mandag, 100.prosent, inntektsmelding)
        val søknadArbeidsgiverdag = Sykdomstidslinje.arbeidsgiverdager(1.mandag, 1.mandag, 100.prosent, søknad)

        val tidslinje1 = inntektsmeldingArbeidsgiverdag.merge(søknadArbeidsgiverdag, Dagturnering.TURNERING::beste)

        assertTrue(tidslinje1[1.mandag].kommerFra(hendelse = Inntektsmelding::class))
    }

}

