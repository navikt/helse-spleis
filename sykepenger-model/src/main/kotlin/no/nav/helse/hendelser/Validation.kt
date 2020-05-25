package no.nav.helse.hendelser

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidstakerHendelse
import java.time.LocalDate

internal class Validation private constructor(private val hendelse: ArbeidstakerHendelse) {
    private lateinit var errorBlock: () -> Unit

    internal companion object {
        internal fun validation(hendelse: ArbeidstakerHendelse, block: Validation.() -> Unit) {
            Validation(hendelse).apply(block)
        }
    }

    internal fun onError(block: () -> Unit) {
        errorBlock = block
    }

    internal fun valider(feilmelding: String? = null, isValid: () -> Boolean) {
        if (hendelse.hasErrors()) return
        if (isValid()) return
        feilmelding?.also { hendelse.error(it) }
        errorBlock()
    }

    internal fun onSuccess(successBlock: () -> Unit) {
        if (!hendelse.hasBehov()) successBlock()
    }
}

internal fun Validation.validerYtelser(
    periode: Periode,
    ytelser: Ytelser
) = valider {
    !ytelser.valider(periode).hasErrors()
}

internal fun Validation.overlappende(
    sykdomsperiode: Periode,
    foreldrepermisjon: Foreldrepermisjon
) = valider("Har overlappende foreldrepengeperioder med syketilfelle") {
    !foreldrepermisjon.overlapper(sykdomsperiode)
}

internal fun Validation.harInntektshistorikk(
    arbeidsgiver: Arbeidsgiver,
    førsteDag: LocalDate
) = valider("Vi har ikke inntektshistorikken vi trenger") {
    arbeidsgiver.inntekt(førsteDag) != null
}
