package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Testhendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.Utbetalingslinje
import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.testhelpers.Uke
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class UtbetalingBuilderTest {

    companion object {
        private val sendtSykmelding =
            Testhendelse(Uke(3).mandag.atStartOfDay())
        private val inntektsmeldingHendelse = Testhendelse(
            Uke(3).tirsdag.atStartOfDay(),
            Dag.NøkkelHendelseType.Inntektsmelding
        )
    }

    //Mandag
    private var startDato = 1.januar

    private val inntektHistorie = InntektHistorie()

    init {
        inntektHistorie.add(1.januar.minusDays(5), 1200.00)
        inntektHistorie.add(23.januar, 1000.00)
    }

    private val fødselsnummer = "02029812345"

    private val fødselsnummer67År = "01015112345"

    @Test
    fun `to dager blir betalt av arbeidsgiver`() {
        assertEquals(0, 2.S.utbetalingslinjer(inntektHistorie, fødselsnummer).size)
    }

    @Test
    fun `en utbetalingslinje med tre dager`() {
        val betalingslinjer = (16.S + 3.S).utbetalingslinjer(inntektHistorie, fødselsnummer)

        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 17.januar, 19.januar, 1200)
    }

    @Test
    fun `en utbetalingslinje med helg`() {
        val betalingslinjer = (16.S + 6.S).utbetalingslinjer(inntektHistorie, fødselsnummer)

        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 17.januar, 22.januar, 1200)
    }

    @Test
    fun `utbetalingslinjer starter aldri med helg`() {
        val betalingslinjer = (3.A + 16.S + 6.S).utbetalingslinjer(inntektHistorie, fødselsnummer)

        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 22.januar, 25.januar, 1200)
    }

    @Test
    fun `Sykedager med inneklemte arbeidsdager`() {
        val betalingslinjer = (16.S + 7.S + 2.A + 1.S).utbetalingslinjer(inntektHistorie, fødselsnummer) //6 utbetalingsdager

        assertEquals(2, betalingslinjer.size)
        assert(betalingslinjer.first(), 17.januar, 23.januar, 1200)
        assert(betalingslinjer.last(), 26.januar, 26.januar, 1000)
    }

    @Test
    fun `Arbeidsdager i arbeidsgiverperioden`() {
        val betalingslinjer = (15.S + 2.A + 1.S + 7.S).utbetalingslinjer(inntektHistorie, fødselsnummer) //6 utbetalingsdager

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
        val betalingslinjer = (S + 2.F + 13.S + S).utbetalingslinjer(inntektHistorie, fødselsnummer)

        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 17.januar, 17.januar, 1200)
    }

    @Test
    fun `Arbeidsdag etter feire i arbeidsgiverperioden`() {
        val betalingslinjer = (S + 2.F + A + S + 14.S + 3.S).utbetalingslinjer(inntektHistorie, fødselsnummer)

        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 22.januar, 22.januar, 1200)
    }

    @Test
    fun `Arbeidsdag før ferie i arbeidsgiverperioden`() {
        val betalingslinjer = (S + A + 2.F + S + 14.S + 3.S).utbetalingslinjer(inntektHistorie, fødselsnummer)

        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 22.januar, 22.januar, 1200)
    }

    @Test
    fun `Ferie etter arbeidsgiverperioden`() {
        val betalingslinjer = (16.S + 2.F + S).utbetalingslinjer(inntektHistorie, fødselsnummer)

        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 19.januar, 19.januar, 1200)
    }

    @Test
    fun `Arbeidsdag etter ferie teller som gap`() {
        val betalingslinjer = (15.S + 2.F + A + S).utbetalingslinjer(inntektHistorie, fødselsnummer)

        assertEquals(0, betalingslinjer.size)
    }

    @Test
    fun `Ferie rett etter arbeidsgiverperioden teller ikke som opphold`() {
        val betalingslinjer = (16.S + 16.F + A + 3.S).utbetalingslinjer(inntektHistorie, fødselsnummer)

        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 5.februar, 5.februar, 1000)
    }

    @Test
    fun `Ferie i slutten av arbeidsgiverperioden teller som opphold`() {
        val betalingslinjer = (15.S + 16.F + A + 3.S).utbetalingslinjer(inntektHistorie, fødselsnummer)
        assertEquals(0, betalingslinjer.size)
    }

    @Test
    fun `Ferie og arbeid påvirker ikke initiell tilstand`() {
        val betalingslinjer = (2.F + 2.A + 16.S + 2.F).utbetalingslinjer(inntektHistorie, fødselsnummer)
        assertEquals(0, betalingslinjer.size)
    }

    @Test
    fun `Arbeidsgiverperioden resettes når det er opphold over 16 dager`() {
        val betalingslinjer = (10.S + 20.F + A + 10.S + 20.F).utbetalingslinjer(inntektHistorie, fødselsnummer)
        assertEquals(0, betalingslinjer.size)
    }

    @Test
    fun `Ferie fullfører arbeidsgiverperioden`() {
        val betalingslinjer = (10.S + 20.F + 10.S).utbetalingslinjer(inntektHistorie, fødselsnummer)

        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 31.januar, 9.februar, 1200)
    }

    @Test
    fun `Ferie mer enn 16 dager gir ikke ny arbeidsgiverperiode`() {
        val betalingslinjer = (20.S + 20.F + 10.S).utbetalingslinjer(inntektHistorie, fødselsnummer)

        assertEquals(2, betalingslinjer.size)
        assert(betalingslinjer.first(), 17.januar, 20.januar, 1200)
        assert(betalingslinjer.last(), 12.februar, 19.februar, 1200)
    }

    @Test
    fun `egenmelding sammen med sykdom oppfører seg som sykdom`() {
        val betalingslinjer = (5.E + 15.S).utbetalingslinjer(inntektHistorie, fødselsnummer)

        assertEquals(1, betalingslinjer.size)
        assert(betalingslinjer.first(), 17.januar, 20.januar, 1200)
    }

