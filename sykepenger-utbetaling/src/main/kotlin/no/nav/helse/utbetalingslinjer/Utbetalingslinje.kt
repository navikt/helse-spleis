package no.nav.helse.utbetalingslinjer

import java.time.DayOfWeek
import java.time.LocalDate
import no.nav.helse.dto.EndringskodeDto
import no.nav.helse.dto.KlassekodeDto
import no.nav.helse.dto.SatstypeDto
import no.nav.helse.dto.deserialisering.UtbetalingslinjeInnDto
import no.nav.helse.dto.serialisering.UtbetalingslinjeUtDto
import no.nav.helse.erHelg
import no.nav.helse.erRettFør
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.til
import no.nav.helse.utbetalingslinjer.Endringskode.ENDR
import no.nav.helse.utbetalingslinjer.Endringskode.NY
import no.nav.helse.utbetalingslinjer.Endringskode.UEND
import no.nav.helse.utbetalingslinjer.Klassekode.RefusjonIkkeOpplysningspliktig

class Utbetalingslinje(
    val fom: LocalDate,
    val tom: LocalDate,
    internal val satstype: Satstype = Satstype.Daglig,
    val beløp: Int?,
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

        fun List<Utbetalingslinje>.kobleTil(fagsystemId: String) = map { linje ->
            linje.kopier(refFagsystemId = fagsystemId)
        }

        internal fun normaliserLinjer(fagsystemId: String, linjer: List<Utbetalingslinje>): List<Utbetalingslinje> {
            val linjerMedBeløp = fjernLinjerUtenUtbetalingsdager(linjer)
            val nyeLinjerSkalPekePåFagsystemId = nyeLinjer(fagsystemId, linjerMedBeløp)
            val ferdigeLinjer = sisteLinjeSkalIkkeTrekkesIHelg(nyeLinjerSkalPekePåFagsystemId)
            return kjedeSammenLinjer(ferdigeLinjer)
        }

        private fun sisteLinjeSkalIkkeTrekkesIHelg(linjer: List<Utbetalingslinje>): List<Utbetalingslinje> {
            val utbetalingslinje = linjer.dropLast(1)
            val siste = linjer.takeLast(1).mapNotNull { it.kuttHelg() }
            return utbetalingslinje + siste
        }

        // alle nye linjer skal peke på fagsystemId, foruten linje nr 1
        private fun nyeLinjer(fagsystemId: String, linjer: List<Utbetalingslinje>) =
            linjer.take(1).map { it.kopier(refFagsystemId = null) } + linjer.drop(1).map { it.kopier(refFagsystemId = fagsystemId) }

        // linjer med beløp 0 kr er ugyldige/ikke ønsket å sende OS
        private fun fjernLinjerUtenUtbetalingsdager(linjer: List<Utbetalingslinje>) =
            linjer.filterNot { it.beløp == null || it.beløp == 0 }

        // oppdraget utgjør på sett og vis en linket liste hvor hver linje har et nummer, og peker tilbake på forrige linje
        internal fun kjedeSammenLinjer(linjer: List<Utbetalingslinje>, koblingslinje: Utbetalingslinje? = null): List<Utbetalingslinje> {
            if (linjer.isEmpty()) return emptyList()
            val førstelinje = linjer.first().let { førstelinje -> koblingslinje?.let { førstelinje.kobleTil(it) } ?: førstelinje }
            var forrige = førstelinje
            val result = mutableListOf(forrige)
            linjer.drop(1).forEach { linje ->
                forrige = linje.kobleTil(forrige)
                result.add(forrige)
            }
            return result
        }

        internal fun gjenopprett(dto: UtbetalingslinjeInnDto): Utbetalingslinje {
            return Utbetalingslinje(
                fom = dto.fom,
                tom = dto.tom,
                satstype = when (dto.satstype) {
                    SatstypeDto.Daglig -> Satstype.Daglig
                    SatstypeDto.Engang -> Satstype.Engang
                },
                beløp = dto.beløp,
                grad = dto.grad,
                refFagsystemId = dto.refFagsystemId,
                delytelseId = dto.delytelseId,
                refDelytelseId = dto.refDelytelseId,
                endringskode = Endringskode.gjenopprett(dto.endringskode),
                klassekode = Klassekode.gjenopprett(dto.klassekode),
                datoStatusFom = dto.datoStatusFom
            )
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

    fun opphørslinje(datoStatusFom: LocalDate) = kopier(
        endringskode = ENDR,
        refFagsystemId = null,
        refDelytelseId = null,
        datoStatusFom = datoStatusFom
    )

    fun kopier(
        fom: LocalDate = this.fom,
        tom: LocalDate = this.tom,
        endringskode: Endringskode = this.endringskode,
        delytelseId: Int = this.delytelseId,
        refDelytelseId: Int? = this.refDelytelseId,
        refFagsystemId: String? = this.refFagsystemId,
        klassekode: Klassekode = this.klassekode,
        datoStatusFom: LocalDate? = this.datoStatusFom,
        beløp: Int? = this.beløp,
    ) =
        Utbetalingslinje(
            fom = fom,
            tom = tom,
            satstype = satstype,
            beløp = beløp,
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
        other == sisteLinjeITidligereOppdrag && (this.fom > other.fom)

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

    internal fun begrensFra(førsteDag: LocalDate) = kopier(fom = førsteDag)
    internal fun begrensTil(sisteDato: LocalDate) = kopier(tom = sisteDato).kuttHelg()

    fun slåSammenLinje(førsteLinjeIForrige: Utbetalingslinje): Utbetalingslinje? {
        if (this.beløp != førsteLinjeIForrige.beløp || this.grad != førsteLinjeIForrige.grad || !this.tom.erRettFør(førsteLinjeIForrige.fom)) return null
        return kopier(tom = førsteLinjeIForrige.tom)
    }

    fun erForskjell() = endringskode != UEND

    fun erOpphør() = datoStatusFom != null

    internal fun kuttHelg(): Utbetalingslinje? {
        return when (tom.dayOfWeek) {
            DayOfWeek.SUNDAY -> tom.minusDays(2).takeUnless { it < fom}?.let { nyTom -> kopier(tom = nyTom) }
            DayOfWeek.SATURDAY -> tom.forrigeDag.takeUnless { it < fom}?.let { nyTom -> kopier(tom = nyTom) }
            else -> this
        }
    }

    fun behovdetaljer() = mapOf<String, Any?>(
        "fom" to fom.toString(),
        "tom" to tom.toString(),
        "satstype" to "$satstype",
        "sats" to beløp,
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

    fun dto() = UtbetalingslinjeUtDto(
        fom = this.fom,
        tom = this.tom,
        satstype = when {
            satstype === Satstype.Daglig -> SatstypeDto.Daglig
            satstype === Satstype.Engang -> SatstypeDto.Engang
            else -> error("ukjent statype: $satstype")
        },
        beløp = this.beløp!!,
        grad = this.grad,
        stønadsdager = stønadsdager(),
        totalbeløp = this.totalbeløp(),
        refFagsystemId = this.refFagsystemId,
        delytelseId = this.delytelseId,
        refDelytelseId = this.refDelytelseId,
        endringskode = when (endringskode) {
            NY -> EndringskodeDto.NY
            UEND -> EndringskodeDto.UEND
            ENDR -> EndringskodeDto.ENDR
        },
        klassekode = when (klassekode) {
            RefusjonIkkeOpplysningspliktig -> KlassekodeDto.RefusjonIkkeOpplysningspliktig
            Klassekode.RefusjonFeriepengerIkkeOpplysningspliktig -> KlassekodeDto.RefusjonFeriepengerIkkeOpplysningspliktig
            Klassekode.SykepengerArbeidstakerOrdinær -> KlassekodeDto.SykepengerArbeidstakerOrdinær
            Klassekode.SykepengerArbeidstakerFeriepenger -> KlassekodeDto.SykepengerArbeidstakerFeriepenger
        },
        datoStatusFom = this.datoStatusFom,
        statuskode = this.statuskode
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