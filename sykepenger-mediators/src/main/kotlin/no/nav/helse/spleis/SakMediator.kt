package no.nav.helse.spleis

import no.nav.helse.Topics
import no.nav.helse.behov.Behov
import no.nav.helse.behov.BehovProbe
import no.nav.helse.hendelser.Hendelsetype
import no.nav.helse.hendelser.inntektsmelding.Inntektsmelding
import no.nav.helse.hendelser.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.hendelser.påminnelse.Påminnelse
import no.nav.helse.hendelser.saksbehandling.ManuellSaksbehandlingHendelse
import no.nav.helse.hendelser.søknad.NySøknadHendelse
import no.nav.helse.hendelser.søknad.SendtSøknadHendelse
import no.nav.helse.hendelser.søknad.Sykepengesøknad
import no.nav.helse.hendelser.ytelser.Ytelser
import no.nav.helse.sak.*
import no.nav.helse.spleis.inntektsmelding.InntektsmeldingProbe
import no.nav.helse.spleis.søknad.SøknadProbe
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

internal class SakMediator(
    private val sakRepository: SakRepository,
    private val lagreSakDao: SakObserver,
    private val utbetalingsreferanseRepository: UtbetalingsreferanseRepository,
    private val lagreUtbetalingDao: SakObserver,
    private val vedtaksperiodeProbe: VedtaksperiodeProbe = VedtaksperiodeProbe,
    private val producer: KafkaProducer<String, String>,
    hendelseConsumer: HendelseConsumer
) : SakObserver, HendelseConsumer.MessageListener {

    private val log = LoggerFactory.getLogger(SakMediator::class.java)

    init {
        hendelseConsumer.addListener(this)
    }

    override fun onPåminnelse(påminnelse: Påminnelse) {
        finnSak(påminnelse) { sak -> sak.håndter(påminnelse) }
    }

    override fun onLøstBehov(behov: Behov) {
        BehovProbe.mottattBehov(behov)

        when (behov.hendelsetype()) {
            Hendelsetype.Ytelser -> Ytelser(behov).also {
                finnSak(it) { sak -> sak.håndter(it) }
            }
            Hendelsetype.ManuellSaksbehandling -> ManuellSaksbehandlingHendelse(behov).also {
                finnSak(it) { sak -> sak.håndter(it) }
            }
        }
    }

    override fun onInntektsmelding(inntektsmelding: Inntektsmelding) {
        InntektsmeldingProbe.mottattInntektsmelding(inntektsmelding)

        InntektsmeldingHendelse(inntektsmelding).also {
            finnSak(it) { sak -> sak.håndter(it) }
        }
    }

    override fun onFremtidigSøknad(søknad: Sykepengesøknad) {
        if (søknad.type !in listOf("ARBEIDSTAKERE", "SELVSTENDIGE_OG_FRILANSERE")) return

        SøknadProbe.mottattSøknad(søknad)

        NySøknadHendelse(søknad).also {
            finnSak(it) { sak -> sak.håndter(it) }
        }
    }

    override fun onNySøknad(søknad: Sykepengesøknad) {
        if (søknad.type !in listOf("ARBEIDSTAKERE", "SELVSTENDIGE_OG_FRILANSERE")) return

        SøknadProbe.mottattSøknad(søknad)

        NySøknadHendelse(søknad).also {
            finnSak(it) { sak -> sak.håndter(it) }
        }
    }

    override fun onSendtSøknad(søknad: Sykepengesøknad) {
        if (søknad.type !in listOf("ARBEIDSTAKERE", "SELVSTENDIGE_OG_FRILANSERE")) return

        SøknadProbe.mottattSøknad(søknad)

        SendtSøknadHendelse(søknad).also {
            finnSak(it) { sak -> sak.håndter(it) }
        }
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
