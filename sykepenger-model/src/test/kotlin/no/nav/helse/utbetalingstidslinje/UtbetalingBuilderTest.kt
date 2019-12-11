package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Testhendelse
import no.nav.helse.sykdomstidslinje.ArbeidsgiverSykdomstidslinje
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.SakSykdomstidslinje
import no.nav.helse.sykdomstidslinje.Utbetalingslinje
import no.nav.helse.testhelpers.Uke
import no.nav.helse.utbetalingstidslinje.AlderReglerTest.Companion.UNG_PERSON_FNR
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class UtbetalingBuilderTest {

    companion object {
        private val sendtSykmelding =
            Testhendelse(Uke(3).mandag.atStartOfDay())
    }

    //Mandag
    private var startDato = 1.januar

    private val inntektHistorie = InntektHistorie()

    init {
        inntektHistorie.add(1.januar.minusDays(5), 1200.00)
        inntektHistorie.add(23.januar, 1000.00)
    }

    @Test
    fun `to dager blir betalt av arbeidsgiver`() {
        assertEquals(0, 2.S.tidslinje().size)
    }

    @Test
    fun `en utbetalingslinje med tre dager`() {
        val betalingslinjer = (16.S + 3.S).tidslinje()

        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 17.januar, 19.januar, 1200)
    }

    @Test
    fun `en utbetalingslinje med helg`() {
        val betalingslinjer = (16.S + 6.S).tidslinje()

        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 17.januar, 22.januar, 1200)
    }

    @Test
    fun `utbetalingslinjer starter aldri med helg`() {
        val betalingslinjer = (3.A + 16.S + 6.S).tidslinje()

        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 22.januar, 25.januar, 1200)
    }

    @Test
    fun `Sykedager med inneklemte arbeidsdager`() {
        val betalingslinjer = (16.S + 7.S + 2.A + 1.S).tidslinje() //6 utbetalingsdager

        assertEquals(2, betalingslinjer.size)
        assert(betalingslinjer.first(), 17.januar, 23.januar, 1200)
        assert(betalingslinjer.last(), 26.januar, 26.januar, 1000)
    }

    @Test
    fun `Arbeidsdager i arbeidsgiverperioden`() {
        val betalingslinjer = (15.S + 2.A + 1.S + 7.S).tidslinje() //6 utbetalingsdager

        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 19.januar, 25.januar, 1200)
    }

    private fun assert(betalingslinje: Utbetalingslinje, fom: LocalDate, tom: LocalDate, dagsats: Int) {
        assertEquals(fom, betalingslinje.fom)
        assertEquals(tom, betalingslinje.tom)
        assertEquals(dagsats, betalingslinje.dagsats)
    }

    @Test
    fun `Ferie i arbeidsgiverperiode`() {
        val betalingslinjer = (S + 2.F + 13.S + S).tidslinje()

        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 17.januar, 17.januar, 1200)
    }

    @Test
    fun `Arbeidsdag etter feire i arbeidsgiverperioden`() {
        val betalingslinjer = (S + 2.F + A + S + 14.S + 3.S).tidslinje()

        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 22.januar, 22.januar, 1200)
    }

    @Test
    fun `Arbeidsdag før ferie i arbeidsgiverperioden`() {
        val betalingslinjer = (S + A + 2.F + S + 14.S + 3.S).tidslinje()

        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 22.januar, 22.januar, 1200)
    }

    @Test
    fun `Ferie etter arbeidsgiverperioden`() {
        val betalingslinjer = (16.S + 2.F + S).tidslinje()

        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 19.januar, 19.januar, 1200)
    }

    @Test
    fun `Arbeidsdag etter ferie teller som gap`() {
        val betalingslinjer = (15.S + 2.F + A + S).tidslinje()

        assertEquals(0, betalingslinjer.size)
    }

    @Test
    fun `Ferie rett etter arbeidsgiverperioden teller ikke som opphold`() {
        val betalingslinjer = (16.S + 16.F + A + 3.S).tidslinje()

        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 5.februar, 5.februar, 1000)
    }

    @Test
    fun `Ferie i slutten av arbeidsgiverperioden teller som opphold`() {
        val betalingslinjer = (15.S + 16.F + A + 3.S).tidslinje()
        assertEquals(0, betalingslinjer.size)
    }

    @Test
    fun `Ferie og arbeid påvirker ikke initiell tilstand`() {
        val betalingslinjer = (2.F + 2.A + 16.S + 2.F).tidslinje()
        assertEquals(0, betalingslinjer.size)
    }

    @Test
    fun `Arbeidsgiverperioden resettes når det er opphold over 16 dager`() {
        val betalingslinjer = (10.S + 20.F + A + 10.S + 20.F).tidslinje()
        assertEquals(0, betalingslinjer.size)
    }

    @Test
    fun `Ferie fullfører arbeidsgiverperioden`() {
        val betalingslinjer = (10.S + 20.F + 10.S).tidslinje()

        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 31.januar, 9.februar, 1200)
    }

    @Test
    fun `Ferie mer enn 16 dager gir ikke ny arbeidsgiverperiode`() {
        val betalingslinjer = (20.S + 20.F + 10.S).tidslinje()

        assertEquals(2, betalingslinjer.size)
        assert(betalingslinjer.first(), 17.januar, 20.januar, 1200)
        assert(betalingslinjer.last(), 12.februar, 19.februar, 1200)
    }

    @Test
    fun `egenmelding sammen med sykdom oppfører seg som sykdom`() {
        val betalingslinjer = (5.E + 15.S).tidslinje()

        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 17.januar, 20.januar, 1200)
    }

