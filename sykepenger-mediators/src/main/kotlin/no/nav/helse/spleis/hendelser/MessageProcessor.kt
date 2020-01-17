package no.nav.helse.spleis.hendelser

import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.spleis.hendelser.model.*

// Acts as a GoF visitor
internal interface MessageProcessor {
    fun process(message: NySøknadMessage, aktivitetslogger: Aktivitetslogger)
    fun process(message: FremtidigSøknadMessage, aktivitetslogger: Aktivitetslogger)
    fun process(message: SendtSøknadMessage, aktivitetslogger: Aktivitetslogger)
    fun process(message: InntektsmeldingMessage, aktivitetslogger: Aktivitetslogger)
    fun process(message: PåminnelseMessage, aktivitetslogger: Aktivitetslogger)
    fun process(message: YtelserMessage, aktivitetslogger: Aktivitetslogger)
    fun process(message: VilkårsgrunnlagMessage, aktivitetslogger: Aktivitetslogger)
    fun process(message: ManuellSaksbehandlingMessage, aktivitetslogger: Aktivitetslogger)
}
