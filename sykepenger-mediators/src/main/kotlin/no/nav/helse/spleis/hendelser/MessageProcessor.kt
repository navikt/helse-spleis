package no.nav.helse.spleis.hendelser

import no.nav.helse.spleis.hendelser.model.*

// Acts as a GoF visitor
internal interface MessageProcessor {
    fun process(message: NySøknadMessage)
    fun process(message: SendtSøknadArbeidsgiverMessage)
    fun process(message: SendtSøknadNavMessage)
    fun process(message: InntektsmeldingMessage)
    fun process(message: PåminnelseMessage)
    fun process(message: YtelserMessage)
    fun process(message: VilkårsgrunnlagMessage)
    fun process(message: ManuellSaksbehandlingMessage)
    fun process(message: UtbetalingMessage)
    fun process(message: SimuleringMessage)
    fun process(message: KansellerUtbetalingMessage)
}
