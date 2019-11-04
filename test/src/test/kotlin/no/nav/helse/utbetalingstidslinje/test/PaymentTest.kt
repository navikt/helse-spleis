package no.nav.helse.utbetalingstidslinje.test

import no.nav.helse.Testhendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.Uke
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class PaymentTest {

    companion object {
        private val sendtSykmelding = Testhendelse(Uke(3).mandag.atStartOfDay())
    }


    //Mandag
    private var startDato = LocalDate.of(2018, 1, 1)

    private val dagsats = 1200.toBigDecimal()

    @Test
    internal fun `to dager blir betalt av arbeidsgiver`() {
        val tosykedager = S + S

        val betalingslinjer = tosykedager.betalingslinjer(dagsats)

        assertEquals(0, betalingslinjer.size)
    }

    @Test
    internal fun `tjue dager gir 4 dager betalt av NAV`() {
        val tosykedager = 20.S

        val betalingslinjer = tosykedager.betalingslinjer(dagsats)

        assertEquals(1, betalingslinjer.size)
        assertEquals(LocalDate.of(2018, 1, 17), betalingslinjer.first().fom())
        assertEquals(LocalDate.of(2018, 1, 20), betalingslinjer.first().tom())
    }

    @Test
    internal fun `Sykedager med inneklemt ferie`() {
        val sykdomstidslinje = 21.S + 2.S + 2.F + S
        val betalingslinjer = sykdomstidslinje.betalingslinjer(dagsats)

        assertEquals(2, betalingslinjer.size)
        assertEquals(LocalDate.of(2018, 1, 17), betalingslinjer.first().fom())
        assertEquals(LocalDate.of(2018, 1, 23), betalingslinjer.first().tom())

        assertEquals(LocalDate.of(2018, 1, 26), betalingslinjer.last().fom())
        assertEquals(LocalDate.of(2018, 1, 26), betalingslinjer.last().tom())
    }

    @Test
    internal fun `Ferie i arbeidsgiverperiode`() {
        val sykdomstidslinje = S + 2.F + 13.S + S
        val betalingslinjer = sykdomstidslinje.betalingslinjer(dagsats)

        assertEquals(1, betalingslinjer.size)
        assertEquals(LocalDate.of(2018, 1, 17), betalingslinjer.first().fom())
        assertEquals(LocalDate.of(2018, 1, 17), betalingslinjer.first().tom())
    }

    @Test
    internal fun `Arbeidsdag etter feire i arbeidsgiverperioden`() {
        val sykdomstidslinje = S + 2.F + A + S + 14.S + S
        val betalingslinjer = sykdomstidslinje.betalingslinjer(dagsats)

        assertEquals(1, betalingslinjer.size)
        assertEquals(LocalDate.of(2018, 1, 20), betalingslinjer.first().fom())
        assertEquals(LocalDate.of(2018, 1, 20), betalingslinjer.first().tom())
    }

    @Test
    internal fun `Arbeidsdag før ferie i arbeidsgiverperioden`() {
        val sykdomstidslinje = S + A + 2.F + S + 14.S + S
        val betalingslinjer = sykdomstidslinje.betalingslinjer(dagsats)

        assertEquals(1, betalingslinjer.size)
        assertEquals(LocalDate.of(2018, 1, 20), betalingslinjer.first().fom())
        assertEquals(LocalDate.of(2018, 1, 20), betalingslinjer.first().tom())
    }

    @Test
    internal fun `Ferie etter arbeidsgiverperioden`() {
        val sykdomstidslinje = 14.S + 2.S + 2.F + S
        val betalingslinjer = sykdomstidslinje.betalingslinjer(dagsats)

        assertEquals(1, betalingslinjer.size)
        assertEquals(LocalDate.of(2018, 1, 19), betalingslinjer.first().fom())
        assertEquals(LocalDate.of(2018, 1, 19), betalingslinjer.first().tom())
    }


    @Test
    internal fun `Arbeidsdag etter ferie teller som gap`() {
        val sykdomstidslinje = 14.S + S + 2.F + A + S
        val betalingslinjer = sykdomstidslinje.betalingslinjer(dagsats)

        assertEquals(0, betalingslinjer.size)

    }




    private val S
        get() = Sykdomstidslinje.sykedag(startDato, sendtSykmelding).also {
            startDato = startDato.plusDays(1)
        }

    private val A
        get() = Sykdomstidslinje.ikkeSykedag(startDato, sendtSykmelding).also {
            startDato = startDato.plusDays(1)
        }


    private val Int.S
        get() = Sykdomstidslinje.sykedager(startDato, startDato.plusDays(this.toLong() - 1), sendtSykmelding)
            .also { startDato = startDato.plusDays(this.toLong()) }

    private val Int.F
        get() = Sykdomstidslinje.ferie(startDato, startDato.plusDays(this.toLong() - 1), sendtSykmelding)
            .also { startDato = startDato.plusDays(this.toLong()) }
}

