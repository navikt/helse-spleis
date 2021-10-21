package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.til
import no.nav.helse.person.OppdragVisitor
import no.nav.helse.serde.api.UtbetalingerDTO
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingslinjer.Endringskode.*
import no.nav.helse.utbetalingslinjer.Klassekode.RefusjonFeriepengerIkkeOpplysningspliktig
import no.nav.helse.utbetalingslinjer.Klassekode.RefusjonIkkeOpplysningspliktig
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate

internal sealed class Oppdragslinje(protected var fom: LocalDate,
                                    protected var tom: LocalDate,
                                    protected var satstype: Satstype = Satstype.DAG,
                                    protected var beløp: Int?, //TODO: arbeidsgiverbeløp || personbeløp
                                    protected var aktuellDagsinntekt: Int?,
                                    protected val grad: Double?,
                                    protected var refFagsystemId: String? = null,
                                    protected var delytelseId: Int = 1,
                                    protected var refDelytelseId: Int? = null,
                                    protected var endringskode: Endringskode = NY,
                                    protected var klassekode: Klassekode = RefusjonIkkeOpplysningspliktig): Iterable<LocalDate> {
    internal val periode get() = fom til tom
    fun beløp() = beløp
    fun endreBeløp(nyttBeløp: Int) { beløp = nyttBeløp }
    fun endreAktuellDagsinntekt(nyAktuellDagsinntekt: Int) { aktuellDagsinntekt = nyAktuellDagsinntekt }
    override operator fun iterator() = periode.iterator()
    fun flyttStart(nyFom: LocalDate) { this.fom = nyFom}
    open fun totalbeløp() = beløp?.let { if (klassekode == RefusjonFeriepengerIkkeOpplysningspliktig) it else it * stønadsdager() } ?: 0
    internal fun dager() = fom.datesUntil(tom.plusDays(1)).filter { !it.erHelg() }.toList()
    internal fun kobleTil(other: Oppdragslinje) {
        this.delytelseId = other.delytelseId + 1
        this.refDelytelseId = other.delytelseId
    }
    internal fun opphørslinje(datoStatusFom: LocalDate) = Opphørslinje(fom = fom, tom = tom, beløp = beløp, aktuellDagsinntekt = aktuellDagsinntekt, grad = grad, refFagsystemId = null, delytelseId = delytelseId, refDelytelseId = null, endringskode = ENDR, datoStatusFom = datoStatusFom, satstype = satstype, klassekode = klassekode)
    internal fun erForskjell() = endringskode != UEND
    internal open fun toMap() = mutableMapOf<String, Any?>(
        "fom" to fom.toString(),
        "tom" to tom.toString(),
        "satstype" to satstype.name,
        "sats" to beløp,
        // TODO: Skal bort etter apper er migrert over til sats
        "dagsats" to beløp,
        "lønn" to aktuellDagsinntekt,
        "grad" to grad,
        "stønadsdager" to stønadsdager(),
        "totalbeløp" to totalbeløp(),
        "endringskode" to endringskode.toString(),
        "delytelseId" to delytelseId,
        "refDelytelseId" to refDelytelseId,
        "refFagsystemId" to refFagsystemId,
        "statuskode" to null,
        "datoStatusFom" to null,
        "klassekode" to klassekode.verdi
    )
    internal fun kanEndreEksisterendeLinje(other: Oppdragslinje, sisteLinjeITidligereOppdrag: Oppdragslinje) =
        other == sisteLinjeITidligereOppdrag &&
            other !is Opphørslinje &&
            this.fom == other.fom &&
            this.beløp == other.beløp &&
            this.grad == other.grad

    internal fun skalOpphøreOgErstatte(other: Oppdragslinje, sisteLinjeITidligereOppdrag: Oppdragslinje) =
        other == sisteLinjeITidligereOppdrag &&
            (this.fom > other.fom)
    internal fun markerUendret(tidligere: Oppdragslinje) = copyWith(UEND, tidligere)
    internal fun endreEksisterendeLinje(tidligere: Oppdragslinje) = copyWith(ENDR, tidligere)
        .also {
            this.refDelytelseId = null
            this.refFagsystemId = null
        }
    private fun copyWith(linjetype: Endringskode, tidligere: Oppdragslinje) {
        this.refFagsystemId = tidligere.refFagsystemId
        this.delytelseId = tidligere.delytelseId
        this.refDelytelseId = tidligere.refDelytelseId
        this.klassekode = tidligere.klassekode
        this.endringskode = linjetype
    }

    abstract fun førstedato(): LocalDate
    fun sistedato(): LocalDate = tom
    abstract fun stønadsdager(): Int
    abstract fun accept(visitor: OppdragVisitor)
    fun tilDTO() = UtbetalingerDTO.UtbetalingslinjeDTO(fom = fom, tom = tom, dagsats = beløp!!, grad = grad!!)
    override fun toString() = "$fom til $tom $endringskode $grad"
    fun dagSatser() = dager().map { it to beløp }
    fun manglerBeløp() = beløp == null
    fun harSammeBeløp(fagområde: Fagområde, dag: Utbetalingstidslinje.Utbetalingsdag.NavDag) = beløp == fagområde.beløp(dag.økonomi)
    fun harSammeGrad(grad: Double) = this.grad == grad
    fun harSammeGrad(other: Oppdragslinje) = this.grad == other.grad
    fun manglerReferanse() = this.refFagsystemId == null
    fun refererTil(fagsystemId: String?) {
        this.refFagsystemId = fagsystemId
    }

    companion object {
        fun lagOppdragslinje(fom: LocalDate,
                   tom: LocalDate,
                   satstype: Satstype = Satstype.DAG,
                   beløp: Int?,
                   aktuellDagsinntekt: Int?,
                   grad: Double?,
                   refFagsystemId: String? = null,
                   delytelseId: Int = 1,
                   refDelytelseId: Int? = null,
                   endringskode: Endringskode = NY,
                   klassekode: Klassekode = RefusjonIkkeOpplysningspliktig,
                   datoStatusFom: LocalDate? = null) =
            when(datoStatusFom) {
                null -> Utbetalingslinje(fom, tom, satstype, beløp, aktuellDagsinntekt, grad, refFagsystemId, delytelseId, refDelytelseId, endringskode, klassekode)
                else -> Opphørslinje(fom, tom, satstype, beløp, aktuellDagsinntekt, grad, refFagsystemId, delytelseId, refDelytelseId, endringskode, klassekode, datoStatusFom)
            }
    }
}

