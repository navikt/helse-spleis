package no.nav.helse.hendelser

import no.nav.helse.behov.BehovType
import java.util.*

interface HendelseObserver {
    fun onBehov(kontekstId: UUID, behov: BehovType)
}
