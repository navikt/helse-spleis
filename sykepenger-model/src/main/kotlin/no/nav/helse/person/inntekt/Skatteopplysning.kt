package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import no.nav.helse.dto.InntekttypeDto
import no.nav.helse.dto.SkatteopplysningDto
import no.nav.helse.etterlevelse.Inntektsubsumsjon
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.isWithinRangeOf
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer

data class Skatteopplysning(
    val hendelseId: MeldingsreferanseId,
    val beløp: Inntekt,
    val måned: YearMonth,
    val type: Inntekttype,
    val fordel: String,
    val beskrivelse: String,
    val tidsstempel: LocalDateTime = LocalDateTime.now()
) {
    enum class Inntekttype {
        LØNNSINNTEKT,
        NÆRINGSINNTEKT,
        PENSJON_ELLER_TRYGD,
        YTELSE_FRA_OFFENTLIGE;

        fun somStreng() = when (this) {
            LØNNSINNTEKT -> "LØNNSINNTEKT"
            NÆRINGSINNTEKT -> "NÆRINGSINNTEKT"
            PENSJON_ELLER_TRYGD -> "PENSJON_ELLER_TRYGD"
            YTELSE_FRA_OFFENTLIGE -> "YTELSE_FRA_OFFENTLIGE"
        }
    }

    companion object {
        fun sisteMåneder(dato: LocalDate, antallMåneder: Int, inntektsopplysninger: List<Skatteopplysning>) =
            inntektsopplysninger.filter { it.måned.isWithinRangeOf(dato, antallMåneder.toLong()) }

        fun sisteTreMåneder(dato: LocalDate, inntektsopplysninger: List<Skatteopplysning>) =
            sisteMåneder(dato, 3, inntektsopplysninger)

        fun omregnetÅrsinntekt(liste: List<Skatteopplysning>) = liste
            .map { it.beløp }
            .summer()
            .coerceAtLeast(Inntekt.INGEN)
            .div(3)

        fun rapportertInntekt(liste: List<Skatteopplysning>) = liste
            .map { it.beløp }
            .summer()
            .div(12)

        fun List<Skatteopplysning>.subsumsjonsformat() = this.map {
            Inntektsubsumsjon(
                beløp = it.beløp.månedlig,
                årMåned = it.måned,
                type = it.type.toString(),
                fordel = it.fordel,
                beskrivelse = it.beskrivelse
            )
        }

        fun gjenopprett(dto: SkatteopplysningDto): Skatteopplysning {
            return Skatteopplysning(
                hendelseId = MeldingsreferanseId.gjenopprett(dto.hendelseId),
                beløp = Inntekt.gjenopprett(dto.beløp),
                måned = dto.måned,
                type = when (dto.type) {
                    InntekttypeDto.LØNNSINNTEKT -> Inntekttype.LØNNSINNTEKT
                    InntekttypeDto.NÆRINGSINNTEKT -> Inntekttype.NÆRINGSINNTEKT
                    InntekttypeDto.PENSJON_ELLER_TRYGD -> Inntekttype.PENSJON_ELLER_TRYGD
                    InntekttypeDto.YTELSE_FRA_OFFENTLIGE -> Inntekttype.YTELSE_FRA_OFFENTLIGE
                },
                fordel = dto.fordel,
                beskrivelse = dto.beskrivelse,
                tidsstempel = dto.tidsstempel
            )
        }
    }

    internal fun dto() = SkatteopplysningDto(
        hendelseId = this.hendelseId.dto(),
        beløp = this.beløp.dtoMånedligDouble(),
        måned = this.måned,
        type = when (this.type) {
            Inntekttype.LØNNSINNTEKT -> InntekttypeDto.LØNNSINNTEKT
            Inntekttype.NÆRINGSINNTEKT -> InntekttypeDto.NÆRINGSINNTEKT
            Inntekttype.PENSJON_ELLER_TRYGD -> InntekttypeDto.PENSJON_ELLER_TRYGD
            Inntekttype.YTELSE_FRA_OFFENTLIGE -> InntekttypeDto.YTELSE_FRA_OFFENTLIGE
        },
        fordel = this.fordel,
        beskrivelse = this.beskrivelse,
        tidsstempel = this.tidsstempel
    )
}
