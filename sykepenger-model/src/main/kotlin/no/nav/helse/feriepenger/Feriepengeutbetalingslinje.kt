package no.nav.helse.feriepenger

import java.time.LocalDate
import no.nav.helse.dto.EndringskodeDto
import no.nav.helse.dto.KlassekodeDto
import no.nav.helse.dto.deserialisering.FeriepengeutbetalingslinjeInnDto
import no.nav.helse.dto.serialisering.FeriepengeutbetalingslinjeUtDto
import no.nav.helse.hendelser.til
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Endringskode.ENDR
import no.nav.helse.utbetalingslinjer.Endringskode.NY
import no.nav.helse.utbetalingslinjer.Endringskode.UEND
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.helse.utbetalingslinjer.Klassekode.RefusjonIkkeOpplysningspliktig
import no.nav.helse.utbetalingslinjer.OppdragDetaljer

data class Feriepengeutbetalingslinje(
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: Int,
    val refFagsystemId: String? = null,
    val delytelseId: Int = 1,
    val refDelytelseId: Int? = null,
    val endringskode: Endringskode = NY,
    val klassekode: Klassekode,
    val datoStatusFom: LocalDate? = null
) : Iterable<LocalDate> {

    companion object {
        internal fun gjenopprett(dto: FeriepengeutbetalingslinjeInnDto): Feriepengeutbetalingslinje {
            return Feriepengeutbetalingslinje(
                fom = dto.fom,
                tom = dto.tom,
                beløp = dto.beløp,
                refFagsystemId = dto.refFagsystemId,
                delytelseId = dto.delytelseId,
                refDelytelseId = dto.refDelytelseId,
                endringskode = Endringskode.Companion.gjenopprett(dto.endringskode),
                klassekode = Klassekode.Companion.gjenopprett(dto.klassekode),
                datoStatusFom = dto.datoStatusFom
            )
        }
    }

    val statuskode get() = datoStatusFom?.let { "OPPH" }

    val periode get() = fom til tom

    override operator fun iterator() = periode.iterator()

    override fun toString() = "$fom til $tom $endringskode ${datoStatusFom?.let { "opphører fom $it" }}"

    internal fun detaljer() =
        OppdragDetaljer.LinjeDetaljer(
            fom = fom,
            tom = tom,
            sats = beløp,
            grad = null,
            stønadsdager = 1,
            totalbeløp = beløp,
            statuskode = statuskode
        )

    fun opphørslinje(datoStatusFom: LocalDate) = copy(
        endringskode = ENDR,
        refFagsystemId = null,
        refDelytelseId = null,
        datoStatusFom = datoStatusFom
    )

    fun erForskjell() = endringskode != UEND

    fun erOpphør() = datoStatusFom != null

    fun behovdetaljer() = mapOf<String, Any?>(
        "fom" to fom.toString(),
        "tom" to tom.toString(),
        "satstype" to "ENG",
        "sats" to beløp,
        "endringskode" to endringskode.toString(),
        "delytelseId" to delytelseId,
        "refDelytelseId" to refDelytelseId,
        "refFagsystemId" to refFagsystemId,
        "statuskode" to statuskode,
        "datoStatusFom" to datoStatusFom?.toString(),
        "klassekode" to klassekode.verdi
    )

    fun dto() = FeriepengeutbetalingslinjeUtDto(
        fom = this.fom,
        tom = this.tom,
        beløp = this.beløp,
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
