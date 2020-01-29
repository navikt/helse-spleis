package no.nav.helse.hendelser

import no.nav.helse.person.ArbeidstakerHendelse

internal class Validation(private val hendelse: ArbeidstakerHendelse){
    private lateinit var errorBlock: ErrorBlock

    internal fun onError(block:ErrorBlock){
        errorBlock = block
    }

    internal fun onSuccess(successBlock:SuccessBlock) {
        if(!hendelse.hasErrors()) successBlock()
    }

    internal fun valider(block: ValiderBlock) {
        if (hendelse.hasErrors()) return
        val steg = block()
        if (steg.isValid()) return
        hendelse.error(steg.feilmelding())
        errorBlock()
    }

}

internal typealias ErrorBlock = () -> Unit
internal typealias SuccessBlock = () -> Unit
internal typealias ValiderBlock = () -> Valideringssteg
internal interface Valideringssteg {
    fun isValid(): Boolean
    fun feilmelding(): String
}
