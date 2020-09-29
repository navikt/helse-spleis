package no.nav.helse.hendelser

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Periodetype
import no.nav.helse.person.Person
import java.time.LocalDate

internal class Validation private constructor(private val hendelse: ArbeidstakerHendelse) {
    private var errorBlock: () -> Unit = {}

    internal companion object {
        internal fun validation(hendelse: ArbeidstakerHendelse, block: Validation.() -> Unit) {
            Validation(hendelse).apply(block)
        }
    }

    internal fun onError(block: () -> Unit) {
        errorBlock = block
    }

    internal fun valider(feilmelding: String? = null, isValid: () -> Boolean) {
        if (hendelse.hasErrorsOrWorse()) return
        if (isValid()) return
        feilmelding?.also { hendelse.error(it) }
        errorBlock()
    }

    internal fun onSuccess(successBlock: () -> Unit) {
        if (!hendelse.hasErrorsOrWorse()) successBlock()
    }
}

internal fun Validation.validerYtelser(
    periode: Periode,
    ytelser: Ytelser,
    periodetype: Periodetype
) = valider {
    !ytelser.valider(periode, periodetype).hasErrorsOrWorse()
}

internal fun Validation.overlappende(
    sykdomsperiode: Periode,
    foreldrepermisjon: Foreldrepermisjon
) = valider("Har overlappende foreldrepengeperioder med syketilfelle") {
    !foreldrepermisjon.overlapper(sykdomsperiode)
}

internal fun Validation.overlappende(
    sykdomsperiode: Periode,
    pleiepenger: Pleiepenger
) = valider("Har overlappende pleiepengeytelse med syketilfelle") {
    !pleiepenger.overlapper(sykdomsperiode)
}

internal fun Validation.overlappende(
    periode: Periode,
    person: Person,
    ytelser: Ytelser
) = valider("Invaliderer alle perioder pga flere arbeidsgivere") {
    !person.arbeidsgiverOverlapper(periode, ytelser)
}

internal fun Validation.harInntektshistorikk(
    arbeidsgiver: Arbeidsgiver,
    førsteDag: LocalDate
) = valider("Vi har ikke inntektshistorikken vi trenger") {
    arbeidsgiver.inntekt(førsteDag) != null
}