//    @Test
//    fun `beregn maksdato i et sykdomsforløp som slutter på en fredag`() {
//        val maksdato = (20.S).tidslinje(.maksdato
//
//        assertEquals(sykdomstidslinje.sluttdato().plusDays(342), maksdato)
//    }

//    @Test
//    fun `beregn maksdato i et sykdomsforløp med opphold i sykdom`() {
//        val maksdato = (2.A + 20.S + 7.A + 20.S // Siste dag er 2018-02-18).tidslinje(.maksdato
//
//        assertEquals(LocalDate.of(2019,1,8), maksdato)
//    }
//
//    @Test
//    fun `beregn maksdato (med rest) der den ville falt på en lørdag`() {
//        val maksdato = (351.S + 1.F + S).tidslinje(.maksdato
//        assertEquals(LocalDate.of(2018, 12, 31), maksdato)
//    }
//
//    @Test
//    fun `beregn maksdato (med rest) der den ville falt på en søndag`() {
//        val maksdato = (23.S + 2.F + S).tidslinje(.maksdato
//        assertEquals(LocalDate.of(2019,1,1), maksdato )
//
//    }
//
//    @Test
//    fun `maksdato forskyves av ferie etterfulgt av sykedager`() {
//        val maksdato = (21.S + 3.F + S).tidslinje(.maksdato
//
//        assertEquals(LocalDate.of(2019,1,2), maksdato)
//    }
//
//    @Test
//    fun `maksdato forskyves ikke av ferie på tampen av sykdomstidslinjen`() {
//        val maksdato = (21.S + 3.F).tidslinje(.maksdato
//
//        assertEquals(LocalDate.of(2018,12,28), maksdato)
//    }
//
//    @Test
//    fun `maksdato er null om vi ikke har noen utbetalingsdager`() {
//        val maksdato = (16.S).tidslinje(.maksdato
//
//        assertNull(maksdato)
//    }
//


    @Test
    fun `arbeidsgiverperiode med to påfølgende sykedager i helg blir ingen utbetalingslinjer`() {
        val linjer = (3.A + 18.S).tidslinje()
        assertEquals(0, linjer.size)
    }

    @Test
    fun `arbeidsgiverperiode med tre påfølgende sykedager i helg`() {
        val linjer = (3.A + 19.S).tidslinje()
        assertEquals(1, linjer.size)
        assert(linjer.first(), 22.januar, 22.januar, 1200)
    }

    @Test
    fun `arbeidsgiverperiode med tre påfølgende sykedager i helg blir ingen utbetalingslinjer`() {
        val linjer = (2.A + 19.S).tidslinje()
        assertEquals(1, linjer.size)
        assert(linjer.first(), 19.januar, 21.januar, 1200)
    }

    @Test
    fun `arbeidsgiverperiode til fredag med sykedager over helg gir ingen betalingslinjer`() {
        val linjer = (16.S + 3.A + 2.S).tidslinje()
        assertEquals(0, linjer.size)
    }

    @Test
    fun `arbeidsgiverperioden slutter på en søndag`() {
        val linjer = (3.A + 5.S + 2.F + 13.S).tidslinje()
        assertEquals(1, linjer.size)
        assert(linjer.first(), 22.januar, 23.januar, 1200)
    }
