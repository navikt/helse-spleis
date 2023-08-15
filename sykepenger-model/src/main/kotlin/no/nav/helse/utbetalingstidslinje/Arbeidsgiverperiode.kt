package no.nav.helse.utbetalingstidslinje

import java.time.DayOfWeek
import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.erRettFør
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.intersect
import no.nav.helse.hendelser.Periode.Companion.periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.inntekt.Refusjonsopplysning
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode.Companion.Utbetalingssituasjon.IKKE_UTBETALT
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode.Companion.Utbetalingssituasjon.INGENTING_Å_UTBETALE
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode.Companion.Utbetalingssituasjon.UTBETALT
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavDag

internal class Arbeidsgiverperiode private constructor(private val perioder: List<Periode>, førsteUtbetalingsdag: LocalDate?) : Iterable<LocalDate>, Comparable<LocalDate> {
    constructor(perioder: List<Periode>) : this(perioder, null)

    private val kjenteDager = mutableListOf<Periode>()
    private val utbetalingsdager = mutableListOf<Periode>()
    private val oppholdsdager = mutableListOf<Periode>()

    init {
        check(perioder.isNotEmpty() || førsteUtbetalingsdag != null) {
            "Enten må arbeidsgiverperioden være oppgitt eller så må første utbetalingsdag være oppgitt"
        }
        førsteUtbetalingsdag?.also { utbetalingsdag(it) }
    }

    private val arbeidsgiverperioden get() = perioder.first().start til perioder.last().endInclusive
    private val førsteKjente get() = listOfNotNull(perioder.firstOrNull()?.start, utbetalingsdager.firstOrNull()?.start, kjenteDager.firstOrNull()?.start).minOf { it }
    private val sisteKjente get() = listOfNotNull(perioder.lastOrNull()?.endInclusive, utbetalingsdager.lastOrNull()?.endInclusive, kjenteDager.lastOrNull()?.endInclusive).maxOf { it }
    private val innflytelseperioden get() = førsteKjente til sisteKjente

    internal fun fiktiv() = perioder.isEmpty()

    internal fun periode(sisteDag: LocalDate) = førsteKjente til sisteDag

    internal fun kjentDag(dagen: LocalDate) {
        kjenteDager.add(dagen)
    }

    override fun compareTo(other: LocalDate) =
        førsteKjente.compareTo(other)

    operator fun contains(dato: LocalDate) =
        dato in innflytelseperioden

    operator fun contains(periode: Periode) =
        innflytelseperioden.overlapperMed(periode)

    internal fun forventerInntekt(
        periode: Periode,
        sykdomstidslinje: Sykdomstidslinje,
        subsumsjonObserver: SubsumsjonObserver?
    ): Boolean {
        if (!dekkesAvArbeidsgiver(periode)) return erFørsteUtbetalingsdagFørEllerLik(periode)
        subsumsjonObserver?.`§ 8-17 ledd 1 bokstav a - arbeidsgiversøknad`(this, sykdomstidslinje.subsumsjonsformat())
        return false
    }

    internal fun dekkesAvArbeidsgiver(periode: Periode): Boolean {
        if (fiktiv()) return false
        val arbeidsgiversAnsvar = dagerSomarbeidsgiverUtbetaler() ?: return false
        return (periode.overlapperMed(arbeidsgiversAnsvar) && arbeidsgiversAnsvar.slutterEtter(periode.endInclusive))
    }

    private fun dagerSomarbeidsgiverUtbetaler(): Periode? {
        val heleInklHelg = arbeidsgiverperioden.justerForHelg()
        val utbetalingsperiode = utbetalingsdager.periode() ?: return heleInklHelg
        return heleInklHelg.trimDagerFør(utbetalingsperiode)
    }

    internal fun hørerTil(periode: Periode, sisteKjente: LocalDate = this.sisteKjente) =
        periode.overlapperMed(førsteKjente til sisteKjente)

    internal fun sammenlign(other: List<Periode>): Boolean {
        if (fiktiv()) return true
        val thisSiste = this.perioder.last().endInclusive
        val otherSiste = other.lastOrNull()?.endInclusive?.coerceAtMost(this.sisteKjente) ?: return false
        return otherSiste == thisSiste || (thisSiste.erHelg() && otherSiste.erRettFør(thisSiste)) || (otherSiste.erHelg() && thisSiste.erRettFør(otherSiste))
    }

    internal fun erFørsteUtbetalingsdagFørEllerLik(periode: Periode): Boolean {
        // forventer inntekt dersom vi overlapper med en utbetalingsperiode…
        if (utbetalingsdager.any { periode.overlapperMed(it) }) return true
        // …eller dersom det ikke har vært gap siden forrige utbetaling
        val forrigeUtbetaling = utbetalingsdager.lastOrNull { other -> other.endInclusive < periode.endInclusive } ?: return false
        return oppholdsdager.none { it.overlapperMed(forrigeUtbetaling.endInclusive til periode.endInclusive) }
    }

