package no.nav.helse.utbetalingstidslinje.test

import no.nav.helse.Testhendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.dag.Dag
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
        private val inntektsmeldingHendelse = Testhendelse(Uke(3).tirsdag.atStartOfDay(), Dag.NøkkelHendelseType.Inntektsmelding)
    }

    //Mandag
    private var startDato = LocalDate.of(2018, 1, 1)

    private val dagsats = 1200

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
        assertEquals(LocalDate.of(2018, 1, 19), betalingslinjer.first().tom)
    }

    @Test
    fun `Sykedager med inneklemt ferie`() {
        val sykdomstidslinje = 21.S + 2.S + 2.F + S //6 utbetalingsdager
        val betalingslinjer = sykdomstidslinje.utbetalingsberegning(dagsats).utbetalingslinjer

        assertEquals(3, betalingslinjer.size)
        assertEquals(LocalDate.of(2018, 1, 17), betalingslinjer[0].fom)
        assertEquals(LocalDate.of(2018, 1, 19), betalingslinjer[0].tom)

        assertEquals(LocalDate.of(2018, 1, 22), betalingslinjer[1].fom)
        assertEquals(LocalDate.of(2018, 1, 23), betalingslinjer[1].tom)

        assertEquals(LocalDate.of(2018, 1, 26), betalingslinjer[2].fom)
        assertEquals(LocalDate.of(2018, 1, 26), betalingslinjer[2].tom)
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
        val sykdomstidslinje = S + 2.F + A + S + 14.S + 3.S
        val betalingslinjer = sykdomstidslinje.utbetalingsberegning(dagsats).utbetalingslinjer

        assertEquals(1, betalingslinjer.size)
        assertEquals(LocalDate.of(2018, 1, 22), betalingslinjer.first().fom)
        assertEquals(LocalDate.of(2018, 1, 22), betalingslinjer.first().tom)
    }

    @Test
    fun `Arbeidsdag før ferie i arbeidsgiverperioden`() {
        val sykdomstidslinje = S + A + 2.F + S + 14.S + 3.S
        val betalingslinjer = sykdomstidslinje.utbetalingsberegning(dagsats).utbetalingslinjer

        assertEquals(1, betalingslinjer.size)
        assertEquals(LocalDate.of(2018, 1, 22), betalingslinjer.first().fom)
        assertEquals(LocalDate.of(2018, 1, 22), betalingslinjer.first().tom)
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
    fun `Ferie rett etter arbeidsgiverperioden teller ikke som opphold`() {
        val sykdomstidslinje = 16.S + 16.F + A + 3.S
        val betalingslinjer = sykdomstidslinje.utbetalingsberegning(dagsats).utbetalingslinjer

        assertEquals(1, betalingslinjer.size)
        assertEquals(LocalDate.of(2018, 2, 5), betalingslinjer.first().fom)
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

        assertThat(betalingslinjer).hasSize(2)
        assertEquals(LocalDate.of(2018, 1, 31), betalingslinjer.first().fom)
        assertEquals(LocalDate.of(2018, 2, 2), betalingslinjer.first().tom)

        assertEquals(LocalDate.of(2018, 2, 5), betalingslinjer.last().fom)
        assertEquals(LocalDate.of(2018, 2, 9), betalingslinjer.last().tom)
    }

    @Test
    fun `Ferie mer enn 16 dager gir ikke ny arbeidsgiverperiode`() {
        val sykdomstidslinje = 20.S + 20.F + 10.S
        val betalingslinjer = sykdomstidslinje.utbetalingsberegning(dagsats).utbetalingslinjer

        assertThat(betalingslinjer).hasSize(3)
        assertEquals(LocalDate.of(2018, 1, 17), betalingslinjer[0].fom)
        assertEquals(LocalDate.of(2018, 1, 19), betalingslinjer[0].tom)

        assertEquals(LocalDate.of(2018, 2, 12), betalingslinjer[1].fom)
        assertEquals(LocalDate.of(2018, 2, 16), betalingslinjer[1].tom)

        assertEquals(LocalDate.of(2018, 2, 19), betalingslinjer[2].fom)
        assertEquals(LocalDate.of(2018, 2, 19), betalingslinjer[2].tom)
    }

    @Test
    fun `opphold i sykedager over 16 dager etter arbeidsgiverperioden blir avvist`() {
        assertThrows<Exception> { (19.S + 40.I + 5.S).utbetalingsberegning(1200) }
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

        assertEquals(LocalDate.of(2019,1,2), maksdato)
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

    @Test
    fun `splitter utbetalingslinjer på helg`() {
        val sykdomstidslinje = 30.S
        val linjer = sykdomstidslinje.utbetalingsberegning(dagsats).utbetalingslinjer

        assertEquals(3, linjer.size)
        assertEquals(LocalDate.of(2018,1,17), linjer[0].fom)
        assertEquals(LocalDate.of(2018,1,19), linjer[0].tom)

        assertEquals(LocalDate.of(2018,1,22), linjer[1].fom)
        assertEquals(LocalDate.of(2018,1,26), linjer[1].tom)

        assertEquals(LocalDate.of(2018,1,29), linjer[2].fom)
        assertEquals(LocalDate.of(2018,1,30), linjer[2].tom)
    }

    @Test
    fun `de første 10 dagene av arbeidsgiverperioden er egenmeldingsdager`() {
        val start = startDato
        val sykdomstidslinje = 10.E + 30.S +
                Sykdomstidslinje.egenmeldingsdager(start.plusDays(15), start.plusDays(25), inntektsmeldingHendelse)
        val linjer = sykdomstidslinje.utbetalingsberegning(dagsats).utbetalingslinjer

        assertEquals(4, linjer.size)
        assertEquals(LocalDate.of(2018,1,17), linjer[0].fom)
        assertEquals(LocalDate.of(2018,1,19), linjer[0].tom)

        assertEquals(LocalDate.of(2018,1,22), linjer[1].fom)
        assertEquals(LocalDate.of(2018,1,26), linjer[1].tom)

        assertEquals(LocalDate.of(2018,1,29), linjer[2].fom)
        assertEquals(LocalDate.of(2018,2,2), linjer[2].tom)

        assertEquals(LocalDate.of(2018,2,5), linjer[3].fom)
        assertEquals(LocalDate.of(2018,2,9), linjer[3].tom)
    }

    @Test
    fun `arbeidsgiverperiode med to påfølgende sykedager i helg blir ingen utbetalingslinjer`() {
        val sykdomstidslinje = 3.A + 18.S
        val linjer = sykdomstidslinje.utbetalingsberegning(dagsats).utbetalingslinjer
        assertEquals(0, linjer.size)
    }

    @Test
    fun `arbeidsgiverperioden slutter på en søndag`() {
        val sykdomstidslinje = 3.A + 5.S + 2.F + 13.S
        val linjer = sykdomstidslinje.utbetalingsberegning(dagsats).utbetalingslinjer
        assertEquals(1, linjer.size)
        assertEquals(LocalDate.of(2018,1,22), linjer[0].fom)
        assertEquals(LocalDate.of(2018,1,23), linjer[0].tom)
    }

    @Test
    fun `når rest av ukedager gjør at maksdato går over helg, så skal helgen ikke telle med som sykedag`() {
        startDato = LocalDate.of(2019,10,11)
        val sykdomstidslinje = 4.E + 4.S + 2.E + 5.S + 1.E + S + 14.S
        val utbetalingsberegning = sykdomstidslinje.utbetalingsberegning(dagsats)
        val utbetalingslinjer = utbetalingsberegning.utbetalingslinjer
        assertEquals(2, utbetalingslinjer.size)
        assertEquals(LocalDate.of(2020,10,7), utbetalingsberegning.maksdato)

        assertEquals(LocalDate.of(2019,10,28), utbetalingslinjer[0].fom)
        assertEquals(LocalDate.of(2019,11,1), utbetalingslinjer[0].tom)
        assertEquals(LocalDate.of(2019,11,4), utbetalingslinjer[1].fom)
        assertEquals(LocalDate.of(2019,11,8), utbetalingslinjer[1].tom)
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

    private val Int.E
        get() = Sykdomstidslinje.egenmeldingsdager(startDato, startDato.plusDays(this.toLong() - 1), sendtSykmelding)
            .also { startDato = startDato.plusDays(this.toLong()) }
}

