package no.nav.helse.hendelser

import no.nav.helse.Fødselsnummer
import no.nav.helse.hendelser.Arbeidsforhold.Companion.grupperArbeidsforholdPerOrgnummer
import no.nav.helse.person.*
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

class Vilkårsgrunnlag(
    meldingsreferanseId: UUID,
    private val vedtaksperiodeId: String,
    private val aktørId: String,
    private val fødselsnummer: Fødselsnummer,
    private val orgnummer: String,
    private val inntektsvurdering: Inntektsvurdering,
    private val opptjeningvurdering: Opptjeningvurdering,
    private val medlemskapsvurdering: Medlemskapsvurdering,
    private val inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag,
    arbeidsforhold: List<Arbeidsforhold>
) : ArbeidstakerHendelse(meldingsreferanseId) {
    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    private var grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata? = null
    private val arbeidsforhold = arbeidsforhold.filter { it.orgnummer.isNotBlank() }


    internal fun erRelevant(other: UUID) = other.toString() == vedtaksperiodeId

    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer.toString()
    override fun organisasjonsnummer() = orgnummer

    internal fun valider(
        grunnlagForSykepengegrunnlag: Sykepengegrunnlag,
        sammenligningsgrunnlag: Sammenligningsgrunnlag,
        skjæringstidspunkt: LocalDate,
        antallArbeidsgivereFraAareg: Int,
        periodetype: Periodetype
    ): IAktivitetslogg {
        if (grunnlagForSykepengegrunnlag.inntektsopplysningPerArbeidsgiver().values.all { it is Inntektshistorikk.SkattComposite }) {
            error("Bruker mangler nødvendig inntekt ved validering av Vilkårsgrunnlag")
        }
        inntektsvurderingForSykepengegrunnlag.valider(this)
        inntektsvurderingForSykepengegrunnlag.loggInteressantFrilanserInformasjon(skjæringstidspunkt)

        val inntektsvurderingOk = inntektsvurdering.valider(
            this,
            grunnlagForSykepengegrunnlag,
            sammenligningsgrunnlag.sammenligningsgrunnlag,
            antallArbeidsgivereFraAareg
        )
        val opptjeningvurderingOk = opptjeningvurdering.valider(this, skjæringstidspunkt)
        val medlemskapsvurderingOk = medlemskapsvurdering.valider(this, periodetype)
        val minimumInntektvurderingOk = validerMinimumInntekt(this, fødselsnummer, skjæringstidspunkt, grunnlagForSykepengegrunnlag)

        grunnlagsdata = VilkårsgrunnlagHistorikk.Grunnlagsdata(
            skjæringstidspunkt = skjæringstidspunkt,
            sykepengegrunnlag = grunnlagForSykepengegrunnlag,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            avviksprosent = inntektsvurdering.avviksprosent(),
            antallOpptjeningsdagerErMinst = opptjeningvurdering.antallOpptjeningsdager,
            harOpptjening = opptjeningvurdering.harOpptjening(),
            medlemskapstatus = medlemskapsvurdering.medlemskapstatus,
            harMinimumInntekt = minimumInntektvurderingOk,
            vurdertOk = inntektsvurderingOk && opptjeningvurderingOk && medlemskapsvurderingOk && minimumInntektvurderingOk,
            meldingsreferanseId = meldingsreferanseId(),
            vilkårsgrunnlagId = UUID.randomUUID()
        )
        return this
    }


    internal fun lagreInntekter(person: Person, skjæringstidspunkt: LocalDate) {
        inntektsvurdering.lagreInntekter(person, skjæringstidspunkt, this)
    }

    internal fun grunnlagsdata() = requireNotNull(grunnlagsdata) { "Må kalle valider() først" }

    internal fun lagreSkatteinntekter(person: Person, skjæringstidspunkt: LocalDate) {
        inntektsvurderingForSykepengegrunnlag.lagreInntekter(person, skjæringstidspunkt, this)
    }

    internal fun loggUkjenteArbeidsforhold(person: Person, skjæringstidspunkt: LocalDate) {
        val arbeidsforholdForSkjæringstidspunkt = arbeidsforhold.filter { it.gjelder(skjæringstidspunkt) }
        if (arbeidsforholdForSkjæringstidspunkt.any { it.harArbeidetMindreEnnTreMåneder(skjæringstidspunkt) }) {
            sikkerlogg.info("Person har et relevant arbeidsforhold som har vart mindre enn 3 måneder (8-28b) - fødselsnummer: $fødselsnummer")
        }
        person.brukOuijaBrettForÅKommunisereMedPotensielleSpøkelser(arbeidsforholdForSkjæringstidspunkt.map(Arbeidsforhold::orgnummer), skjæringstidspunkt)
        person.loggUkjenteOrgnummere(arbeidsforhold.map { it.orgnummer })
    }

    internal fun lagreArbeidsforhold(person: Person, skjæringstidspunkt: LocalDate) {
        arbeidsforhold
            .filter { it.gjelder(skjæringstidspunkt) }
            .grupperArbeidsforholdPerOrgnummer().forEach { (orgnummer, arbeidsforhold) ->
                if (arbeidsforhold.any { it.erSøppel() }) {
                    warn("Vi fant ugyldige arbeidsforhold i Aareg, burde sjekkes opp nærmere") // TODO: må ses på av en voksen
                }
                person.lagreArbeidsforhold(orgnummer, arbeidsforhold, this, skjæringstidspunkt)
            }
    }

}
