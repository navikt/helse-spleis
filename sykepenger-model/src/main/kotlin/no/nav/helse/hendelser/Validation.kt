package no.nav.helse.hendelser

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse

internal class Validation(private val hendelse: ArbeidstakerHendelse){
    private lateinit var errorBlock: ErrorBlock

    internal fun onError(block:ErrorBlock){
        errorBlock = block
    }

    internal fun valider(block: ValiderBlock) {
        if (hendelse.hasErrors()) return
        val steg = block()
        if (steg.isValid()) return
        hendelse.error(steg.feilmelding())
        errorBlock()
    }

    internal fun onSuccess(successBlock:SuccessBlock) {
        if(!hendelse.hasErrors()) successBlock()
    }
}

internal typealias ErrorBlock = () -> Unit
internal typealias SuccessBlock = () -> Unit
internal typealias ValiderBlock = () -> Valideringssteg
internal interface Valideringssteg {
    fun isValid(): Boolean
    fun feilmelding(): String
}

// Invoke internal validation of a Hendelse
internal class ValiderSykdomshendelse(private val hendelse: SykdomstidslinjeHendelse) : Valideringssteg {
    override fun isValid() =
        !hendelse.valider().hasErrors()
    override fun feilmelding() = "Kunne ikke validere hendelse"
}

// Confirm that only one Arbeidsgiver exists for a Person (temporary; remove in Epic 7)
internal class ValiderKunEnArbeidsgiver(
    private val arbeidsgivere: List<Arbeidsgiver>
) : Valideringssteg {
    override fun isValid() = arbeidsgivere.size == 1
    override fun feilmelding() = "Bruker har mer enn en arbeidsgiver"
}

// Continue processing Hendelse with appropriate Arbeidsgiver
internal class ArbeidsgiverHåndterHendelse(
    private val hendelse: SykdomstidslinjeHendelse,
    private val arbeidsgiver: Arbeidsgiver?
) : Valideringssteg {
    override fun isValid(): Boolean {
        hendelse.fortsettÅBehandle(arbeidsgiver)  // Double dispatch to invoke correct method
        return !hendelse.hasErrors()
    }
    override fun feilmelding() = "Feil under hendelseshåndtering"
}
