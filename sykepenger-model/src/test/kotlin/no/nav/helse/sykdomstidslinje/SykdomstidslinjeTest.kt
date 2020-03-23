package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.ImplisittDag
import no.nav.helse.sykdomstidslinje.dag.Ubestemtdag
import no.nav.helse.testhelpers.fredag
import no.nav.helse.testhelpers.mandag
import no.nav.helse.testhelpers.onsdag
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

        assertEquals(" SSSAAII AASSS", arbeidsgiverperiode.toShortString())
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
