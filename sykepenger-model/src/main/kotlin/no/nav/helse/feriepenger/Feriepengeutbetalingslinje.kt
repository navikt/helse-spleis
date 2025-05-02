package no.nav.helse.feriepenger

import java.time.LocalDate
import no.nav.helse.dto.FeriepengerendringskodeDto
import no.nav.helse.dto.FeriepengerklassekodeDto
import no.nav.helse.dto.deserialisering.FeriepengeutbetalingslinjeInnDto
import no.nav.helse.dto.serialisering.FeriepengeutbetalingslinjeUtDto
import no.nav.helse.feriepenger.Feriepengerendringskode.ENDR
import no.nav.helse.feriepenger.Feriepengerendringskode.NY
import no.nav.helse.feriepenger.Feriepengerendringskode.UEND
import no.nav.helse.hendelser.til

data class Feriepengeutbetalingslinje(
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: Int,
    val refFagsystemId: String? = null,
    val delytelseId: Int = 1,
    val refDelytelseId: Int? = null,
    val endringskode: Feriepengerendringskode = NY,
    val klassekode: Feriepengerklassekode,
    val datoStatusFom: LocalDate? = null
) : Iterable<LocalDate> {

    init {
        check(beløp != 0) {
            "beløp kan ikke være 0"
        }
    }

    companion object {
        internal fun gjenopprett(dto: FeriepengeutbetalingslinjeInnDto): Feriepengeutbetalingslinje {
            return Feriepengeutbetalingslinje(
                fom = dto.fom,
                tom = dto.tom,
                beløp = dto.beløp,
                refFagsystemId = dto.refFagsystemId,
                delytelseId = dto.delytelseId,
                refDelytelseId = dto.refDelytelseId,
                endringskode = Feriepengerendringskode.gjenopprett(dto.endringskode),
                klassekode = Feriepengerklassekode.gjenopprett(dto.klassekode),
                datoStatusFom = dto.datoStatusFom
            )
        }
    }

    val statuskode get() = datoStatusFom?.let { "OPPH" }

    val periode get() = fom til tom

    override operator fun iterator() = periode.iterator()

    override fun toString() = "$fom til $tom $endringskode ${datoStatusFom?.let { "opphører fom $it" }}"

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
            NY -> FeriepengerendringskodeDto.NY
            UEND -> FeriepengerendringskodeDto.UEND
            ENDR -> FeriepengerendringskodeDto.ENDR
        },
        klassekode = when (klassekode) {
            Feriepengerklassekode.RefusjonFeriepengerIkkeOpplysningspliktig -> FeriepengerklassekodeDto.RefusjonFeriepengerIkkeOpplysningspliktig
            Feriepengerklassekode.SykepengerArbeidstakerFeriepenger -> FeriepengerklassekodeDto.SykepengerArbeidstakerFeriepenger
        },
        datoStatusFom = this.datoStatusFom,
        statuskode = this.statuskode
    )
}

enum class Feriepengerendringskode {
    NY, UEND, ENDR;

    companion object {
        fun gjenopprett(dto: FeriepengerendringskodeDto) = when (dto) {
            FeriepengerendringskodeDto.ENDR -> ENDR
            FeriepengerendringskodeDto.NY -> NY
            FeriepengerendringskodeDto.UEND -> UEND
        }
    }
}

enum class Feriepengerklassekode(val verdi: String) {
    RefusjonFeriepengerIkkeOpplysningspliktig(verdi = "SPREFAGFER-IOP"),
    SykepengerArbeidstakerFeriepenger(verdi = "SPATFER");

    companion object {
        private val map = entries.associateBy(Feriepengerklassekode::verdi)
        fun from(verdi: String) = requireNotNull(map[verdi]) { "Støtter ikke klassekode: $verdi" }
        fun gjenopprett(dto: FeriepengerklassekodeDto) = when (dto) {
            FeriepengerklassekodeDto.RefusjonFeriepengerIkkeOpplysningspliktig -> RefusjonFeriepengerIkkeOpplysningspliktig
            FeriepengerklassekodeDto.SykepengerArbeidstakerFeriepenger -> SykepengerArbeidstakerFeriepenger
        }
    }
}
