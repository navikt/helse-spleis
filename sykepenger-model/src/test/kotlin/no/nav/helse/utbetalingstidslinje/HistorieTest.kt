package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.sykdomstidslinje.Dag.Companion.replace
import no.nav.helse.sykdomstidslinje.SykdomshistorikkHendelse.Hendelseskilde
import no.nav.helse.sykdomstidslinje.SykdomshistorikkHendelse.Hendelseskilde.Companion.INGEN
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.ARB
import no.nav.helse.testhelpers.AVV
import no.nav.helse.testhelpers.FOR
import no.nav.helse.testhelpers.FRI
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.resetSeed
import no.nav.helse.testhelpers.somVilkårsgrunnlagHistorikk
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
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
        ArbeidsgiverUtbetalingsperiode(orgnr, fom,  tom, grad, inntekt)

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
                arbeidskategorikoder = emptyMap()
            )
        )
    }

    protected fun beregn(orgnr: String, vararg inntektsdatoer: LocalDate, regler: ArbeidsgiverRegler = NormalArbeidstaker): Utbetalingstidslinje {
        val sykdomstidslinje = arbeidsgiverSykdomstidslinje.getValue(orgnr)
        val inntekter = Inntekter(
            hendelse = Aktivitetslogg(),
            organisasjonsnummer = orgnr,
            vilkårsgrunnlagHistorikk = inntektsdatoer.associateWith { Inntektsmelding(it, UUID.randomUUID(), 25000.månedlig, LocalDateTime.now()) }.somVilkårsgrunnlagHistorikk(orgnr),
            regler = regler,
            subsumsjonslogg = Subsumsjonslogg.NullObserver
        )
        val utbetalingstidslinjebuilder = UtbetalingstidslinjeBuilder(inntekter, sykdomstidslinje.periode()!!)
        return infotrygdhistorikk.buildUtbetalingstidslinje(orgnr, sykdomstidslinje, utbetalingstidslinjebuilder, Subsumsjonslogg.NullObserver).let { utbetalingstidslinjebuilder.result() }
    }

    protected fun skjæringstidspunkt(fom: LocalDate) = infotrygdhistorikk.skjæringstidspunkt(arbeidsgiverSykdomstidslinje.values.toList()).beregnSkjæringstidspunkt(fom.somPeriode(), null)

    protected fun assertAlleDager(utbetalingstidslinje: Utbetalingstidslinje, periode: Periode, vararg dager: KClass<out Utbetalingsdag>) {
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
}
