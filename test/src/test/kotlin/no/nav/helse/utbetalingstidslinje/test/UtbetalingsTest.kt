package no.nav.helse.utbetalingstidslinje.test

import no.nav.helse.Testhendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.Uke
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class UtbetalingsTest {

    companion object {
        private val sendtSykmelding = Testhendelse(Uke(3).mandag.atStartOfDay())
    }

    @Test
    fun `en sykdomstidslinje innenfor arbeidsgiverperioden gir ingen betalingslinjer`() {
        val sykdomstidslinje = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(2).mandag, sendtSykmelding)
        val betalingslinjer = sykdomstidslinje.betalingslinjer(1200.toBigDecimal())

        assertEquals(0, betalingslinjer.size)
    }

    @Test
    fun `en sykdomstidslinje over arbeidsgiverperioden gir 1 betalingslinje`() {
        val sykdomstidslinje = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(3).fredag, sendtSykmelding)
        val betalingslinjer = sykdomstidslinje.betalingslinjer(1200.toBigDecimal())

        assertEquals(1, betalingslinjer.size)

        val betalingslinje = betalingslinjer.first()
        assertEquals(Uke(3).onsdag, betalingslinje.fom())
        assertEquals(Uke(3).fredag, betalingslinje.tom())
        assertEquals(1200.toBigDecimal(), betalingslinje.dagsats())
    }

    @Test
    fun `opphold i sykedager etter arbeidsgiverperioden gir flere betalingslinjer`() {
        val førsteSykdom = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(3).fredag, sendtSykmelding)
        val arbeidsdager = Sykdomstidslinje.ikkeSykedager(Uke(4).mandag, Uke(4).fredag, sendtSykmelding)
        val andreSykdom = Sykdomstidslinje.sykedager(Uke(5).mandag, Uke(5).fredag, sendtSykmelding)
        val betalingslinjer = (førsteSykdom + arbeidsdager + andreSykdom).betalingslinjer(1200.toBigDecimal())

        assertEquals(2, betalingslinjer.size)

        val førsteBetalingslinje = betalingslinjer.first()
        assertEquals(Uke(3).onsdag, førsteBetalingslinje.fom())
        assertEquals(Uke(3).fredag, førsteBetalingslinje.tom())
        assertEquals(1200.toBigDecimal(), førsteBetalingslinje.dagsats())

        val andreBetalingslinje = betalingslinjer.last()
        assertEquals(Uke(5).mandag, andreBetalingslinje.fom())
        assertEquals(Uke(5).fredag, andreBetalingslinje.tom())
        assertEquals(1200.toBigDecimal(), andreBetalingslinje.dagsats())
    }

    @Test
    fun `opphold i sykedager over 16 dager etter arbeidsgiverperioden blir avvist`() {

    }
}
