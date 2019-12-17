package no.nav.helse.spleis

import no.nav.helse.behov.Behov
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.NySøknad
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.SendtSøknad

interface HendelseListener {
    fun onPåminnelse(påminnelse: Påminnelse) {}
    fun onLøstBehov(behov: Behov) {}
    fun onInntektsmelding(inntektsmelding: Inntektsmelding) {}
    fun onNySøknad(søknad: NySøknad) {}
    fun onSendtSøknad(søknad: SendtSøknad) {}
}
