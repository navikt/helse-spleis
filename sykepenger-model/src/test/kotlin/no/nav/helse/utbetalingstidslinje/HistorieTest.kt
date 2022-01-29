package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.sykdomstidslinje.Dag.Companion.replace
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde.Companion.INGEN
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.reflect.KClass

internal abstract class HistorieTest {

    protected companion object {
        const val FNR = "12345678910"
        const val AKTØRID = "1234567891011"
        const val AG1 = "1234"
        const val AG2 = "2345"
    }

    private val tidligereUtbetalinger = mutableMapOf<String, Utbetalingstidslinje>()
    private val arbeidsgiverSykdomstidslinje = mutableMapOf<String, Sykdomstidslinje>()
    protected lateinit var infotrygdhistorikk: Infotrygdhistorikk

    @BeforeEach
    fun beforeEach() {
        infotrygdhistorikk = Infotrygdhistorikk()
        arbeidsgiverSykdomstidslinje.clear()
        resetSeed()
    }

    protected fun utbetaling(fom: LocalDate, tom: LocalDate, inntekt: Inntekt = 1000.daglig, grad: Prosentdel = 100.prosent, orgnr: String = AG1) =
        ArbeidsgiverUtbetalingsperiode(orgnr.toString(), fom,  tom, grad, inntekt)

    protected fun ferie(fom: LocalDate, tom: LocalDate) =
        Friperiode(fom,  tom)

    protected fun navdager(fom: LocalDate, tom: LocalDate) =
        tidslinjeOf(fom.dagerMellom(tom).NAV, startDato = fom)

    protected fun arbeidsdager(fom: LocalDate, tom: LocalDate) =
        tidslinjeOf(fom.dagerMellom(tom).ARB, startDato = fom)

    protected fun foreldetdager(fom: LocalDate, tom: LocalDate) =
        tidslinjeOf(fom.dagerMellom(tom).FOR, startDato = fom)

    protected fun avvistedager(fom: LocalDate, tom: LocalDate) =
        tidslinjeOf(fom.dagerMellom(tom).AVV, startDato = fom)

    protected fun feriedager(fom: LocalDate, tom: LocalDate) =
        tidslinjeOf(fom.dagerMellom(tom).FRI, startDato = fom)

    protected fun LocalDate.dagerMellom(tom: LocalDate) =
        ChronoUnit.DAYS.between(this, tom).toInt() + 1

    protected fun sykedager(fom: LocalDate, tom: LocalDate, grad: Prosentdel = 100.prosent, kilde: Hendelseskilde = INGEN) =
        Sykdomstidslinje.sykedager(fom, tom, grad, kilde)

    protected fun addTidligereUtbetaling(orgnr: String, utbetalingstidslinje: Utbetalingstidslinje) {
        tidligereUtbetalinger[orgnr] = tidligereUtbetalinger.getOrDefault(orgnr, Utbetalingstidslinje()) + utbetalingstidslinje
        addSykdomshistorikk(orgnr, Utbetalingstidslinje.konverter(tidligereUtbetalinger.getValue(orgnr)))
    }

    protected fun addSykdomshistorikk(orgnr: String, sykdomstidslinje: Sykdomstidslinje) {
        arbeidsgiverSykdomstidslinje[orgnr] = arbeidsgiverSykdomstidslinje.getOrDefault(orgnr, Sykdomstidslinje()).merge(sykdomstidslinje, replace)
    }

    protected fun historie(vararg perioder: no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode) {
        infotrygdhistorikk.oppdaterHistorikk(
            InfotrygdhistorikkElement.opprett(
                oppdatert = LocalDateTime.now(),
                hendelseId = UUID.randomUUID(),
                perioder = perioder.toList(),
                inntekter = emptyList(),
                arbeidskategorikoder = emptyMap(),
                ugyldigePerioder = emptyList(),
                harStatslønn = false
            )
        )
    }

    protected fun beregn(orgnr: String, periode: Periode, vararg inntektsdatoer: LocalDate, regler: ArbeidsgiverRegler = NormalArbeidstaker): Utbetalingstidslinje {
        val inntektshistorikk = Inntektshistorikk()
        inntektshistorikk.append {
            inntektsdatoer.forEach {
                addInntektsmelding(it, UUID.randomUUID(), 25000.månedlig)
            }
        }
        val sykdomstidslinje = arbeidsgiverSykdomstidslinje.getValue(orgnr)
        val builder = UtbetalingstidslinjeBuilder(
            skjæringstidspunkter = infotrygdhistorikk.skjæringstidspunkter(arbeidsgiverSykdomstidslinje.values.toList()),
            inntektPerSkjæringstidspunkt = inntektsdatoer.associateWith { Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), it, UUID.randomUUID(), 25000.månedlig) },
            arbeidsgiverRegler = regler,
            subsumsjonObserver = MaskinellJurist()
        )
        return infotrygdhistorikk.builder(orgnr, builder).also { it.build(sykdomstidslinje, periode) }.result()
    }

    protected fun skjæringstidspunkt(fom: LocalDate) = infotrygdhistorikk.skjæringstidspunkt(Periode(fom, fom), arbeidsgiverSykdomstidslinje.values.toList())

    protected fun assertSkjæringstidspunkter(vararg datoer: LocalDate) {
        assertEquals(datoer.toList(), infotrygdhistorikk.skjæringstidspunkter(arbeidsgiverSykdomstidslinje.values.toList()))
    }

    protected fun assertAlleDager(utbetalingstidslinje: Utbetalingstidslinje, periode: Periode, vararg dager: KClass<out Utbetalingstidslinje.Utbetalingsdag>) {
        utbetalingstidslinje.subset(periode).also { tidslinje ->
            assertTrue(tidslinje.all { it::class in dager }) {
                val ulikeDager = tidslinje.filter { it::class !in dager }
                "Forventet at alle dager skal være en av: ${dager.joinToString { it.simpleName ?: "UKJENT" }}.\n" +
                    ulikeDager.joinToString(prefix = "  - ", separator = "\n  - ", postfix = "\n") {
                        "${it.dato} er ${it::class.simpleName}"
                    } + "\nUtbetalingstidslinje:\n" + tidslinje.toString() + "\n"
            }
        }
    }

    protected fun assertSkjæringstidspunkt(utbetalingstidslinje: Utbetalingstidslinje, periode: Periode, forventetSkjæringstidspunkt: LocalDate?) {
        utbetalingstidslinje.subset(periode).also { tidslinje ->
            assertTrue(tidslinje.all { it.økonomi.medData { _, _, _, skjæringstidspunkt, _, _, _, _, _ ->
                skjæringstidspunkt == forventetSkjæringstidspunkt
            } }) {
                val ulikeDager = tidslinje.filter {
                    it.økonomi.medData { _, _, _, skjæringstidspunkt, _, _, _, _, _ ->
                        skjæringstidspunkt != forventetSkjæringstidspunkt
                    }
                }
                "Forventet at alle dager skal ha skjæringstidspunkt $forventetSkjæringstidspunkt.\n" +
                    ulikeDager.joinToString(prefix = "  - ", separator = "\n  - ", postfix = "\n") {
                        "${it.dato} har ${it.økonomi.medData { _, _, _, skjæringstidspunkt, _, _, _, _, _ -> skjæringstidspunkt }}"
                    } + "\nUtbetalingstidslinje:\n" + tidslinje.toString() + "\n"
            }
        }
    }
}
