package no.nav.helse.spleis.hendelser

import no.nav.helse.spleis.hendelser.model.BehovMessage
import no.nav.helse.spleis.hendelser.model.InntektsmeldingMessage
import no.nav.helse.spleis.hendelser.model.PåminnelseMessage
import no.nav.helse.spleis.hendelser.model.SøknadMessage

// Acts as a GoF visitor
internal interface MessageProcessor {
    fun process(message: SøknadMessage, problems: MessageProblems)
    fun process(message: InntektsmeldingMessage, problems: MessageProblems)
    fun process(message: BehovMessage, problems: MessageProblems)
    fun process(message: PåminnelseMessage, problems: MessageProblems)
}
