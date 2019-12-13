package no.nav.helse.spleis

import no.nav.helse.Topics
import no.nav.helse.behov.Behov
import no.nav.helse.hendelser.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.hendelser.påminnelse.Påminnelse
import no.nav.helse.hendelser.saksbehandling.ManuellSaksbehandlingHendelse
import no.nav.helse.hendelser.sykepengehistorikk.SykepengehistorikkHendelse
import no.nav.helse.hendelser.søknad.NySøknadHendelse
import no.nav.helse.hendelser.søknad.SendtSøknadHendelse
import no.nav.helse.sak.ArbeidstakerHendelse
import no.nav.helse.sak.Sak
import no.nav.helse.sak.SakObserver
import no.nav.helse.sak.SakskjemaForGammelt
import no.nav.helse.sak.UtenforOmfangException
import no.nav.helse.sak.VedtaksperiodeObserver
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

internal class SakMediator(
    private val sakRepository: SakRepository,
    private val lagreSakDao: SakObserver,
    private val utbetalingsreferanseRepository: UtbetalingsreferanseRepository,
    private val lagreUtbetalingDao: SakObserver,
    private val vedtaksperiodeProbe: VedtaksperiodeProbe = VedtaksperiodeProbe,
    private val producer: KafkaProducer<String, String>
) : SakObserver {

    private val log = LoggerFactory.getLogger("SakMediator")

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

    fun håndter(påminnelse: Påminnelse) {
        finnSak(påminnelse) { sak -> sak.håndter(påminnelse) }
    }

    fun hentSak(aktørId: String): Sak? = sakRepository.hentSak(aktørId)

    fun hentSakForUtbetaling(utbetalingsreferanse: String): Sak? {
        return utbetalingsreferanseRepository.hentUtbetaling(utbetalingsreferanse)?.let {
            sakRepository.hentSak(it.aktørId)
        }
    }

    override fun sakEndret(sakEndretEvent: SakObserver.SakEndretEvent) {}

    override fun vedtaksperiodeTrengerLøsning(event: Behov) {
        producer.send(event.producerRecord()).also {
            log.info("produserte behov=$event, recordMetadata=$it")
        }
    }

    override fun vedtaksperiodeEndret(event: VedtaksperiodeObserver.StateChangeEvent) {
        producer.send(event.producerRecord())
    }

    override fun vedtaksperiodeTilUtbetaling(event: VedtaksperiodeObserver.UtbetalingEvent) {
        producer.send(event.producerRecord())
    }

    private fun Behov.producerRecord() =
        ProducerRecord<String, String>(Topics.behovTopic, id().toString(), toJson())

    private fun finnSak(arbeidstakerHendelse: ArbeidstakerHendelse) =
        (sakRepository.hentSak(arbeidstakerHendelse.aktørId()) ?: Sak(
            aktørId = arbeidstakerHendelse.aktørId(),
            fødselsnummer = arbeidstakerHendelse.fødselsnummer()
        )).also {
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
