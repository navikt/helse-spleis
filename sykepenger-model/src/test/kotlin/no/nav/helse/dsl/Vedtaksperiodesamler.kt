package no.nav.helse.dsl

import java.util.UUID
import no.nav.helse.person.PersonObserver
import no.nav.helse.somOrganisasjonsnummer

internal class Vedtaksperiodesamler : PersonObserver {
    private val vedtaksperioder = mutableMapOf<String, MutableSet<UUID>>()
    private fun forrigeFor(orgnummer: String) = vedtaksperioder.getOrDefault(orgnummer, emptySet()).lastOrNull()

    internal fun vedtaksperiodeId(orgnummer: String, indeks: Int) =
        vedtaksperioder.getValue(orgnummer).elementAt(indeks)

    internal fun fangVedtaksperiode(orgnummer: String, block: () -> Any): UUID? {
        val forrige = forrigeFor(orgnummer)
        block()
        return forrigeFor(orgnummer)?.takeUnless { it == forrige }
    }

    override fun vedtaksperiodeEndret(
        event: PersonObserver.VedtaksperiodeEndretEvent
    ) {
        vedtaksperioder.getOrPut(event.yrkesaktivitetssporing.somOrganisasjonsnummer) { mutableSetOf() }.add(event.vedtaksperiodeId)
    }
}
