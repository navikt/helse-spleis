package no.nav.helse.sykdomstidslinje

import no.nav.helse.*
import no.nav.helse.hendelser.SendtSøknad
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class OverlappingCompositeTest {
    private val Sykmelding = no.nav.helse.hendelser.Sykmelding.SykmeldingDagFactory
    private val Søknad = SendtSøknad.SøknadDagFactory

    @Test
    internal fun sykedagerOgFerie() {
        perioder(2.sykedager.fra(mandag, Sykmelding), 2.feriedager.fra(mandag, Søknad)) { sykedager, _ ->
            assertInterval(sykedager.førsteDag(), sykedager.sisteDag(), 2)
        }
    }

    @Test
    internal fun overlappendeSykedager() {
        perioder(2.sykedager.fra(mandag, Sykmelding), 2.sykedager.fra(mandag, Søknad)) { sykedager, _ ->
            assertInterval(sykedager.førsteDag(), sykedager.sisteDag(), 2)
        }
    }

    @Test
    internal fun trailingOverlapp() {
        perioder(4.sykedager.fra(mandag, Sykmelding), 2.arbeidsdager.fra(onsdag, Søknad)) { sykedager, _ ->
            assertInterval(sykedager.førsteDag(), sykedager.sisteDag(), 4)
        }
    }

    @Test
    internal fun leadingOverlapp() {
        perioder(4.sykedager.fra(mandag, Sykmelding), 2.arbeidsdager.fra(mandag, Søknad)) { sykedager, _ ->
            assertInterval(sykedager.førsteDag(), sykedager.sisteDag(), 4)
        }
    }

    @Test
    internal fun arbeidIMidtenAvSykdom() {
        perioder(4.sykedager.fra(mandag, Sykmelding), 2.arbeidsdager.fra(tirsdag, Søknad)) { sykedager, _ ->
            assertInterval(sykedager.førsteDag(), sykedager.sisteDag(), 4)
        }
    }

    @Test
    internal fun leadingAndTrailingIntervals() {
        perioder(3.sykedager.fra(mandag, Sykmelding), 3.arbeidsdager.fra(tirsdag, Søknad)) { sykedager, arbeidsdager ->
            assertInterval(sykedager.førsteDag(), arbeidsdager.sisteDag(), 4)
        }
    }

    @Test
    internal fun sykHelgMedLedendeHelg() {
        perioder(2.sykedager.fra(torsdag, Sykmelding), 2.sykHelgdager, 1.sykedager, 2.feriedager.fra(onsdag, Søknad)) { _, _, sykedagerEtterHelg, ferie ->
            assertInterval(ferie.førsteDag(), sykedagerEtterHelg.sisteDag(), 6)
        }
    }

    @Test
    internal fun friskHelg() {
        perioder(5.sykedager.fra(torsdag, Sykmelding), 2.feriedager.fra(onsdag, Søknad)) { sykedager, ferie ->
            assertInterval(ferie.førsteDag(), sykedager.sisteDag(), 6)
        }
    }


    @Test
    internal fun `sykdomstidslinjer som er kant i kant overlapper ikke`(){
        perioder(3.sykedager.fra(mandag), 5.feriedager.fra(torsdag)) { sykedager, ferie ->
            assertFalse(sykedager.overlapperMed(ferie))
        }
    }


    @Test
    internal fun `sykdomstidslinjer overlapper`(){
        perioder(3.sykedager.fra(mandag), 5.feriedager.fra(onsdag)) { sykedager, ferie ->
            assertTrue(sykedager.overlapperMed(ferie))
        }
    }

    @Test
    internal fun `sykdomstidslinjer med et gap på en hel dag overlapper ikke`() {
        perioder(2.sykedager.fra(mandag), 5.feriedager.fra(torsdag)) { sykedager, ferie ->
            assertFalse(sykedager.overlapperMed(ferie))
        }
    }


    private fun ConcreteSykdomstidslinje.assertInterval(
        startdag: LocalDate,
        sluttdag: LocalDate,
        forventetLengde: Int
    ) {
        assertEquals(startdag, this.førsteDag())
        assertEquals(sluttdag, this.sisteDag())
        assertEquals(forventetLengde, this.flatten().size)
    }
}
