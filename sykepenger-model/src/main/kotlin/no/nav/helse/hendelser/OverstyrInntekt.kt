package no.nav.helse.hendelser

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Arbeidsgiver.Companion.harPeriodeSomBlokkererOverstyring
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.PersonObserver
import no.nav.helse.økonomi.Inntekt

class Subsumsjon(
    internal val paragraf: String,
    internal val ledd: Int?,
    internal val bokstav: String?,
)

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

    internal fun addInntekt(inntektshistorikk: Inntektshistorikk) {
        inntektshistorikk.append { addSaksbehandler(skjæringstidspunkt, meldingsreferanseId(), inntekt) }
    }

    internal fun tilRevurderingAvvistEvent(): PersonObserver.RevurderingAvvistEvent =
        PersonObserver.RevurderingAvvistEvent(
            fødselsnummer = fødselsnummer,
            errors = this.errorsAndWorse()
        )


    internal fun leggTil(hendelseIder: MutableSet<Dokumentsporing>) {
        hendelseIder.add(Dokumentsporing.overstyrInntekt(meldingsreferanseId()))
    }

    internal fun valider(arbeidsgivere: MutableList<Arbeidsgiver>) {
        if (arbeidsgivere.none { it.harSykdomFor(skjæringstidspunkt) }) {
            severe("Kan ikke overstyre inntekt hvis vi ikke har en arbeidsgiver med sykdom for skjæringstidspunktet")
        }
        if (arbeidsgivere.harPeriodeSomBlokkererOverstyring(skjæringstidspunkt)) {
            severe("Kan ikke overstyre inntekt for ghost for en pågående behandling der én eller flere perioder er behandlet ferdig")
        }
    }
}
