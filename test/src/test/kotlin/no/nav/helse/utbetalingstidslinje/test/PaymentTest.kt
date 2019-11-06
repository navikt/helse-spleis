package no.nav.helse.utbetalingstidslinje.test

import no.nav.helse.Testhendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.Uke
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PaymentTest {

    companion object {
        private val sendtSykmelding = Testhendelse(Uke(3).mandag.atStartOfDay())
    }

    //Mandag
    private var startDato = LocalDate.of(2018, 1, 1)

    private val dagsats = 1200.toBigDecimal()

    @Test
    fun `to dager blir betalt av arbeidsgiver`() {
        val tosykedager = S + S

        val betalingslinjer = tosykedager.betalingslinjer(dagsats)

        assertEquals(0, betalingslinjer.size)
    }

    @Test
    fun `tjue dager gir 4 dager betalt av NAV`() {
        val tosykedager = 20.S

        val betalingslinjer = tosykedager.betalingslinjer(dagsats)

        assertEquals(1, betalingslinjer.size)
        assertEquals(LocalDate.of(2018, 1, 17), betalingslinjer.first().fom)
        assertEquals(LocalDate.of(2018, 1, 20), betalingslinjer.first().tom)
    }

    @Test
    fun `Sykedager med inneklemt ferie`() {
        val sykdomstidslinje = 21.S + 2.S + 2.F + S
        val betalingslinjer = sykdomstidslinje.betalingslinjer(dagsats)

        assertEquals(2, betalingslinjer.size)
        assertEquals(LocalDate.of(2018, 1, 17), betalingslinjer.first().fom)
        assertEquals(LocalDate.of(2018, 1, 23), betalingslinjer.first().tom)

        assertEquals(LocalDate.of(2018, 1, 26), betalingslinjer.last().fom)
        assertEquals(LocalDate.of(2018, 1, 26), betalingslinjer.last().tom)
    }

    @Test
    fun `Ferie i arbeidsgiverperiode`() {
        val sykdomstidslinje = S + 2.F + 13.S + S
        val betalingslinjer = sykdomstidslinje.betalingslinjer(dagsats)

        assertEquals(1, betalingslinjer.size)
        assertEquals(LocalDate.of(2018, 1, 17), betalingslinjer.first().fom)
        assertEquals(LocalDate.of(2018, 1, 17), betalingslinjer.first().tom)
    }

    @Test
    fun `Arbeidsdag etter feire i arbeidsgiverperioden`() {
        val sykdomstidslinje = S + 2.F + A + S + 14.S + S
        val betalingslinjer = sykdomstidslinje.betalingslinjer(dagsats)

        assertEquals(1, betalingslinjer.size)
        assertEquals(LocalDate.of(2018, 1, 20), betalingslinjer.first().fom)
        assertEquals(LocalDate.of(2018, 1, 20), betalingslinjer.first().tom)
    }

    @Test
    fun `Arbeidsdag før ferie i arbeidsgiverperioden`() {
        val sykdomstidslinje = S + A + 2.F + S + 14.S + S
        val betalingslinjer = sykdomstidslinje.betalingslinjer(dagsats)

        assertEquals(1, betalingslinjer.size)
        assertEquals(LocalDate.of(2018, 1, 20), betalingslinjer.first().fom)
        assertEquals(LocalDate.of(2018, 1, 20), betalingslinjer.first().tom)
    }

    @Test
    fun `Ferie etter arbeidsgiverperioden`() {
        val sykdomstidslinje = 14.S + 2.S + 2.F + S
        val betalingslinjer = sykdomstidslinje.betalingslinjer(dagsats)

        assertEquals(1, betalingslinjer.size)
        assertEquals(LocalDate.of(2018, 1, 19), betalingslinjer.first().fom)
        assertEquals(LocalDate.of(2018, 1, 19), betalingslinjer.first().tom)
    }

    @Test
    fun `Arbeidsdag etter ferie teller som gap`() {
        val sykdomstidslinje = 14.S + S + 2.F + A + S
        val betalingslinjer = sykdomstidslinje.betalingslinjer(dagsats)

        assertEquals(0, betalingslinjer.size)

    }

    @Test
    fun `Ferie rett etter arbeidsgiverperioden vacation teller ikke som opphold`() {
        val sykdomstidslinje = 16.S + 16.F + A + 3.S
        val betalingslinjer = sykdomstidslinje.betalingslinjer(dagsats)

        assertEquals(1, betalingslinjer.size)
        assertEquals(LocalDate.of(2018, 2, 3), betalingslinjer.first().fom)
        assertEquals(LocalDate.of(2018, 2, 5), betalingslinjer.first().tom)

    }

    @Test
    fun `Ferie i slutten av arbeidsgiverperioden teller som opphold`() {
        val sykdomstidslinje = 15.S + 16.F + A + 3.S
        val betalingslinjer = sykdomstidslinje.betalingslinjer(dagsats)

        assertEquals(0, betalingslinjer.size)
    }

    @Test
    fun `Ferie og arbeid påvirker ikke initiell tilsand`() {
        val sykdomstidslinje = 2.F + 2.A + 16.S + 2.F
        val betalingslinjer = sykdomstidslinje.betalingslinjer(dagsats)

        assertThat(betalingslinjer).isEmpty()
    }

    @Test
    fun `20 feriedager med påfølgende arbeidsdag resetter arbeidsgiverperioden`() {
        val sykdomstidslinje = 10.S + 20.F + A + 10.S + 20.F
        val betalingslinjer = sykdomstidslinje.betalingslinjer(dagsats)

        assertThat(betalingslinjer).isEmpty()
    }

    @Test
    fun `Ferie fullfører arbeidsgiverperioden`() {
        val sykdomstidslinje = 10.S + 20.F + 10.S
        val betalingslinjer = sykdomstidslinje.betalingslinjer(dagsats)

        assertThat(betalingslinjer).hasSize(1)
        assertEquals(LocalDate.of(2018, 1, 31), betalingslinjer.first().fom)
        assertEquals(LocalDate.of(2018, 2, 9), betalingslinjer.first().tom)
    }

    @Test
    fun `Ferie mer enn 16 dager gir ikke ny arbeidsgiverperiode for betalingslinje 2`() {
        val sykdomstidslinje = 20.S + 20.F + 10.S
        val betalingslinjer = sykdomstidslinje.betalingslinjer(dagsats)

        assertThat(betalingslinjer).hasSize(2)
        assertEquals(LocalDate.of(2018, 1, 17), betalingslinjer.first().fom)
        assertEquals(LocalDate.of(2018, 1, 20), betalingslinjer.first().tom)
        assertEquals(LocalDate.of(2018, 2, 10), betalingslinjer.last().fom)
        assertEquals(LocalDate.of(2018, 2, 19), betalingslinjer.last().tom)
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

    private val Int.A
        get() = Sykdomstidslinje.ikkeSykedager(startDato, startDato.plusDays(this.toLong() - 1), sendtSykmelding)
            .also { startDato = startDato.plusDays(this.toLong()) }
}

