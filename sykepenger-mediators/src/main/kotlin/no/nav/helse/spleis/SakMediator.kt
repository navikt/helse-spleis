package no.nav.helse.spleis

import no.nav.helse.Topics
import no.nav.helse.behov.Behov
import no.nav.helse.hendelser.*
import no.nav.helse.sak.*
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
) : SakObserver, HendelseListener {

    private val log = LoggerFactory.getLogger(SakMediator::class.java)

    override fun onPåminnelse(påminnelse: Påminnelse) {
        finnSak(påminnelse) { sak -> sak.håndter(påminnelse) }
    }

    override fun onYtelser(ytelser: Ytelser) {
        finnSak(ytelser) { sak -> sak.håndter(ytelser) }
    }

    override fun onManuellSaksbehandling(manuellSaksbehandling: ManuellSaksbehandling) {
        finnSak(manuellSaksbehandling) { sak -> sak.håndter(manuellSaksbehandling) }
    }

    override fun onInntektsmelding(inntektsmelding: Inntektsmelding) {
        finnSak(inntektsmelding) { sak -> sak.håndter(inntektsmelding) }
    }

    override fun onNySøknad(søknad: NySøknad) {
        finnSak(søknad) { sak -> sak.håndter(søknad) }
    }

    override fun onSendtSøknad(søknad: SendtSøknad) {
        finnSak(søknad) { sak -> sak.håndter(søknad) }
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
