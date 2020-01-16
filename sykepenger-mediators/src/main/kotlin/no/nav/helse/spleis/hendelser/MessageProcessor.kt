package no.nav.helse.spleis.hendelser

import no.nav.helse.person.Problemer
import no.nav.helse.spleis.hendelser.model.*

// Acts as a GoF visitor
internal interface MessageProcessor {
    fun process(message: NySøknadMessage, problemer: Problemer)
    fun process(message: FremtidigSøknadMessage, problemer: Problemer)
    fun process(message: SendtSøknadMessage, problemer: Problemer)
    fun process(message: InntektsmeldingMessage, problemer: Problemer)
    fun process(message: PåminnelseMessage, problemer: Problemer)
    fun process(message: YtelserMessage, problemer: Problemer)
    fun process(message: VilkårsgrunnlagMessage, problemer: Problemer)
    fun process(message: ManuellSaksbehandlingMessage, problemer: Problemer)
}
