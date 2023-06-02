package no.nav.helse.serde.api.dto

import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.serde.api.speil.builders.SykepengegrunnlagsgrenseDTO

interface Vilkårsgrunnlag {
    val skjæringstidspunkt: LocalDate
    val beregningsgrunnlag: Double
    val sammenligningsgrunnlag: Double?
    val sykepengegrunnlag: Double
    val inntekter: List<Arbeidsgiverinntekt>
    val arbeidsgiverrefusjoner: List<Arbeidsgiverrefusjon>
    val vilkårsgrunnlagtype: Vilkårsgrunnlagtype
}

enum class Vilkårsgrunnlagtype {
    INFOTRYGD,
    SPLEIS
}

data class SpleisVilkårsgrunnlag(
    override val skjæringstidspunkt: LocalDate,
    override val beregningsgrunnlag: Double,
    override val sammenligningsgrunnlag: Double,
    override val sykepengegrunnlag: Double,
    override val inntekter: List<Arbeidsgiverinntekt>,
    override val arbeidsgiverrefusjoner: List<Arbeidsgiverrefusjon>,
    val avviksprosent: Double?,
    val grunnbeløp: Int,
    val sykepengegrunnlagsgrense: SykepengegrunnlagsgrenseDTO,
    val antallOpptjeningsdagerErMinst: Int,
    val opptjeningFra: LocalDate,
    val oppfyllerKravOmMinstelønn: Boolean,
    val oppfyllerKravOmOpptjening: Boolean,
    val oppfyllerKravOmMedlemskap: Boolean?
) : Vilkårsgrunnlag {
    override val vilkårsgrunnlagtype = Vilkårsgrunnlagtype.SPLEIS
}

data class InfotrygdVilkårsgrunnlag(
    override val skjæringstidspunkt: LocalDate,
    override val beregningsgrunnlag: Double,
    override val sammenligningsgrunnlag: Double?,
    override val sykepengegrunnlag: Double,
    override val inntekter: List<Arbeidsgiverinntekt>,
    override val arbeidsgiverrefusjoner: List<Arbeidsgiverrefusjon>
) : Vilkårsgrunnlag {
    override val vilkårsgrunnlagtype = Vilkårsgrunnlagtype.INFOTRYGD
}

data class Arbeidsgiverinntekt(
    val organisasjonsnummer: String,
    val omregnetÅrsinntekt: OmregnetÅrsinntekt?,
    val sammenligningsgrunnlag: Double? = null,
    val deaktivert: Boolean
)

data class Arbeidsgiverrefusjon(
    val arbeidsgiver: String,
    val refusjonsopplysninger: List<Refusjonselement>
)

data class Refusjonselement(
    val fom: LocalDate,
    val tom: LocalDate?,
    val beløp: Double,
    val meldingsreferanseId: UUID
)

enum class Inntektkilde {
    Saksbehandler, Inntektsmelding, Infotrygd, AOrdningen, IkkeRapportert, SkjønnsmessigFastsatt
}

data class OmregnetÅrsinntekt(
    val kilde: Inntektkilde,
    val beløp: Double,
    val månedsbeløp: Double,
    val inntekterFraAOrdningen: List<InntekterFraAOrdningen>? = null //kun gyldig for A-ordningen
)

data class InntekterFraAOrdningen(
    val måned: YearMonth,
    val sum: Double
)
