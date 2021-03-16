package no.nav.helse.spleis

import no.nav.helse.person.PersonObserver
import no.nav.helse.spleis.db.HendelseRepository
import no.nav.helse.spleis.meldinger.model.HendelseMessage

internal class ReplayMediator(
    private val hendelseMediator: IHendelseMediator,
    private val hendelseRepository: HendelseRepository
    ): PersonObserver {

    private val replays = mutableListOf<HendelseMessage>()

    internal fun finalize() {
        if(replays.isEmpty()) return
        replays.removeAt(0).behandle(hendelseMediator)
    }

    override fun inntektsmeldingReplay(event: PersonObserver.InntektsmeldingReplayEvent) {
        replays.addAll(hendelseRepository.gjennopprettInntektsmelding(event.fnr, event.vedtaksperiodeId))
    }
}
