package no.nav.helse.utbetalingstidslinje.test

import no.nav.helse.Testhendelse
import no.nav.helse.sykdomstidlinje.test.SykedagerTest
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.dag.Feriedag
import no.nav.helse.testhelpers.Uke
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class UtbetalingsTest {

    companion object {
        private val sendtSykmelding = Testhendelse(Uke(3).mandag.atStartOfDay())
    }


    @Test
    fun `enkel sykdomstidslinje mapper til utbetalingsstidslinje uten helg`(){
        val syk = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(3).søndag, sendtSykmelding)
        val syketilfelle = syk.syketilfeller()[0]

        val utbetalingstidslinje = syketilfelle.tilUtbetalingstidslinje()

        assertEquals(21, syketilfelle.tidslinje.length())
        assertEquals(15, utbetalingstidslinje.size)
        assertEquals(BigDecimal.ZERO, utbetalingstidslinje[11].dagsats)
        assertEquals(BigDecimal.TEN, utbetalingstidslinje[12].dagsats)

        assertThat(utbetalingstidslinje)
            .filteredOn{it.arbeidsgiverperiode}
            .hasSize(12)
            .allMatch{ it.dagsats == BigDecimal.ZERO }
            .noneMatch{it.dag.erHelg()}
        assertThat(utbetalingstidslinje)
            .filteredOn{!it.arbeidsgiverperiode}
            .hasSize(3)
            .allMatch{it.dagsats == BigDecimal.TEN}
            .noneMatch{it.dag.erHelg()}

        assertThat(syketilfelle.tidslinje.flatten().filterNot { dag -> dag in utbetalingstidslinje.map { it.dag } })
            .hasSize(6)
            .allMatch{it.erHelg()}
    }

    @Test
    fun `gitt en person som blir syk på en mandag og er syk i 15 dager, har ferie i 12 dager og så syk i 2 dager er utbetalingsstidlinjen 12 dager, hvorav de første 11 i arbeidsgiverperioden`(){
        val influensa = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(3).mandag, sendtSykmelding)
        val ferie = Sykdomstidslinje.ferie(Uke(3).tirsdag, Uke(4).lørdag, sendtSykmelding)
        val spysyka = Sykdomstidslinje.sykedager(Uke(4).søndag, Uke(5).mandag, sendtSykmelding)
        val syketilfelle = (influensa + ferie + spysyka).syketilfeller()[0]

        val utbetalingstidslinje = syketilfelle.tilUtbetalingstidslinje()

        assertEquals(29, syketilfelle.tidslinje.length())
        assertEquals(12, utbetalingstidslinje.size)

        assertThat(utbetalingstidslinje)
            .filteredOn{it.arbeidsgiverperiode}
            .hasSize(11)
            .allMatch{ it.dagsats == BigDecimal.ZERO }
            .noneMatch{it.dag.erHelg()}
        assertThat(utbetalingstidslinje)
            .filteredOn{!it.arbeidsgiverperiode}
            .hasSize(1)
            .allMatch{it.dagsats == BigDecimal.TEN}
            .noneMatch{it.dag.erHelg()}

        assertThat(syketilfelle.tidslinje.flatten().filterNot { dag -> dag in utbetalingstidslinje.map { it.dag } })
            .hasSize(17)
            .allMatch{it.erHelg() || it is Feriedag}
    }


    @Test
    fun `gitt en person som er syk i 5 dager, har 14 dager ferie og så syk i 5 dager igjen, skal arbeidsgiverperioden utløpe på dag 16 og første utbetalingsdag skal være første dag etter ferie`() {
        val influensa = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).fredag, sendtSykmelding)
        val ferie = Sykdomstidslinje.ferie(Uke(2).mandag, Uke(3).fredag, sendtSykmelding)
        val spysyka = Sykdomstidslinje.sykedager(Uke(4).mandag, Uke(4).fredag, sendtSykmelding)

        val syketilfeller = (influensa + ferie + spysyka).syketilfeller()
        assertEquals(1, syketilfeller.size)

        val utbetalingstidslinje = syketilfeller[0].tilUtbetalingstidslinje()

        val arbeidsgiverPeriodensSisteDag = Uke(1).mandag.plusDays(15)
        val førsteUtbetalingsdag = Uke(4).mandag
        assertEquals(
            arbeidsgiverPeriodensSisteDag,
            syketilfeller.first().arbeidsgiverperiode!!.sluttdato(),
            "feil arbeidsgiverperiode"
        )
        assertEquals(
            førsteUtbetalingsdag,
            utbetalingstidslinje.filterNot { it.arbeidsgiverperiode }.first().dag.startdato(),
            "feil utbetalingsstart"
        )
    }


}
