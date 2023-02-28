package no.nav.helse.inspectors

import no.nav.helse.person.Sykefraværstilfelleeventyr
import no.nav.helse.person.Sykefraværstilfelleeventyr.Companion.varsleObservers
import no.nav.helse.person.SykefraværstilfelleeventyrObserver

internal val List<Sykefraværstilfelleeventyr>.inspektør get() = SykefraværstilfelleeventyrInspektør(this)

internal class SykefraværstilfelleeventyrInspektør(eventyr: List<Sykefraværstilfelleeventyr>) : SykefraværstilfelleeventyrObserver{
    lateinit var event: List<SykefraværstilfelleeventyrObserver.SykefraværstilfelleeventyrObserverEvent>

    init {
        eventyr.varsleObservers(listOf(this))
    }

    override fun sykefraværstilfelle(sykefraværstilfeller: List<SykefraværstilfelleeventyrObserver.SykefraværstilfelleeventyrObserverEvent>) {
        this.event = sykefraværstilfeller
    }
}