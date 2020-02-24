package no.nav.helse.spleis

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.Topics
import no.nav.helse.behov.BehovType
import no.nav.helse.hendelser.HendelseObserver
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.*
import no.nav.helse.serde.SerialisertPerson
import no.nav.helse.spleis.db.HendelseRecorder
import no.nav.helse.spleis.db.PersonRepository
import no.nav.helse.spleis.hendelser.JsonMessage
import no.nav.helse.spleis.hendelser.MessageProcessor
import no.nav.helse.spleis.hendelser.Parser
import no.nav.helse.spleis.hendelser.model.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource
import kotlin.system.exitProcess

// Understands how to communicate messages to other objects
// Acts like a GoF Mediator to forward messages to observers
// Uses GoF Observer pattern to notify events
internal class HendelseMediator(
    rapid: KafkaRapid,
    private val personRepository: PersonRepository,
    private val lagrePersonDao: PersonObserver,
    private val lagreUtbetalingDao: PersonObserver,
    private val vedtaksperiodeProbe: VedtaksperiodeProbe = VedtaksperiodeProbe,
    private val producer: KafkaProducer<String, String>,
    private val hendelseProbe: HendelseProbe,
    private val hendelseRecorder: HendelseRecorder,
    private val dataSource: DataSource? = null
) : Parser.ParserDirector {
    private val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")
    private val parser = Parser(this)

    private val personObserver = PersonMediator(producer, sikkerLogg)

    init {
        rapid.addListener(parser)

        parser.register(NySøknadMessage.Factory)
        parser.register(SendtSøknadMessage.Factory)
        parser.register(InntektsmeldingMessage.Factory)
        parser.register(YtelserMessage.Factory)
        parser.register(VilkårsgrunnlagMessage.Factory)
        parser.register(ManuellSaksbehandlingMessage.Factory)
        parser.register(UtbetalingMessage.Factory)
        parser.register(PåminnelseMessage.Factory)
    }

    override fun onRecognizedMessage(
        message: JsonMessage,
        aktivitetslogger: Aktivitetslogger,
        aktivitetslogg: Aktivitetslogg
    ) {
        dataSource?.also {
            using(sessionOf(it)) { session ->
                session.list(queryOf("select data from person where fnr in (select (data ->> 'fnr')::text from melding where lest_dato > ? AND melding_type=? AND (data -> 'arbeidGjenopptatt')::text != 'null') AND id in(select max(id) from person group by aktor_id)", LocalDateTime.parse("2020-02-18T17:48:00.000"), "SENDT_SØKNAD")) {
                    SerialisertPerson(it.string("data"))
                }
            }
                .also { sikkerLogg.info("Hentet {} personer som skal invalideres", it.size) }
                .map { it.deserialize() }
                .onEach {
                    it.addObserver(personObserver)
                    it.addObserver(lagrePersonDao)
                    it.addObserver(lagreUtbetalingDao)
                    it.addObserver(vedtaksperiodeProbe)
                }
                .forEach {
                    sikkerLogg.info("Invaliderer alle perioder for person på grunn av produksjonsfeil som kan ha tatt bort arbeidsdager fra sykdomstidslinjene")
                    it.invaliderPerioder()
                }
            exitProcess(0)
        }

        val messageProcessor = Processor(BehovMediator(producer, aktivitetslogg, sikkerLogg))
        try {
            message.accept(hendelseRecorder)
            message.accept(messageProcessor)

            if (aktivitetslogger.hasErrorsOld()) {
                sikkerLogg.error("aktivitetslogger inneholder errors: ${aktivitetslogger.toReport()}")
            } else if (aktivitetslogger.hasMessagesOld()) {
                sikkerLogg.info("aktivitetslogger inneholder meldinger: ${aktivitetslogger.toReport()}")
            }
        } catch (err: Aktivitetslogger.AktivitetException) {
            sikkerLogg.error("feil på melding: $err")
        } catch (err: UtenforOmfangException) {
            sikkerLogg.error("melding er utenfor omfang: ${err.message}", err)
        } finally {
            AktivitetsloggerProbe.inspiser(aktivitetslogger)
        }
    }

    override fun onMessageError(aktivitetException: Aktivitetslogger.AktivitetException) {
        sikkerLogg.error("feil på melding: ${aktivitetException.message}", aktivitetException)
        AktivitetsloggerProbe.inspiser(aktivitetException)
    }

    override fun onMessageError(aktivitetException: Aktivitetslogg.AktivitetException) {
        sikkerLogg.error("feil på melding: ${aktivitetException.message}", aktivitetException)
        // TODO: pull message counts from Aktivitetslogg for statistics
//        AktivitetsloggerProbe.inspiser(aktivitetException)
    }

    override fun onUnrecognizedMessage(aktivitetslogger: Aktivitetslogger, aktivitetslogg: Aktivitetslogg) {
        sikkerLogg.debug("ukjent melding: ${aktivitetslogger.toReport()}")
    }

    private inner class Processor(private val behovMediator: BehovMediator) : MessageProcessor {
        override fun process(message: NySøknadMessage, aktivitetslogger: Aktivitetslogger) {
            val sykmelding = message.asSykmelding()

            hendelseProbe.onSykmelding(sykmelding)
            person(sykmelding).håndter(sykmelding)
            behovMediator.finalize(sykmelding)
        }

        override fun process(message: SendtSøknadMessage, aktivitetslogger: Aktivitetslogger) {
            val søknad = message.asSøknad()
            hendelseProbe.onSøknad(søknad)
            person(søknad).håndter(søknad)
            behovMediator.finalize(søknad)
        }

        override fun process(message: InntektsmeldingMessage, aktivitetslogger: Aktivitetslogger) {
            val inntektsmelding = message.asInntektsmelding()
            hendelseProbe.onInntektsmelding(inntektsmelding)
            person(inntektsmelding).håndter(inntektsmelding)
            behovMediator.finalize(inntektsmelding)
        }

        override fun process(message: YtelserMessage, aktivitetslogger: Aktivitetslogger) {
            val ytelser = message.asYtelser()
            hendelseProbe.onYtelser(ytelser)
            person(ytelser).håndter(ytelser)
            behovMediator.finalize(ytelser)
        }

        override fun process(message: VilkårsgrunnlagMessage, aktivitetslogger: Aktivitetslogger) {
            val vilkårsgrunnlag = message.asVilkårsgrunnlag()
            hendelseProbe.onVilkårsgrunnlag(vilkårsgrunnlag)
            person(vilkårsgrunnlag).håndter(vilkårsgrunnlag)
            behovMediator.finalize(vilkårsgrunnlag)
        }

        override fun process(message: ManuellSaksbehandlingMessage, aktivitetslogger: Aktivitetslogger) {
            val manuellSaksbehandling = message.asManuellSaksbehandling()
            hendelseProbe.onManuellSaksbehandling(manuellSaksbehandling)
            person(manuellSaksbehandling).håndter(manuellSaksbehandling)
            behovMediator.finalize(manuellSaksbehandling)
        }

        override fun process(message: UtbetalingMessage, aktivitetslogger: Aktivitetslogger) {
            val utbetaling = message.asUtbetaling()
            hendelseProbe.onUtbetaling(utbetaling)
            person(utbetaling).håndter(utbetaling)
            behovMediator.finalize(utbetaling)
        }

        override fun process(message: PåminnelseMessage, aktivitetslogger: Aktivitetslogger) {
            val påminnelse = message.asPåminnelse()
            hendelseProbe.onPåminnelse(påminnelse)
            person(påminnelse).håndter(påminnelse)
            behovMediator.finalize(påminnelse)
        }

        private fun person(arbeidstakerHendelse: ArbeidstakerHendelse): Person {
            arbeidstakerHendelse.addObserver(behovMediator)
            arbeidstakerHendelse.addObserver(vedtaksperiodeProbe)
            return (personRepository.hentPerson(arbeidstakerHendelse.aktørId()) ?: Person(
                aktørId = arbeidstakerHendelse.aktørId(),
                fødselsnummer = arbeidstakerHendelse.fødselsnummer()
            )).also {
                it.addObserver(personObserver)
                it.addObserver(lagrePersonDao)
                it.addObserver(lagreUtbetalingDao)
                it.addObserver(vedtaksperiodeProbe)
            }
        }
    }

    private class PersonMediator(
        private val producer: KafkaProducer<String, String>,
        private val sikkerLogg: Logger
    ) : PersonObserver {
        private val log = LoggerFactory.getLogger(this::class.java)

        override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {}

        override fun vedtaksperiodePåminnet(påminnelse: Påminnelse) {
            producer.send(påminnelse.producerRecord()).get()
        }

        override fun vedtaksperiodeEndret(event: PersonObserver.VedtaksperiodeEndretTilstandEvent) {
            producer.send(event.producerRecord()).get()
        }

        override fun vedtaksperiodeUtbetalt(event: PersonObserver.UtbetaltEvent) {
            producer.send(event.producerRecord()).get().also {
                log.info(
                    "legger utbetalt vedtak: {} på topic med {} og {}",
                    keyValue("vedtaksperiodeId", event.vedtaksperiodeId),
                    keyValue("partisjon", it.partition()),
                    keyValue("offset", it.offset())
                )
            }
        }

        override fun vedtaksperiodeIkkeFunnet(vedtaksperiodeEvent: PersonObserver.VedtaksperiodeIkkeFunnetEvent) {
            producer.send(vedtaksperiodeEvent.producerRecord())
        }
    }

    private class BehovMediator(
        private val producer: KafkaProducer<String, String>,
        private val aktivitetslogg: Aktivitetslogg,
        private val sikkerLogg: Logger
    ) : HendelseObserver {
        private companion object {
            private val objectMapper = jacksonObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .registerModule(JavaTimeModule())
        }

        private val behov = mutableListOf<BehovType>()
        override fun onBehov(behov: BehovType) {
            this.behov.add(behov)
        }

        fun finalize(hendelse: ArbeidstakerHendelse) {
            if (behov.isEmpty()) return
            sikkerLogg.info("sender ${behov.size} needs: ${behov.map { it.navn }} pga. ${hendelse::class.simpleName}")
            producer.send(
                ProducerRecord(
                    Topics.rapidTopic,
                    behov.first().fødselsnummer,
                    behov.toJson(aktivitetslogg).also {
                        sikkerLogg.info("sender $it pga. ${hendelse::class.simpleName}")
                    })
            )
        }

        private fun List<BehovType>.toJson(aktivitetslogg: Aktivitetslogg) =
            fold(objectMapper.createObjectNode()) { acc: ObjectNode, behovType: BehovType ->
                val node = objectMapper.convertValue<ObjectNode>(behovType.toMap())
                if (acc.harKonflikterMed(node))
                    aktivitetslogg.severe("Prøvde å sette sammen behov med konfliktende verdier, $acc og $node")
                acc.withArray("@behov").add(behovType.navn)
                acc.setAll(node)
            }
                .put("@event_name", "behov")
                .set<ObjectNode>("@opprettet", objectMapper.convertValue(LocalDateTime.now()))
                .set<ObjectNode>("@id", objectMapper.convertValue(UUID.randomUUID()))
                .toString()

        private fun ObjectNode.harKonflikterMed(other: ObjectNode) = fields()
            .asSequence()
            .filter { it.key in other.fieldNames().asSequence() }
            .any { it.value != other[it.key] }

    }
}
