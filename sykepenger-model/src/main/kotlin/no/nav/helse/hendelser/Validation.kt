package no.nav.helse.hendelser

import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode

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

    internal inline fun valider(kode: Varselkode? = null, isValid: Validation.() -> Boolean) {
        if (harFunksjonelleFeilEllerVerre()) return
        if (isValid(this)) return
        onValidationFailed(kode)
    }

    internal inline fun onSuccess(successBlock: Validation.() -> Unit) {
        if (!harFunksjonelleFeilEllerVerre()) successBlock(this)
    }

    override fun harFunksjonelleFeilEllerVerre() = hasErrors || hendelse.harFunksjonelleFeilEllerVerre()

    private fun onValidationFailed(kode: Varselkode?) {
        hasErrors = true
        kode?.also { funksjonellFeil(it) }
        errorBlock(this)
    }
}
