package no.nav.helse.hendelser

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.inntekt.Sykepengegrunnlag
import no.nav.helse.økonomi.Inntekt

class OverstyrInntekt(
    meldingsreferanseId: UUID,
    fødselsnummer: String,
    aktørId: String,
    organisasjonsnummer: String,
    internal val inntekt: Inntekt,
    internal val skjæringstidspunkt: LocalDate,
    internal val forklaring: String,
    internal val subsumsjon: Subsumsjon?
) : ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, organisasjonsnummer) {
    internal fun erRelevant(skjæringstidspunkt: LocalDate) = this.skjæringstidspunkt == skjæringstidspunkt

    internal fun tilRevurderingAvvistEvent(): PersonObserver.RevurderingAvvistEvent =
        PersonObserver.RevurderingAvvistEvent(
            fødselsnummer = fødselsnummer,
            errors = this.funksjonelleFeilOgVerre()
        )


    internal fun leggTil(hendelseIder: MutableSet<Dokumentsporing>) {
        hendelseIder.add(Dokumentsporing.overstyrInntekt(meldingsreferanseId()))
    }

    internal fun overstyr(builder: Sykepengegrunnlag.SaksbehandlerOverstyringer) {
        builder.leggTilInntekt(organisasjonsnummer, meldingsreferanseId(), inntekt, forklaring, subsumsjon)
    }
}
