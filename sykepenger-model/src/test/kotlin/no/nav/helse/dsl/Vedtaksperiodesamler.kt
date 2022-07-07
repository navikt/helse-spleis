package no.nav.helse.dsl

import java.util.UUID
import no.nav.helse.hendelser.Hendelseskontekst
import no.nav.helse.person.PersonObserver
import org.junit.jupiter.api.Assertions.assertNotEquals

internal class Vedtaksperiodesamler : PersonObserver {
    private var sisteVedtaksperiode: UUID? = null
    private val vedtaksperioder = mutableMapOf<String, MutableSet<UUID>>()

    internal fun vedtaksperiodeId(orgnummer: String, indeks: Int) =
        vedtaksperioder.getValue(orgnummer).elementAt(indeks)

    internal fun fangVedtaksperiode(block: () -> Any): UUID {
        val forrige = sisteVedtaksperiode
        block()
        assertNotEquals(forrige, sisteVedtaksperiode) { "Det ble ikke opprettet noen vedtaksperiode, forrige er $forrige" }
        return requireNotNull(sisteVedtaksperiode)
    }

    override fun vedtaksperiodeEndret(
        hendelseskontekst: Hendelseskontekst,
        event: PersonObserver.VedtaksperiodeEndretEvent
    ) {
        val detaljer = mutableMapOf<String, String>().apply { hendelseskontekst.appendTo(this::put) }
        val orgnr = detaljer.getValue("organisasjonsnummer")
        val vedtaksperiodeId = UUID.fromString(detaljer.getValue("vedtaksperiodeId"))

        sisteVedtaksperiode = vedtaksperiodeId
        vedtaksperioder.getOrPut(orgnr) { mutableSetOf() }.add(vedtaksperiodeId)
    }
}