package no.nav.helse.hendelser

import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode

internal class Validation private constructor(private val aktivitetslogg: IAktivitetslogg) : IAktivitetslogg by (aktivitetslogg) {
    private var hasErrors = false
    private var errorBlock: Validation.() -> Unit = {}

    internal companion object {
        internal inline fun validation(aktivitetslogg: IAktivitetslogg, block: Validation.() -> Unit) {
            Validation(aktivitetslogg).apply(block)
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

    override fun harFunksjonelleFeilEllerVerre() = hasErrors || aktivitetslogg.harFunksjonelleFeilEllerVerre()

    private fun onValidationFailed(kode: Varselkode?) {
        hasErrors = true
        kode?.also { funksjonellFeil(it) }
        errorBlock(this)
    }
}
