package no.nav.helse.serde.api.builders

import no.nav.helse.Grunnbeløp
import no.nav.helse.person.*
import no.nav.helse.serde.api.InntektsgrunnlagDTO
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class InntektshistorikkBuilder(private val person: Person) {
    private val inntektshistorikk = mutableMapOf<String, Inntektshistorikk>()
    private val nøkkeldataOmInntekter = mutableListOf<NøkkeldataOmInntekt>()

    internal fun inntektshistorikk(organisasjonsnummer: String, inntektshistorikk: Inntektshistorikk) {
        this.inntektshistorikk[organisasjonsnummer] = inntektshistorikk
    }

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
            val sykepengegrunnlag = vilkårsgrunnlag.sykepengegrunnlag()
            val grunnlagForSykepengegrunnlag = vilkårsgrunnlag.grunnlagForSykepengegrunnlag()
            val sammenligningsgrunnlag = vilkårsgrunnlag.sammenligningsgrunnlag()

            val arbeidsgiverinntekt: List<InntektsgrunnlagDTO.ArbeidsgiverinntektDTO> =
                ArbeidsgiverInntektopplysningerVisitor(vilkårsgrunnlag, nøkkeldata.skjæringstidspunkt).arbeidsgiverInntektDTO()

            val arbeidsgivereMedBareSammenligningsgrunnlag = inntektshistorikk
                .filterKeys { it !in arbeidsgiverinntekt.map { it.arbeidsgiver } }
                .map { (orgnummer, inntektshistorikk) -> arbeidsgiverinntekt(nøkkeldata.skjæringstidspunkt, orgnummer, inntektshistorikk) }
                .filter { person.harAktivtArbeidsforholdEllerInntekt(nøkkeldata.skjæringstidspunkt, it.arbeidsgiver) } // TODO: bruke vilkårsgrunnlaget

            InntektsgrunnlagDTO(
                skjæringstidspunkt = nøkkeldata.skjæringstidspunkt,
                sykepengegrunnlag = sykepengegrunnlag.reflection { årlig, _, _, _ -> årlig },
                omregnetÅrsinntekt = grunnlagForSykepengegrunnlag.reflection { årlig, _, _, _ -> årlig },
                sammenligningsgrunnlag = sammenligningsgrunnlag?.reflection { årlig, _, _, _ -> årlig },
                avviksprosent = nøkkeldata.avviksprosent,
                maksUtbetalingPerDag = sykepengegrunnlag.reflection { _, _, daglig, _ -> daglig },
                inntekter = arbeidsgiverinntekt + arbeidsgivereMedBareSammenligningsgrunnlag, // TODO: lagre sammenligningsgrunnlaget i stedet for å hente de ut fra inntektshistorikk
                oppfyllerKravOmMinstelønn = sykepengegrunnlag > person.minimumInntekt(nøkkeldata.skjæringstidspunkt),
                grunnbeløp = (Grunnbeløp.`1G`
                    .beløp(nøkkeldata.skjæringstidspunkt, nøkkeldata.sisteDagISammenhengendePeriode)
                    .reflection { årlig, _, _, _ -> årlig })
                    .toInt(),
            )
        }

    private fun arbeidsgiverinntekt(
        skjæringstidspunkt: LocalDate,
        orgnummer: String,
        inntektshistorikk: Inntektshistorikk
    ): InntektsgrunnlagDTO.ArbeidsgiverinntektDTO {
        val sammenligningsgrunnlagDTO = inntektshistorikk.grunnlagForSammenligningsgrunnlag(skjæringstidspunkt)
            ?.let { sammenligningsgrunnlagsopplysning ->
                SammenligningsgrunnlagVisitor(sammenligningsgrunnlagsopplysning, sammenligningsgrunnlagsopplysning.grunnlagForSammenligningsgrunnlag()).sammenligningsgrunnlagDTO
            }
        return InntektsgrunnlagDTO.ArbeidsgiverinntektDTO(
            orgnummer,
            null,
            sammenligningsgrunnlagDTO
        )
    }

    private inner class ArbeidsgiverInntektopplysningerVisitor(
        vilkårsgrunnlag: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement,
        private val skjæringstidspunkt: LocalDate
    ) :
        VilkårsgrunnlagHistorikkVisitor {
        private val arbeidsgiverInntektDTO = mutableListOf<InntektsgrunnlagDTO.ArbeidsgiverinntektDTO>()
        private val skattegreier = mutableListOf<InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntekterFraAOrdningenDTO>()

        private lateinit var orgnummer: String
        lateinit var omregnetÅrsinntektDTO: InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO

        fun arbeidsgiverInntektDTO() = arbeidsgiverInntektDTO

        init {
            vilkårsgrunnlag.accept(skjæringstidspunkt, this)
        }

        override fun preVisitArbeidsgiverInntektsopplysning(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning, orgnummer: String) {
            this.orgnummer = orgnummer
        }

        override fun postVisitArbeidsgiverInntektsopplysning(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning, orgnummer: String) {
            val sammenligningsgrunnlagDTO =
                inntektshistorikk[orgnummer]?.grunnlagForSammenligningsgrunnlag(skjæringstidspunkt)
                    ?.let { sammenligningsgrunnlagsopplysning ->
                        SammenligningsgrunnlagVisitor(sammenligningsgrunnlagsopplysning, sammenligningsgrunnlagsopplysning.grunnlagForSammenligningsgrunnlag()).sammenligningsgrunnlagDTO
                    }

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
        inntektsopplysning: Inntektshistorikk.Inntektsopplysning,
        private val inntekt: Inntekt
    ) : InntekthistorikkVisitor {
        lateinit var sammenligningsgrunnlagDTO: InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.SammenligningsgrunnlagDTO
        private val skattegreier = mutableListOf<InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.SammenligningsgrunnlagDTO.InntekterFraAOrdningenDTO>()

        init {
            inntektsopplysning.accept(this)
        }

        override fun preVisitSkatt(skattComposite: Inntektshistorikk.SkattComposite, id: UUID, dato: LocalDate) {
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

        override fun postVisitSkatt(skattComposite: Inntektshistorikk.SkattComposite, id: UUID, dato: LocalDate) {
            sammenligningsgrunnlagDTO = InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.SammenligningsgrunnlagDTO(
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
