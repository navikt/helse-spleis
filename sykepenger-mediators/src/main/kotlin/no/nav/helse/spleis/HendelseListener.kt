package no.nav.helse.spleis

import no.nav.helse.hendelser.*
import no.nav.helse.person.Aktivitetslogger

interface HendelseListener {
    fun onPåminnelse(påminnelse: Påminnelse) {}
    fun onYtelser(ytelser: ModelYtelser) {}
    fun onVilkårsgrunnlag(vilkårsgrunnlag: ModelVilkårsgrunnlag) {}
    fun onManuellSaksbehandling(manuellSaksbehandling: ModelManuellSaksbehandling) {}
    fun onInntektsmelding(inntektsmelding: ModelInntektsmelding) {}
    fun onNySøknad(søknad: ModelNySøknad, aktivitetslogger: Aktivitetslogger) {}
    fun onSendtSøknad(søknad: ModelSendtSøknad) {}
    fun onUnprocessedMessage(message: String) {}
}
