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

    internal fun valider(steg: Valideringssteg) {
        if (hendelse.hasErrors()) return
        if (!steg.valider()) hendelse.error(steg.melding())
        errorBlock()
    }

}

internal typealias ErrorBlock = () -> Unit
internal typealias SuccessBlock = () -> Unit
internal interface Valideringssteg {
    fun valider(): Boolean
    fun melding(): String
}
