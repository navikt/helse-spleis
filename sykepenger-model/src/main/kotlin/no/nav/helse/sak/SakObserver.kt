package no.nav.helse.sak

interface SakObserver : SakskompleksObserver {
    data class SakEndretEvent(val aktørId: String,
                              val sykdomshendelse: ArbeidstakerHendelse,
                              val memento: Sak.Memento)

    fun sakEndret(sakEndretEvent: SakEndretEvent)
}
