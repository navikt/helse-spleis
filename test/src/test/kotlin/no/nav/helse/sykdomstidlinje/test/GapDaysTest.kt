package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.Testhendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.Uke
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GapDaysTest {
    private val sendtSøknad = Testhendelse()

    @Test
    internal fun tidslinjerMedAvstandMellom() {
        val førsteTidslinje = Sykdomstidslinje.sykedager(
            Uke(1).mandag,
            Uke(1).tirsdag,
            sendtSøknad
        )
        val andreTidslinje = Sykdomstidslinje.sykedager(
            Uke(1).fredag,
            Uke(2).mandag,
            sendtSøknad
        )

        assertEquals(2, førsteTidslinje.antallDagerMellom(andreTidslinje))
        assertEquals(2, andreTidslinje.antallDagerMellom(førsteTidslinje))
    }

    @Test
    internal fun tidslinjerSammenhengende() {
        val førsteTidslinje = Sykdomstidslinje.sykedager(
            Uke(1).mandag,
            Uke(1).tirsdag,
            sendtSøknad
        )
        val andreTidslinje = Sykdomstidslinje.sykedager(
            Uke(1).onsdag,
            Uke(1).fredag,
            sendtSøknad
        )

        assertEquals(0, førsteTidslinje.antallDagerMellom(andreTidslinje))
        assertEquals(0, andreTidslinje.antallDagerMellom(førsteTidslinje))
    }

    @Test
    internal fun overlappendeTidslinjer() {
        val førsteTidslinje = Sykdomstidslinje.sykedager(
            Uke(1).mandag,
            Uke(1).onsdag,
            sendtSøknad
        )
        val andreTidslinje = Sykdomstidslinje.sykedager(
            Uke(1).tirsdag,
            Uke(1).fredag,
            sendtSøknad
        )

        assertEquals(-2, førsteTidslinje.antallDagerMellom(andreTidslinje))
        assertEquals(-2, andreTidslinje.antallDagerMellom(førsteTidslinje))
    }

    @Test
    internal fun denEneSomDelAvDenAndre() {
        val førsteTidslinje = Sykdomstidslinje.sykedager(
            Uke(1).mandag,
            Uke(2).mandag,
            sendtSøknad
        )
        val andreTidslinje = Sykdomstidslinje.sykedager(
            Uke(1).onsdag,
            Uke(1).fredag,
            sendtSøknad
        )

        assertEquals(-3, førsteTidslinje.antallDagerMellom(andreTidslinje))
        assertEquals(-3, andreTidslinje.antallDagerMellom(førsteTidslinje))
    }
}
