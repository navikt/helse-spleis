package no.nav.helse.e2e

import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType
import java.util.*

internal class TestObservatør : PersonObserver {
    internal val tilstander = mutableMapOf<UUID, MutableList<TilstandType>>()
    private lateinit var utbetalingsreferanseFraUtbetalingEvent: String
    val utbetalteVedtaksperioder = mutableListOf<UUID>()
    val manglendeInntektsmeldingVedtaksperioder = mutableListOf<UUID>()

    override fun vedtaksperiodeUtbetalt(event: PersonObserver.UtbetaltEvent) {
        utbetalteVedtaksperioder.add(event.vedtaksperiodeId)
    }

    override fun vedtaksperiodeEndret(event: PersonObserver.VedtaksperiodeEndretTilstandEvent) {
        tilstander.getOrPut(event.id) { mutableListOf(TilstandType.START) }.add(event.gjeldendeTilstand)
    }

    override fun vedtaksperiodeTilUtbetaling(event: PersonObserver.UtbetalingEvent) {
        utbetalingsreferanseFraUtbetalingEvent = event.utbetalingsreferanse
    }

    override fun manglerInntektsmelding(event: PersonObserver.ManglendeInntektsmeldingEvent) {
        manglendeInntektsmeldingVedtaksperioder.add(event.vedtaksperiodeId)
    }
}
