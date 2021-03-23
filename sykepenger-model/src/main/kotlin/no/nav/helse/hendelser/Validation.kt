package no.nav.helse.hendelser

import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Person
import java.time.LocalDate

internal class Validation private constructor(private val hendelse: ArbeidstakerHendelse) : IAktivitetslogg by(hendelse) {
    private var hasErrors = false
    private var errorBlock: Validation.() -> Unit = {}

    internal companion object {
        internal inline fun validation(hendelse: ArbeidstakerHendelse, block: Validation.() -> Unit) {
            Validation(hendelse).apply(block)
        }
    }

    internal fun onError(block: Validation.() -> Unit) {
        errorBlock = block
    }

    internal fun validerHvis(feilmelding: String? = null, hvis: Boolean, isValid: Validation.() -> Boolean) {
        if (!hvis) return
        valider(feilmelding, isValid)
    }

    internal inline fun valider(feilmelding: String? = null, isValid: Validation.() -> Boolean) {
        if (hasErrorsOrWorse()) return
        if (isValid(this)) return
        onError(feilmelding)
    }

    internal inline fun onSuccess(successBlock: Validation.() -> Unit) {
        if (!hasErrorsOrWorse()) successBlock(this)
    }

    override fun hasErrorsOrWorse() = hasErrors || hendelse.hasErrorsOrWorse()

    private fun onError(feilmelding: String?) {
        hasErrors = true
        feilmelding?.also { error(it) }
        errorBlock(this)
    }
}

internal fun Validation.harNødvendigInntekt(
    person: Person,
    skjæringstidspunkt: LocalDate
) = valider("Vi har ikke inntektshistorikken vi trenger for $skjæringstidspunkt") {
    person.harNødvendigInntekt(skjæringstidspunkt)
}
