package no.nav.helse.dsl

import no.nav.helse.hendelser.Hendelseskontekst
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver

internal class UgyldigeSituasjonerObservatør(private val person: Person): PersonObserver {

    init {
        person.addObserver(this)
    }

    override fun vedtaksperiodeEndret(
        hendelseskontekst: Hendelseskontekst,
        event: PersonObserver.VedtaksperiodeEndretEvent
    ) {
        bekreftIngenOverlappende()
    }

    internal fun bekreftIngenOverlappende() {
        person.inspektør.vedtaksperioder()
            .filterValues { it.size > 1 }
            .forEach { (orgnr, perioder) ->
                var nåværende = perioder.first().inspektør
                perioder.subList(1, perioder.size).forEach { periode ->
                    val inspektør = periode.inspektør
                    check(!inspektør.periode.overlapperMed(nåværende.periode)) {
                        "For Arbeidsgiver $orgnr overlapper Vedtaksperiode ${inspektør.id} (${inspektør.periode}) og Vedtaksperiode ${nåværende.id} (${nåværende.periode}) med hverandre!"
                    }
                    nåværende = inspektør
                }
            }
    }
}