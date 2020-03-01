package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.sykdomstidslinje.dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.dag.ImplisittDag
import no.nav.helse.sykdomstidslinje.dag.Sykedag
import no.nav.helse.testhelpers.Uke
import no.nav.helse.testhelpers.get
import no.nav.helse.tournament.historiskDagturnering
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class CompositeSykdomstidslinjeTest {

    @Test
    internal fun `tom tidslinje kaster exception`() {
        assertThrows<IllegalArgumentException> { CompositeSykdomstidslinje(emptyList()) }
    }

    @Test
    internal fun `kan bestemme hvilken type dager mellom to perioder skal ha`() {
        val arbeidsgiverperiode1 = ConcreteSykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).onsdag, 100.0, Søknad.SøknadDagFactory)
        val arbeidsgiverperiode2 = ConcreteSykdomstidslinje.sykedager(Uke(2).onsdag, Uke(2).fredag, 100.0, Søknad.SøknadDagFactory)

        val arbeidsgiverperiode =
            arbeidsgiverperiode1.plus(arbeidsgiverperiode2, { ConcreteSykdomstidslinje.ikkeSykedag(it, Inntektsmelding.InntektsmeldingDagFactory) }, historiskDagturnering)

        assertEquals(Sykedag.Søknad::class, arbeidsgiverperiode[Uke(1).mandag]!!::class)
        assertEquals(Sykedag.Søknad::class, arbeidsgiverperiode[Uke(1).tirsdag]!!::class)
        assertEquals(Sykedag.Søknad::class, arbeidsgiverperiode[Uke(1).onsdag]!!::class)
        assertEquals(Arbeidsdag.Inntektsmelding::class, arbeidsgiverperiode[Uke(1).torsdag]!!::class)
        assertEquals(Arbeidsdag.Inntektsmelding::class, arbeidsgiverperiode[Uke(1).fredag]!!::class)
        assertEquals(ImplisittDag::class, arbeidsgiverperiode[Uke(1).lørdag]!!::class)
        assertEquals(ImplisittDag::class, arbeidsgiverperiode[Uke(1).søndag]!!::class)
        assertEquals(Arbeidsdag.Inntektsmelding::class, arbeidsgiverperiode[Uke(2).mandag]!!::class)
        assertEquals(Arbeidsdag.Inntektsmelding::class, arbeidsgiverperiode[Uke(2).tirsdag]!!::class)
        assertEquals(Sykedag.Søknad::class, arbeidsgiverperiode[Uke(2).onsdag]!!::class)
        assertEquals(Sykedag.Søknad::class, arbeidsgiverperiode[Uke(2).torsdag]!!::class)
        assertEquals(Sykedag.Søknad::class, arbeidsgiverperiode[Uke(2).fredag]!!::class)
    }

    @Test
    internal fun `to sykeperioder med mellomrom får riktig slutt og start dato`() {
        val førsteInterval = ConcreteSykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).tirsdag, 100.0, Søknad.SøknadDagFactory)
        val andreInterval = ConcreteSykdomstidslinje.sykedager(Uke(1).fredag, Uke(2).mandag, 100.0, Søknad.SøknadDagFactory)

        val interval = andreInterval + førsteInterval

        assertEquals(Uke(1).mandag, interval.førsteDag())
        assertEquals(Uke(2).mandag, interval.sisteDag())
        assertEquals(8, interval.flatten().size)
    }

    @Test
    internal fun `tidslinje med ubestemt dag er utenfor omfang`() {
        val studiedag = ConcreteSykdomstidslinje.studiedag(Uke(1).mandag, Søknad.SøknadDagFactory)
        val sykedag = ConcreteSykdomstidslinje.sykedag(Uke(1).mandag, 100.0, Søknad.SøknadDagFactory)
        val tidslinje = studiedag + sykedag

        val aktivitetslogg = Aktivitetslogg()
        assertFalse(tidslinje.valider(aktivitetslogg))
        assertTrue(aktivitetslogg.hasErrors())
    }

    @Test
    internal fun `tidslinje med permisjonsdag er utenfor omfang`() {
        val permisjonsdag = ConcreteSykdomstidslinje.permisjonsdag(Uke(1).mandag, Søknad.SøknadDagFactory)
        val aktivitetslogg = Aktivitetslogg()
        assertFalse(permisjonsdag.valider(aktivitetslogg))
        assertTrue(aktivitetslogg.hasErrors())
    }

    private operator fun ConcreteSykdomstidslinje.plus(other: ConcreteSykdomstidslinje) = this.plus(other, ::ImplisittDag, historiskDagturnering)
}
