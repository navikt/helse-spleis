package no.nav.helse.hendelser

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.Periode.Companion.sammenhengende
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Companion.grupperArbeidsforholdPerOrgnummer
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Companion.opptjeningsperiode
import no.nav.helse.person.Arbeidsforholdhistorikk
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Opptjening
import no.nav.helse.person.Person
import no.nav.helse.person.Sammenligningsgrunnlag
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
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
        opptjening: Opptjening,
        antallArbeidsgivereFraAareg: Int,
        subsumsjonObserver: SubsumsjonObserver
    ): IAktivitetslogg {
        val sykepengegrunnlagOk = grunnlagForSykepengegrunnlag.valider(this)
        inntektsvurderingForSykepengegrunnlag.valider(this)
        inntektsvurderingForSykepengegrunnlag.loggInteressantFrilanserInformasjon(skjæringstidspunkt)

        val inntektsvurderingOk = inntektsvurdering.valider(
            this,
            grunnlagForSykepengegrunnlag,
            sammenligningsgrunnlag.sammenligningsgrunnlag,
            antallArbeidsgivereFraAareg,
            subsumsjonObserver
        )
        val opptjeningvurderingOk = opptjening.valider(this)
        val medlemskapsvurderingOk = medlemskapsvurdering.valider(this)
        grunnlagsdata = VilkårsgrunnlagHistorikk.Grunnlagsdata(
            skjæringstidspunkt = skjæringstidspunkt,
            sykepengegrunnlag = grunnlagForSykepengegrunnlag,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            avviksprosent = inntektsvurdering.avviksprosent(),
            opptjening = opptjening,
            medlemskapstatus = medlemskapsvurdering.medlemskapstatus,
            vurdertOk = sykepengegrunnlagOk && inntektsvurderingOk && opptjeningvurderingOk && medlemskapsvurderingOk,
            meldingsreferanseId = meldingsreferanseId(),
            vilkårsgrunnlagId = UUID.randomUUID()
        )
        return this
    }


    internal fun lagreRapporterteInntekter(person: Person, skjæringstidspunkt: LocalDate) {
        inntektsvurdering.lagreRapporterteInntekter(person, skjæringstidspunkt, this)
    }

    internal fun grunnlagsdata() = requireNotNull(grunnlagsdata) { "Må kalle valider() først" }

    internal fun lagreSkatteinntekter(person: Person, skjæringstidspunkt: LocalDate) {
        inntektsvurderingForSykepengegrunnlag.lagreOmregnetÅrsinntekter(person, skjæringstidspunkt, this)
    }

    internal fun loggUkjenteArbeidsforhold(person: Person, skjæringstidspunkt: LocalDate) {
        val arbeidsforholdForSkjæringstidspunkt = arbeidsforhold.filter { it.gjelder(skjæringstidspunkt) }
        val harFlereRelevanteArbeidsforhold = arbeidsforholdForSkjæringstidspunkt.grupperArbeidsforholdPerOrgnummer().keys.size > 1
        if (harFlereRelevanteArbeidsforhold && arbeidsforholdForSkjæringstidspunkt.any { it.harArbeidetMindreEnnTreMåneder(skjæringstidspunkt) }) {
            sikkerlogg.info("Person har flere arbeidsgivere og et relevant arbeidsforhold som har vart mindre enn 3 måneder (8-28b) - fødselsnummer: $fødselsnummer")
        }
        person.brukOuijaBrettForÅKommunisereMedPotensielleSpøkelser(arbeidsforholdForSkjæringstidspunkt.map(Arbeidsforhold::orgnummer), skjæringstidspunkt)
        person.loggUkjenteOrgnummere(arbeidsforhold.map { it.orgnummer })
    }

    internal fun lagreArbeidsforhold(person: Person, skjæringstidspunkt: LocalDate) {
        val opptjeningsperiode = arbeidsforhold.opptjeningsperiode(skjæringstidspunkt)
        arbeidsforhold
            .filter { it.erDelAvOpptjeningsperiode(opptjeningsperiode) }
            .grupperArbeidsforholdPerOrgnummer().forEach { (orgnummer, arbeidsforhold) ->
                if (arbeidsforhold.any { it.erSøppel() }) {
                    varsel("Vi fant ugyldige arbeidsforhold i Aareg, burde sjekkes opp nærmere") // TODO: må ses på av en voksen
                }
                person.lagreArbeidsforhold(orgnummer, arbeidsforhold, this, skjæringstidspunkt)
            }
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

        fun erSøppel() =
            ansattTom != null && ansattTom < ansattFom

        internal fun erDelAvOpptjeningsperiode(opptjeningsperiode: Periode) = ansattFom in opptjeningsperiode

        private fun erGyldig(skjæringstidspunkt: LocalDate) =
            ansattFom < skjæringstidspunkt && !erSøppel()

        private fun periode(skjæringstidspunkt: LocalDate): Periode? {
            if (!erGyldig(skjæringstidspunkt)) return null
            return ansattFom til (ansattTom ?: skjæringstidspunkt)
        }

        internal fun gjelder(skjæringstidspunkt: LocalDate) = ansattFom <= skjæringstidspunkt && (ansattTom == null || ansattTom >= skjæringstidspunkt)

        internal fun erRelevant(arbeidsgiver: Arbeidsgiver) = orgnummer == arbeidsgiver.organisasjonsnummer()

        internal fun harArbeidetMindreEnnTreMåneder(skjæringstidspunkt: LocalDate) = ansattFom > skjæringstidspunkt.withDayOfMonth(1).minusMonths(3)

        internal companion object {
            internal fun List<Arbeidsforhold>.toEtterlevelseMap() = map {
                mapOf(
                    "orgnummer" to it.orgnummer,
                    "fom" to it.ansattFom,
                    "tom" to it.ansattTom
                )
            }

            private fun LocalDate.datesUntilReversed(tom: LocalDate) = tom.toEpochDay().downTo(this.toEpochDay())
                .asSequence()
                .map(LocalDate::ofEpochDay)

            fun opptjeningsdager(
                arbeidsforhold: List<Arbeidsforhold>,
                aktivitetslogg: IAktivitetslogg,
                skjæringstidspunkt: LocalDate
            ): Int {
                if (arbeidsforhold.any(Arbeidsforhold::erSøppel))
                    aktivitetslogg.varsel("Opptjeningsvurdering må gjøres manuelt fordi opplysningene fra AA-registeret er ufullstendige")

                val ranges = arbeidsforhold.mapNotNull { it.periode(skjæringstidspunkt) }
                if (ranges.none { skjæringstidspunkt in it }) {
                    aktivitetslogg.info("Personen er ikke i arbeid ved skjæringstidspunktet")
                    return 0
                }

                val min = ranges.minOf { it.start }
                return min.datesUntilReversed(skjæringstidspunkt.minusDays(1))
                    .takeWhile { cursor -> ranges.any { periode -> cursor in periode } }
                    .count()
            }

            internal fun List<Arbeidsforhold>.grupperArbeidsforholdPerOrgnummer() = groupBy { it.orgnummer }

            internal fun Iterable<Arbeidsforhold>.opptjeningsperiode(skjæringstidspunkt: LocalDate) = mapNotNull { it.periode(skjæringstidspunkt) }
                .sammenhengende(skjæringstidspunkt)
        }
    }
}
