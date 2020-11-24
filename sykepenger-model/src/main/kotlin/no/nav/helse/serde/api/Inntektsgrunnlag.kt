package no.nav.helse.serde.api

import no.nav.helse.person.InntekthistorikkVisitor
import no.nav.helse.person.InntektshistorikkVol2
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal fun inntektsgrunnlag(inntektshistorikk: Map<String, InntektshistorikkVol2>, skjæringstidspunkter: List<LocalDate>): List<InntektsgrunnlagDTO> {
    return skjæringstidspunkter
        .map { skjæringstidspunkt ->
            val arbeidsgiverinntekt: List<InntektsgrunnlagDTO.ArbeidsgiverinntektDTO> =
                inntektshistorikk.map { (orgnummer, inntekthist) -> Inntektsgrunnlag(skjæringstidspunkt, orgnummer, inntekthist).result() }

            InntektsgrunnlagDTO(
                skjæringstidspunkt = skjæringstidspunkt,
                sykepengegrunnlag = 123.0,
                omregnetÅrsinntekt = 123.0,
                sammenligningsgrunnlag = 123.0,
                avviksprosent = 0.0,
                grunnbeløp = 100000,
                maksUtbetalingPerDag = 1300,
                inntekter = arbeidsgiverinntekt
            )
        }
}

private class Inntektsgrunnlag(
    private val skjæringstidspunkt: LocalDate,
    private val orgnummer: String,
    inntektshistorikk: InntektshistorikkVol2
) : InntekthistorikkVisitor {
    private var isRunning = true

    init {
        inntektshistorikk.accept(this)
    }

    fun result(): InntektsgrunnlagDTO.ArbeidsgiverinntektDTO {
        return InntektsgrunnlagDTO.ArbeidsgiverinntektDTO(
            orgnummer,
            omregnetÅrsinntektDTO,
            InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.SammenligningsgrunnlagDTO(0.0, emptyList())
        )
    }

    private lateinit var omregnetÅrsinntektDTO: InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO
    private var prioritet: Int = -1

    override fun visitInntektsmelding(
        inntektsmelding: InntektshistorikkVol2.Inntektsmelding,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        if(isRunning) {
            if (this.prioritet < inntektsmelding.prioritet && dato == skjæringstidspunkt) {
                omregnetÅrsinntektDTO = InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO(
                    beløp.reflection { årlig, _, _, _ -> årlig },
                    beløp.reflection { _, månedlig, _, _ -> månedlig }
                )
            }
        }
    }

    override fun postVisitInnslag(innslag: InntektshistorikkVol2.Innslag) {
        isRunning = false
    }
}
