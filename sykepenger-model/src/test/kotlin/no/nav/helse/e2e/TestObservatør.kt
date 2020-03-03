package no.nav.helse.e2e

import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType
import org.junit.jupiter.api.Assertions

internal class TestObservatør : PersonObserver {
    internal var endreTeller = 0
    private var periodeIndek = -1
    private val periodeIndekser = mutableMapOf<String, Int>()
    private val vedtaksperiodeIder = mutableMapOf<Int, String>()
    internal val tilstander = mutableMapOf<Int, MutableList<TilstandType>>()
    internal lateinit var utbetalingsreferanseFraUtbetalingEvent: String
    val utbetalteVedtaksperioder = mutableListOf<String>()

    override fun vedtaksperiodeUtbetalt(event: PersonObserver.UtbetaltEvent) {
        utbetalteVedtaksperioder.add(event.vedtaksperiodeId.toString())
    }

    internal fun vedtaksperiodeIder(indeks: Int) = vedtaksperiodeIder[indeks] ?: Assertions.fail(
        "Missing vedtaksperiodeId"
    )

    override fun vedtaksperiodeEndret(event: PersonObserver.VedtaksperiodeEndretTilstandEvent) {
        endreTeller += 1
        val indeks = periodeIndeks(event.id.toString())
        tilstander[indeks]?.add(event.gjeldendeTilstand) ?: Assertions.fail("Missing collection initialization")
    }

    override fun vedtaksperiodeTilUtbetaling(event: PersonObserver.UtbetalingEvent) {
        utbetalingsreferanseFraUtbetalingEvent = event.utbetalingsreferanse
    }

    private fun periodeIndeks(vedtaksperiodeId: String): Int {
        return periodeIndekser.getOrPut(vedtaksperiodeId, {
            periodeIndek++
            tilstander[periodeIndek] = mutableListOf(TilstandType.START)
            vedtaksperiodeIder[periodeIndek] = vedtaksperiodeId
            periodeIndek
        })
    }
}
