package no.nav.helse.hendelser

import no.nav.helse.hendelser.Arbeidsforhold.Companion.grupperArbeidsforholdPerOrgnummer
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Person
import java.time.LocalDate
import java.util.*

class Utbetalingsgrunnlag(
    meldingsreferanseId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val vedtaksperiodeId: UUID,
    private val inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag,
    private val arbeidsforhold: List<Arbeidsforhold>
) : ArbeidstakerHendelse(meldingsreferanseId) {

    override fun organisasjonsnummer() = orgnummer

    override fun aktørId() = aktørId

    override fun fødselsnummer() = fødselsnummer

    internal fun lagreInntekter(person: Person, skjæringstidspunkt: LocalDate) {
        inntektsvurderingForSykepengegrunnlag.lagreInntekter(person, skjæringstidspunkt, this)
    }

    internal fun loggUkjenteArbeidsforhold(person: Person, skjæringstidspunkt: LocalDate) {
        person.brukOuijaBrettForÅKommunisereMedPotensielleSpøkelser(arbeidsforhold
            .filter { it.gjelder(skjæringstidspunkt) }
            .map(Arbeidsforhold::orgnummer), skjæringstidspunkt)
        person.loggUkjenteOrgnummere(arbeidsforhold.map { it.orgnummer })
    }

    internal fun erRelevant(other: UUID) = other == vedtaksperiodeId
    internal fun lagreArbeidsforhold(person: Person, skjæringstidspunkt: LocalDate) {
        arbeidsforhold.grupperArbeidsforholdPerOrgnummer().forEach { (orgnummer, arbeidsforhold) ->
            if (arbeidsforhold.any { it.erSøppel() }) {
                // warn("Vi fant ugyldige arbeidsforhold i Aareg, burde sjekkes opp nærmere") // TODO: må ses på av en voksen
            }
            person.lagreArbeidsforhold(orgnummer, arbeidsforhold, this, skjæringstidspunkt)
        }
    }
}
