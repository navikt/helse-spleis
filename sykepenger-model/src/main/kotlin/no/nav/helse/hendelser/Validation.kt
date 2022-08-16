package no.nav.helse.hendelser

import no.nav.helse.person.IAktivitetslogg

internal class Validation private constructor(private val hendelse: IAktivitetslogg) : IAktivitetslogg by(hendelse) {
    private var hasErrors = false
    private var errorBlock: Validation.() -> Unit = {}

    internal companion object {
        internal inline fun validation(hendelse: IAktivitetslogg, block: Validation.() -> Unit) {
            Validation(hendelse).apply(block)
        }
    }

    internal fun onValidationFailed(block: Validation.() -> Unit) {
        errorBlock = block
    }

    internal fun validerHvis(feilmelding: String? = null, hvis: Boolean, isValid: Validation.() -> Boolean) {
        validerHvis(feilmelding, { hvis }, isValid)
    }

    internal fun validerHvis(feilmelding: String? = null, hvis: () -> Boolean, isValid: Validation.() -> Boolean) {
        if (!hvis()) return
        valider(feilmelding, isValid)
    }

    internal inline fun valider(feilmelding: String? = null, isValid: Validation.() -> Boolean) {
        if (harFunksjonelleFeilEllerVerre()) return
        if (isValid(this)) return
        onValidationFailed(feilmelding)
    }

    internal inline fun onSuccess(successBlock: Validation.() -> Unit) {
        if (!harFunksjonelleFeilEllerVerre()) successBlock(this)
    }

    override fun harFunksjonelleFeilEllerVerre() = hasErrors || hendelse.harFunksjonelleFeilEllerVerre()

    private fun onValidationFailed(feilmelding: String?) {
        hasErrors = true
        feilmelding?.also { funksjonellFeil(it) }
        errorBlock(this)
    }
}
