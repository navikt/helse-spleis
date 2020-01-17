package no.nav.helse.spleis

import no.nav.helse.hendelser.*
import no.nav.helse.person.Aktivitetslogger

interface HendelseListener {
    fun onPåminnelse(påminnelse: Påminnelse) {}
    fun onYtelser(ytelser: Ytelser) {}
    fun onVilkårsgrunnlag(vilkårsgrunnlag: Vilkårsgrunnlag) {}
    fun onManuellSaksbehandling(manuellSaksbehandling: ManuellSaksbehandling) {}
    fun onInntektsmelding(inntektsmelding: Inntektsmelding) {}
    fun onNySøknad(søknad: NySøknad) {}
    fun onSendtSøknad(søknad: SendtSøknad) {}
    fun onUnprocessedMessage(message: String) {}
    fun onNySøknad(søknad: ModelNySøknad, aktivitetslogger: Aktivitetslogger) {}
}
