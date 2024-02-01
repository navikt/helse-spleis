package no.nav.helse.spekemat

import no.nav.helse.Toggle
import no.nav.helse.person.PersonObserver
import no.nav.helse.spekemat.fabrikk.Pølse
import no.nav.helse.spekemat.fabrikk.Pølsefabrikk
import no.nav.helse.spekemat.fabrikk.Pølsestatus

class Spekemat : PersonObserver {
    private val hendelser = mutableListOf<Any>()
    private val arbeidsgivere = mutableMapOf<String, Pølsefabrikk>()

    fun resultat(orgnr: String) =
        if (Toggle.Spekemat.enabled) arbeidsgivere.getValue(orgnr).pakke()
        else emptyList()

    override fun nyGenerasjon(event: PersonObserver.GenerasjonOpprettetEvent) {
        hendelser.add(event)
        arbeidsgivere.getOrPut(event.organisasjonsnummer) { Pølsefabrikk() }
            .nyPølse(Pølse(
                vedtaksperiodeId = event.vedtaksperiodeId,
                generasjonId = event.generasjonId,
                status = Pølsestatus.ÅPEN,
                kilde = event.kilde.meldingsreferanseId
            ))
    }

    override fun generasjonLukket(event: PersonObserver.GenerasjonLukketEvent) {
        hendelser.add(event)
        arbeidsgivere.getValue(event.organisasjonsnummer)
            .oppdaterPølse(event.vedtaksperiodeId, event.generasjonId, Pølsestatus.LUKKET)
    }

    override fun generasjonForkastet(event: PersonObserver.GenerasjonForkastetEvent) {
        hendelser.add(event)
        arbeidsgivere.getValue(event.organisasjonsnummer)
            .oppdaterPølse(event.vedtaksperiodeId, event.generasjonId, Pølsestatus.FORKASTET)
    }
}