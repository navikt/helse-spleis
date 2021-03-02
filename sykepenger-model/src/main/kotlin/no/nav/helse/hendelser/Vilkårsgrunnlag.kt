package no.nav.helse.hendelser

import no.nav.helse.person.*
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent
import java.time.LocalDate
import java.util.*

class Vilkårsgrunnlag(
    meldingsreferanseId: UUID,
    internal val vedtaksperiodeId: String,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val inntektsvurdering: Inntektsvurdering,
    private val opptjeningvurdering: Opptjeningvurdering,
    private val medlemskapsvurdering: Medlemskapsvurdering
    ) : ArbeidstakerHendelse(meldingsreferanseId) {
    private var grunnlagsdata: Grunnlagsdata? = null

    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = orgnummer

    internal fun valider(
        grunnlagForSykepengegrunnlag: Inntekt,
        sammenligningsgrunnlag: Inntekt,
        skjæringstidspunkt: LocalDate,
        periodetype: Periodetype
    ): IAktivitetslogg {
        inntektsvurdering.valider(this, grunnlagForSykepengegrunnlag, sammenligningsgrunnlag, periodetype)
        opptjeningvurdering.valider(this, orgnummer, skjæringstidspunkt)
        medlemskapsvurdering.valider(this, periodetype)
        grunnlagsdata = Grunnlagsdata(
            beregnetÅrsinntektFraInntektskomponenten = sammenligningsgrunnlag,
            avviksprosent = inntektsvurdering.avviksprosent(),
            antallOpptjeningsdagerErMinst = opptjeningvurdering.opptjeningsdager(orgnummer),
            harOpptjening = opptjeningvurdering.harOpptjening(orgnummer),
            medlemskapstatus = medlemskapsvurdering.medlemskapstatus
        )
        return this
    }

    internal fun lagreInntekter(person: Person, skjæringstidspunkt: LocalDate) {
        inntektsvurdering.lagreInntekter(person, skjæringstidspunkt, this)
    }

    internal fun grunnlagsdata() = requireNotNull(grunnlagsdata) { "Må kalle valider() først" }

    internal class Grunnlagsdata (
        internal val beregnetÅrsinntektFraInntektskomponenten: Inntekt,
        internal val avviksprosent: Prosent?,
        internal val antallOpptjeningsdagerErMinst: Int,
        internal val harOpptjening: Boolean,
        internal val medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus
    ) : VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement {

        override fun valider(aktivitetslogg: Aktivitetslogg) {
        }

        override fun isOk() = false

        override fun accept(personVisitor: PersonVisitor) {
        }
    }
}
