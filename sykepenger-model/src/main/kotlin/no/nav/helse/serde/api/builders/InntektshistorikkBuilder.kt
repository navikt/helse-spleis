package no.nav.helse.serde.api.builders

import no.nav.helse.Grunnbeløp
import no.nav.helse.person.InntekthistorikkVisitor
import no.nav.helse.person.InntektshistorikkVol2
import no.nav.helse.person.Person
import no.nav.helse.serde.api.InntektsgrunnlagDTO
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class InntektshistorikkBuilder(private val person: Person) {
    private val inntektshistorikk = mutableMapOf<String, InntektshistorikkVol2>()
    private val nøkkeldataOmInntekter = mutableListOf<NøkkeldataOmInntekt>()

    internal fun inntektshistorikk(organisasjonsnummer: String, inntektshistorikkVol2: InntektshistorikkVol2) {
        inntektshistorikk[organisasjonsnummer] = inntektshistorikkVol2
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
        .map { nøkkeldata ->
            val sykepengegrunnlag =
                person.sykepengegrunnlag(nøkkeldata.skjæringstidspunkt, nøkkeldata.sisteDagISammenhengendePeriode)
            val grunnlagForSykepengegrunnlag =
                person.grunnlagForSykepengegrunnlag(nøkkeldata.skjæringstidspunkt, nøkkeldata.sisteDagISammenhengendePeriode)
            val sammenligningsgrunnlag = person.sammenligningsgrunnlag(nøkkeldata.skjæringstidspunkt)

            val arbeidsgiverinntekt: List<InntektsgrunnlagDTO.ArbeidsgiverinntektDTO> =
                inntektshistorikk.map { (orgnummer, inntekthist) ->
                    arbeidsgiverinntekt(
                        nøkkeldata.skjæringstidspunkt,
                        nøkkeldata.sisteDagISammenhengendePeriode,
                        orgnummer,
                        inntekthist
                    )
                }

            InntektsgrunnlagDTO(
                skjæringstidspunkt = nøkkeldata.skjæringstidspunkt,
                sykepengegrunnlag = sykepengegrunnlag?.reflection { årlig, _, _, _ -> årlig },
                omregnetÅrsinntekt = grunnlagForSykepengegrunnlag?.reflection { årlig, _, _, _ -> årlig },
                sammenligningsgrunnlag = sammenligningsgrunnlag?.reflection { årlig, _, _, _ -> årlig },
                avviksprosent = nøkkeldata.avviksprosent,
                maksUtbetalingPerDag = sykepengegrunnlag?.reflection { _, _, daglig, _ -> daglig },
                inntekter = arbeidsgiverinntekt,
                oppfyllerKravOmMinstelønn = sykepengegrunnlag?.let { it > person.minimumInntekt(nøkkeldata.skjæringstidspunkt) },
                grunnbeløp = (Grunnbeløp.`1G`
                    .beløp(nøkkeldata.skjæringstidspunkt, nøkkeldata.sisteDagISammenhengendePeriode)
                    .reflection { årlig, _, _, _ -> årlig })
                    .toInt(),
            )
        }

    private fun arbeidsgiverinntekt(
        skjæringstidspunkt: LocalDate,
        sisteDagISammenhengendePeriode: LocalDate,
        orgnummer: String,
        inntektshistorikk: InntektshistorikkVol2
    ): InntektsgrunnlagDTO.ArbeidsgiverinntektDTO {
        val omregnetÅrsinntektDTO = inntektshistorikk.grunnlagForSykepengegrunnlagMedMetadata(skjæringstidspunkt, sisteDagISammenhengendePeriode)
            ?.let { (inntektsopplysning, inntekt) ->
                OmregnetÅrsinntektVisitor(inntektsopplysning, inntekt).omregnetÅrsinntektDTO
            }
        val sammenligningsgrunnlagDTO = inntektshistorikk.grunnlagForSammenligningsgrunnlagMedMetadata(skjæringstidspunkt)
            ?.let { (sammenligningsgrunnlagsopplysning, sammenligningsgrunnlag) ->
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
        lateinit var omregnetÅrsinntektDTO: InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO
        private val skattegreier = mutableListOf<InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntekterFraAOrdningenDTO>()

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
            omregnetÅrsinntektDTO = InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO(
                kilde = InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.Saksbehandler,
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
            omregnetÅrsinntektDTO = InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO(
                kilde = InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.Inntektsmelding,
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
            omregnetÅrsinntektDTO = InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO(
                kilde = InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.Infotrygd,
                beløp = beløp.reflection { årlig, _, _, _ -> årlig },
                månedsbeløp = beløp.reflection { _, mnd, _, _ -> mnd }
            )
        }

        override fun preVisitSkatt(skattComposite: InntektshistorikkVol2.SkattComposite, id: UUID) {
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
                InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntekterFraAOrdningenDTO(
                    måned = måned,
                    sum = beløp.reflection { _, mnd, _, _ -> mnd }
                ))
        }

        override fun postVisitSkatt(skattComposite: InntektshistorikkVol2.SkattComposite, id: UUID) {
            omregnetÅrsinntektDTO = InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO(
                kilde = InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.AOrdningen,
                beløp = inntekt.reflection { årlig, _, _, _ -> årlig },
                månedsbeløp = inntekt.reflection { _, mnd, _, _ -> mnd },
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

    private class SammenligningsgrunnlagVisitor(
        inntektsopplysning: InntektshistorikkVol2.Inntektsopplysning,
        private val inntekt: Inntekt
    ) : InntekthistorikkVisitor {
        lateinit var sammenligningsgrunnlagDTO: InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.SammenligningsgrunnlagDTO
        private val skattegreier = mutableListOf<InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.SammenligningsgrunnlagDTO.InntekterFraAOrdningenDTO>()

        init {
            inntektsopplysning.accept(this)
        }

        override fun preVisitSkatt(skattComposite: InntektshistorikkVol2.SkattComposite, id: UUID) {
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
                InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.SammenligningsgrunnlagDTO.InntekterFraAOrdningenDTO(
                    måned = måned,
                    sum = beløp.reflection { _, mnd, _, _ -> mnd }
                ))
        }

        override fun postVisitSkatt(skattComposite: InntektshistorikkVol2.SkattComposite, id: UUID) {
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