    override fun equals(other: Any?) = other is Arbeidsgiverperiode && other.førsteKjente == this.førsteKjente
    override fun hashCode() = førsteKjente.hashCode()

    override fun iterator(): Iterator<LocalDate> {
        return object : Iterator<LocalDate> {
            private val periodeIterators = perioder.map { it.iterator() }.iterator()
            private var current: Iterator<LocalDate>? = null

            override fun hasNext(): Boolean {
                val iterator = current
                if (iterator != null && iterator.hasNext()) return true
                if (!periodeIterators.hasNext()) return false
                current = periodeIterators.next()
                return true
            }

            override fun next(): LocalDate {
                return current?.next() ?: throw NoSuchElementException()
            }
        }
    }

    internal fun utbetalingsdag(dato: LocalDate) = apply {
        if (!dato.erHelg()) this.utbetalingsdager.add(dato)
        kjentDag(dato)
    }

    internal fun oppholdsdag(dato: LocalDate) = apply {
        this.oppholdsdager.add(dato)
        kjentDag(dato)
    }

    internal fun kopierMed(other: Arbeidsgiverperiode): Arbeidsgiverperiode {
        this.kjenteDager.addAll(other.kjenteDager)
        this.utbetalingsdager.addAll(other.utbetalingsdager)
        this.oppholdsdager.addAll(other.oppholdsdager)
        return this
    }

    internal fun utbetalingssituasjon(perioder: List<Periode>, utbetalingstidslinje: Utbetalingstidslinje?): Utbetalingssituasjon {
        val overlapp = perioder.intersect(utbetalingsdager).flatten()
        if (overlapp.isEmpty()) return INGENTING_Å_UTBETALE
        if (utbetalingstidslinje == null) return IKKE_UTBETALT
        if (overlapp.all { utbetalingstidslinje.navDagMedBeløp(it) }) return UTBETALT
        return IKKE_UTBETALT
    }

    private fun Utbetalingstidslinje.navDagMedBeløp(dag: LocalDate) = get(dag).let {
        it is NavDag && it.økonomi.harBeløp()
    }

    internal fun klinLik(other: Arbeidsgiverperiode?): Boolean {
        if (other == null) return false
        if (this.toSet().containsAll(other.toSet())) return true
        return false
    }

    internal companion object {
        internal enum class Utbetalingssituasjon {
            UTBETALT,
            IKKE_UTBETALT,
            INGENTING_Å_UTBETALE
        }

        internal fun fiktiv(førsteUtbetalingsdag: LocalDate) = Arbeidsgiverperiode(emptyList(), førsteUtbetalingsdag)

        internal fun forventerInntekt(
            arbeidsgiverperiode: Arbeidsgiverperiode?,
            periode: Periode,
            sykdomstidslinje: Sykdomstidslinje,
            subsumsjonObserver: SubsumsjonObserver?
        ) =
            arbeidsgiverperiode?.forventerInntekt(periode, sykdomstidslinje, subsumsjonObserver) ?: false

        internal fun List<Arbeidsgiverperiode>.finn(periode: Periode) = lastOrNull { arbeidsgiverperiode ->
            periode in arbeidsgiverperiode
        }

        private fun Periode.justerForHelg() = when (endInclusive.dayOfWeek) {
            DayOfWeek.SATURDAY -> start til endInclusive.plusDays(1)
            DayOfWeek.FRIDAY -> start til endInclusive.plusDays(2)
            else -> this
        }

        private fun MutableList<Periode>.add(dagen: LocalDate) {
            if (isNotEmpty() && last().endInclusive.plusDays(1) == dagen) {
                this[size - 1] = last().oppdaterTom(dagen)
            } else {
                add(dagen.somPeriode())
            }
        }

        internal fun harNødvendigeRefusjonsopplysninger(skjæringstidspunkt: LocalDate, periode: Periode, refusjonsopplysninger: Refusjonsopplysning.Refusjonsopplysninger, arbeidsgiverperiode: Arbeidsgiverperiode, hendelse: IAktivitetslogg, organisasjonsnummer: String): Boolean {
            val utbetalingsdager = periode.filter { dag -> arbeidsgiverperiode.utbetalingsdager.any { utbetalingsperiode -> dag in utbetalingsperiode }}
            val førsteUtbetalingsdag = utbetalingsdager.firstOrNull() ?: return true
            val sisteOppholdsdagFørUtbetalingsdager = arbeidsgiverperiode.oppholdsdager.lastOrNull { it.endInclusive < førsteUtbetalingsdag }?.endInclusive ?: arbeidsgiverperiode.firstOrNull()?.forrigeDag
            return refusjonsopplysninger.harNødvendigRefusjonsopplysninger(skjæringstidspunkt, utbetalingsdager, sisteOppholdsdagFørUtbetalingsdager, hendelse, organisasjonsnummer)
        }
    }
}
