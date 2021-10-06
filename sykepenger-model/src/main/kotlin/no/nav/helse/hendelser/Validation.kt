package no.nav.helse.hendelser

import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Person
import java.time.LocalDate

internal class Validation private constructor(private val hendelse: IAktivitetslogg) : IAktivitetslogg by(hendelse) {
    private var hasErrors = false
    private var errorBlock: Validation.(errorLogAlreadyDone: Boolean) -> Unit = {}

    internal companion object {
        internal inline fun validation(hendelse: IAktivitetslogg, block: Validation.() -> Unit) {
            Validation(hendelse).apply(block)
        }
    }

    internal fun onValidationFailed(block: Validation.(errorLogAlreadyDone: Boolean) -> Unit) {
        errorBlock = block
    }

    internal fun validerHvis(feilmelding: String? = null, hvis: Boolean, isValid: Validation.() -> Boolean) {
        if (!hvis) return
        valider(feilmelding, isValid)
    }

    internal inline fun valider(feilmelding: String? = null, isValid: Validation.() -> Boolean) {
        if (hasErrorsOrWorse()) return
        if (isValid(this)) return
        onValidationFailed(feilmelding)
    }

    internal inline fun onSuccess(successBlock: Validation.() -> Unit) {
        if (!hasErrorsOrWorse()) successBlock(this)
    }

    override fun hasErrorsOrWorse() = hasErrors || hendelse.hasErrorsOrWorse()

    private fun onValidationFailed(feilmelding: String?) {
        hasErrors = true
        feilmelding?.also { error(it) }
        errorBlock(this, hendelse.hasErrorsOrWorse())
    }
}

internal fun Validation.harNødvendigInntekt(
    person: Person,
    skjæringstidspunkt: LocalDate
) = valider("Vi har ikke inntektshistorikken vi trenger for skjæringstidspunktet") {
    person.harNødvendigInntekt(skjæringstidspunkt)
}
