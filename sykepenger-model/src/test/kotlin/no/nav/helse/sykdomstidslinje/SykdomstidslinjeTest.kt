package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.ImplisittDag
import no.nav.helse.sykdomstidslinje.dag.Ubestemtdag
import no.nav.helse.testhelpers.fredag
import no.nav.helse.testhelpers.mandag
import no.nav.helse.testhelpers.onsdag
import no.nav.helse.testhelpers.tirsdag
import no.nav.helse.tournament.Dagturnering
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class SykdomstidslinjeTest {

    @Test internal fun `tom tidslinje er gyldig`() {
        assertEquals(0, Sykdomstidslinje().size)
    }

    @Test internal fun `kan bestemme hvilken type dager mellom to perioder skal ha`() {
        val tidslinje1 = Sykdomstidslinje.sykedager(
            1.mandag, 1.onsdag, 100.0, Søknad.SøknadDagFactory)
        val tidslinje2 = Sykdomstidslinje.sykedager(
            2.onsdag, 2.fredag, 100.0, Søknad.SøknadDagFactory)
        val tidslinje = tidslinje1.merge(tidslinje2, KonfliktskyDagturnering) {
            Sykdomstidslinje.ikkeSykedag(it, Inntektsmelding.InntektsmeldingDagFactory)
        }
        assertEquals(" SSSAARR AASSS", tidslinje.toShortString())
    }

    @Test
    internal fun `to sykeperioder med mellomrom får riktig slutt og start dato`() {
        val tidslinje1 = Sykdomstidslinje.sykedager(1.mandag, 1.tirsdag, 100.0, Søknad.SøknadDagFactory)
        val tidslinje2 = Sykdomstidslinje.sykedager(1.fredag, 2.mandag, 100.0, Søknad.SøknadDagFactory)

        val tidslinje = tidslinje2 + tidslinje1

        assertEquals(1.mandag, tidslinje.førsteDag())
        assertEquals(2.mandag, tidslinje.sisteDag())
        assertEquals(8, tidslinje.size)
        assertEquals(" SSIISHH S", tidslinje.toShortString())
    }

    @Test
    internal fun `tidslinje med ubestemt dag er utenfor omfang`() {
        val tidslinje = Sykdomstidslinje.ubestemtdager(1.mandag, 1.mandag, Søknad.SøknadDagFactory)
        val aktivitetslogg = Aktivitetslogg()
        assertFalse(tidslinje.valider(aktivitetslogg))
        assertTrue(aktivitetslogg.hasErrors())
    }


    @Test
    internal fun `tidslinje med permisjonsdag er utenfor omfang`() {
        val tidslinje = Sykdomstidslinje.permisjonsdager(1.mandag, 1.mandag, Søknad.SøknadDagFactory)
        val aktivitetslogg = Aktivitetslogg()
        assertFalse(tidslinje.valider(aktivitetslogg))
        assertTrue(aktivitetslogg.hasErrors())
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
