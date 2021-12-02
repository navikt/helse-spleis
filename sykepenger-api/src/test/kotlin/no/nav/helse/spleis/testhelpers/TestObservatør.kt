package no.nav.helse.spleis.testhelpers

import no.nav.helse.hendelser.Hendelseskontekst
import no.nav.helse.person.*
import no.nav.helse.person.PersonObserver.VedtaksperiodeEndretEvent
import org.junit.jupiter.api.fail
import java.util.*

internal class TestObservatør : PersonObserver {
    private lateinit var sisteVedtaksperiode: UUID

    private val tilstandsendringer = mutableMapOf<UUID, MutableList<TilstandType>>()
    private val utbetalteVedtaksperioder = mutableListOf<UUID>()
    private val vedtaksperioder = mutableMapOf<String, MutableSet<UUID>>()
    private val vedtaksperiodeendringer = mutableMapOf<UUID, MutableList<VedtaksperiodeEndretEvent>>()

    fun hendelseider(vedtaksperiodeId: UUID) =
        vedtaksperiodeendringer[vedtaksperiodeId]?.last()?.hendelser ?: fail { "VedtaksperiodeId $vedtaksperiodeId har ingen hendelser tilknyttet" }

    fun vedtaksperiode(orgnummer: String, indeks: Int) = vedtaksperioder.getValue(orgnummer).toList()[indeks]

    override fun vedtaksperiodeEndret(hendelseskontekst: Hendelseskontekst, event: VedtaksperiodeEndretEvent) {
        sisteVedtaksperiode = hendelseskontekst.vedtaksperiodeId()
        vedtaksperiodeendringer.getOrPut(hendelseskontekst.vedtaksperiodeId()) { mutableListOf(event) }.add(event)
        vedtaksperioder.getOrPut(hendelseskontekst.orgnummer()) { mutableSetOf() }.add(sisteVedtaksperiode)
        if (event.gjeldendeTilstand != event.forrigeTilstand) {
            tilstandsendringer.getOrPut(hendelseskontekst.vedtaksperiodeId()) { mutableListOf(TilstandType.START) }.add(event.gjeldendeTilstand)
        }
        if (event.gjeldendeTilstand == TilstandType.AVSLUTTET) utbetalteVedtaksperioder.add(hendelseskontekst.vedtaksperiodeId())
    }

    companion object {
        private fun Hendelseskontekst.toMap() = mutableMapOf<String, String>().also { appendTo(it::set) }

        private fun Hendelseskontekst.vedtaksperiodeId(): UUID {
            return UUID.fromString(toMap()["vedtaksperiodeId"])
        }

        private fun Hendelseskontekst.orgnummer(): String {
            return toMap()["organisasjonsnummer"]!!
        }
    }
}
