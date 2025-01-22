package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.dto.deserialisering.InntektsdataInnDto
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto
import no.nav.helse.dto.serialisering.InntektsdataUtDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.økonomi.Inntekt

data class Inntektsdata(
    val hendelseId: UUID,
    val dato: LocalDate,
    val beløp: Inntekt,
    val tidsstempel: LocalDateTime
) {

    fun funksjoneltLik(other: Inntektsdata) =
        this.dato == other.dato && this.beløp == other.beløp

    fun dto() = InntektsdataUtDto(
        hendelseId = hendelseId,
        dato = dato,
        beløp = beløp.dto(),
        tidsstempel = tidsstempel
    )

    companion object {
        fun ingen(hendelseId: UUID, dato: LocalDate, tidsstempel: LocalDateTime = LocalDateTime.now()) = Inntektsdata(
            hendelseId = hendelseId,
            dato = dato,
            beløp = Inntekt.INGEN,
            tidsstempel = tidsstempel
        )

        fun gjenopprett(dto: InntektsdataInnDto) = Inntektsdata(
            hendelseId = dto.hendelseId,
            dato = dto.dato,
            beløp = Inntekt.gjenopprett(dto.beløp),
            tidsstempel = dto.tidsstempel
        )
    }
}

internal sealed class Inntektsopplysning(
    val id: UUID,
    val inntektsdata: Inntektsdata
) {
    fun funksjoneltLik(other: Inntektsopplysning) =
        this::class == other::class && this.inntektsdata.funksjoneltLik(other.inntektsdata)

    internal companion object {
        internal fun List<Inntektsopplysning>.markerFlereArbeidsgivere(aktivitetslogg: IAktivitetslogg) {
            if (distinctBy { it.inntektsdata.dato }.size <= 1 && none { it is SkattSykepengegrunnlag }) return
            aktivitetslogg.varsel(Varselkode.RV_VV_2)
        }

        internal fun gjenopprett(dto: InntektsopplysningInnDto): Inntektsopplysning {
            return when (dto) {
                is InntektsopplysningInnDto.InfotrygdDto -> Infotrygd.gjenopprett(dto)
                is InntektsopplysningInnDto.ArbeidsgiverinntektDto -> Arbeidsgiverinntekt.gjenopprett(dto)
                is InntektsopplysningInnDto.SkattSykepengegrunnlagDto -> SkattSykepengegrunnlag.gjenopprett(dto)
            }
        }
    }

    internal abstract fun dto(): InntektsopplysningUtDto
}
