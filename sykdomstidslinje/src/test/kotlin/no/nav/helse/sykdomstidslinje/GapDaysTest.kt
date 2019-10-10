package no.nav.helse.sykdomstidslinje

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GapDaysTest {

    companion object {
        private val tidspunktRapportert = Testhendelse()

        private val mandag = LocalDate.of(2019,9,23)
        private val tirsdag = LocalDate.of(2019,9,24)
        private val onsdag = LocalDate.of(2019,9,25)
        private val fredag = LocalDate.of(2019,9,27)
        private val andreMandag = LocalDate.of(2019,9,30)
    }

    @Test
    internal fun tidslinjerMedAvstandMellom() {
        val førsteTidslinje = Sykdomstidslinje.sykedager(
            mandag,
            tirsdag,
            tidspunktRapportert
        )
        val andreTidslinje = Sykdomstidslinje.sykedager(
            fredag,
            andreMandag,
            tidspunktRapportert
        )

        assertEquals(2, førsteTidslinje.antallDagerMellom(andreTidslinje))
        assertEquals(2, andreTidslinje.antallDagerMellom(førsteTidslinje))
    }

    @Test
    internal fun tidslinjerSammenhengende() {
        val førsteTidslinje = Sykdomstidslinje.sykedager(
            mandag,
            tirsdag,
            tidspunktRapportert
        )
        val andreTidslinje = Sykdomstidslinje.sykedager(
            onsdag,
            fredag,
            tidspunktRapportert
        )

        assertEquals(0, førsteTidslinje.antallDagerMellom(andreTidslinje))
        assertEquals(0, andreTidslinje.antallDagerMellom(førsteTidslinje))
    }

    @Test
    internal fun overlappendeTidslinjer() {
        val førsteTidslinje = Sykdomstidslinje.sykedager(
            mandag,
            onsdag,
            tidspunktRapportert
        )
        val andreTidslinje = Sykdomstidslinje.sykedager(
            tirsdag,
            fredag,
            tidspunktRapportert
        )

        assertEquals(-2, førsteTidslinje.antallDagerMellom(andreTidslinje))
        assertEquals(-2, andreTidslinje.antallDagerMellom(førsteTidslinje))
    }

    @Test
    internal fun denEneSomDelAvDenAndre() {
        val førsteTidslinje = Sykdomstidslinje.sykedager(
            mandag,
            andreMandag,
            tidspunktRapportert
        )
        val andreTidslinje = Sykdomstidslinje.sykedager(
            onsdag,
            fredag,
            tidspunktRapportert
        )

        assertEquals(-3, førsteTidslinje.antallDagerMellom(andreTidslinje))
        assertEquals(-3, andreTidslinje.antallDagerMellom(førsteTidslinje))
    }
}
