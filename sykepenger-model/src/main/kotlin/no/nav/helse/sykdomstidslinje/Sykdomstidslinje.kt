package no.nav.helse.sykdomstidslinje

import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.tournament.Dagturnering
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.AvvistDag
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.streams.toList

// Understands a specific period that probably involves illness
internal class Sykdomstidslinje internal constructor(private val dager: List<Dag>) {

    internal constructor(): this(emptyList<Dag>())

    internal val size get() = dager.size

    internal fun length() = size

    internal fun førsteDag() = dager.first().dagen

    internal fun sisteDag() = dager.last().dagen

    internal fun flatten() = dager

    operator fun get(dato: LocalDate) = dag(dato)

    private fun dag(dato: LocalDate) = dager.find { it.dagen == dato }

    internal operator fun plus(other: Sykdomstidslinje) = join(other)

    private fun join(other: Sykdomstidslinje, inneklemtDag: (LocalDate) -> Dag = ::ImplisittDag): Sykdomstidslinje {
        require(!overlapperMed(other)) { "Kan ikke koble sammen overlappende tidslinjer uten å oppgi en turneringsmetode." }
        return merge(other) { this.dag(it) ?: other.dag(it) ?: inneklemtDag(it) }
    }

    internal fun merge(other: Sykdomstidslinje, dagturnering: Dagturnering, gapDayCreator: (LocalDate) -> Dag = ::ImplisittDag): Sykdomstidslinje {
        return merge(other) {
            beste(dagturnering, dag(it), other.dag(it)) ?: gapDayCreator(it)
        }
    }

    private fun merge(other: Sykdomstidslinje, mapper: (LocalDate) -> Dag): Sykdomstidslinje {
        if (other.size == 0) return this
        if (this.size == 0) return other
        val førsteDag = this.førsteStartdato(other)
        val sisteDag = this.sisteSluttdato(other).plusDays(1)
        return Sykdomstidslinje(førsteDag.datesUntil(sisteDag).map(mapper).toList())
    }

    internal fun valider(aktivitetslogg: IAktivitetslogg): Boolean {
        val ugyldigeDager = listOf(Permisjonsdag.Søknad::class, Permisjonsdag.Aareg::class, Ubestemtdag::class)
        return dager.filter { it::class in ugyldigeDager }
            .distinctBy { it::class.simpleName }
            .onEach { aktivitetslogg.error("Sykdomstidslinjen inneholder ustøttet dag: %s", it::class.simpleName) }
            .isEmpty()
    }

    internal fun overlapperMed(other: Sykdomstidslinje) =
        when {
            this.size == 0 && other.size == 0 -> true
            this.size == 0 || other.size == 0 -> false
            else -> this.overlapp(other)
        }

    private fun overlapp(other: Sykdomstidslinje): Boolean {
        if (this.førsteDag() > other.førsteDag()) return other.overlapp(this)
        return this.sisteDag() >= other.førsteDag() && other.sisteDag() >= this.førsteDag() // Trust me!
    }

    internal fun harTilstøtende(other: Sykdomstidslinje) =
        this.sisteDag().harTilstøtende(other.førsteDag())

    internal fun subset(fom: LocalDate?, tom:LocalDate): Sykdomstidslinje? {
        if (fom == null) return kutt(tom)
        return dager
            .filter { it.dagen >= fom && it.dagen <= tom }
            .takeIf(List<*>::isNotEmpty)
            ?.let { Sykdomstidslinje(it) }
    }

    internal fun kutt(kuttDag: LocalDate): Sykdomstidslinje? {
        if (kuttDag.isBefore(førsteDag())) return null
        return Sykdomstidslinje(dager.filterNot { it.dagen.isAfter(kuttDag) })
    }

    internal fun førsteFraværsdag(): LocalDate? {
        return førsteSykedagDagEtterSisteIkkeSykedag() ?: førsteSykedag()
    }

    private fun førsteSykedagDagEtterSisteIkkeSykedag() =
        kuttEtterSisteSykedag().let { dager ->
            dager.firstOrNull { it is Arbeidsdag || it is ImplisittDag }?.let { ikkeSykedag ->
                dager.lastOrNull {
                    it.dagen.isAfter(ikkeSykedag.dagen) && erEnSykedag(it)
                }?.dagen
            }
        }

