package no.nav.helse.hendelser

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.Periode.Companion.sammenhengende
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Companion.beregnOpptjening
import no.nav.helse.person.Arbeidsforholdhistorikk
import no.nav.helse.person.Arbeidsforholdhistorikk.Companion.opptjening
import no.nav.helse.person.Opptjening
import no.nav.helse.person.Person
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.Companion.NullObserver
import no.nav.helse.person.inntekt.Sammenligningsgrunnlag
import no.nav.helse.person.inntekt.Sykepengegrunnlag

class Vilkårsgrunnlag(
    meldingsreferanseId: UUID,
    private val vedtaksperiodeId: String,
    aktørId: String,
    personidentifikator: Personidentifikator,
    orgnummer: String,
    private val inntektsvurdering: Inntektsvurdering,
    private val medlemskapsvurdering: Medlemskapsvurdering,
    private val inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag,
    arbeidsforhold: List<Arbeidsforhold>
) : ArbeidstakerHendelse(meldingsreferanseId, personidentifikator.toString(), aktørId, orgnummer) {
    private var grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata? = null
    private val arbeidsforhold = arbeidsforhold.filter { it.orgnummer.isNotBlank() }

    internal fun erRelevant(other: UUID) = other.toString() == vedtaksperiodeId

    internal fun avklarSykepengegrunnlag(person: Person, skjæringstidspunkt: LocalDate, subsumsjonObserver: SubsumsjonObserver): Sykepengegrunnlag {
        val opptjening = arbeidsforhold.beregnOpptjening(skjæringstidspunkt, NullObserver)
        return inntektsvurderingForSykepengegrunnlag.avklarSykepengegrunnlag(this, person, opptjening, skjæringstidspunkt, meldingsreferanseId(), subsumsjonObserver)
    }

    internal fun valider(
        grunnlagForSykepengegrunnlag: Sykepengegrunnlag,
        skjæringstidspunkt: LocalDate,
        subsumsjonObserver: SubsumsjonObserver
    ): IAktivitetslogg {
        val sammenligningsgrunnlag = sammenligningsgrunnlag(skjæringstidspunkt, subsumsjonObserver)

        val sykepengegrunnlagOk = grunnlagForSykepengegrunnlag.valider(this)
        inntektsvurderingForSykepengegrunnlag.valider(this)
        inntektsvurderingForSykepengegrunnlag.loggInteressantFrilanserInformasjon(skjæringstidspunkt)

        val opptjening = arbeidsforhold.beregnOpptjening(skjæringstidspunkt, subsumsjonObserver)
        val opptjeningvurderingOk = opptjening.valider(this)
        val medlemskapsvurderingOk = medlemskapsvurdering.valider(this)
        grunnlagsdata = VilkårsgrunnlagHistorikk.Grunnlagsdata(
            skjæringstidspunkt = skjæringstidspunkt,
            sykepengegrunnlag = grunnlagForSykepengegrunnlag,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            avviksprosent = grunnlagForSykepengegrunnlag.avviksprosent(
                sammenligningsgrunnlag,
                subsumsjonObserver
            ),
            opptjening = opptjening,
            medlemskapstatus = medlemskapsvurdering.medlemskapstatus,
            vurdertOk = sykepengegrunnlagOk && opptjeningvurderingOk && medlemskapsvurderingOk,
            meldingsreferanseId = meldingsreferanseId(),
            vilkårsgrunnlagId = UUID.randomUUID()
        )
        return this
    }


    internal fun grunnlagsdata() = requireNotNull(grunnlagsdata) { "Må kalle valider() først" }

    internal fun lagre(person: Person, skjæringstidspunkt: LocalDate) {
        val opptjening = arbeidsforhold.beregnOpptjening(skjæringstidspunkt, NullObserver)
        opptjening.lagreArbeidsforhold(person, this)
    }

    private fun sammenligningsgrunnlag(skjæringstidspunkt: LocalDate, subsumsjonObserver: SubsumsjonObserver): Sammenligningsgrunnlag {
        return inntektsvurdering.sammenligningsgrunnlag(skjæringstidspunkt, meldingsreferanseId(), subsumsjonObserver)
    }

    class Arbeidsforhold(
        internal val orgnummer: String,
        private val ansattFom: LocalDate,
        private val ansattTom: LocalDate? = null
    ) {
        internal fun tilDomeneobjekt() = Arbeidsforholdhistorikk.Arbeidsforhold(
            ansattFom = ansattFom,
            ansattTom = ansattTom,
            deaktivert = false
        )

        private fun erSøppel() =
            ansattTom != null && ansattTom < ansattFom

        internal fun erDelAvOpptjeningsperiode(opptjeningsperiode: Periode) = ansattFom in opptjeningsperiode

        private fun erGyldig(skjæringstidspunkt: LocalDate) =
            ansattFom < skjæringstidspunkt && !erSøppel()

        private fun periode(skjæringstidspunkt: LocalDate): Periode? {
            if (!erGyldig(skjæringstidspunkt)) return null
            return ansattFom til (ansattTom ?: skjæringstidspunkt)
        }

        internal companion object {
            internal fun Iterable<Arbeidsforhold>.opptjeningsperiode(skjæringstidspunkt: LocalDate) = this
                .mapNotNull { it.periode(skjæringstidspunkt) }
                .sammenhengende(skjæringstidspunkt)

            internal fun List<Arbeidsforhold>.beregnOpptjening(
                skjæringstidspunkt: LocalDate,
                subsumsjonObserver: SubsumsjonObserver
            ): Opptjening {
                val opptjeningsperiode = this.opptjeningsperiode(skjæringstidspunkt)
                return this
                    .filter { it.erDelAvOpptjeningsperiode(opptjeningsperiode) }
                    .groupBy({ it.orgnummer }) { it.tilDomeneobjekt() }
                    .opptjening(skjæringstidspunkt, subsumsjonObserver)
            }
        }
    }
}
