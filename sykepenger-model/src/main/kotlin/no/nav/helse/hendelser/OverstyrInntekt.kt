package no.nav.helse.hendelser

import no.nav.helse.person.*
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.util.*


class OverstyrInntekt(
    meldingsreferanseId: UUID,
    fødselsnummer: String,
    aktørId: String,
    organisasjonsnummer: String,
    internal val inntekt: Inntekt,
    internal val skjæringstidspunkt: LocalDate
) : ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, organisasjonsnummer) {

    internal fun addInntekt(inntektshistorikk: Inntektshistorikk) {
        inntektshistorikk.append { addSaksbehandler(skjæringstidspunkt, meldingsreferanseId(), inntekt) }
    }

    internal fun tilRevurderingAvvistEvent(): PersonObserver.RevurderingAvvistEvent =
        PersonObserver.RevurderingAvvistEvent(
            fødselsnummer = fødselsnummer,
            errors = this.errorsAndWorse()
        )

    internal fun loggførHendelsesreferanse(person: Person) {
        person.loggførHendelsesreferanse(organisasjonsnummer, skjæringstidspunkt, this)
    }

    internal fun leggTil(hendelseIder: MutableSet<Sporing>) {
        hendelseIder.add(Sporing(meldingsreferanseId(), Sporing.Type.OverstyrInntekt))
    }
}
