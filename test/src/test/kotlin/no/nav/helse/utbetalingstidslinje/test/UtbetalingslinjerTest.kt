package no.nav.helse.utbetalingstidslinje.test

import no.nav.helse.Testhendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.Uke
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class UtbetalingslinjerTest {

    companion object {
        private val sendtSykmelding = Testhendelse(Uke(3).mandag.atStartOfDay())
    }

    //Mandag
    private var startDato = LocalDate.of(2018, 1, 1)

    private val dagsats = 1200.toBigDecimal()

    @Test
    fun `to dager blir betalt av arbeidsgiver`() {
        val tosykedager = S + S

        val betalingslinjer = tosykedager.utbetalingsberegning(dagsats).utbetalingslinjer

        assertEquals(0, betalingslinjer.size)
    }

    @Test
    fun `tjue dager gir 4 dager betalt av NAV`() {
        val tosykedager = 20.S

        val betalingslinjer = tosykedager.utbetalingsberegning(dagsats).utbetalingslinjer

        assertEquals(1, betalingslinjer.size)
        assertEquals(LocalDate.of(2018, 1, 17), betalingslinjer.first().fom)
        assertEquals(LocalDate.of(2018, 1, 20), betalingslinjer.first().tom)
    }

    @Test
    fun `Sykedager med inneklemt ferie`() {
        val sykdomstidslinje = 21.S + 2.S + 2.F + S //6 utbetalingsdager
        val betalingslinjer = sykdomstidslinje.utbetalingsberegning(dagsats).utbetalingslinjer

        assertEquals(2, betalingslinjer.size)
        assertEquals(LocalDate.of(2018, 1, 17), betalingslinjer.first().fom)
        assertEquals(LocalDate.of(2018, 1, 23), betalingslinjer.first().tom)

        assertEquals(LocalDate.of(2018, 1, 26), betalingslinjer.last().fom)
        assertEquals(LocalDate.of(2018, 1, 26), betalingslinjer.last().tom)
    }

    @Test
    fun `Ferie i arbeidsgiverperiode`() {
        val sykdomstidslinje = S + 2.F + 13.S + S
        val betalingslinjer = sykdomstidslinje.utbetalingsberegning(dagsats).utbetalingslinjer

        assertEquals(1, betalingslinjer.size)
        assertEquals(LocalDate.of(2018, 1, 17), betalingslinjer.first().fom)
        assertEquals(LocalDate.of(2018, 1, 17), betalingslinjer.first().tom)
    }

    @Test
    fun `Arbeidsdag etter feire i arbeidsgiverperioden`() {
        val sykdomstidslinje = S + 2.F + A + S + 14.S + S
        val betalingslinjer = sykdomstidslinje.utbetalingsberegning(dagsats).utbetalingslinjer

        assertEquals(1, betalingslinjer.size)
        assertEquals(LocalDate.of(2018, 1, 20), betalingslinjer.first().fom)
        assertEquals(LocalDate.of(2018, 1, 20), betalingslinjer.first().tom)
    }

    @Test
    fun `Arbeidsdag før ferie i arbeidsgiverperioden`() {
        val sykdomstidslinje = S + A + 2.F + S + 14.S + S
        val betalingslinjer = sykdomstidslinje.utbetalingsberegning(dagsats).utbetalingslinjer

        assertEquals(1, betalingslinjer.size)
        assertEquals(LocalDate.of(2018, 1, 20), betalingslinjer.first().fom)
        assertEquals(LocalDate.of(2018, 1, 20), betalingslinjer.first().tom)
    }

    @Test
    fun `Ferie etter arbeidsgiverperioden`() {
        val sykdomstidslinje = 14.S + 2.S + 2.F + S
        val betalingslinjer = sykdomstidslinje.utbetalingsberegning(dagsats).utbetalingslinjer

        assertEquals(1, betalingslinjer.size)
        assertEquals(LocalDate.of(2018, 1, 19), betalingslinjer.first().fom)
        assertEquals(LocalDate.of(2018, 1, 19), betalingslinjer.first().tom)
    }

    @Test
    fun `Arbeidsdag etter ferie teller som gap`() {
        val sykdomstidslinje = 14.S + S + 2.F + A + S
        val betalingslinjer = sykdomstidslinje.utbetalingsberegning(dagsats).utbetalingslinjer

        assertEquals(0, betalingslinjer.size)

    }

    @Test
    fun `Ferie rett etter arbeidsgiverperioden vacation teller ikke som opphold`() {
        val sykdomstidslinje = 16.S + 16.F + A + 3.S
        val betalingslinjer = sykdomstidslinje.utbetalingsberegning(dagsats).utbetalingslinjer

        assertEquals(1, betalingslinjer.size)
        assertEquals(LocalDate.of(2018, 2, 3), betalingslinjer.first().fom)
        assertEquals(LocalDate.of(2018, 2, 5), betalingslinjer.first().tom)

    }

    @Test
    fun `Ferie i slutten av arbeidsgiverperioden teller som opphold`() {
        val sykdomstidslinje = 15.S + 16.F + A + 3.S
        val betalingslinjer = sykdomstidslinje.utbetalingsberegning(dagsats).utbetalingslinjer

        assertEquals(0, betalingslinjer.size)
    }

    @Test
    fun `Ferie og arbeid påvirker ikke initiell tilsand`() {
        val sykdomstidslinje = 2.F + 2.A + 16.S + 2.F
        val betalingslinjer = sykdomstidslinje.utbetalingsberegning(dagsats).utbetalingslinjer

        assertThat(betalingslinjer).isEmpty()
    }

    @Test
    fun `20 feriedager med påfølgende arbeidsdag resetter arbeidsgiverperioden`() {
        val sykdomstidslinje = 10.S + 20.F + A + 10.S + 20.F
        val betalingslinjer = sykdomstidslinje.utbetalingsberegning(dagsats).utbetalingslinjer

        assertThat(betalingslinjer).isEmpty()
    }

    @Test
    fun `Ferie fullfører arbeidsgiverperioden`() {
        val sykdomstidslinje = 10.S + 20.F + 10.S
        val betalingslinjer = sykdomstidslinje.utbetalingsberegning(dagsats).utbetalingslinjer

        assertThat(betalingslinjer).hasSize(1)
        assertEquals(LocalDate.of(2018, 1, 31), betalingslinjer.first().fom)
        assertEquals(LocalDate.of(2018, 2, 9), betalingslinjer.first().tom)
    }

    @Test
    fun `Ferie mer enn 16 dager gir ikke ny arbeidsgiverperiode for betalingslinje 2`() {
        val sykdomstidslinje = 20.S + 20.F + 10.S
        val betalingslinjer = sykdomstidslinje.utbetalingsberegning(dagsats).utbetalingslinjer

        assertThat(betalingslinjer).hasSize(2)
        assertEquals(LocalDate.of(2018, 1, 17), betalingslinjer.first().fom)
        assertEquals(LocalDate.of(2018, 1, 20), betalingslinjer.first().tom)
        assertEquals(LocalDate.of(2018, 2, 10), betalingslinjer.last().fom)
        assertEquals(LocalDate.of(2018, 2, 19), betalingslinjer.last().tom)
    }

    @Test
    fun `opphold i sykedager over 16 dager etter arbeidsgiverperioden blir avvist`() {
        assertThrows<Exception> { (19.S + 40.I + 5.S).utbetalingsberegning(1200.toBigDecimal()) }
    }

    @Test
    fun `beregn maksdato i et sykdomsforløp som slutter på en fredag`() {
        val sykdomstidslinje = 20.S
        val maksdato = sykdomstidslinje.utbetalingsberegning(dagsats).maksdato

        assertEquals(sykdomstidslinje.sluttdato().plusDays(342), maksdato)
    }

    @Test
    fun `beregn maksdato i et sykdomsforløp med opphold i sykdom`() {
        val sykdomstidslinje = 2.A + 20.S + 7.A + 20.S // Siste dag er 2018-02-18
        val maksdato = sykdomstidslinje.utbetalingsberegning(dagsats).maksdato

        assertEquals(LocalDate.of(2019,1,8), maksdato)
    }

    @Test
    fun `beregn maksdato (med rest) der den ville falt på en lørdag`() {
        val sykdomstidslinje = 351.S + 1.F + S
        val maksdato = sykdomstidslinje.utbetalingsberegning(dagsats).maksdato
        assertEquals(LocalDate.of(2018, 12, 31), maksdato)
    }

    @Test
    fun `beregn maksdato (med rest) der den ville falt på en søndag`() {
        val sykdomstidslinje = 23.S + 2.F + S
        val maksdato = sykdomstidslinje.utbetalingsberegning(dagsats).maksdato
        assertEquals(LocalDate.of(2019,1,1), maksdato )

    }

    @Test
    fun `maksdato forskyves av ferie etterfulgt av sykedager`() {
        val sykdomstidslinje = 21.S + 3.F + S
        val maksdato = sykdomstidslinje.utbetalingsberegning(dagsats).maksdato

        assertEquals(LocalDate.of(2018,12,31), maksdato)
    }

    @Test
    fun `maksdato forskyves ikke av ferie på tampen av sykdomstidslinjen`() {
        val sykdomstidslinje = 21.S + 3.F
        val maksdato = sykdomstidslinje.utbetalingsberegning(dagsats).maksdato

        assertEquals(LocalDate.of(2018,12,28), maksdato)
    }

    @Test
    fun `maksdato er null om vi ikke har noen utbetalingsdager`() {
        val sykdomstidslinje = 16.S
        val maksdato = sykdomstidslinje.utbetalingsberegning(dagsats).maksdato

        assertNull(maksdato)
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

    private val Int.I
        get() = Sykdomstidslinje.implisittdager(startDato, startDato.plusDays(this.toLong() - 1), sendtSykmelding)
            .also { startDato = startDato.plusDays(this.toLong()) }
}

