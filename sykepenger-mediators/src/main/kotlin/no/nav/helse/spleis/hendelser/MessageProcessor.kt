package no.nav.helse.spleis.hendelser

import no.nav.helse.hendelser.ManuellSaksbehandling
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.spleis.hendelser.model.*

// Acts as a GoF visitor
internal interface MessageProcessor {
    fun process(message: NySøknadMessage, problems: MessageProblems)
    fun process(message: FremtidigSøknadMessage, problems: MessageProblems)
    fun process(message: SendtSøknadMessage, problems: MessageProblems)
    fun process(message: InntektsmeldingMessage, problems: MessageProblems)
    fun process(message: PåminnelseMessage, problems: MessageProblems)
    fun process(message: YtelserMessage, problems: MessageProblems)
    fun process(message: VilkårsgrunnlagMessage, problems: MessageProblems)
    fun process(message: ManuellSaksbehandlingMessage, problems: MessageProblems)
}
