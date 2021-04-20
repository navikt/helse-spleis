package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.*
import no.nav.helse.person.*
import no.nav.helse.person.Periodetype.*
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Utbetalingsperiode
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

internal class HistoriePeriodetypeTest {
    private companion object {
        private const val aktørId = "aktørId"
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AG1 = "AG1"
    }
    private lateinit var person: Person
    private lateinit var arbeidsgiver: Arbeidsgiver

    @BeforeEach
    fun setup() {
        person = Person(aktørId, UNG_PERSON_FNR_2018)
        arbeidsgiver = Arbeidsgiver(person, AG1)
        person.håndter(Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = aktørId,
            orgnummer = AG1,
            sykeperioder = listOf(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent)),
            opprettet = 1.februar.atStartOfDay()
        ))
    }

    private fun utbetaling(fom: LocalDate, tom: LocalDate, inntekt: Inntekt = 1000.daglig, grad: Prosentdel = 100.prosent, orgnr: String = AG1) =
        Utbetalingsperiode(orgnr, fom til tom, grad, inntekt)

    private fun ferie(fom: LocalDate, tom: LocalDate) =
        Friperiode(fom til tom)

    private fun historie(vararg perioder: Infotrygdperiode) {
        person.håndter(Utbetalingshistorikk(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = AG1,
            vedtaksperiodeId = UUID.randomUUID().toString(),
            arbeidskategorikoder = emptyMap(),
            harStatslønn = false,
            perioder = perioder.toList(),
            inntektshistorikk = emptyList(),
            ugyldigePerioder = emptyList(),
            besvart = LocalDateTime.now()
        ))
    }

    private fun navdager(fom: LocalDate, tom: LocalDate) =
        tidslinjeOf(fom.dagerMellom(tom).NAV, startDato = fom)

    private fun arbeidsdager(fom: LocalDate, tom: LocalDate) =
        tidslinjeOf(fom.dagerMellom(tom).ARB, startDato = fom)

    private fun feriedager(fom: LocalDate, tom: LocalDate) =
        tidslinjeOf(fom.dagerMellom(tom).FRI, startDato = fom)

    private fun LocalDate.dagerMellom(tom: LocalDate) =
        ChronoUnit.DAYS.between(this, tom).toInt() + 1

    private fun sykedager(fom: LocalDate, tom: LocalDate, grad: Prosentdel = 100.prosent, kilde: SykdomstidslinjeHendelse.Hendelseskilde = SykdomstidslinjeHendelse.Hendelseskilde.INGEN) =
        Sykdomstidslinje.sykedager(fom, tom, grad, kilde)

    private fun addTidligereUtbetalinger(utbetalingstidslinje: Utbetalingstidslinje) {
        arbeidsgiver.oppdaterSykdom(object : SykdomstidslinjeHendelse(UUID.randomUUID(), LocalDateTime.now()) {
            override fun organisasjonsnummer() = AG1
            override fun aktørId() = aktørId
            override fun fødselsnummer() = UNG_PERSON_FNR_2018
            override fun sykdomstidslinje() = Utbetalingstidslinje.konverter(utbetalingstidslinje)
            override fun valider(periode: Periode): IAktivitetslogg { return this }
            override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {}
        })

        MaksimumUtbetaling(listOf(utbetalingstidslinje), Aktivitetslogg(), 1.januar).betal()
        arbeidsgiver.lagreUtbetalingstidslinjeberegning(AG1, utbetalingstidslinje)
        val utbetaling = arbeidsgiver.lagUtbetaling(Aktivitetslogg(), UNG_PERSON_FNR_2018, LocalDate.MAX, 0, 0, utbetalingstidslinje.periode(), null)
        utbetaling.håndter(
            Utbetalingsgodkjenning(
                meldingsreferanseId = UUID.randomUUID(),
                aktørId = aktørId,
                fødselsnummer = UNG_PERSON_FNR_2018,
                organisasjonsnummer = AG1,
                utbetalingId = utbetaling.id,
                vedtaksperiodeId = UUID.randomUUID().toString(),
                saksbehandler = "Z999999",
                saksbehandlerEpost = "z999999@nav.no",
                utbetalingGodkjent = true,
                godkjenttidspunkt = LocalDateTime.now(),
                automatiskBehandling = true
        ))
    }

    private val Utbetaling.id: UUID get() {
        var _id = UUID.randomUUID()
        accept(object : UtbetalingVisitor {
            override fun preVisitUtbetaling(
                utbetaling: Utbetaling,
                id: UUID,
                beregningId: UUID,
                type: Utbetaling.Utbetalingtype,
                tilstand: Utbetaling.Tilstand,
                tidsstempel: LocalDateTime,
                oppdatert: LocalDateTime,
                arbeidsgiverNettoBeløp: Int,
                personNettoBeløp: Int,
                maksdato: LocalDate,
                forbrukteSykedager: Int?,
                gjenståendeSykedager: Int?
            ) {
                _id = id
            }
        })
        return _id
    }

    private fun addSykdomshistorikk(sykdomstidslinje: Sykdomstidslinje) {
        arbeidsgiver.oppdaterSykdom(object : SykdomstidslinjeHendelse(UUID.randomUUID(), LocalDateTime.now()) {
            override fun organisasjonsnummer() = AG1
            override fun aktørId() = aktørId
            override fun fødselsnummer() = UNG_PERSON_FNR_2018
            override fun sykdomstidslinje() = sykdomstidslinje
            override fun valider(periode: Periode): IAktivitetslogg { return this }
            override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {}
        })
    }

    @Test
    fun `infotrygd - gap - spleis - gap - infotrygd - spleis - spleis`() {
        historie(utbetaling(1.januar, 31.januar), utbetaling(9.april, 30.april))
        addTidligereUtbetalinger(navdager(1.mars, 30.mars))
        addTidligereUtbetalinger(navdager(1.mai, 31.mai))
        addSykdomshistorikk(sykedager(1.juni, 30.juni))

        assertFalse(arbeidsgiver.forlengerInfotrygd(1.mars til 31.mars))
        assertEquals(FØRSTEGANGSBEHANDLING, arbeidsgiver.periodetype(1.mars til 31.mars))

        assertTrue(arbeidsgiver.forlengerInfotrygd(1.mai til 31.mai))
        assertEquals(OVERGANG_FRA_IT, arbeidsgiver.periodetype(1.mai til 31.mai))

        assertTrue(arbeidsgiver.forlengerInfotrygd(1.juni til 30.juni))
        assertEquals(INFOTRYGDFORLENGELSE, arbeidsgiver.periodetype(1.juni til 30.juni))
    }

    @Test
    fun `infotrygd - spleis - spleis`() {
        historie(utbetaling(1.januar, 31.januar))
        addTidligereUtbetalinger(navdager(1.februar, 28.februar))
        addSykdomshistorikk(sykedager(1.mars, 31.mars))

        assertTrue(arbeidsgiver.forlengerInfotrygd(1.februar til 28.februar))
        assertEquals(OVERGANG_FRA_IT, arbeidsgiver.periodetype(1.februar til 28.februar))
        assertEquals(INFOTRYGDFORLENGELSE, arbeidsgiver.periodetype(1.mars til 31.mars))
    }

    @Test
    fun `infotrygd - spleis - infotrygd - spleis`() {
        historie(utbetaling(1.januar, 31.januar), utbetaling(1.mars, 31.mars))
        addTidligereUtbetalinger(navdager(1.februar, 28.februar))
        addSykdomshistorikk(sykedager(1.april, 30.april))

        assertTrue(arbeidsgiver.forlengerInfotrygd(1.februar til 28.februar))
        assertEquals(OVERGANG_FRA_IT, arbeidsgiver.periodetype(1.februar til 28.februar))

        assertTrue(arbeidsgiver.forlengerInfotrygd(1.april til 30.april))
        assertEquals(INFOTRYGDFORLENGELSE, arbeidsgiver.periodetype(1.april til 30.april))
    }

    @Test
    fun `spleis - infotrygd - spleis`() {
        historie(utbetaling(1.februar, 28.februar))
        addTidligereUtbetalinger(navdager(1.januar, 31.januar))
        addSykdomshistorikk(sykedager(1.mars, 31.mars))
        assertEquals(FØRSTEGANGSBEHANDLING, arbeidsgiver.periodetype(1.januar til 31.januar))
        assertFalse(arbeidsgiver.forlengerInfotrygd(1.mars til 31.mars))
        assertEquals(FORLENGELSE, arbeidsgiver.periodetype(1.mars til 31.mars))
    }

    @Test
    fun `infotrygd - spleis`() {
        historie(utbetaling(1.januar, 31.januar))
        addSykdomshistorikk(sykedager(1.februar, 28.februar))
        assertTrue(arbeidsgiver.forlengerInfotrygd(1.februar til 28.februar))
        assertEquals(OVERGANG_FRA_IT, arbeidsgiver.periodetype(1.februar til 28.februar))
    }

    @Test
    fun `infotrygd - gap - spleis`() {
        historie(utbetaling(1.februar, 27.februar))
        addSykdomshistorikk(sykedager(1.mars, 31.mars))
        assertEquals(FØRSTEGANGSBEHANDLING, arbeidsgiver.periodetype(1.mars til 31.mars))
        assertFalse(arbeidsgiver.forlengerInfotrygd(1.mars til 31.mars))
    }

    @Test
    fun `spleis - gap - infotrygd - spleis`() {
        historie(utbetaling(1.februar, 28.februar))
        addTidligereUtbetalinger(navdager(1.januar, 30.januar))
        addSykdomshistorikk(sykedager(1.mars, 31.mars))
        assertEquals(OVERGANG_FRA_IT, arbeidsgiver.periodetype(1.mars til 31.mars))
    }

    @Test
    fun `ubetalt spleis - ubetalt spleis`() {
        addSykdomshistorikk(sykedager(1.februar, 28.februar))
        addSykdomshistorikk(sykedager(1.mars, 31.mars))
        assertEquals(FØRSTEGANGSBEHANDLING, arbeidsgiver.periodetype(1.januar til 28.februar))
        assertFalse(arbeidsgiver.erForlengelse(1.januar til 28.februar))
        assertFalse(arbeidsgiver.forlengerInfotrygd(1.mars til 31.mars))
        assertEquals(FØRSTEGANGSBEHANDLING, arbeidsgiver.periodetype(1.mars til 31.mars))
        assertTrue(arbeidsgiver.erForlengelse(1.mars til 31.mars))
    }

    @Test
    fun `spleis - ubetalt spleis - ubetalt spleis`() {
        addTidligereUtbetalinger(navdager(1.januar, 31.januar))
        addSykdomshistorikk(sykedager(1.februar, 28.februar))
        addSykdomshistorikk(sykedager(1.mars, 31.mars))
        assertEquals(FØRSTEGANGSBEHANDLING, arbeidsgiver.periodetype(1.januar til 31.januar))
        assertFalse(arbeidsgiver.forlengerInfotrygd(1.februar til 28.februar))
        assertEquals(FORLENGELSE, arbeidsgiver.periodetype(1.februar til 28.februar))
        assertFalse(arbeidsgiver.forlengerInfotrygd(1.mars til 31.mars))
        assertEquals(FORLENGELSE, arbeidsgiver.periodetype(1.mars til 31.mars))
    }
}
