package no.nav.helse.inspectors.view

import java.util.UUID
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.ArbeidstakerFaktaavklartInntekt
import no.nav.helse.person.inntekt.Inntektsdata
import no.nav.helse.person.inntekt.Inntektsgrunnlag
import no.nav.helse.person.inntekt.Inntektshistorikk
import no.nav.helse.person.inntekt.Inntektsmeldinginntekt
import no.nav.helse.person.inntekt.Saksbehandler
import no.nav.helse.person.inntekt.SelvstendigFaktaavklartInntekt
import no.nav.helse.person.inntekt.SelvstendigInntektsopplysning
import no.nav.helse.økonomi.Inntekt

internal sealed interface FaktaavklartInntektView {
    val hendelseId: UUID
    val beløp: Inntekt
}

internal data class ArbeistakerFaktaavklartInntektView(override val hendelseId: UUID, override val beløp: Inntekt) : FaktaavklartInntektView

internal data class SelvstendigFaktaavklartInntektView(override val hendelseId: UUID, override val beløp: Inntekt) : FaktaavklartInntektView

internal class SaksbehandlerView(val hendelseId: UUID, val beløp: Inntekt)

internal data class InntektsmeldinginntektView(
    val id: UUID,
    val inntektsdata: Inntektsdata
)

internal data class InntektshistorikkView(val inntekter: List<InntektsmeldinginntektView>)

internal data class InntektsgrunnlagView(
    val sykepengegrunnlag: Inntekt,
    val omregnetÅrsinntekt: Inntekt,
    val beregningsgrunnlag: Inntekt,
    val `6G`: Inntekt,
    val begrensning: Inntektsgrunnlag.Begrensning,
    val vurdertInfotrygd: Boolean,
    val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
    val selvstendigInntektsopplysning: SelvstendigInntektsopplysning?,
    val deaktiverteArbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
    val deaktiverteArbeidsforhold: List<String>
)

internal fun ArbeidstakerFaktaavklartInntekt.view() = ArbeistakerFaktaavklartInntektView(inntektsdata.hendelseId.id, inntektsdata.beløp)

internal fun SelvstendigFaktaavklartInntekt.view() = SelvstendigFaktaavklartInntektView(inntektsdata.hendelseId.id, normalinntekt)

internal fun Saksbehandler.view() = SaksbehandlerView(inntektsdata.hendelseId.id, inntektsdata.beløp)

internal fun Inntektsmeldinginntekt.view() = InntektsmeldinginntektView(
    id = id,
    inntektsdata = inntektsdata
)

internal fun Inntektshistorikk.view() = InntektshistorikkView(
    inntekter = historikk.map { it.view() }
)

internal fun Inntektsgrunnlag.view() = InntektsgrunnlagView(
    sykepengegrunnlag = sykepengegrunnlag,
    omregnetÅrsinntekt = omregnetÅrsinntekt,
    beregningsgrunnlag = beregningsgrunnlag,
    `6G` = `6G`,
    begrensning = begrensning,
    vurdertInfotrygd = vurdertInfotrygd,
    arbeidsgiverInntektsopplysninger = arbeidsgiverInntektsopplysninger,
    selvstendigInntektsopplysning = selvstendigInntektsopplysning,
    deaktiverteArbeidsgiverInntektsopplysninger = deaktiverteArbeidsforhold,
    deaktiverteArbeidsforhold = deaktiverteArbeidsforhold.map { it.orgnummer }
)
