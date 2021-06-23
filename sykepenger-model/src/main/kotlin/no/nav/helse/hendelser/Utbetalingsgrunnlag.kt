package no.nav.helse.hendelser

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
    private val inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag
) : ArbeidstakerHendelse(meldingsreferanseId) {

    override fun organisasjonsnummer() = orgnummer

    override fun aktørId() = aktørId

    override fun fødselsnummer() = fødselsnummer

    internal fun lagreInntekter(person: Person, skjæringstidspunkt: LocalDate) {
        inntektsvurderingForSykepengegrunnlag.lagreInntekter(person, skjæringstidspunkt, this)
    }

    internal fun erRelevant(other: UUID) = other == vedtaksperiodeId
}
