package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Testhendelse
import no.nav.helse.sykdomstidslinje.dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.dag.ImplisittDag
import no.nav.helse.sykdomstidslinje.dag.Sykedag
import no.nav.helse.testhelpers.Uke
import no.nav.helse.testhelpers.get
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class CompositeSykdomstidslinjeTest {

    companion object {
        private val tidspunktRapportert = Testhendelse(
            rapportertdato = LocalDateTime.of(
                2019,
                7,
                31,
                20,
                0
            )
        )
    }

    @Test
    internal fun `kan bestemme hvilken type dager mellom to perioder skal ha`() {
        val arbeidsgiverperiode1 = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).onsdag, tidspunktRapportert)
        val arbeidsgiverperiode2 = Sykdomstidslinje.sykedager(Uke(2).onsdag, Uke(2).fredag, tidspunktRapportert)

        val  arbeidsgiverperiode = arbeidsgiverperiode1.plus(arbeidsgiverperiode2, Sykdomstidslinje.Companion::ikkeSykedag)

        assertEquals(Sykedag::class, arbeidsgiverperiode[Uke(1).mandag]!!::class)
        assertEquals(Sykedag::class, arbeidsgiverperiode[Uke(1).tirsdag]!!::class)
        assertEquals(Sykedag::class, arbeidsgiverperiode[Uke(1).onsdag]!!::class)
        assertEquals(Arbeidsdag::class, arbeidsgiverperiode[Uke(1).torsdag]!!::class)
        assertEquals(Arbeidsdag::class, arbeidsgiverperiode[Uke(1).fredag]!!::class)
        assertEquals(ImplisittDag::class, arbeidsgiverperiode[Uke(1).lørdag]!!::class)
        assertEquals(ImplisittDag::class, arbeidsgiverperiode[Uke(1).søndag]!!::class)
        assertEquals(Arbeidsdag::class, arbeidsgiverperiode[Uke(2).mandag]!!::class)
        assertEquals(Arbeidsdag::class, arbeidsgiverperiode[Uke(2).tirsdag]!!::class)
        assertEquals(Sykedag::class, arbeidsgiverperiode[Uke(2).onsdag]!!::class)
        assertEquals(Sykedag::class, arbeidsgiverperiode[Uke(2).torsdag]!!::class)
        assertEquals(Sykedag::class, arbeidsgiverperiode[Uke(2).fredag]!!::class)
    }

    @Test
    internal fun `to sykeperioder med mellomrom får riktig slutt og start dato`() {
        val førsteInterval = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).tirsdag, tidspunktRapportert)
        val andreInterval = Sykdomstidslinje.sykedager(Uke(1).fredag, Uke(2).mandag, tidspunktRapportert)

        val interval = andreInterval + førsteInterval

        assertEquals(Uke(1).mandag, interval.startdato())
        assertEquals(Uke(2).mandag, interval.sluttdato())
        assertEquals(6, interval.antallSykedagerHvorViTellerMedHelg())
        assertEquals(8, interval.flatten().size)
    }

    @Test
    internal fun `tidslinje med ubestemt dag er utenfor omfang`() {
        val studiedag = Sykdomstidslinje.studiedag(Uke(1).mandag, tidspunktRapportert)
        val sykedag = Sykdomstidslinje.sykedag(Uke(1).mandag, tidspunktRapportert)
        val tidslinje = studiedag + sykedag

        assertTrue(tidslinje.erUtenforOmfang())
    }

    @Test
    internal fun `tidslinje med permisjonsdag er utenfor omfang`() {
        val permisjonsdag = Sykdomstidslinje.permisjonsdag(Uke(1).mandag, tidspunktRapportert)
        assertTrue(permisjonsdag.erUtenforOmfang())
    }
}
