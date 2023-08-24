package no.nav.helse.hendelser

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.Periode.Companion.sammenhengende
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Companion.opptjening
import no.nav.helse.person.Opptjening
import no.nav.helse.person.Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Companion.opptjening
import no.nav.helse.person.Person
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.inntekt.Sykepengegrunnlag

class Vilkårsgrunnlag(
    meldingsreferanseId: UUID,
    private val vedtaksperiodeId: String,
    private val skjæringstidspunkt: LocalDate,
    aktørId: String,
    personidentifikator: Personidentifikator,
    orgnummer: String,
    private val inntektsvurdering: Inntektsvurdering,
    private val medlemskapsvurdering: Medlemskapsvurdering,
    private val inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag,
    private val arbeidsforhold: List<Arbeidsforhold>
) : ArbeidstakerHendelse(meldingsreferanseId, personidentifikator.toString(), aktørId, orgnummer) {
    private var grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata? = null
    private val opptjening = arbeidsforhold.opptjening(skjæringstidspunkt)

    internal fun erRelevant(other: UUID, skjæringstidspunktVedtaksperiode: LocalDate): Boolean {
        if (other.toString() != vedtaksperiodeId) return false
        if (skjæringstidspunktVedtaksperiode == skjæringstidspunkt) return true
        info("Vilkårsgrunnlag var relevant for Vedtaksperiode, men skjæringstidspunktene var ulikte: [$skjæringstidspunkt, $skjæringstidspunktVedtaksperiode]")
        return false
    }

    internal fun avklarSykepengegrunnlag(person: Person, subsumsjonObserver: SubsumsjonObserver): Sykepengegrunnlag {
        val sammenligningsgrunnlag = inntektsvurdering.sammenligningsgrunnlag(skjæringstidspunkt, meldingsreferanseId(), subsumsjonObserver)
        return inntektsvurderingForSykepengegrunnlag.avklarSykepengegrunnlag(this, person, opptjening, skjæringstidspunkt, sammenligningsgrunnlag, meldingsreferanseId(), subsumsjonObserver)
    }

    internal fun valider(sykepengegrunnlag: Sykepengegrunnlag, subsumsjonObserver: SubsumsjonObserver): IAktivitetslogg {
        val sykepengegrunnlagOk = sykepengegrunnlag.valider(this)
        inntektsvurderingForSykepengegrunnlag.valider(this)
        inntektsvurderingForSykepengegrunnlag.loggInteressantFrilanserInformasjon(skjæringstidspunkt)
        arbeidsforhold.forEach { it.loggFrilans(this, skjæringstidspunkt, arbeidsforhold) }
        val opptjening = opptjening.opptjening(skjæringstidspunkt, subsumsjonObserver)
        val opptjeningvurderingOk = opptjening.valider(this)
        val medlemskapsvurderingOk = medlemskapsvurdering.valider(this)
        grunnlagsdata = VilkårsgrunnlagHistorikk.Grunnlagsdata(
            skjæringstidspunkt = skjæringstidspunkt,
            sykepengegrunnlag = sykepengegrunnlag,
            opptjening = opptjening,
            medlemskapstatus = medlemskapsvurdering.medlemskapstatus,
            vurdertOk = sykepengegrunnlagOk && opptjeningvurderingOk && medlemskapsvurderingOk,
            meldingsreferanseId = meldingsreferanseId(),
            vilkårsgrunnlagId = UUID.randomUUID()
        )
        return this
    }


    internal fun grunnlagsdata() = requireNotNull(grunnlagsdata) { "Må kalle valider() først" }

    class Arbeidsforhold(
        private val orgnummer: String,
        private val ansettelseperiode: Periode,
        private val type: Arbeidsforholdtype
    ) {
        enum class Arbeidsforholdtype {
            FORENKLET_OPPGJØRSORDNING,
            FRILANSER,
            MARITIMT,
            ORDINÆRT
        }

        constructor(orgnummer: String, ansattFom: LocalDate, ansattTom: LocalDate? = null, type: Arbeidsforholdtype) : this(orgnummer, ansattFom til (ansattTom ?: LocalDate.MAX), type)

        init {
            check(orgnummer.isNotBlank())
        }

        fun loggFrilans(aktivitetslogg: IAktivitetslogg, skjæringstidspunkt: LocalDate, andre: List<Arbeidsforhold>) {
            if (type != Arbeidsforholdtype.FRILANSER) return
            if (skjæringstidspunkt !in ansettelseperiode) return
            aktivitetslogg.info("Vedkommende har et aktivt frilansoppdrag på skjæringstidspunktet")

            if (andre.count { it.orgnummer == this.orgnummer && it.ansettelseperiode.overlapperMed(this.ansettelseperiode) } > 1) {
                aktivitetslogg.info("Vedkommende har andre overlappende arbeidsforhold i samme virksomhet hvor vedkommende har frilansoppdrag")
            }
        }

        internal fun tilDomeneobjekt() = Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Arbeidsforhold(
            ansattFom = ansettelseperiode.start,
            ansattTom = ansettelseperiode.endInclusive.takeUnless { it == LocalDate.MAX },
            deaktivert = false
        )

        private fun kanBrukes() = type != Arbeidsforholdtype.FRILANSER // filtrerer ut frilans-arbeidsforhold enn så lenge

        internal fun erDelAvOpptjeningsperiode(opptjeningsperiode: Periode) = kanBrukes() && ansettelseperiode.overlapperMed(opptjeningsperiode)

        private fun periode(skjæringstidspunkt: LocalDate): Periode? {
            if (!kanBrukes()) return null
            val opptjeningsperiode = LocalDate.MIN til skjæringstidspunkt.forrigeDag
            if (ansettelseperiode.starterEtter(opptjeningsperiode)) return null
            return ansettelseperiode.subset(opptjeningsperiode)
        }

        internal companion object {
            private fun Iterable<Arbeidsforhold>.opptjeningsperiode(skjæringstidspunkt: LocalDate) = this
                .mapNotNull { it.periode(skjæringstidspunkt) }
                .sammenhengende(skjæringstidspunkt.forrigeDag)

            internal fun Iterable<Arbeidsforhold>.opptjening(skjæringstidspunkt: LocalDate) = opptjeningsperiode(skjæringstidspunkt).let { opptjeningsperiode ->
                this
                    .filter { it.erDelAvOpptjeningsperiode(opptjeningsperiode) }
                    .groupBy({ it.orgnummer }) { it.tilDomeneobjekt() }
            }
        }
    }
}
