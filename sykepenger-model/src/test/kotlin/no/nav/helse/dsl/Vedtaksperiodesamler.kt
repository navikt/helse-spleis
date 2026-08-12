package no.nav.helse.dsl

import java.util.UUID
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.Person
import no.nav.helse.person.EventSubscription
import no.nav.helse.somOrganisasjonsnummer

internal class Vedtaksperiodesamler(person: Person? = null) : EventSubscription {
    private val vedtaksperioder = person?.inspektør?.vedtaksperioder()?.mapValues { (_, perioder) ->
        perioder.map { it.inspektør.id }.toMutableSet()
    }?.toMutableMap() ?: mutableMapOf()

    private fun forrigeFor(orgnummer: String) = vedtaksperioder.getOrDefault(orgnummer, emptySet()).lastOrNull()

    internal fun vedtaksperiodeId(orgnummer: String, indeks: Int) =
        vedtaksperioder.getValue(orgnummer).elementAt(indeks)

    internal fun sisteVedtaksperiode(orgnummer: String) = vedtaksperioder.getValue(orgnummer).last()

    internal fun fangVedtaksperiode(orgnummer: String, block: () -> Any): UUID? {
        val forrige = forrigeFor(orgnummer)
        block()
        return forrigeFor(orgnummer)?.takeUnless { it == forrige }
    }

    override fun vedtaksperiodeEndret(
        event: EventSubscription.VedtaksperiodeEndretEvent
    ) {
        vedtaksperioder.getOrPut(event.yrkesaktivitetssporing.somOrganisasjonsnummer) { mutableSetOf() }.add(event.vedtaksperiodeId)
    }
}
