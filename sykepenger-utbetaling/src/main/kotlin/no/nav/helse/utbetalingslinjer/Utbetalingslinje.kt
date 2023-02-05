package no.nav.helse.utbetalingslinjer

import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.erRettFør
import no.nav.helse.hendelser.til
import no.nav.helse.utbetalingslinjer.Endringskode.ENDR
import no.nav.helse.utbetalingslinjer.Endringskode.NY
import no.nav.helse.utbetalingslinjer.Endringskode.UEND
import no.nav.helse.utbetalingslinjer.Klassekode.RefusjonIkkeOpplysningspliktig

class Utbetalingslinje(
    var fom: LocalDate,
    var tom: LocalDate,
    internal var satstype: Satstype = Satstype.Daglig,
    val beløp: Int?,
    val aktuellDagsinntekt: Int?,
    val grad: Int?,
    val refFagsystemId: String? = null,
    private val delytelseId: Int = 1,
    private val refDelytelseId: Int? = null,
    private val endringskode: Endringskode = NY,
    private val klassekode: Klassekode = RefusjonIkkeOpplysningspliktig,
    private val datoStatusFom: LocalDate? = null
) : Iterable<LocalDate> {

    companion object {
        fun stønadsdager(linjer: List<Utbetalingslinje>): Int {
            return linjer
                .filterNot { it.erOpphør() }
                .flatten()
                .distinct()
                .filterNot { it.erHelg() }
                .size
        }

        fun ferdigUtbetalingslinje(
            fom: LocalDate,
            tom: LocalDate,
            satstype: Satstype,
            sats: Int,
            lønn: Int?,
            grad: Int?,
            refFagsystemId: String?,
            delytelseId: Int,
            refDelytelseId: Int?,
            endringskode: Endringskode,
            klassekode: Klassekode,
            datoStatusFom: LocalDate?
        ): Utbetalingslinje = Utbetalingslinje(
            fom = fom,
            tom = tom,
            satstype = satstype,
            beløp = sats,
            aktuellDagsinntekt = lønn,
            grad = grad,
            refFagsystemId = refFagsystemId,
            delytelseId = delytelseId,
            refDelytelseId = refDelytelseId,
            endringskode = endringskode,
            klassekode = klassekode,
            datoStatusFom = datoStatusFom
        )

        fun List<Utbetalingslinje>.kobleTil(fagsystemId: String) = map { linje ->
            linje.kopier(refFagsystemId = fagsystemId)
        }

        fun kjedeSammenLinjer(linjer: List<Utbetalingslinje>): List<Utbetalingslinje> {
            if (linjer.isEmpty()) return emptyList()
            var forrige = linjer.first()
            val result = mutableListOf(forrige)
            linjer.drop(1).forEach { linje ->
                forrige = linje.kobleTil(forrige)
                result.add(forrige)
            }
            return result
        }
    }

    private val statuskode get() = datoStatusFom?.let { "OPPH" }

    val periode get() = fom til tom

    override operator fun iterator() = periode.iterator()

    override fun toString() = "$fom til $tom $endringskode $grad ${datoStatusFom?.let { "opphører fom $it" }}"

    fun accept(visitor: UtbetalingslinjeVisitor) {
        visitor.visitUtbetalingslinje(
            this,
            fom,
            tom,
            stønadsdager(),
            totalbeløp(),
            satstype,
            beløp,
            aktuellDagsinntekt,
            grad,
            delytelseId,
            refDelytelseId,
            refFagsystemId,
            endringskode,
            datoStatusFom,
            statuskode,
            klassekode
        )
    }

    fun kobleTil(other: Utbetalingslinje) = kopier(
        endringskode = NY,
        datoStatusFom = null,
        delytelseId = other.delytelseId + 1,
        refDelytelseId = other.delytelseId,
        refFagsystemId = other.refFagsystemId ?: this.refFagsystemId
    )

    fun førsteLinje() = kopier(refFagsystemId = null)

    fun opphørslinje(datoStatusFom: LocalDate) = kopier(
        endringskode = ENDR,
        refFagsystemId = null,
        refDelytelseId = null,
        datoStatusFom = datoStatusFom
    )

    fun kopier(
        fom: LocalDate = this.fom,
        endringskode: Endringskode = this.endringskode,
        delytelseId: Int = this.delytelseId,
        refDelytelseId: Int? = this.refDelytelseId,
        refFagsystemId: String? = this.refFagsystemId,
        klassekode: Klassekode = this.klassekode,
        datoStatusFom: LocalDate? = this.datoStatusFom,
        beløp: Int? = this.beløp,
        aktuellDagsinntekt: Int? = this.aktuellDagsinntekt
    ) =
        Utbetalingslinje(
            fom = fom,
            tom = tom,
            satstype = satstype,
            beløp = beløp,
            aktuellDagsinntekt = aktuellDagsinntekt,
            grad = grad,
            refFagsystemId = refFagsystemId,
            delytelseId = delytelseId,
            refDelytelseId = refDelytelseId,
            endringskode = endringskode,
            klassekode = klassekode,
            datoStatusFom = datoStatusFom
        )


    fun datoStatusFom() = datoStatusFom
    fun totalbeløp() = satstype.totalbeløp(beløp ?: 0, stønadsdager())
    fun stønadsdager() = if (!erOpphør()) filterNot(LocalDate::erHelg).size else 0

    fun dager() = fom
        .datesUntil(tom.plusDays(1))
        .filter { !it.erHelg() }
        .toList()

    override fun equals(other: Any?) = other is Utbetalingslinje && this.equals(other)

    private fun equals(other: Utbetalingslinje) =
        this.fom == other.fom &&
                this.tom == other.tom &&
                this.beløp == other.beløp &&
                this.grad == other.grad &&
                this.datoStatusFom == other.datoStatusFom

    fun kanEndreEksisterendeLinje(other: Utbetalingslinje, sisteLinjeITidligereOppdrag: Utbetalingslinje) =
        other == sisteLinjeITidligereOppdrag &&
                this.fom == other.fom &&
                this.beløp == other.beløp &&
                this.grad == other.grad &&
                this.datoStatusFom == other.datoStatusFom

    fun skalOpphøreOgErstatte(other: Utbetalingslinje, sisteLinjeITidligereOppdrag: Utbetalingslinje) =
        other == sisteLinjeITidligereOppdrag &&
                (this.fom > other.fom)

    override fun hashCode(): Int {
        return fom.hashCode() * 37 +
                tom.hashCode() * 17 +
                beløp.hashCode() * 41 +
                grad.hashCode() * 61 +
                endringskode.name.hashCode() * 59 +
                datoStatusFom.hashCode() * 23
    }

    fun markerUendret(tidligere: Utbetalingslinje) = kopier(
        endringskode = UEND,
        delytelseId = tidligere.delytelseId,
        refDelytelseId = tidligere.refDelytelseId,
        refFagsystemId = tidligere.refFagsystemId,
        klassekode = tidligere.klassekode,
        datoStatusFom = tidligere.datoStatusFom
    )

    fun endreEksisterendeLinje(tidligere: Utbetalingslinje) = kopier(
        endringskode = ENDR,
        delytelseId = tidligere.delytelseId,
        refDelytelseId = null,
        refFagsystemId = null,
        klassekode = tidligere.klassekode,
        datoStatusFom = tidligere.datoStatusFom
    )

    fun slåSammenLinje(førsteLinjeIForrige: Utbetalingslinje) = this.let { sisteLinjeINytt: Utbetalingslinje ->
        if (sisteLinjeINytt.beløp != førsteLinjeIForrige.beløp || sisteLinjeINytt.grad != førsteLinjeIForrige.grad || !sisteLinjeINytt.tom.erRettFør(
                førsteLinjeIForrige.fom
            )
        ) null
        else Utbetalingslinje(
            fom = sisteLinjeINytt.fom,
            tom = førsteLinjeIForrige.tom,
            satstype = sisteLinjeINytt.satstype,
            beløp = sisteLinjeINytt.beløp,
            aktuellDagsinntekt = sisteLinjeINytt.aktuellDagsinntekt,
            grad = sisteLinjeINytt.grad,
            refFagsystemId = sisteLinjeINytt.refFagsystemId,
            delytelseId = sisteLinjeINytt.delytelseId,
            refDelytelseId = sisteLinjeINytt.refDelytelseId,
            endringskode = sisteLinjeINytt.endringskode,
            klassekode = sisteLinjeINytt.klassekode,
            datoStatusFom = sisteLinjeINytt.datoStatusFom
        )
    }

    fun erForskjell() = endringskode != UEND

    fun erOpphør() = datoStatusFom != null

    fun toHendelseMap() = mapOf<String, Any?>(
        "fom" to fom.toString(),
        "tom" to tom.toString(),
        "satstype" to "$satstype",
        "sats" to beløp,
        // TODO: Skal bort etter apper er migrert over til sats
        "dagsats" to beløp,
        "lønn" to aktuellDagsinntekt,
        "grad" to grad?.toDouble(), // backwards-compatibility mot andre systemer som forventer double: må gjennomgås
        "stønadsdager" to stønadsdager(),
        "totalbeløp" to totalbeløp(),
        "endringskode" to endringskode.toString(),
        "delytelseId" to delytelseId,
        "refDelytelseId" to refDelytelseId,
        "refFagsystemId" to refFagsystemId,
        "statuskode" to statuskode,
        "datoStatusFom" to datoStatusFom?.toString(),
        "klassekode" to klassekode.verdi
    )
}

interface UtbetalingslinjeVisitor {
    fun visitUtbetalingslinje(
        linje: Utbetalingslinje,
        fom: LocalDate,
        tom: LocalDate,
        stønadsdager: Int,
        totalbeløp: Int,
        satstype: Satstype,
        beløp: Int?,
        aktuellDagsinntekt: Int?,
        grad: Int?,
        delytelseId: Int,
        refDelytelseId: Int?,
        refFagsystemId: String?,
        endringskode: Endringskode,
        datoStatusFom: LocalDate?,
        statuskode: String?,
        klassekode: Klassekode
    ) {
    }
}