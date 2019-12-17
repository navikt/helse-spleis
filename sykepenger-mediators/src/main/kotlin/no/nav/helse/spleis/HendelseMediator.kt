package no.nav.helse.spleis

import no.nav.helse.behov.Behov
import no.nav.helse.hendelser.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.hendelser.påminnelse.Påminnelse
import no.nav.helse.hendelser.søknad.NySøknadHendelse
import no.nav.helse.hendelser.søknad.SendtSøknadHendelse

internal class HendelseMediator(
    consumer: HendelseConsumer,
    private val lagreHendelseDao: HendelseConsumer.MessageListener,
    private val sakMediator: SakMediator
) : HendelseConsumer.MessageListener {

    private val hendelseProbe = HendelseProbe()

    init {
        consumer.addListener(hendelseProbe)
        consumer.addListener(this)
    }

    override fun onPåminnelse(påminnelse: Påminnelse) {
        lagreHendelseDao.onPåminnelse(påminnelse)
        sakMediator.onPåminnelse(påminnelse)
    }

    override fun onLøstBehov(behov: Behov) {
        lagreHendelseDao.onLøstBehov(behov)
        sakMediator.onLøstBehov(behov)
    }

    override fun onInntektsmelding(inntektsmelding: InntektsmeldingHendelse) {
        lagreHendelseDao.onInntektsmelding(inntektsmelding)
        sakMediator.onInntektsmelding(inntektsmelding)
    }

    override fun onNySøknad(søknad: NySøknadHendelse) {
        lagreHendelseDao.onNySøknad(søknad)
        sakMediator.onNySøknad(søknad)
    }

    override fun onSendtSøknad(søknad: SendtSøknadHendelse) {
        lagreHendelseDao.onSendtSøknad(søknad)
        sakMediator.onSendtSøknad(søknad)
    }
}