//    @Test
//    fun `beregn maksdato i et sykdomsforløp som slutter på en fredag`() {
//        val maksdato = (20.S).utbetalingslinjer(inntektHistorie, fødselsnummer).maksdato
//
//        assertEquals(sykdomstidslinje.sluttdato().plusDays(342), maksdato)
//    }

//    @Test
//    fun `beregn maksdato i et sykdomsforløp med opphold i sykdom`() {
//        val maksdato = (2.A + 20.S + 7.A + 20.S // Siste dag er 2018-02-18).utbetalingslinjer(inntektHistorie, fødselsnummer).maksdato
//
//        assertEquals(LocalDate.of(2019,1,8), maksdato)
//    }
//
//    @Test
//    fun `beregn maksdato (med rest) der den ville falt på en lørdag`() {
//        val maksdato = (351.S + 1.F + S).utbetalingslinjer(inntektHistorie, fødselsnummer).maksdato
//        assertEquals(LocalDate.of(2018, 12, 31), maksdato)
//    }
//
//    @Test
//    fun `beregn maksdato (med rest) der den ville falt på en søndag`() {
//        val maksdato = (23.S + 2.F + S).utbetalingslinjer(inntektHistorie, fødselsnummer).maksdato
//        assertEquals(LocalDate.of(2019,1,1), maksdato )
//
//    }
//
//    @Test
//    fun `maksdato forskyves av ferie etterfulgt av sykedager`() {
//        val maksdato = (21.S + 3.F + S).utbetalingslinjer(inntektHistorie, fødselsnummer).maksdato
//
//        assertEquals(LocalDate.of(2019,1,2), maksdato)
//    }
//
//    @Test
//    fun `maksdato forskyves ikke av ferie på tampen av sykdomstidslinjen`() {
//        val maksdato = (21.S + 3.F).utbetalingslinjer(inntektHistorie, fødselsnummer).maksdato
//
//        assertEquals(LocalDate.of(2018,12,28), maksdato)
//    }
//
//    @Test
//    fun `maksdato er null om vi ikke har noen utbetalingsdager`() {
//        val maksdato = (16.S).utbetalingslinjer(inntektHistorie, fødselsnummer).maksdato
//
//        assertNull(maksdato)
//    }
//

