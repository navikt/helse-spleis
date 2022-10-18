package no.nav.helse.hendelser

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.Periode.Companion.sammenhengende
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Companion.beregnOpptjening
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Companion.grupperArbeidsforholdPerOrgnummer
import no.nav.helse.person.Arbeidsforholdhistorikk
import no.nav.helse.person.Arbeidsforholdhistorikk.Companion.opptjening
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Opptjening
import no.nav.helse.person.Person
import no.nav.helse.person.Sammenligningsgrunnlag
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.Varselkode.RV_VV_1
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.Companion.NullObserver
import org.slf4j.LoggerFactory

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
    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    private var grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata? = null
    private val arbeidsforhold = arbeidsforhold.filter { it.orgnummer.isNotBlank() }

    internal fun erRelevant(other: UUID) = other.toString() == vedtaksperiodeId

    internal fun valider(
        grunnlagForSykepengegrunnlag: Sykepengegrunnlag,
        sammenligningsgrunnlag: Sammenligningsgrunnlag,
        skjæringstidspunkt: LocalDate,
        antallArbeidsgivereFraAareg: Int,
        subsumsjonObserver: SubsumsjonObserver
    ): IAktivitetslogg {
        val sykepengegrunnlagOk = grunnlagForSykepengegrunnlag.valider(this)
        inntektsvurderingForSykepengegrunnlag.valider(this)
        inntektsvurderingForSykepengegrunnlag.loggInteressantFrilanserInformasjon(skjæringstidspunkt)

        val opptjening = arbeidsforhold.beregnOpptjening(skjæringstidspunkt, subsumsjonObserver)
        val inntektsvurderingOk = inntektsvurdering.valider(this, antallArbeidsgivereFraAareg)
        val opptjeningvurderingOk = opptjening.valider(this)
        val medlemskapsvurderingOk = medlemskapsvurdering.valider(this)
        grunnlagsdata = VilkårsgrunnlagHistorikk.Grunnlagsdata(
            skjæringstidspunkt = skjæringstidspunkt,
            sykepengegrunnlag = grunnlagForSykepengegrunnlag,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            avviksprosent = grunnlagForSykepengegrunnlag.avviksprosent(sammenligningsgrunnlag.sammenligningsgrunnlag, subsumsjonObserver),
            opptjening = opptjening,
            medlemskapstatus = medlemskapsvurdering.medlemskapstatus,
            vurdertOk = sykepengegrunnlagOk && inntektsvurderingOk && opptjeningvurderingOk && medlemskapsvurderingOk,
            meldingsreferanseId = meldingsreferanseId(),
            vilkårsgrunnlagId = UUID.randomUUID()
        )
        return this
    }


    internal fun grunnlagsdata() = requireNotNull(grunnlagsdata) { "Må kalle valider() først" }

    internal fun lagre(person: Person, skjæringstidspunkt: LocalDate, subsumsjonObserver: SubsumsjonObserver) {
        arbeidsforhold.beregnOpptjening(skjæringstidspunkt, NullObserver)
            .lagreArbeidsforhold(person, this)
        lagreSkatteinntekter(person, skjæringstidspunkt)
        lagreRapporterteInntekter(person, skjæringstidspunkt)
        loggUkjenteArbeidsforhold(person, skjæringstidspunkt)
        if (person.harVedtaksperiodeForArbeidsgiverMedUkjentArbeidsforhold(skjæringstidspunkt)) {
            varsel(RV_VV_1)
        }
    }
    private fun lagreRapporterteInntekter(person: Person, skjæringstidspunkt: LocalDate) {
        inntektsvurdering.lagreRapporterteInntekter(person, skjæringstidspunkt, this)
    }

    private fun lagreSkatteinntekter(person: Person, skjæringstidspunkt: LocalDate) {
        inntektsvurderingForSykepengegrunnlag.lagreOmregnetÅrsinntekter(person, skjæringstidspunkt, this)
    }

    private fun loggUkjenteArbeidsforhold(person: Person, skjæringstidspunkt: LocalDate) {
        val arbeidsforholdForSkjæringstidspunkt = arbeidsforhold.filter { it.gjelder(skjæringstidspunkt) }
        val harFlereRelevanteArbeidsforhold = arbeidsforholdForSkjæringstidspunkt.grupperArbeidsforholdPerOrgnummer().keys.size > 1
        if (harFlereRelevanteArbeidsforhold && arbeidsforholdForSkjæringstidspunkt.any { it.harArbeidetMindreEnnTreMåneder(skjæringstidspunkt) }) {
            sikkerlogg.info("Person har flere arbeidsgivere og et relevant arbeidsforhold som har vart mindre enn 3 måneder (8-28b) - fødselsnummer: $fødselsnummer")
        }
        person.brukOuijaBrettForÅKommunisereMedPotensielleSpøkelser(arbeidsforholdForSkjæringstidspunkt.map(Arbeidsforhold::orgnummer), skjæringstidspunkt)
        person.loggUkjenteOrgnummere(arbeidsforhold.map { it.orgnummer })
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

        internal fun gjelder(skjæringstidspunkt: LocalDate) = ansattFom <= skjæringstidspunkt && (ansattTom == null || ansattTom >= skjæringstidspunkt)

        internal fun harArbeidetMindreEnnTreMåneder(skjæringstidspunkt: LocalDate) = ansattFom > skjæringstidspunkt.withDayOfMonth(1).minusMonths(3)

        internal companion object {
            internal fun List<Arbeidsforhold>.grupperArbeidsforholdPerOrgnummer() = this
                .groupBy { it.orgnummer }
            internal fun Iterable<Arbeidsforhold>.opptjeningsperiode(skjæringstidspunkt: LocalDate) = this
                .mapNotNull { it.periode(skjæringstidspunkt) }
                .sammenhengende(skjæringstidspunkt)

            internal fun List<Arbeidsforhold>.beregnOpptjening(skjæringstidspunkt: LocalDate, subsumsjonObserver: SubsumsjonObserver): Opptjening {
                val opptjeningsperiode = this.opptjeningsperiode(skjæringstidspunkt)
                return this
                    .filter { it.erDelAvOpptjeningsperiode(opptjeningsperiode) }
                    .groupBy({ it.orgnummer }) { it.tilDomeneobjekt() }
                    .opptjening(skjæringstidspunkt, subsumsjonObserver)
            }
        }
    }
}
