package no.nav.helse.sykdomstidslinje

import no.nav.helse.*
import no.nav.helse.hendelser.Testhendelse
import no.nav.helse.sykdomstidslinje.dag.Dag.NøkkelHendelseType.Sykmelding
import no.nav.helse.sykdomstidslinje.dag.Dag.NøkkelHendelseType.Søknad
import no.nav.helse.testhelpers.Uke
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class OverlappingCompositeTest {

    private val nySøknad = Testhendelse(
        rapportertdato = Uke(2).fredag.atTime(12, 0),
        hendelsetype = Sykmelding
    )
    private val sendtSøknad = Testhendelse(
        rapportertdato = Uke(3).fredag.atTime(12, 0),
        hendelsetype = Søknad
    )

    @Test
    internal fun sykedagerOgFerie() {
        perioder(2.sykedager.fra(mandag, nySøknad), 2.feriedager.fra(mandag, sendtSøknad)) { sykedager, _ ->
            assertInterval(sykedager.førsteDag(), sykedager.sisteDag(), 2)
        }
    }

    @Test
    internal fun overlappendeSykedager() {
        perioder(2.sykedager.fra(mandag, nySøknad), 2.sykedager.fra(mandag, sendtSøknad)) { sykedager, _ ->
            assertInterval(sykedager.førsteDag(), sykedager.sisteDag(), 2)
        }
    }

    @Test
    internal fun trailingOverlapp() {
        perioder(4.sykedager.fra(mandag, nySøknad), 2.arbeidsdager.fra(onsdag, sendtSøknad)) { sykedager, _ ->
            assertInterval(sykedager.førsteDag(), sykedager.sisteDag(), 4)
        }
    }

    @Test
    internal fun leadingOverlapp() {
        perioder(4.sykedager.fra(mandag, nySøknad), 2.arbeidsdager.fra(mandag, sendtSøknad)) { sykedager, _ ->
            assertInterval(sykedager.førsteDag(), sykedager.sisteDag(), 4)
        }
    }

    @Test
    internal fun arbeidIMidtenAvSykdom() {
        perioder(4.sykedager.fra(mandag, nySøknad), 2.arbeidsdager.fra(tirsdag, sendtSøknad)) { sykedager, _ ->
            assertInterval(sykedager.førsteDag(), sykedager.sisteDag(), 4)
        }
    }

    @Test
    internal fun leadingAndTrailingIntervals() {
        perioder(3.sykedager.fra(mandag, nySøknad), 3.arbeidsdager.fra(tirsdag, sendtSøknad)) { sykedager, arbeidsdager ->
            assertInterval(sykedager.førsteDag(), arbeidsdager.sisteDag(), 4)
        }
    }

    @Test
    internal fun sykHelgMedLedendeHelg() {
        perioder(5.sykedager.fra(torsdag, nySøknad), 2.feriedager.fra(onsdag, sendtSøknad)) { sykedager, ferie ->
            assertInterval(ferie.førsteDag(), sykedager.sisteDag(), 6)
        }
    }

    @Test
    internal fun friskHelg() {
        perioder(5.sykedager.fra(torsdag, nySøknad), 2.feriedager.fra(onsdag, sendtSøknad)) { sykedager, ferie ->
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


    private fun Sykdomstidslinje.assertInterval(
        startdag: LocalDate,
        sluttdag: LocalDate,
        forventetLengde: Int
    ) {
        assertEquals(startdag, this.førsteDag())
        assertEquals(sluttdag, this.sisteDag())
        assertEquals(forventetLengde, this.flatten().size)
    }
}
