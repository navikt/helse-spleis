package no.nav.helse.hendelser

import no.nav.helse.behov.BehovType

interface HendelseObserver {
    fun onBehov(behov: BehovType)
}
