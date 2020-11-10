package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.OppdragVisitor
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingslinjer.Endringskode.*
import no.nav.helse.utbetalingslinjer.Klassekode.RefusjonIkkeOpplysningspliktig
import java.time.LocalDate
import kotlin.streams.toList

internal class Utbetalingslinje internal constructor(
    internal var fom: LocalDate,
    internal var tom: LocalDate,
    internal var beløp: Int?, //TODO: arbeidsgiverbeløp || personbeløp
    internal var aktuellDagsinntekt: Int,
    internal val grad: Double,
    internal var refFagsystemId: String? = null,
    private var delytelseId: Int = 1,
    private var refDelytelseId: Int? = null,
    private var endringskode: Endringskode = NY,
    private var klassekode: Klassekode = RefusjonIkkeOpplysningspliktig,
    private var datoStatusFom: LocalDate? = null
) : Iterable<LocalDate> {

    override operator fun iterator() = object : Iterator<LocalDate> {
        private val periodeIterator = Periode(fom, tom).iterator()
        override fun hasNext() = periodeIterator.hasNext()
        override fun next() = periodeIterator.next()
    }

    internal fun accept(visitor: OppdragVisitor) {
        visitor.visitUtbetalingslinje(
            this,
            fom,
            tom,
            beløp,
            aktuellDagsinntekt,
            grad,
            delytelseId,
            refDelytelseId,
            refFagsystemId,
            endringskode,
            datoStatusFom
        )
    }

    internal fun linkTo(other: Utbetalingslinje) {
        this.delytelseId = other.delytelseId + 1
        this.refDelytelseId = other.delytelseId
    }

    internal fun datoStatusFom() = datoStatusFom
    internal fun totalbeløp() = beløp?.let { it * antallDager() } ?: 0

    internal fun dager() = fom
        .datesUntil(tom.plusDays(1))
        .filter { !it.erHelg() }
        .toList()

    private fun antallDager() = dager().size

    override fun equals(other: Any?) = other is Utbetalingslinje && this.equals(other)

    private fun equals(other: Utbetalingslinje) =
        this.fom == other.fom &&
            this.tom == other.tom &&
            this.beløp == other.beløp &&
            this.grad == other.grad

    internal fun kunTomForskjelligFra(other: Utbetalingslinje) =
        this.fom == other.fom &&
            this.beløp == other.beløp &&
            this.grad == other.grad

    override fun hashCode(): Int {
        return fom.hashCode() * 37 +
            tom.hashCode() * 17 +
            beløp.hashCode() * 41 +
            grad.hashCode() * 61 +
            endringskode.name.hashCode() * 59 +
            datoStatusFom.hashCode() * 23
    }

    internal fun ghostFrom(tidligere: Utbetalingslinje) = copyWith(UEND, tidligere)

    internal fun utvidTom(tidligere: Utbetalingslinje) = copyWith(ENDR, tidligere)
        .also {
            this.refDelytelseId = null
            this.refFagsystemId = null
        }

    private fun copyWith(linjetype: Endringskode, tidligere: Utbetalingslinje) {
        this.refFagsystemId = tidligere.refFagsystemId
        this.delytelseId = tidligere.delytelseId
        this.refDelytelseId = tidligere.refDelytelseId
        this.klassekode = tidligere.klassekode
        this.endringskode = linjetype
    }

    internal fun erForskjell() = endringskode != UEND

    internal fun deletion(datoStatusFom: LocalDate) =
        Utbetalingslinje(
            fom = fom,
            tom = tom,
            beløp = beløp,
            aktuellDagsinntekt = aktuellDagsinntekt,
            grad = grad,
            refFagsystemId = null,
            delytelseId = delytelseId,
            refDelytelseId = null,
            endringskode = ENDR,
            datoStatusFom = datoStatusFom
        )

    internal fun erOpphør() = datoStatusFom != null
}

internal enum class Endringskode {
    NY, UEND, ENDR
}

internal enum class Klassekode(internal val verdi: String) {
    RefusjonIkkeOpplysningspliktig(verdi = "SPREFAG-IOP");

    internal companion object {
        private val map = values().associateBy(Klassekode::verdi)
        fun from(verdi: String) = requireNotNull(map[verdi]) { "Støtter ikke klassekode: $verdi" }
    }
}

internal enum class Fagområde(internal val verdi: String, private val linjerStrategy: (Utbetaling) -> Oppdrag) {
    SykepengerRefusjon("SPREF", Utbetaling::arbeidsgiverOppdrag),
    Sykepenger("SP", Utbetaling::personOppdrag);

    override fun toString() = verdi

    internal fun utbetalingslinjer(utbetaling: Utbetaling): Oppdrag =
        linjerStrategy(utbetaling)

    internal companion object {
        private val map = values().associateBy(Fagområde::verdi)
        fun from(verdi: String) = requireNotNull(map[verdi]) { "Støtter ikke klassekode: $verdi" }
    }
}
