package no.nav.helse.hendelser

import no.nav.helse.hendelser.Arbeidsforhold.Companion.grupperArbeidsforholdPerOrgnummer
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Person
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

class Utbetalingsgrunnlag(
    meldingsreferanseId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val vedtaksperiodeId: UUID,
    private val inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag,
    arbeidsforhold: List<Arbeidsforhold>
) : ArbeidstakerHendelse(meldingsreferanseId) {
    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    private val arbeidsforhold = arbeidsforhold.filter { it.orgnummer.isNotBlank() }

    override fun organisasjonsnummer() = orgnummer

    override fun aktørId() = aktørId

    override fun fødselsnummer() = fødselsnummer

    internal fun lagreInntekter(person: Person, skjæringstidspunkt: LocalDate) {
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

    internal fun erRelevant(other: UUID) = other == vedtaksperiodeId
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
