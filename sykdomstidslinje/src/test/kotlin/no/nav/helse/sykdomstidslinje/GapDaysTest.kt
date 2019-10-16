package no.nav.helse.sykdomstidslinje

import no.nav.helse.testhelpers.fredag
import no.nav.helse.testhelpers.mandag
import no.nav.helse.testhelpers.onsdag
import no.nav.helse.testhelpers.tirsdag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GapDaysTest {
    private val sendtSøknad = Testhendelse()

    @Test
    internal fun tidslinjerMedAvstandMellom() {
        val førsteTidslinje = Sykdomstidslinje.sykedager(
            1.mandag,
            1.tirsdag,
            sendtSøknad
        )
        val andreTidslinje = Sykdomstidslinje.sykedager(
            1.fredag,
            2.mandag,
            sendtSøknad
        )

        assertEquals(2, førsteTidslinje.antallDagerMellom(andreTidslinje))
        assertEquals(2, andreTidslinje.antallDagerMellom(førsteTidslinje))
    }

    @Test
    internal fun tidslinjerSammenhengende() {
        val førsteTidslinje = Sykdomstidslinje.sykedager(
            1.mandag,
            1.tirsdag,
            sendtSøknad
        )
        val andreTidslinje = Sykdomstidslinje.sykedager(
            1.onsdag,
            1.fredag,
            sendtSøknad
        )

        assertEquals(0, førsteTidslinje.antallDagerMellom(andreTidslinje))
        assertEquals(0, andreTidslinje.antallDagerMellom(førsteTidslinje))
    }

    @Test
    internal fun overlappendeTidslinjer() {
        val førsteTidslinje = Sykdomstidslinje.sykedager(
            1.mandag,
            1.onsdag,
            sendtSøknad
        )
        val andreTidslinje = Sykdomstidslinje.sykedager(
            1.tirsdag,
            1.fredag,
            sendtSøknad
        )

        assertEquals(-2, førsteTidslinje.antallDagerMellom(andreTidslinje))
        assertEquals(-2, andreTidslinje.antallDagerMellom(førsteTidslinje))
    }

    @Test
    internal fun denEneSomDelAvDenAndre() {
        val førsteTidslinje = Sykdomstidslinje.sykedager(
            1.mandag,
            2.mandag,
            sendtSøknad
        )
        val andreTidslinje = Sykdomstidslinje.sykedager(
            1.onsdag,
            1.fredag,
            sendtSøknad
        )

        assertEquals(-3, førsteTidslinje.antallDagerMellom(andreTidslinje))
        assertEquals(-3, andreTidslinje.antallDagerMellom(førsteTidslinje))
    }
}
