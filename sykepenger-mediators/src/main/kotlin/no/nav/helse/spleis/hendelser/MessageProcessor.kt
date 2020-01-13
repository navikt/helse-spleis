package no.nav.helse.spleis.hendelser

import no.nav.helse.spleis.hendelser.model.*

// Acts as a GoF visitor
internal interface MessageProcessor {
    fun process(message: NySøknadMessage, problems: MessageProblems)
    fun process(message: FremtidigSøknadMessage, problems: MessageProblems)
    fun process(message: SendtSøknadMessage, problems: MessageProblems)
    fun process(message: InntektsmeldingMessage, problems: MessageProblems)
    fun process(message: BehovMessage, problems: MessageProblems)
    fun process(message: PåminnelseMessage, problems: MessageProblems)
}
