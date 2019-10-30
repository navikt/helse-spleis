package no.nav.helse.utbetalingstidslinje.test

import no.nav.helse.Testhendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.dag.Feriedag
import no.nav.helse.testhelpers.Uke
import no.nav.helse.utbetalingstidslinje.tilUtbetalingstidslinjer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class UtbetalingsTest {

    companion object {
        private val sendtSykmelding = Testhendelse(Uke(3).mandag.atStartOfDay())
    }


    @Test
    fun `enkel sykdomstidslinje mapper til utbetalingsstidslinje uten helg`(){
        val syk = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(3).søndag, sendtSykmelding)
        val syketilfelle = syk.syketilfeller()[0]

        val utbetalingstidslinjer = syketilfelle.tilUtbetalingstidslinjer()

        assertThat(utbetalingstidslinjer).hasSize(1)
        val utbetalingsdager = utbetalingstidslinjer.first().utbetalingsdager


        assertEquals(21, syketilfelle.tidslinje.length())
        assertEquals(15, utbetalingsdager.size)

        assertThat(utbetalingsdager)
            .filteredOn{it.arbeidsgiverperiode}
            .hasSize(12)
            .noneMatch{it.dag.erHelg()}
        assertThat(utbetalingsdager)
            .filteredOn{!it.arbeidsgiverperiode}
            .hasSize(3)
            .noneMatch{it.dag.erHelg()}

        assertThat(syketilfelle.tidslinje.flatten().filterNot { dag -> dag in utbetalingsdager.map { it.dag } })
            .hasSize(6)
            .allMatch{it.erHelg()}
    }


    @Test
    fun `enkel sykdomstidslinje splitter`(){
        val fri = Sykdomstidslinje.ferie(Uke(0).fredag, Uke(0).søndag, sendtSykmelding)
        val influensa = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(3).mandag, sendtSykmelding)
        val ferie = Sykdomstidslinje.ferie(Uke(3).tirsdag, Uke(4).lørdag, sendtSykmelding)
        val spysyka = Sykdomstidslinje.sykedager(Uke(4).søndag, Uke(5).mandag, sendtSykmelding)
        val syketilfelle = (fri + influensa + ferie + spysyka).syketilfeller()[0]

        val utbetalingsdager = syketilfelle.tilUtbetalingstidslinjer()
        println(utbetalingsdager)

    }

    @Test
    fun `gitt en person som blir syk på en mandag og er syk i 15 dager, har ferie i 12 dager og så syk i 2 dager er utbetalingsstidlinjen 12 dager, hvorav de første 11 i arbeidsgiverperioden`(){
        val influensa = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(3).mandag, sendtSykmelding)
        val ferie = Sykdomstidslinje.ferie(Uke(3).tirsdag, Uke(4).lørdag, sendtSykmelding)
        val spysyka = Sykdomstidslinje.sykedager(Uke(4).søndag, Uke(5).mandag, sendtSykmelding)
        val syketilfelle = (influensa + ferie + spysyka).syketilfeller()[0]

        val utbetalingstidslinjer = syketilfelle.tilUtbetalingstidslinjer()

        assertThat(utbetalingstidslinjer).hasSize(1)
        val utbetalingsdager = utbetalingstidslinjer.first().utbetalingsdager

        assertEquals(29, syketilfelle.tidslinje.length())
        assertEquals(12, utbetalingsdager.size)

        assertThat(utbetalingsdager)
            .filteredOn{it.arbeidsgiverperiode}
            .hasSize(11)
            .noneMatch{it.dag.erHelg()}
        assertThat(utbetalingsdager)
            .filteredOn{!it.arbeidsgiverperiode}
            .hasSize(1)
            .noneMatch{it.dag.erHelg()}

        assertThat(syketilfelle.tidslinje.flatten().filterNot { dag -> dag in utbetalingsdager.map { it.dag } })
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

        val utbetalingstidslinjer = syketilfeller[0].tilUtbetalingstidslinjer()

        assertThat(utbetalingstidslinjer).hasSize(1)
        val utbetalingsdager = utbetalingstidslinjer.first().utbetalingsdager

        val arbeidsgiverPeriodensSisteDag = Uke(1).mandag.plusDays(15)
        val førsteUtbetalingsdag = Uke(4).mandag
        assertEquals(
            arbeidsgiverPeriodensSisteDag,
            syketilfeller.first().arbeidsgiverperiode!!.sluttdato(),
            "feil arbeidsgiverperiode"
        )
        assertEquals(
            førsteUtbetalingsdag,
            utbetalingsdager.filterNot { it.arbeidsgiverperiode }.first().dag.startdato(),
            "feil utbetalingsstart"
        )
    }


}
