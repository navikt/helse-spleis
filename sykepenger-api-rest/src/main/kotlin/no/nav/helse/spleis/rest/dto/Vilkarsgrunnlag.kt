package no.nav.helse.spleis.rest.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = SpleisVilkarsgrunnlag::class, name = "SpleisVilkarsgrunnlag"),
    JsonSubTypes.Type(value = InfotrygdVilkarsgrunnlag::class, name = "InfotrygdVilkarsgrunnlag")
)
sealed interface Vilkarsgrunnlag {
    val id: UUID
    val skjaeringstidspunkt: LocalDate
    val omregnetArsinntekt: Double
    val sykepengegrunnlag: Double
    val inntekter: List<Arbeidsgiverinntekt>
    val arbeidsgiverrefusjoner: List<Arbeidsgiverrefusjon>
    val opptjeningsvurderingId: UUID
}

data class SpleisVilkarsgrunnlag(
    override val id: UUID,
    override val skjaeringstidspunkt: LocalDate,
    override val omregnetArsinntekt: Double,
    override val sykepengegrunnlag: Double,
    override val inntekter: List<Arbeidsgiverinntekt>,
    override val arbeidsgiverrefusjoner: List<Arbeidsgiverrefusjon>,
    val beregningsgrunnlag: Double,
    val grunnbelop: Int,
    val sykepengegrunnlagsgrense: Sykepengegrunnlagsgrense,
    val antallOpptjeningsdagerErMinst: Int,
    val opptjeningFra: LocalDate,
    val oppfyllerKravOmMinstelonn: Boolean,
    val oppfyllerKravOmOpptjening: Boolean,
    val oppfyllerKravOmMedlemskap: Boolean?,
    val forsikringsvurderingId: UUID?,
    override val opptjeningsvurderingId: UUID
) : Vilkarsgrunnlag {
    val skjonnsmessigFastsattAarlig: Double? =
        inntekter
            .filter { it.deaktivert != true }
            .mapNotNull { it.skjonnsmessigFastsatt }
            .takeIf(List<*>::isNotEmpty)
            ?.sumOf { it.belop }
}

data class InfotrygdVilkarsgrunnlag(
    override val id: UUID,
    override val skjaeringstidspunkt: LocalDate,
    override val omregnetArsinntekt: Double,
    override val sykepengegrunnlag: Double,
    override val arbeidsgiverrefusjoner: List<Arbeidsgiverrefusjon>,
    override val inntekter: List<Arbeidsgiverinntekt>,
    override val opptjeningsvurderingId: UUID
) : Vilkarsgrunnlag

data class Sykepengegrunnlagsgrense(
    val grunnbelop: Int,
    val grense: Int,
    val virkningstidspunkt: LocalDate
)

enum class Inntektskilde {
    Saksbehandler,
    Inntektsmelding,
    Infotrygd,
    AOrdningen,
    IkkeRapportert
}

data class InntekterFraAOrdningen(
    val maned: YearMonth,
    val sum: Double
)

data class SkjonnsmessigFastsatt(
    val belop: Double,
    val manedsbelop: Double
)

data class OmregnetArsinntekt(
    val kilde: Inntektskilde,
    val belop: Double,
    val manedsbelop: Double,
    val inntekterFraAOrdningen: List<InntekterFraAOrdningen>?
)

data class Arbeidsgiverinntekt(
    val arbeidsgiver: String,
    val omregnetArsinntekt: OmregnetArsinntekt,
    // TODO: speil bruker kun <beløp>, og verdien kunne altså vært forenklet til en <Double?>
    val skjonnsmessigFastsatt: SkjonnsmessigFastsatt?,
    val fom: LocalDate,
    val tom: LocalDate?,
    val deaktivert: Boolean? = null
)

data class Arbeidsgiverrefusjon(
    val arbeidsgiver: String,
    val refusjonsopplysninger: List<Refusjonselement>
)

data class Refusjonselement(
    val fom: LocalDate,
    val tom: LocalDate?,
    val belop: Double,
    val meldingsreferanseId: UUID
)
