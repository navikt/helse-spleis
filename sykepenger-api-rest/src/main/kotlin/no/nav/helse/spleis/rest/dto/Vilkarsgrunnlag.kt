package no.nav.helse.spleis.rest.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ApiSpleisVilkarsgrunnlag::class, name = "SpleisVilkarsgrunnlag"),
    JsonSubTypes.Type(value = ApiInfotrygdVilkarsgrunnlag::class, name = "InfotrygdVilkarsgrunnlag")
)
sealed interface ApiVilkarsgrunnlag {
    val id: UUID
    val skjaeringstidspunkt: LocalDate
    val omregnetArsinntekt: Double
    val sykepengegrunnlag: Double
    val inntekter: List<ApiArbeidsgiverinntekt>
    val arbeidsgiverrefusjoner: List<ApiArbeidsgiverrefusjon>
    val opptjeningsvurderingId: UUID
}

data class ApiSpleisVilkarsgrunnlag(
    override val id: UUID,
    override val skjaeringstidspunkt: LocalDate,
    override val omregnetArsinntekt: Double,
    override val sykepengegrunnlag: Double,
    override val inntekter: List<ApiArbeidsgiverinntekt>,
    override val arbeidsgiverrefusjoner: List<ApiArbeidsgiverrefusjon>,
    val beregningsgrunnlag: Double,
    val grunnbelop: Int,
    val sykepengegrunnlagsgrense: ApiSykepengegrunnlagsgrense,
    val antallOpptjeningsdagerErMinst: Int,
    val opptjeningFra: LocalDate,
    val oppfyllerKravOmMinstelonn: Boolean,
    val oppfyllerKravOmOpptjening: Boolean,
    val oppfyllerKravOmMedlemskap: Boolean?,
    val forsikringsvurderingId: UUID?,
    override val opptjeningsvurderingId: UUID,
    val skjonnsmessigFastsattAarlig: Double?
) : ApiVilkarsgrunnlag

data class ApiInfotrygdVilkarsgrunnlag(
    override val id: UUID,
    override val skjaeringstidspunkt: LocalDate,
    override val omregnetArsinntekt: Double,
    override val sykepengegrunnlag: Double,
    override val arbeidsgiverrefusjoner: List<ApiArbeidsgiverrefusjon>,
    override val inntekter: List<ApiArbeidsgiverinntekt>,
    override val opptjeningsvurderingId: UUID
) : ApiVilkarsgrunnlag

data class ApiSykepengegrunnlagsgrense(
    val grunnbelop: Int,
    val grense: Int,
    val virkningstidspunkt: LocalDate
)

enum class ApiInntektskilde {
    Saksbehandler,
    Inntektsmelding,
    Infotrygd,
    AOrdningen,
    IkkeRapportert
}

data class ApiInntekterFraAOrdningen(
    val maned: YearMonth,
    val sum: Double
)

data class ApiSkjonnsmessigFastsatt(
    val belop: Double,
    val manedsbelop: Double
)

data class ApiOmregnetArsinntekt(
    val kilde: ApiInntektskilde,
    val belop: Double,
    val manedsbelop: Double,
    val inntekterFraAOrdningen: List<ApiInntekterFraAOrdningen>?
)

data class ApiArbeidsgiverinntekt(
    val arbeidsgiver: String,
    val omregnetArsinntekt: ApiOmregnetArsinntekt,
    // TODO: speil bruker kun <beløp>, og verdien kunne altså vært forenklet til en <Double?>
    val skjonnsmessigFastsatt: ApiSkjonnsmessigFastsatt?,
    val fom: LocalDate,
    val tom: LocalDate?,
    val deaktivert: Boolean? = null
)

data class ApiArbeidsgiverrefusjon(
    val arbeidsgiver: String,
    val refusjonsopplysninger: List<ApiRefusjonselement>
)

data class ApiRefusjonselement(
    val fom: LocalDate,
    val tom: LocalDate?,
    val belop: Double,
    val meldingsreferanseId: UUID
)
