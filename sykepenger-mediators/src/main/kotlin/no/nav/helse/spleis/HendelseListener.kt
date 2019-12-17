package no.nav.helse.spleis

import no.nav.helse.behov.Behov
import no.nav.helse.hendelser.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.hendelser.påminnelse.Påminnelse
import no.nav.helse.hendelser.søknad.NySøknadHendelse
import no.nav.helse.hendelser.søknad.SendtSøknadHendelse

interface HendelseListener {
    fun onPåminnelse(påminnelse: Påminnelse) {}
    fun onLøstBehov(behov: Behov) {}
    fun onInntektsmelding(inntektsmelding: InntektsmeldingHendelse) {}
    fun onNySøknad(søknad: NySøknadHendelse) {}
    fun onSendtSøknad(søknad: SendtSøknadHendelse) {}
}
