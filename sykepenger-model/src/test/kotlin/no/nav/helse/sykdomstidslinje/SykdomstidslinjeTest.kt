package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.testhelpers.*
import no.nav.helse.tournament.Dagturnering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SykdomstidslinjeTest {

    @Test internal fun `tom tidslinje er gyldig`() {
        assertEquals(0, Sykdomstidslinje().size)
    }

    @Test internal fun `kan bestemme hvilken type dager mellom to perioder skal ha`() {
        val arbeidsgiverperiode1 = Sykdomstidslinje.sykedager(
            1.mandag, 1.onsdag, 100.0, Søknad.SøknadDagFactory)
        val arbeidsgiverperiode2 = Sykdomstidslinje.sykedager(
            2.onsdag, 2.fredag, 100.0, Søknad.SøknadDagFactory)

        val arbeidsgiverperiode = arbeidsgiverperiode1.merge(arbeidsgiverperiode2, KonfliktskyDagturnering) {
            Sykdomstidslinje.ikkeSykedag(it, Inntektsmelding.InntektsmeldingDagFactory)
        }

        assertEquals("SSSAAIIAASSS", arbeidsgiverperiode.toShortString())

        assertEquals(Sykedag.Søknad::class, arbeidsgiverperiode[1.mandag]!!::class)
        assertEquals(Sykedag.Søknad::class, arbeidsgiverperiode[1.tirsdag]!!::class)
        assertEquals(Sykedag.Søknad::class, arbeidsgiverperiode[1.onsdag]!!::class)
        assertEquals(Arbeidsdag.Inntektsmelding::class, arbeidsgiverperiode[1.torsdag]!!::class)
        assertEquals(Arbeidsdag.Inntektsmelding::class, arbeidsgiverperiode[1.fredag]!!::class)
        assertEquals(ImplisittDag::class, arbeidsgiverperiode[1.lørdag]!!::class)
        assertEquals(ImplisittDag::class, arbeidsgiverperiode[1.søndag]!!::class)
        assertEquals(Arbeidsdag.Inntektsmelding::class, arbeidsgiverperiode[2.mandag]!!::class)
        assertEquals(Arbeidsdag.Inntektsmelding::class, arbeidsgiverperiode[2.tirsdag]!!::class)
        assertEquals(Sykedag.Søknad::class, arbeidsgiverperiode[2.onsdag]!!::class)
        assertEquals(Sykedag.Søknad::class, arbeidsgiverperiode[2.torsdag]!!::class)
        assertEquals(Sykedag.Søknad::class, arbeidsgiverperiode[2.fredag]!!::class)
    }

    private object KonfliktskyDagturnering : Dagturnering {
        override fun beste(venstre: Dag, høyre: Dag): Dag {
            return when {
                venstre is ImplisittDag -> høyre
                høyre is ImplisittDag -> venstre
                else -> Ubestemtdag(venstre.dagen)
            }
        }
    }
}