//
//    @Test
//    fun `arbeidsgiverperiode med to påfølgende sykedager i helg blir ingen utbetalingslinjer`() {
//        val linjer = (3.A + 18.S).utbetalingslinjer(inntektHistorie, fødselsnummer).utbetalingslinjer
//        assertEquals(0, linjer.size)
//    }
//
//    @Test
//    fun `arbeidsgiverperioden slutter på en søndag`() {
//        val linjer = (3.A + 5.S + 2.F + 13.S).utbetalingslinjer(inntektHistorie, fødselsnummer).utbetalingslinjer
//        assertEquals(1, linjer.size)
//        assertEquals(LocalDate.of(2018,1,22), linjer[0].fom)
//        assertEquals(LocalDate.of(2018,1,23), linjer[0].tom)
//    }
//
//    @Test
//    fun `når rest av ukedager gjør at maksdato går over helg, så skal helgen ikke telle med som sykedag`() {
//        startDato = LocalDate.of(2019,10,11)
//        val (4.E + 4.S + 2.E + 5.S + 1.E + S + 14.S) = sykdomstidslinje.utbetalingslinjer(inntektHistorie, fødselsnummer)
//        val utbetalingslinjer = utbetalingsberegning.utbetalingslinjer
//        assertEquals(2, utbetalingslinjer.size)
//        assertEquals(LocalDate.of(2020,10,7), utbetalingsberegning.maksdato)
//
//        assertEquals(LocalDate.of(2019,10,28), utbetalingslinjer[0].fom)
//        assertEquals(LocalDate.of(2019,11,1), utbetalingslinjer[0].tom)
//        assertEquals(LocalDate.of(2019,11,4), utbetalingslinjer[1].fom)
//        assertEquals(LocalDate.of(2019,11,8), utbetalingslinjer[1].tom)
//    }
//
//    @Test
//    fun `når sykepengeperioden går over maksdato, så skal utbetaling stoppe ved maksdato`() {
//        val beregning = (368.S).utbetalingslinjer(inntektHistorie, fødselsnummer)
//        assertEquals(LocalDate.of(2018, 12, 28), beregning.utbetalingslinjer.last().tom)
//        assertEquals(LocalDate.of(2018, 12, 28), beregning.maksdato)
//    }
//
//    @Test
//    fun `når personen fyller 67 blir antall gjenværende dager 60`() {
//        val beregning = (16.S + 90.S).utbetalingslinjer(inntektHistorie, fødselsnummer67År)
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
//    @Test
//    fun `ta hensyn til en andre arbeidsgiverperiode, ferieopphold`() {
//        val sisteUtbetaling = (16.S + 4.S + 16.F + A + 16.S).utbetalingslinjer(inntektHistorie, fødselsnummer).utbetalingslinjer.last()
//        assertEquals(LocalDate.of(2018, 1, 19), sisteUtbetaling.tom)
//    }
//
//    @Test
//    fun `ta hensyn til en andre arbeidsgiverperiode, arbeidsdageropphold`() {
//        val sisteUtbetaling = (16.S + 4.S + 16.A + 16.S).utbetalingslinjer(inntektHistorie, fødselsnummer).utbetalingslinjer.last()
//        assertEquals(LocalDate.of(2018, 1, 19), sisteUtbetaling.tom)
//    }
//
//    @Test
//    fun `ta hensyn til en andre arbeidsgiverperiode, arbeidsdageropphold der sykedager går over helg`() {
//        val sisteUtbetaling = (16.S + 6.S + 16.A + 16.S).utbetalingslinjer(inntektHistorie, fødselsnummer).utbetalingslinjer.last()
//        assertEquals(LocalDate.of(2018, 1, 22), sisteUtbetaling.tom)
//    }

    private val S
        get() = Sykdomstidslinje.sykedag(startDato,
            sendtSykmelding
        ).also {
            startDato = startDato.plusDays(1)
        }

    private val A
        get() = Sykdomstidslinje.ikkeSykedag(startDato,
            sendtSykmelding
        ).also {
            startDato = startDato.plusDays(1)
        }


    private val Int.S
        get() = Sykdomstidslinje.sykedager(startDato, startDato.plusDays(this.toLong() - 1),
            sendtSykmelding
        )
            .also { startDato = startDato.plusDays(this.toLong()) }

    private val Int.F
        get() = Sykdomstidslinje.ferie(startDato, startDato.plusDays(this.toLong() - 1),
            sendtSykmelding
        )
            .also { startDato = startDato.plusDays(this.toLong()) }

    private val Int.A
        get() = Sykdomstidslinje.ikkeSykedager(startDato, startDato.plusDays(this.toLong() - 1),
            sendtSykmelding
        )
            .also { startDato = startDato.plusDays(this.toLong()) }

    private val Int.I
        get() = Sykdomstidslinje.implisittdager(startDato, startDato.plusDays(this.toLong() - 1),
            sendtSykmelding
        )
            .also { startDato = startDato.plusDays(this.toLong()) }

    private val Int.E
        get() = Sykdomstidslinje.egenmeldingsdager(startDato, startDato.plusDays(this.toLong() - 1),
            sendtSykmelding
        )
            .also { startDato = startDato.plusDays(this.toLong()) }

    val Int.januar
        get() = LocalDate.of(2018, 1, this)

    val Int.februar
        get() = LocalDate.of(2018, 2, this)

    val Int.mai
        get() = LocalDate.of(2018, 5, this)

    val Int.desember
        get() = LocalDate.of(2018, 12, this)

}

