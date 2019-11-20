package no.nav.helse.spleis

import no.nav.helse.behov.Behov
import no.nav.helse.behov.BehovProducer
import no.nav.helse.sak.*
import no.nav.helse.sak.Vedtaksperiode.TilstandType.TIL_INFOTRYGD
import no.nav.helse.hendelser.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.hendelser.saksbehandling.ManuellSaksbehandlingHendelse
import no.nav.helse.hendelser.sykepengehistorikk.SykepengehistorikkHendelse
import no.nav.helse.hendelser.søknad.NySøknadHendelse
import no.nav.helse.hendelser.søknad.SendtSøknadHendelse
import no.nav.helse.spleis.oppgave.GosysOppgaveProducer

internal class SakMediator(private val sakRepository: SakRepository,
                           private val lagreSakDao: SakObserver,
                           private val utbetalingsreferanseRepository: UtbetalingsreferanseRepository,
                           private val lagreUtbetalingDao: SakObserver,
                           private val vedtaksperiodeProbe: VedtaksperiodeProbe = VedtaksperiodeProbe,
                           private val behovProducer: BehovProducer,
                           private val gosysOppgaveProducer: GosysOppgaveProducer,
                           private val vedtaksperiodeEventProducer: VedtaksperiodeEventProducer) : SakObserver {

    override fun sakEndret(sakEndretEvent: SakObserver.SakEndretEvent) {}

    override fun vedtaksperiodeTrengerLøsning(event: Behov) {
        behovProducer.sendNyttBehov(event)
    }

    fun håndter(nySøknadHendelse: NySøknadHendelse) =
            try {
                finnSak(nySøknadHendelse)
                        .also { sak ->
                            sak.håndter(nySøknadHendelse)
                        }
            } catch (err: UtenforOmfangException) {
                vedtaksperiodeProbe.utenforOmfang(nySøknadHendelse)
            } catch (err: SakskjemaForGammelt) {
                vedtaksperiodeProbe.forGammelSkjemaversjon(err)
            }

    fun håndter(sendtSøknadHendelse: SendtSøknadHendelse) =
            try {
                finnSak(sendtSøknadHendelse)
                        .also { sak ->
                            sak.håndter(sendtSøknadHendelse)
                        }
            } catch (err: UtenforOmfangException) {
                vedtaksperiodeProbe.utenforOmfang(sendtSøknadHendelse)
            } catch (err: SakskjemaForGammelt) {
                vedtaksperiodeProbe.forGammelSkjemaversjon(err)
            }

    fun håndter(inntektsmeldingHendelse: InntektsmeldingHendelse) =
            try {
                finnSak(inntektsmeldingHendelse).also { sak ->
                    sak.håndter(inntektsmeldingHendelse)
                }
            } catch (err: UtenforOmfangException) {
                vedtaksperiodeProbe.utenforOmfang(inntektsmeldingHendelse)
            } catch (err: SakskjemaForGammelt) {
                vedtaksperiodeProbe.forGammelSkjemaversjon(err)
            }

    fun håndter(sykepengehistorikkHendelse: SykepengehistorikkHendelse) {
        try {
            finnSak(sykepengehistorikkHendelse).also { sak ->
                sak.håndter(sykepengehistorikkHendelse)
            }
        } catch (err: SakskjemaForGammelt) {
            vedtaksperiodeProbe.forGammelSkjemaversjon(err)
        }
    }

    fun håndter(manuellSaksbehandlingHendelse: ManuellSaksbehandlingHendelse) {
        try {
            finnSak(manuellSaksbehandlingHendelse).also { sak ->
                sak.håndter(manuellSaksbehandlingHendelse)
            }
        } catch (err: SakskjemaForGammelt) {
            vedtaksperiodeProbe.forGammelSkjemaversjon(err)
        }
    }

    fun hentSak(aktørId: String): Sak? = sakRepository.hentSak(aktørId)

    fun hentSakForUtbetaling(utbetalingsreferanse: String): Sak? {
        return utbetalingsreferanseRepository.hentUtbetaling(utbetalingsreferanse)?.let {
            sakRepository.hentSak(it.aktørId)
        }
    }

    override fun vedtaksperiodeEndret(event: VedtaksperiodeObserver.StateChangeEvent) {
        if (event.currentState == TIL_INFOTRYGD) {
            gosysOppgaveProducer.opprettOppgave(event.aktørId)
        }

        vedtaksperiodeEventProducer.sendEndringEvent(event)
    }

    private fun finnSak(arbeidstakerHendelse: ArbeidstakerHendelse) =
            (sakRepository.hentSak(arbeidstakerHendelse.aktørId()) ?: Sak(aktørId = arbeidstakerHendelse.aktørId())).also {
                it.addObserver(this)
                it.addObserver(lagreSakDao)
                it.addObserver(lagreUtbetalingDao)
                it.addObserver(vedtaksperiodeProbe)
            }

}