    private fun førsteSykedag() = dager.firstOrNull() { erEnSykedag(it) }?.dagen

    private fun kuttEtterSisteSykedag(): List<Dag> =
        dager.reversed().let { dager ->
            val indeksSisteSykedag = dager.indexOfFirst { erEnSykedag(it) }
            if (indeksSisteSykedag < 0) return emptyList()
            dager.subList(indeksSisteSykedag, dager.size - 1)
        }

    private fun erEnSykedag(it: Dag) =
        it is Sykedag || it is SykHelgedag || it is Egenmeldingsdag || it is KunArbeidsgiverSykedag

    internal fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.preVisitSykdomstidslinje(this)
        dager.forEach { it.accept(visitor) }
        visitor.postVisitSykdomstidslinje(this)
    }

    private fun førsteStartdato(other: Sykdomstidslinje) =
        if (this.førsteDag().isBefore(other.førsteDag())) this.førsteDag() else other.førsteDag()

    private fun sisteSluttdato(other: Sykdomstidslinje) =
        if (this.sisteDag().isAfter(other.sisteDag())) this.sisteDag() else other.sisteDag()

    override fun toString() = toShortString()

    internal fun toDetailedString(): String {
        if (dager.isEmpty()) return "<empty>"
        return dager.joinToString(separator = "\n")
    }

    internal fun toShortString(): String {
        return dager.joinToString(separator = "") {
            (if (it.dagen.dayOfWeek == DayOfWeek.MONDAY) " " else "") +
            when (it::class) {
                Sykedag.Søknad::class -> "S"
                Sykedag.Sykmelding::class -> "S"
                Arbeidsdag.Inntektsmelding::class -> "A"
                Arbeidsdag.Søknad::class -> "A"
                ImplisittDag::class -> "I"
                SykHelgedag.Sykmelding::class -> "H"
                SykHelgedag.Søknad::class -> "H"
                Egenmeldingsdag.Inntektsmelding::class -> "E"
                Egenmeldingsdag.Søknad::class -> "E"
                AvvistDag::class -> "X"
                else -> "?"
            }
        }
    }

    companion object {

        internal fun join(
            liste: List<Sykdomstidslinje>,
            inneklemtDag: (LocalDate) -> Dag = ::ImplisittDag
        ): Sykdomstidslinje = liste.reduce { result, other -> result.join(other, inneklemtDag) }

        internal fun merge(
            liste: List<Sykdomstidslinje>,
            dagturnering: Dagturnering,
            inneklemtDag: (LocalDate) -> Dag = ::ImplisittDag
        ) = liste.reduce { result, other -> result.merge(other, dagturnering, inneklemtDag) }

        internal fun sykedager(fra: LocalDate, til: LocalDate, grad: Double, factory: DagFactory): Sykdomstidslinje =
            dag(fra, til, grad, factory, ::sykedag)

        internal fun sykedager(fra: LocalDate, til: LocalDate, avskjæringsdato: LocalDate, grad: Double, factory: DagFactory): Sykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return Sykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                if (it < avskjæringsdato) kunArbeidsgiverSykedag(it, grad, factory)
                else sykedag(it, grad, factory)
            }.toList())
        }

        internal fun ikkeSykedager(fra: LocalDate, til: LocalDate, factory: DagFactory): Sykdomstidslinje =
            dag(fra, til, factory, ::ikkeSykedag)

        internal fun kunArbeidsgiverSykedager(fra: LocalDate, til: LocalDate, grad: Double, factory: DagFactory): Sykdomstidslinje =
            dag(fra, til, grad, factory, ::kunArbeidsgiverSykedag)

        internal fun egenmeldingsdager(fra: LocalDate, til: LocalDate, factory: DagFactory): Sykdomstidslinje =
            dag(fra, til, factory, ::egenmeldingsdag)

        internal fun ferie(fra: LocalDate, til: LocalDate, factory: DagFactory): Sykdomstidslinje =
            dag(fra, til, factory, ::ferie)

        internal fun ubestemtdager(fra: LocalDate, til: LocalDate, factory: DagFactory): Sykdomstidslinje =
            dag(fra, til, factory, ::ubestemtdag)

        internal fun permisjonsdager(fra: LocalDate, til: LocalDate, factory: DagFactory): Sykdomstidslinje =
            dag(fra, til, factory, ::permisjonsdag)

        internal fun studiedager(fra: LocalDate, til: LocalDate, factory: DagFactory): Sykdomstidslinje =
            dag(fra, til, factory, ::studiedag)

        internal fun utenlandsdager(fra: LocalDate, til: LocalDate, factory: DagFactory): Sykdomstidslinje =
            dag(fra, til, factory, ::utenlandsdag)

        internal fun implisitteDager(fra: LocalDate, til: LocalDate, factory: DagFactory): Sykdomstidslinje =
            dag(fra, til, factory, ::implisittDag)

        private fun dag(fra: LocalDate, til: LocalDate, factory: DagFactory, enDag: EnDag) =
            dag(fra, til, Double.NaN, factory, enDag)

        private fun dag(
            fra: LocalDate,
            til: LocalDate,
            grad: Double,
            factory: DagFactory,
            enDag: EnDag
        ): Sykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return Sykdomstidslinje(fra.datesUntil(til.plusDays(1))
                .map { enDag(it, grad, factory) }
                .toList())
        }

        internal fun sykedag(dato: LocalDate, grad: Double, factory: DagFactory): Dag =
            if (!dato.erHelg()) factory.sykedag(dato, grad) else factory.sykHelgedag(dato, grad)

        private fun kunArbeidsgiverSykedag(dato: LocalDate, grad: Double, factory: DagFactory): Dag =
            if (!dato.erHelg()) factory.kunArbeidsgiverSykedag(dato, grad) else factory.sykHelgedag(dato, grad)

        private fun utenlandsdag(dato: LocalDate, grad_ignored: Double, factory: DagFactory) =
            if (!dato.erHelg()) factory.utenlandsdag(dato) else factory.implisittDag(dato)

        private fun ikkeSykedag(dato: LocalDate, grad_ignored: Double, factory: DagFactory) =
            ikkeSykedag(dato, factory)

        private fun studiedag(dato: LocalDate, grad_ignored: Double, factory: DagFactory) =
            if (!dato.erHelg()) factory.studiedag(dato) else factory.implisittDag(dato)

        private fun permisjonsdag(dato: LocalDate, grad_ignored: Double, factory: DagFactory) =
            if (!dato.erHelg()) factory.permisjonsdag(dato) else factory.implisittDag(dato)

        internal fun ikkeSykedag(dato: LocalDate, factory: DagFactory) =  // For gap days
            if (!dato.erHelg()) factory.arbeidsdag(dato) else factory.implisittDag(dato)

        private fun ferie(dato: LocalDate, grad_ignored: Double, factory: DagFactory) =
            factory.feriedag(dato)

        private fun egenmeldingsdag(dato: LocalDate, grad_ignored: Double, factory: DagFactory) =
            egenmeldingsdag(dato, factory)

        internal fun egenmeldingsdag(dato: LocalDate, factory: DagFactory) =
            factory.egenmeldingsdag(dato)

        private fun implisittDag(dato: LocalDate, grad_ignored: Double, factory: DagFactory) =
            factory.implisittDag(dato)

        private fun ubestemtdag(dato: LocalDate, grad_ignored: Double, factory_ignored: DagFactory) =
            ubestemtdag(dato)

        internal fun ubestemtdag(dato: LocalDate) =
            Ubestemtdag(dato)

        private fun beste(dagturnering: Dagturnering, a: Dag?, b: Dag?): Dag? {
            if (a == null) return b
            if (b == null) return a
            return dagturnering.beste(a, b)
        }
    }
}

internal fun List<Sykdomstidslinje>.join(
    dagturnering: Dagturnering,
    inneklemtDag: (LocalDate) -> Dag = ::ImplisittDag
) = Sykdomstidslinje.merge(this, dagturnering, inneklemtDag)

internal fun List<Sykdomstidslinje>.join(
    inneklemtDag: (LocalDate) -> Dag = ::ImplisittDag) = Sykdomstidslinje.join(this, inneklemtDag)

internal fun List<Sykdomstidslinje>.merge(
    dagturnering: Dagturnering,
    inneklemtDag: (LocalDate) -> Dag = ::ImplisittDag
) = Sykdomstidslinje.merge(this, dagturnering, inneklemtDag)

private typealias EnDag = (LocalDate, Double, DagFactory) -> Dag
