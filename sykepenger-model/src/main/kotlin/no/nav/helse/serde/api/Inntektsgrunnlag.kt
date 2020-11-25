package no.nav.helse.serde.api

import no.nav.helse.person.InntekthistorikkVisitor
import no.nav.helse.person.InntektshistorikkVol2
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
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
    private val inntektshistorikk: InntektshistorikkVol2
) {
    fun result(): InntektsgrunnlagDTO.ArbeidsgiverinntektDTO {
        val (inntektsopplysning, _) = requireNotNull(inntektshistorikk.grunnlagForSykepengegrunnlagMedMetadata(skjæringstidspunkt))
        val omregnetÅrsinntektDTO = InternalVisitor(inntektsopplysning).omregnetÅrsinntektDTO
        return InntektsgrunnlagDTO.ArbeidsgiverinntektDTO(
            orgnummer,
            omregnetÅrsinntektDTO,
            InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.SammenligningsgrunnlagDTO(0.0, emptyList())
        )
    }

    private class InternalVisitor(inntektsopplysning: InntektshistorikkVol2.Inntektsopplysning) : InntekthistorikkVisitor {
        lateinit var omregnetÅrsinntektDTO: InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO
        private val skattegreier = mutableListOf<InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntekterFraAOrdningenDTO>()

        init {
            inntektsopplysning.accept(this)
        }

        override fun visitInntektsmelding(
            inntektsmelding: InntektshistorikkVol2.Inntektsmelding,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            this.omregnetÅrsinntektDTO = InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO(
                beløp = beløp.reflection { årlig, _, _, _ -> årlig },
                månedsbeløp = beløp.reflection { _, månedlig, _, _ -> månedlig }
            )
        }

        override fun visitInfotrygd(
            infotrygd: InntektshistorikkVol2.Infotrygd,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            omregnetÅrsinntektDTO = InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO(
                beløp = beløp.reflection { årlig, _, _, _ -> årlig },
                månedsbeløp = beløp.reflection { _, månedlig, _, _ -> månedlig }
            )
        }

        override fun preVisitSkatt(skattComposite: InntektshistorikkVol2.SkattComposite) {
            skattegreier.clear()
        }

        override fun visitSkattSykepengegrunnlag(
            sykepengegrunnlag: InntektshistorikkVol2.Skatt.Sykepengegrunnlag,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            måned: YearMonth,
            type: InntektshistorikkVol2.Skatt.Inntekttype,
            fordel: String,
            beskrivelse: String,
            tidsstempel: LocalDateTime
        ) {
            skattegreier.add(InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntekterFraAOrdningenDTO(
                måned = måned,
                sum = beløp.reflection { _, mnd, _, _ -> mnd }
            ))
        }

        override fun postVisitSkatt(skattComposite: InntektshistorikkVol2.SkattComposite) {
            val (_, inntekt) = skattComposite.grunnlagForSykepengegrunnlag(skattComposite.dato)!!
            omregnetÅrsinntektDTO = InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO(
                beløp = inntekt.reflection { årlig, _, _, _ -> årlig },
                sumFraAOrdningen = skattegreier.sumOf { it.sum },
                inntekterFraAOrdningen = skattegreier
                    .groupBy({ it.måned }) { it.sum }
                    .map { (måned: YearMonth, beløp: List<Double>) ->
                        InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntekterFraAOrdningenDTO(
                            måned = måned,
                            sum = beløp.sum()
                        )
                    }
            )
        }
    }
}
