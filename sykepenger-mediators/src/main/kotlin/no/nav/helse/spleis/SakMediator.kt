package no.nav.helse.spleis

import no.nav.helse.behov.Behov
import no.nav.helse.behov.BehovProducer
import no.nav.helse.sak.*
import no.nav.helse.sak.Sakskompleks.TilstandType.TIL_INFOTRYGD
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
                           private val sakskompleksProbe: SakskompleksProbe = SakskompleksProbe,
                           private val behovProducer: BehovProducer,
                           private val gosysOppgaveProducer: GosysOppgaveProducer,
                           private val sakskompleksEventProducer: SakskompleksEventProducer) : SakObserver {

    override fun sakEndret(sakEndretEvent: SakObserver.SakEndretEvent) {}

    override fun sakskompleksTrengerLøsning(event: Behov) {
        behovProducer.sendNyttBehov(event)
    }

    fun håndterNySøknad(nySøknadHendelse: NySøknadHendelse) =
            try {
                finnSak(nySøknadHendelse)
                        .also { sak ->
                            sak.håndterNySøknad(nySøknadHendelse)
                        }
            } catch (err: UtenforOmfangException) {
                sakskompleksProbe.utenforOmfang(err, nySøknadHendelse)
            } catch (err: SakskjemaForGammelt) {
                sakskompleksProbe.forGammelSkjemaversjon(err)
            }

    fun håndterSendtSøknad(sendtSøknadHendelse: SendtSøknadHendelse) =
            try {
                finnSak(sendtSøknadHendelse)
                        .also { sak ->
                            sak.håndterSendtSøknad(sendtSøknadHendelse)
                        }
            } catch (err: UtenforOmfangException) {
                sakskompleksProbe.utenforOmfang(err, sendtSøknadHendelse)
            } catch (err: SakskjemaForGammelt) {
                sakskompleksProbe.forGammelSkjemaversjon(err)
            }

    fun håndterInntektsmelding(inntektsmeldingHendelse: InntektsmeldingHendelse) =
            try {
                finnSak(inntektsmeldingHendelse).also { sak ->
                    sak.håndterInntektsmelding(inntektsmeldingHendelse)
                }
            } catch (err: UtenforOmfangException) {
                sakskompleksProbe.utenforOmfang(err, inntektsmeldingHendelse)
            } catch (err: SakskjemaForGammelt) {
                sakskompleksProbe.forGammelSkjemaversjon(err)
            }

    fun håndterSykepengehistorikk(sykepengehistorikkHendelse: SykepengehistorikkHendelse) {
        try {
            finnSak(sykepengehistorikkHendelse).also { sak ->
                sak.håndterSykepengehistorikk(sykepengehistorikkHendelse)
            }
        } catch (err: SakskjemaForGammelt) {
            sakskompleksProbe.forGammelSkjemaversjon(err)
        }
    }

    fun håndterManuellSaksbehandling(manuellSaksbehandlingHendelse: ManuellSaksbehandlingHendelse) {
        try {
            finnSak(manuellSaksbehandlingHendelse).also { sak ->
                sak.håndterManuellSaksbehandling(manuellSaksbehandlingHendelse)
            }
        } catch (err: SakskjemaForGammelt) {
            sakskompleksProbe.forGammelSkjemaversjon(err)
        }
    }

    fun hentSak(aktørId: String): Sak? = sakRepository.hentSak(aktørId)

    fun hentSakForUtbetaling(utbetalingsreferanse: String): Sak? {
        return utbetalingsreferanseRepository.hentUtbetaling(utbetalingsreferanse)?.let {
            sakRepository.hentSak(it.aktørId)
        }
    }

    override fun sakskompleksEndret(event: SakskompleksObserver.StateChangeEvent) {
        if (event.currentState == TIL_INFOTRYGD) {
            gosysOppgaveProducer.opprettOppgave(event.aktørId)
        }

        sakskompleksEventProducer.sendEndringEvent(event)
    }

    private fun finnSak(arbeidstakerHendelse: ArbeidstakerHendelse) =
            (sakRepository.hentSak(arbeidstakerHendelse.aktørId()) ?: Sak(aktørId = arbeidstakerHendelse.aktørId())).also {
                it.addObserver(this)
                it.addObserver(lagreSakDao)
                it.addObserver(lagreUtbetalingDao)
                it.addObserver(sakskompleksProbe)
            }

}