//
//    @Test
//    fun `når rest av ukedager gjør at maksdato går over helg, så skal helgen ikke telle med som sykedag`() {
//        startDato = LocalDate.of(2019,10,11)
//        val (4.E + 4.S + 2.E + 5.S + 1.E + S + 14.S) = sykdomstidslinje.tidslinje(
//        val utbetalingslinjer = utbetalingsberegning.utbetalingslinjer
//        assertEquals(2, utbetalingslinjer.size)
//        assertEquals(LocalDate.of(2020,10,7), utbetalingsberegning.maksdato)
//
//        assertEquals(LocalDate.of(2019,10,28), utbetalingslinjer[0].fom)
//        assertEquals(LocalDate.of(2019,11,1), utbetalingslinjer[0].tom)
//        assertEquals(LocalDate.of(2019,11,4), utbetalingslinjer[1].fom)
//        assertEquals(LocalDate.of(2019,11,8), utbetalingslinjer[1].tom)
//    }

    //    @Test
//    fun `når sykepengeperioden går over maksdato, så skal utbetaling stoppe ved maksdato`() {
//        val beregning = (368.S).tidslinje(
//        assertEquals(LocalDate.of(2018, 12, 28), beregning.utbetalingslinjer.last().tom)
//        assertEquals(LocalDate.of(2018, 12, 28), beregning.maksdato)
//    }
//
//    @Test
//    fun `når personen fyller 67 blir antall gjenværende dager 60`() {
//        val beregning = (16.S + 90.S).utbetalingslinjer(inntektHistorie67År)
//        assertEquals(LocalDate.of(2018, 4, 10), beregning.utbetalingslinjer.last().tom)
//    }
//
//    @Test
//    fun `når personen fyller 67 og 248 dager er brukt opp`() {
//        val beregning = (400.S).utbetalingslinjer(inntektHistorie, "01125112345")
//        assertEquals(LocalDate.of(2018, 12, 28), beregning.utbetalingslinjer.last().tom)
//    }
//
//    @Test
//    fun `når personen fyller 70 skal det ikke utbetales sykepenger`() {
//        val beregning = (400.S).utbetalingslinjer(inntektHistorie, "01024812345")
//        assertEquals(LocalDate.of(2018, 1, 31), beregning.utbetalingslinjer.last().tom)
//    }
//
    @Test
    fun `ta hensyn til en andre arbeidsgiverperiode, ferieopphold`() {
        val betalingslinjer = (16.S + 4.S + 16.F + A + 16.S).tidslinje()
        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 17.januar, 20.januar, 1200)
    }

    @Test
    fun `ta hensyn til en andre arbeidsgiverperiode, arbeidsdageropphold`() {
        val betalingslinjer = (16.S + 4.S + 16.A + 16.S).tidslinje()
        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 17.januar, 20.januar, 1200)
    }

    @Test
    fun `ta hensyn til en andre arbeidsgiverperiode, arbeidsdageropphold der sykedager går over helg`() {
        val betalingslinjer = (16.S + 6.S + 16.A + 16.S).tidslinje()
        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 17.januar, 22.januar, 1200)
    }

    @Test
    fun `permisjon i en arbeidsgiverperiode telles som ferie`() {
        val betalingslinjer = (4.S + 4.P + 9.S).tidslinje()
        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 17.januar, 17.januar, 1200)
    }

    @Test
    fun `permisjon med påfølgende arbeidsdag teller som opphold i sykeperioden`() {
        val betalingslinjer = (4.S + 4.P + 1.A + 16.S).tidslinje()
        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 22.januar, 25.januar, 1200)
    }

    @Test
    fun `permisjon direkte etter arbeidsgiverperioden teller ikke som opphold`() {
        val betalingslinjer = (16.S + 10.P + 15.A + 3.S).tidslinje()
        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 12.februar, 13.februar, 1000)
    }

    @Test
    fun `resetter arbeidsgiverperioden etter 16 arbeidsdager`() {
        val betalingslinjer = (15.S + 16.A + 14.S).tidslinje()
        assertEquals(0, betalingslinjer.size)
    }

    @Test
    fun `utlands-, ubestemt- og utdanningsdager teller som opphold`() {
        val betalingslinjer = (15.S + 10.U + 4.EDU + 3.UT + 14.S).tidslinje()
        assertEquals(0, betalingslinjer.size)
    }

    @Test
    fun `intitialiserer arbeidsgiverperioden med 5 dager`() {
        val betalingslinjer = (15.S).tidslinje(arbeidsgiverperiodeSeed = 5)
        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 12.januar, 15.januar, 1200)
    }

    @Test
    fun `intitialiserer arbeidsgiverperioden med 16 dager`() {
        val betalingslinjer = (15.S).tidslinje(arbeidsgiverperiodeSeed = 16)
        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 1.januar, 15.januar, 1200)
    }

    @Test
    fun `oppdeltArbeidsgiverutbetalingstidslinje har ingen sykedager betalt av nav`() {
        val betalingslinjer = (50.S).tidslinje(1.januar, 10.januar)
        assertEquals(0, betalingslinjer.size)
    }

    @Test
    fun `oppdelt tidslinje i arbeidsgiverperioden`() {
        val betalingslinjer = (50.S).tidslinje(10.januar, 20.januar)
        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 17.januar, 20.januar, 1200)
    }

    @Test
    fun `oppdelt tidslinje etter arbeidsgiverperioden`() {
        val betalingslinjer = (50.S).tidslinje(21.januar, 30.januar)
        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 22.januar, 30.januar, 1200)
    }

    @Test
    fun `oppdelt tidslinje blir bare helg`() {
        val betalingslinjer = (21.S).tidslinje(20.januar, 21.januar)
        assertEquals(0, betalingslinjer.size)
    }

    private val S
        get() = ConcreteSykdomstidslinje.sykedag(
            startDato,
            sendtSykmelding
        ).also {
            startDato = startDato.plusDays(1)
        }

    private val A
        get() = ConcreteSykdomstidslinje.ikkeSykedag(
            startDato,
            sendtSykmelding
        ).also {
            startDato = startDato.plusDays(1)
        }


    private val Int.S
        get() = ConcreteSykdomstidslinje.sykedager(
            startDato, startDato.plusDays(this.toLong() - 1),
            sendtSykmelding
        )
            .also { startDato = startDato.plusDays(this.toLong()) }

    private val Int.F
        get() = ConcreteSykdomstidslinje.ferie(
            startDato, startDato.plusDays(this.toLong() - 1),
            sendtSykmelding
        )
            .also { startDato = startDato.plusDays(this.toLong()) }

    private val Int.A
        get() = ConcreteSykdomstidslinje.ikkeSykedager(
            startDato, startDato.plusDays(this.toLong() - 1),
            sendtSykmelding
        )
            .also { startDato = startDato.plusDays(this.toLong()) }

    private val Int.I
        get() = ConcreteSykdomstidslinje.implisittdager(
            startDato, startDato.plusDays(this.toLong() - 1),
            sendtSykmelding
        )
            .also { startDato = startDato.plusDays(this.toLong()) }

    private val Int.E
        get() = ConcreteSykdomstidslinje.egenmeldingsdager(
            startDato, startDato.plusDays(this.toLong() - 1),
            sendtSykmelding
        )
            .also { startDato = startDato.plusDays(this.toLong()) }

    private val Int.P
        get() = ConcreteSykdomstidslinje.permisjonsdager(
            startDato, startDato.plusDays(this.toLong() - 1),
            sendtSykmelding
        )
            .also { startDato = startDato.plusDays(this.toLong()) }

    private val Int.EDU
        get() = ConcreteSykdomstidslinje.studiedager(
            startDato, startDato.plusDays(this.toLong() - 1),
            sendtSykmelding
        )
            .also { startDato = startDato.plusDays(this.toLong()) }

    private val Int.UT
        get() = ConcreteSykdomstidslinje.utenlandsdager(
            startDato, startDato.plusDays(this.toLong() - 1),
            sendtSykmelding
        )
            .also { startDato = startDato.plusDays(this.toLong()) }

    private val Int.U
        get() = ConcreteSykdomstidslinje.ubestemtdager(
            startDato, startDato.plusDays(this.toLong() - 1),
            sendtSykmelding
        )
            .also { startDato = startDato.plusDays(this.toLong()) }

    private fun ConcreteSykdomstidslinje.tidslinje(
        førsteDag: LocalDate = this.førsteDag(),
        sisteDag: LocalDate = this.sisteDag(),
        arbeidsgiverperiodeSeed: Int = 0
    ): List<Utbetalingslinje> {
        val arbeidsgiverSykdomstidslinje =
            ArbeidsgiverSykdomstidslinje(listOf(this), NormalArbeidstaker, inntektHistorie, arbeidsgiverperiodeSeed)
        return SakSykdomstidslinje(
            listOf(arbeidsgiverSykdomstidslinje),
            AlderRegler(UNG_PERSON_FNR, this.førsteDag(), this.sisteDag()),
            arbeidsgiverSykdomstidslinje,
            førsteDag,
            sisteDag
        ).utbetalingslinjer()
    }

    val Int.januar
        get() = LocalDate.of(2018, 1, this)

    val Int.februar
        get() = LocalDate.of(2018, 2, this)

    val Int.mai
        get() = LocalDate.of(2018, 5, this)

    val Int.desember
        get() = LocalDate.of(2018, 12, this)

}

