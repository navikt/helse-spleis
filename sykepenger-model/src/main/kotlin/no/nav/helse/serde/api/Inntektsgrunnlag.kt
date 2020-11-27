package no.nav.helse.serde.api

import no.nav.helse.person.InntekthistorikkVisitor
import no.nav.helse.person.InntektshistorikkVol2
import no.nav.helse.person.Person
import no.nav.helse.serde.api.InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO
import no.nav.helse.serde.api.InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO
import no.nav.helse.serde.api.InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.SammenligningsgrunnlagDTO
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal fun inntektsgrunnlag(
    person: Person,
    inntektshistorikk: Map<String, InntektshistorikkVol2>,
    skjæringstidspunkter: Map<LocalDate, LocalDate>
): List<InntektsgrunnlagDTO> {
    return skjæringstidspunkter
        .map { (skjæringstidspunkt, personensSisteKjenteSykedagIDenSammenhengdendeSykeperioden) ->
            val arbeidsgiverinntekt: List<InntektsgrunnlagDTO.ArbeidsgiverinntektDTO> =
                inntektshistorikk.map { (orgnummer, inntekthist) -> arbeidsgiverinntekt(skjæringstidspunkt, orgnummer, inntekthist) }

            InntektsgrunnlagDTO(
                skjæringstidspunkt = skjæringstidspunkt,
                sykepengegrunnlag = person.sykepengegrunnlag(skjæringstidspunkt, personensSisteKjenteSykedagIDenSammenhengdendeSykeperioden)
                    .reflection { årlig, _, _, _ -> årlig },
                omregnetÅrsinntekt = 123.0,
                sammenligningsgrunnlag = 123.0,
                avviksprosent = 0.0,
                grunnbeløp = 100000,
                maksUtbetalingPerDag = 1300,
                inntekter = arbeidsgiverinntekt
            )
        }
}

private fun arbeidsgiverinntekt(
    skjæringstidspunkt: LocalDate,
    orgnummer: String,
    inntektshistorikk: InntektshistorikkVol2
): InntektsgrunnlagDTO.ArbeidsgiverinntektDTO {
    val (inntektsopplysning, inntekt) = requireNotNull(inntektshistorikk.grunnlagForSykepengegrunnlagMedMetadata(skjæringstidspunkt))
    val omregnetÅrsinntektDTO = OmregnetÅrsinntektVisitor(inntektsopplysning, inntekt).omregnetÅrsinntektDTO
    val sammenligningsgrunnlagDTO =
        inntektshistorikk.grunnlagForSammenligningsgrunnlagMedMetadata(skjæringstidspunkt)?.let { (sammenligningsgrunnlagsopplysning, sammenligningsgrunnlag) ->
            SammenligningsgrunnlagVisitor(sammenligningsgrunnlagsopplysning, sammenligningsgrunnlag).sammenligningsgrunnlagDTO
        }
    return InntektsgrunnlagDTO.ArbeidsgiverinntektDTO(
        orgnummer,
        omregnetÅrsinntektDTO,
        sammenligningsgrunnlagDTO
    )
}

private class OmregnetÅrsinntektVisitor(
    inntektsopplysning: InntektshistorikkVol2.Inntektsopplysning,
    private val inntekt: Inntekt
) : InntekthistorikkVisitor {
    lateinit var omregnetÅrsinntektDTO: OmregnetÅrsinntektDTO
    private val skattegreier = mutableListOf<OmregnetÅrsinntektDTO.InntekterFraAOrdningenDTO>()

    init {
        inntektsopplysning.accept(this)
    }

    override fun visitSaksbehandler(
        saksbehandler: InntektshistorikkVol2.Saksbehandler,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        omregnetÅrsinntektDTO = OmregnetÅrsinntektDTO(
            kilde = InntektkildeDTO.Saksbehandler,
            beløp = beløp.reflection { årlig, _, _, _ -> årlig },
            månedsbeløp = beløp.reflection { _, mnd, _, _ -> mnd }
        )
    }

    override fun visitInntektsmelding(
        inntektsmelding: InntektshistorikkVol2.Inntektsmelding,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        omregnetÅrsinntektDTO = OmregnetÅrsinntektDTO(
            kilde = InntektkildeDTO.Inntektsmelding,
            beløp = beløp.reflection { årlig, _, _, _ -> årlig },
            månedsbeløp = beløp.reflection { _, mnd, _, _ -> mnd }
        )
    }

    override fun visitInfotrygd(
        infotrygd: InntektshistorikkVol2.Infotrygd,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        omregnetÅrsinntektDTO = OmregnetÅrsinntektDTO(
            kilde = InntektkildeDTO.Infotrygd,
            beløp = beløp.reflection { årlig, _, _, _ -> årlig },
            månedsbeløp = beløp.reflection { _, mnd, _, _ -> mnd }
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
        skattegreier.add(
            OmregnetÅrsinntektDTO.InntekterFraAOrdningenDTO(
                måned = måned,
                sum = beløp.reflection { _, mnd, _, _ -> mnd }
            ))
    }

    override fun postVisitSkatt(skattComposite: InntektshistorikkVol2.SkattComposite) {
        omregnetÅrsinntektDTO = OmregnetÅrsinntektDTO(
            kilde = InntektkildeDTO.AOrdningen,
            beløp = inntekt.reflection { årlig, _, _, _ -> årlig },
            månedsbeløp = inntekt.reflection { _, mnd, _, _ -> mnd },
            inntekterFraAOrdningen = skattegreier
                .groupBy({ it.måned }) { it.sum }
                .map { (måned: YearMonth, beløp: List<Double>) ->
                    OmregnetÅrsinntektDTO.InntekterFraAOrdningenDTO(
                        måned = måned,
                        sum = beløp.sum()
                    )
                }
        )
    }
}

private class SammenligningsgrunnlagVisitor(
    inntektsopplysning: InntektshistorikkVol2.Inntektsopplysning,
    private val inntekt: Inntekt
) : InntekthistorikkVisitor {
    lateinit var sammenligningsgrunnlagDTO: SammenligningsgrunnlagDTO
    private val skattegreier = mutableListOf<SammenligningsgrunnlagDTO.InntekterFraAOrdningenDTO>()

    init {
        inntektsopplysning.accept(this)
    }

    override fun preVisitSkatt(skattComposite: InntektshistorikkVol2.SkattComposite) {
        skattegreier.clear()
    }

    override fun visitSkattSammenligningsgrunnlag(
        sammenligningsgrunnlag: InntektshistorikkVol2.Skatt.Sammenligningsgrunnlag,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        måned: YearMonth,
        type: InntektshistorikkVol2.Skatt.Inntekttype,
        fordel: String,
        beskrivelse: String,
        tidsstempel: LocalDateTime
    ) {
        skattegreier.add(
            SammenligningsgrunnlagDTO.InntekterFraAOrdningenDTO(
                måned = måned,
                sum = beløp.reflection { _, mnd, _, _ -> mnd }
            ))
    }

    override fun postVisitSkatt(skattComposite: InntektshistorikkVol2.SkattComposite) {
        sammenligningsgrunnlagDTO = SammenligningsgrunnlagDTO(
            beløp = inntekt.reflection { årlig, _, _, _ -> årlig },
            inntekterFraAOrdningen = skattegreier
                .groupBy({ it.måned }) { it.sum }
                .map { (måned: YearMonth, beløp: List<Double>) ->
                    SammenligningsgrunnlagDTO.InntekterFraAOrdningenDTO(
                        måned = måned,
                        sum = beløp.sum()
                    )
                }
        )
    }
}
