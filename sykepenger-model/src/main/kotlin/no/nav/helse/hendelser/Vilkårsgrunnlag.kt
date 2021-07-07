package no.nav.helse.hendelser

import no.nav.helse.person.*
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.util.*

class Vilkårsgrunnlag(
    meldingsreferanseId: UUID,
    private val vedtaksperiodeId: String,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val inntektsvurdering: Inntektsvurdering,
    private val opptjeningvurdering: Opptjeningvurdering,
    private val medlemskapsvurdering: Medlemskapsvurdering
) : ArbeidstakerHendelse(meldingsreferanseId) {
    private var grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata? = null

    internal fun erRelevant(other: UUID) = other.toString() == vedtaksperiodeId

    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = orgnummer

    internal fun valider(
        grunnlagForSykepengegrunnlag: Inntekt,
        sammenligningsgrunnlag: Inntekt,
        skjæringstidspunkt: LocalDate,
        periodetype: Periodetype,
        antallArbeidsgivereMedOverlappendeVedtaksperioder: Int
    ): IAktivitetslogg {
        val inntektsvurderingOk = inntektsvurdering.valider(
            this,
            grunnlagForSykepengegrunnlag,
            sammenligningsgrunnlag,
            periodetype,
            antallArbeidsgivereMedOverlappendeVedtaksperioder
        )
        val opptjeningvurderingOk = opptjeningvurdering.valider(this, skjæringstidspunkt)
        val medlemskapsvurderingOk = medlemskapsvurdering.valider(this, periodetype)
        val minimumInntekt = Alder(fødselsnummer).minimumInntekt(skjæringstidspunkt)
        val harMinimumInntekt = grunnlagForSykepengegrunnlag > minimumInntekt

        etterlevelse.`§8-3 ledd 2`(harMinimumInntekt, skjæringstidspunkt, grunnlagForSykepengegrunnlag, minimumInntekt)
        if (harMinimumInntekt) info("Krav til minste sykepengegrunnlag er oppfylt")
        else warn("Perioden er avslått på grunn av at inntekt er under krav til minste sykepengegrunnlag")

        grunnlagsdata = VilkårsgrunnlagHistorikk.Grunnlagsdata(
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            avviksprosent = inntektsvurdering.avviksprosent(),
            antallOpptjeningsdagerErMinst = opptjeningvurdering.antallOpptjeningsdager,
            harOpptjening = opptjeningvurdering.harOpptjening(),
            medlemskapstatus = medlemskapsvurdering.medlemskapstatus,
            harMinimumInntekt = harMinimumInntekt,
            vurdertOk = inntektsvurderingOk && opptjeningvurderingOk && medlemskapsvurderingOk && harMinimumInntekt,
            meldingsreferanseId = meldingsreferanseId()
        )
        return this
    }

    internal fun lagreInntekter(person: Person, skjæringstidspunkt: LocalDate) {
        inntektsvurdering.lagreInntekter(person, skjæringstidspunkt, this)
    }

    internal fun grunnlagsdata() = requireNotNull(grunnlagsdata) { "Må kalle valider() først" }

}
