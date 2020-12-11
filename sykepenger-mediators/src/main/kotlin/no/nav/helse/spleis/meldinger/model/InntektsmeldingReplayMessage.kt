package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.InntektsmeldingReplay
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.MessageDelegate

// Understands a JSON message representing an Inntektsmelding
internal class InntektsmeldingReplayMessage(packet: MessageDelegate) : InntektsmeldingMessage(packet) {
    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, InntektsmeldingReplay(this.id, inntektsmelding))
    }
}
