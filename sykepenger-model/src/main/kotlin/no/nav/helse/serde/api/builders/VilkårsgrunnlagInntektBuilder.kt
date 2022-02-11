package no.nav.helse.serde.api.builders

import no.nav.helse.Grunnbeløp
import no.nav.helse.person.*
import no.nav.helse.serde.api.InntektsgrunnlagDTO
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class VilkårsgrunnlagInntektBuilder(private val person: Person) {
    private val nøkkeldataOmInntekter = mutableListOf<NøkkeldataOmInntekt>()

    internal fun nøkkeldataOmInntekt(nøkkeldataOmInntekt: NøkkeldataOmInntekt) {
        nøkkeldataOmInntekter.add(nøkkeldataOmInntekt)
    }

    fun build(): List<InntektsgrunnlagDTO> {
        return inntektsgrunnlag()
    }

    private fun inntektsgrunnlag() = nøkkeldataOmInntekter
        .groupBy { it.skjæringstidspunkt }
        .mapNotNull { (_, value) -> value.maxByOrNull { it.sisteDagISammenhengendePeriode } }
        .mapNotNull { nøkkeldata -> person.vilkårsgrunnlagFor(nøkkeldata.skjæringstidspunkt)?.let { nøkkeldata to it } }
        .map { (nøkkeldata, vilkårsgrunnlag) ->
            val vilkårsgrunnlagVisitor = VilkårsgrunnlagVisitor(vilkårsgrunnlag, nøkkeldata.skjæringstidspunkt)
            val sykepengegrunnlag = vilkårsgrunnlag.sykepengegrunnlag().sykepengegrunnlag
            val grunnlagForSykepengegrunnlag = vilkårsgrunnlag.grunnlagForSykepengegrunnlag()
            val sammenligningsgrunnlag = vilkårsgrunnlag.sammenligningsgrunnlag()
            // Vi har ikke sammenligningsgrunnlag på vilkårsgrunnlag fastsatt i infotrygd
            val sammenligningsgrunnlagMap = vilkårsgrunnlagVisitor.sammenligningsgrunnlag?.let(::SammenligningsgrunnlagVisitor)?.sammenligningsgrunnlagDTO
                ?: emptyMap()

            val arbeidsgiverinntekt = ArbeidsgiverInntektopplysningerVisitor(vilkårsgrunnlagVisitor.sykepengegrunnlag, sammenligningsgrunnlagMap)
                .arbeidsgiverInntektDTO()

            val arbeidsgivereMedBareSammenligningsgrunnlag = sammenligningsgrunnlagMap
                .filterKeys { orgnummer -> orgnummer !in arbeidsgiverinntekt.map { it.arbeidsgiver } }
                .map { (orgnummer, arbeidsgiverInntektDTO) ->
                    InntektsgrunnlagDTO.ArbeidsgiverinntektDTO(
                        orgnummer,
                        null,
                        arbeidsgiverInntektDTO
                    )
                }

            InntektsgrunnlagDTO(
                skjæringstidspunkt = nøkkeldata.skjæringstidspunkt,
                sykepengegrunnlag = sykepengegrunnlag.reflection { årlig, _, _, _ -> årlig },
                omregnetÅrsinntekt = grunnlagForSykepengegrunnlag.reflection { årlig, _, _, _ -> årlig },
                sammenligningsgrunnlag = sammenligningsgrunnlag?.reflection { årlig, _, _, _ -> årlig },
                avviksprosent = nøkkeldata.avviksprosent,
                maksUtbetalingPerDag = sykepengegrunnlag.reflection { _, _, daglig, _ -> daglig },
                inntekter = arbeidsgiverinntekt + arbeidsgivereMedBareSammenligningsgrunnlag,
                oppfyllerKravOmMinstelønn = sykepengegrunnlag > person.minimumInntekt(nøkkeldata.skjæringstidspunkt),
                grunnbeløp = (Grunnbeløp.`1G`
                    .beløp(nøkkeldata.skjæringstidspunkt, nøkkeldata.sisteDagISammenhengendePeriode)
                    .reflection { årlig, _, _, _ -> årlig })
                    .toInt(),
            )
        }

    private class VilkårsgrunnlagVisitor(
        vilkårsgrunnlag: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement,
        skjæringstidspunkt: LocalDate
    ) : VilkårsgrunnlagHistorikkVisitor {
        lateinit var sykepengegrunnlag: Sykepengegrunnlag
        var sammenligningsgrunnlag: Sammenligningsgrunnlag? = null

        init {
            vilkårsgrunnlag.accept(skjæringstidspunkt, this)
        }

        override fun preVisitSykepengegrunnlag(
            sykepengegrunnlag1: Sykepengegrunnlag,
            sykepengegrunnlag: Inntekt,
            grunnlagForSykepengegrunnlag: Inntekt,
            begrensning: Sykepengegrunnlag.Begrensning,
            deaktiverteArbeidsforhold: List<String>
        ) {
            this.sykepengegrunnlag = sykepengegrunnlag1
        }

        override fun preVisitSammenligningsgrunnlag(sammenligningsgrunnlag1: Sammenligningsgrunnlag, sammenligningsgrunnlag: Inntekt) {
            this.sammenligningsgrunnlag = sammenligningsgrunnlag1
        }
    }

    private class ArbeidsgiverInntektopplysningerVisitor(
        sykepengegrunnlag: Sykepengegrunnlag,
        private val sammenligningsgrunnlagMap: Map<String, InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.SammenligningsgrunnlagDTO>
    ) :
        VilkårsgrunnlagHistorikkVisitor {
        private val arbeidsgiverInntektDTO = mutableListOf<InntektsgrunnlagDTO.ArbeidsgiverinntektDTO>()
        private val skattegreier = mutableListOf<InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntekterFraAOrdningenDTO>()

        private lateinit var orgnummer: String
        lateinit var omregnetÅrsinntektDTO: InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO

        fun arbeidsgiverInntektDTO() = arbeidsgiverInntektDTO

        init {
            sykepengegrunnlag.accept(this)
        }

        override fun preVisitArbeidsgiverInntektsopplysning(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning, orgnummer: String) {
            this.orgnummer = orgnummer
        }

        override fun postVisitArbeidsgiverInntektsopplysning(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning, orgnummer: String) {
            val sammenligningsgrunnlagDTO = sammenligningsgrunnlagMap[orgnummer]

            arbeidsgiverInntektDTO.add(
                InntektsgrunnlagDTO.ArbeidsgiverinntektDTO(
                    orgnummer,
                    omregnetÅrsinntektDTO,
                    sammenligningsgrunnlagDTO
                )
            )
        }

        override fun visitSaksbehandler(
            saksbehandler: Inntektshistorikk.Saksbehandler,
            id: UUID,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            omregnetÅrsinntektDTO = InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO(
                kilde = InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.Saksbehandler,
                beløp = beløp.reflection { årlig, _, _, _ -> årlig },
                månedsbeløp = beløp.reflection { _, mnd, _, _ -> mnd }
            )
        }

        override fun visitInntektsmelding(
            inntektsmelding: Inntektshistorikk.Inntektsmelding,
            id: UUID,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            omregnetÅrsinntektDTO = InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO(
                kilde = InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.Inntektsmelding,
                beløp = beløp.reflection { årlig, _, _, _ -> årlig },
                månedsbeløp = beløp.reflection { _, mnd, _, _ -> mnd }
            )
        }

        override fun visitInfotrygd(
            infotrygd: Inntektshistorikk.Infotrygd,
            id: UUID,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            omregnetÅrsinntektDTO = InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO(
                kilde = InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.Infotrygd,
                beløp = beløp.reflection { årlig, _, _, _ -> årlig },
                månedsbeløp = beløp.reflection { _, mnd, _, _ -> mnd }
            )
        }

        override fun preVisitSkatt(skattComposite: Inntektshistorikk.SkattComposite, id: UUID, dato: LocalDate) {
            skattegreier.clear()
        }

        override fun visitSkattSykepengegrunnlag(
            sykepengegrunnlag: Inntektshistorikk.Skatt.Sykepengegrunnlag,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            måned: YearMonth,
            type: Inntektshistorikk.Skatt.Inntekttype,
            fordel: String,
            beskrivelse: String,
            tidsstempel: LocalDateTime
        ) {
            skattegreier.add(
                InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntekterFraAOrdningenDTO(
                    måned = måned,
                    sum = beløp.reflection { _, mnd, _, _ -> mnd }
                ))
        }

        override fun postVisitSkatt(skattComposite: Inntektshistorikk.SkattComposite, id: UUID, dato: LocalDate) {
            omregnetÅrsinntektDTO = InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO(
                kilde = InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.AOrdningen,
                beløp = skattComposite.grunnlagForSykepengegrunnlag().reflection { årlig, _, _, _ -> årlig },
                månedsbeløp = skattComposite.grunnlagForSykepengegrunnlag().reflection { _, mnd, _, _ -> mnd },
                inntekterFraAOrdningen = skattegreier
                    .groupBy({ it.måned }) { it.sum }
                    .map { (måned: YearMonth, beløp: List<Double>) ->
                        InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntekterFraAOrdningenDTO(
                            måned = måned,
                            sum = beløp.sum()
                        )
                    }.sortedByDescending { it.måned }
            )
        }
    }

    private class SammenligningsgrunnlagVisitor(
        sammenligningsgrunnlag: Sammenligningsgrunnlag
    ) : VilkårsgrunnlagHistorikkVisitor {
        val sammenligningsgrunnlagDTO = mutableMapOf<String, InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.SammenligningsgrunnlagDTO>()
        private val skattegreier = mutableListOf<InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.SammenligningsgrunnlagDTO.InntekterFraAOrdningenDTO>()
        private lateinit var inntekt: Inntekt

        init {
            sammenligningsgrunnlag.accept(this)
        }

        override fun preVisitSkatt(skattComposite: Inntektshistorikk.SkattComposite, id: UUID, dato: LocalDate) {
            inntekt = skattComposite.sammenligningsgrunnlag()!!
            skattegreier.clear()
        }

        override fun visitSkattSammenligningsgrunnlag(
            sammenligningsgrunnlag: Inntektshistorikk.Skatt.Sammenligningsgrunnlag,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            måned: YearMonth,
            type: Inntektshistorikk.Skatt.Inntekttype,
            fordel: String,
            beskrivelse: String,
            tidsstempel: LocalDateTime
        ) {
            skattegreier.add(
                InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.SammenligningsgrunnlagDTO.InntekterFraAOrdningenDTO(
                    måned = måned,
                    sum = beløp.reflection { _, mnd, _, _ -> mnd }
                ))
        }



        override fun postVisitArbeidsgiverInntektsopplysning(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning, orgnummer: String) {
            sammenligningsgrunnlagDTO[orgnummer] = InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.SammenligningsgrunnlagDTO(
                beløp = inntekt.reflection { årlig, _, _, _ -> årlig },
                inntekterFraAOrdningen = skattegreier
                    .groupBy({ it.måned }) { it.sum }
                    .map { (måned: YearMonth, beløp: List<Double>) ->
                        InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.SammenligningsgrunnlagDTO.InntekterFraAOrdningenDTO(
                            måned = måned,
                            sum = beløp.sum()
                        )
                    }
            )
        }
    }

    internal class NøkkeldataOmInntekt(
        val sisteDagISammenhengendePeriode: LocalDate,
        val skjæringstidspunkt: LocalDate,
        var avviksprosent: Double? = null
    )
}
