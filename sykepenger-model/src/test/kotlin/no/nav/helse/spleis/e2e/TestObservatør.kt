package no.nav.helse.spleis.e2e

import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType
import java.util.*

internal class TestObservatør : PersonObserver {
    internal val tilstander = mutableMapOf<UUID, MutableList<TilstandType>>()
    val utbetalteVedtaksperioder = mutableListOf<UUID>()
    val manglendeInntektsmeldingVedtaksperioder = mutableListOf<UUID>()
    val trengerIkkeInntektsmeldingVedtaksperioder = mutableListOf<UUID>()
    val hendelserTilReplay = mutableMapOf<UUID, List<UUID>>()
    val vedtaksperioder = mutableSetOf<UUID>()
    val utbetaltEventer = mutableListOf<PersonObserver.UtbetaltEvent>()
    val avbruttEventer = mutableListOf<PersonObserver.VedtaksperiodeAvbruttEvent>()
    val annulleringer = mutableListOf<PersonObserver.UtbetalingAnnullertEvent>()

    override fun vedtaksperiodeEndret(event: PersonObserver.VedtaksperiodeEndretTilstandEvent) {
        vedtaksperioder.add(event.vedtaksperiodeId)
        tilstander.getOrPut(event.vedtaksperiodeId) { mutableListOf(TilstandType.START) }.add(event.gjeldendeTilstand)
        if (event.gjeldendeTilstand == TilstandType.AVSLUTTET) utbetalteVedtaksperioder.add(event.vedtaksperiodeId)
    }

    override fun vedtaksperiodeReplay(event: PersonObserver.VedtaksperiodeReplayEvent) {
        hendelserTilReplay[event.vedtaksperiodeId] = event.hendelseIder
    }

    override fun manglerInntektsmelding(event: PersonObserver.ManglendeInntektsmeldingEvent) {
        manglendeInntektsmeldingVedtaksperioder.add(event.vedtaksperiodeId)
    }

    override fun trengerIkkeInntektsmelding(event: PersonObserver.TrengerIkkeInntektsmeldingEvent) {
        trengerIkkeInntektsmeldingVedtaksperioder.add(event.vedtaksperiodeId)
    }

    override fun vedtaksperiodeUtbetalt(event: PersonObserver.UtbetaltEvent) {
        utbetaltEventer.add(event)
    }

    override fun annullering(event: PersonObserver.UtbetalingAnnullertEvent) {
        annulleringer.add(event)
    }

    override fun vedtaksperiodeAvbrutt(event: PersonObserver.VedtaksperiodeAvbruttEvent) {
        avbruttEventer.add(event)
    }
}
