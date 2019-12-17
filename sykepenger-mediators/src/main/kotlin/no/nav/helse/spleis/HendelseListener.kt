package no.nav.helse.spleis

import no.nav.helse.behov.Behov
import no.nav.helse.hendelser.inntektsmelding.Inntektsmelding
import no.nav.helse.hendelser.påminnelse.Påminnelse
import no.nav.helse.hendelser.søknad.NySøknad
import no.nav.helse.hendelser.søknad.SendtSøknad

interface HendelseListener {
    fun onPåminnelse(påminnelse: Påminnelse) {}
    fun onLøstBehov(behov: Behov) {}
    fun onInntektsmelding(inntektsmelding: Inntektsmelding) {}
    fun onNySøknad(søknad: NySøknad) {}
    fun onSendtSøknad(søknad: SendtSøknad) {}
}
