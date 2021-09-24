package no.nav.helse.hendelser

import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*


class OverstyrInntekt(
    meldingsreferanseId: UUID,
    private val fødselsnummer: String,
    private val aktørId: String,
    private val organisasjonsnummer: String,
    internal val inntekt: Inntekt,
    internal val skjæringstidspunkt: LocalDate
) : ArbeidstakerHendelse(meldingsreferanseId) {

    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = organisasjonsnummer

    internal fun addInntekt(inntektshistorikk: Inntektshistorikk) {
        inntektshistorikk { addSaksbehandler(skjæringstidspunkt, meldingsreferanseId(), inntekt) }
    }

}