internal class Utbetalingslinje internal constructor(
    fom: LocalDate,
    tom: LocalDate,
    satstype: Satstype,
    beløp: Int?,
    aktuellDagsinntekt: Int?,
    grad: Double?,
    refFagsystemId: String?,
    delytelseId: Int,
    refDelytelseId: Int?,
    endringskode: Endringskode,
    klassekode: Klassekode) : Oppdragslinje(fom, tom, satstype, beløp, aktuellDagsinntekt, grad, refFagsystemId, delytelseId, refDelytelseId, endringskode, klassekode) {
    override fun førstedato() = fom
    override fun accept(visitor: OppdragVisitor) {
        visitor.visitUtbetalingslinje(
            this,
            fom,
            tom,
            satstype,
            beløp,
            aktuellDagsinntekt,
            grad,
            delytelseId,
            refDelytelseId,
            refFagsystemId,
            endringskode,
            null,
            klassekode
        )
    }
    override fun stønadsdager() = filterNot(LocalDate::erHelg).size
    override fun equals(other: Any?) = other is Utbetalingslinje && other !is Opphørslinje && this.equals(other)
    private fun equals(other: Utbetalingslinje) =
        this.fom == other.fom &&
            this.tom == other.tom &&
            this.beløp == other.beløp &&
            this.grad == other.grad
    override fun hashCode(): Int {
        return fom.hashCode() * 37 +
            tom.hashCode() * 17 +
            beløp.hashCode() * 41 +
            grad.hashCode() * 61 +
            endringskode.name.hashCode() * 59
    }

}

internal class Opphørslinje internal constructor(
    fom: LocalDate,
    tom: LocalDate,
    satstype: Satstype = Satstype.DAG,
    beløp: Int?, //TODO: arbeidsgiverbeløp || personbeløp
    aktuellDagsinntekt: Int?,
    grad: Double?,
    refFagsystemId: String? = null,
    delytelseId: Int = 1,
    refDelytelseId: Int? = null,
    endringskode: Endringskode = NY,
    klassekode: Klassekode = RefusjonIkkeOpplysningspliktig,
    private var datoStatusFom: LocalDate
) : Oppdragslinje(fom, tom, satstype, beløp, aktuellDagsinntekt, grad, refFagsystemId, delytelseId, refDelytelseId, endringskode, klassekode) {

    override fun toString() = "${super.toString()} opphører $datoStatusFom"
    override fun førstedato(): LocalDate = datoStatusFom
    fun datoStatusFom() = datoStatusFom
    override fun stønadsdager() = 0
    override fun totalbeløp() = 0

    override fun equals(other: Any?) = other is Opphørslinje && this.equals(other)

    private fun equals(other: Opphørslinje) =
        this.fom == other.fom &&
            this.tom == other.tom &&
            this.beløp == other.beløp &&
            this.grad == other.grad &&
            this.datoStatusFom == other.datoStatusFom

    override fun hashCode(): Int {
        return fom.hashCode() * 37 +
            tom.hashCode() * 17 +
            beløp.hashCode() * 41 +
            grad.hashCode() * 61 +
            endringskode.name.hashCode() * 59 +
            datoStatusFom.hashCode() * 23
    }

    override fun toMap() = super.toMap().also {
        it.replace("statuskode", "OPPH")
        it.replace("datoStatusFom", datoStatusFom.toString())
    }

    override fun accept(visitor: OppdragVisitor) {
        visitor.visitUtbetalingslinje(
            this,
            fom,
            tom,
            satstype,
            beløp,
            aktuellDagsinntekt,
            grad,
            delytelseId,
            refDelytelseId,
            refFagsystemId,
            endringskode,
            datoStatusFom,
            klassekode
        )
    }
}
