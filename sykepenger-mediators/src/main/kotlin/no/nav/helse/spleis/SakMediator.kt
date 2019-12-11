package no.nav.helse.spleis

import no.nav.helse.behov.Behov
import no.nav.helse.behov.BehovProducer
import no.nav.helse.hendelser.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.hendelser.saksbehandling.ManuellSaksbehandlingHendelse
import no.nav.helse.hendelser.sykepengehistorikk.SykepengehistorikkHendelse
import no.nav.helse.hendelser.søknad.NySøknadHendelse
import no.nav.helse.hendelser.søknad.SendtSøknadHendelse
import no.nav.helse.sak.*
import no.nav.helse.sak.TilstandType.TIL_INFOTRYGD
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

    fun håndter(hendelse: NySøknadHendelse) =
        finnSak(hendelse) { sak -> sak.håndter(hendelse) }

    fun håndter(hendelse: SendtSøknadHendelse) =
        finnSak(hendelse) { sak -> sak.håndter(hendelse) }

    fun håndter(hendelse: InntektsmeldingHendelse) =
        finnSak(hendelse) { sak -> sak.håndter(hendelse) }

    fun håndter(hendelse: SykepengehistorikkHendelse) =
        finnSak(hendelse) { sak -> sak.håndter(hendelse) }

    fun håndter(hendelse: ManuellSaksbehandlingHendelse) =
        finnSak(hendelse) { sak -> sak.håndter(hendelse) }

    fun hentSak(aktørId: String): Sak? = sakRepository.hentSak(aktørId)

    fun hentSakForUtbetaling(utbetalingsreferanse: String): Sak? {
        return utbetalingsreferanseRepository.hentUtbetaling(utbetalingsreferanse)?.let {
            sakRepository.hentSak(it.aktørId)
        }
    }

    override fun vedtaksperiodeEndret(event: VedtaksperiodeObserver.StateChangeEvent) {
        if (event.gjeldendeTilstand == TIL_INFOTRYGD) {
            gosysOppgaveProducer.opprettOppgave(event.aktørId, event.fødselsnummer)
        }

        vedtaksperiodeEventProducer.sendEndringEvent(event)
    }

    private fun finnSak(arbeidstakerHendelse: ArbeidstakerHendelse) =
            (sakRepository.hentSak(arbeidstakerHendelse.aktørId()) ?: Sak(aktørId = arbeidstakerHendelse.aktørId(), fødselsnummer = arbeidstakerHendelse.fødselsnummer())).also {
                it.addObserver(this)
                it.addObserver(lagreSakDao)
                it.addObserver(lagreUtbetalingDao)
                it.addObserver(vedtaksperiodeProbe)
            }

    private fun finnSak(
        hendelse: ArbeidstakerHendelse,
        block: (Sak) -> Unit
    ) {
        try {
            block(finnSak(hendelse))
        } catch (err: UtenforOmfangException) {
            vedtaksperiodeProbe.utenforOmfang(hendelse)
        } catch (err: SakskjemaForGammelt) {
            vedtaksperiodeProbe.forGammelSkjemaversjon(err)
        }
    }

}
