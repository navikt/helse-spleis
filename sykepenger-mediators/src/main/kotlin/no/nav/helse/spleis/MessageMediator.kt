package no.nav.helse.spleis

import com.github.navikt.tbd_libs.rapids_and_rivers_api.*
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.serde.DeserializationException
import no.nav.helse.serde.migration.JsonMigrationException
import no.nav.helse.spleis.db.HendelseRepository
import no.nav.helse.spleis.meldinger.*
import no.nav.helse.spleis.meldinger.model.*
import no.nav.helse.spleis.utboks.Utboks.Companion.fireAndForget
import no.nav.helse.spleis.utboks.UtboksDao
import no.nav.helse.spleis.utboks.UtgåendeMelding
import no.nav.helse.spleis.utboks.Utsender
import org.slf4j.LoggerFactory
import java.sql.SQLException
import kotlin.time.DurationUnit
import kotlin.time.measureTime

internal class MessageMediator(
    rapidsConnection: RapidsConnection,
    private val hendelseMediator: IHendelseMediator,
    private val hendelseRepository: HendelseRepository,
    private val utsender: Utsender,
    private val utboksDao: UtboksDao,
) : IMessageMediator {
    private companion object {
        private val log = LoggerFactory.getLogger(MessageMediator::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        DelegatedRapid(rapidsConnection).also {
            NyeSøknaderRiver(it, this)
            NyeFrilansSøknaderRiver(it, this)
            NyeSelvstendigSøknaderRiver(it, this)
            NyeArbeidsledigSøknaderRiver(it, this)
            SendtArbeidsgiverSøknaderRiver(it, this)
            SendtNavSøknaderRiver(it, this)
            SendtFrilansSøknaderRiver(it, this)
            SendtSelvstendigSøknaderRiver(it, this)
            SendtFiskerSøknaderRiver(it, this)
            SendtAnnetSøknaderRiver(it, this)
            SendtArbeidsledigSøknaderRiver(it, this)
            NavNoInntektsmeldingerRiver(it, this)
            NavNoKorrigerteInntektsmeldingerRiver(it, this)
            NavNoSelvbestemtInntektsmeldingerRiver(it, this)
            InntektsmeldingerReplayRiver(it, this)
            InntektsopplysningerFraLagretInntektsmeldingRiver(it, this)
            UtbetalingshistorikkRiver(it, this)
            UtbetalingshistorikkForFeriepengerRiver(it, this)
            YtelserRiver(it, this)
            VilkårsgrunnlagRiver(it, this)
            UtbetalingsgodkjenningerRiver(it, this)
            UtbetalingerRiver(it, this)
            FeriepengeutbetalingerRiver(it, this)
            PåminnelserRiver(it, this)
            PersonPåminnelserRiver(it, this)
            GjenopptaBehandlingerRiver(it, this)
            SimuleringerRiver(it, this)
            AnnullerUtbetalingerRiver(it, this)
            PersonAvstemmingRiver(it, this)
            MigrateRiver(it, this)
            OverstyrTidlinjeRiver(it, this)
            GrunnbeløpsreguleringRiver(it, this)
            OverstyrArbeidsforholdRiver(it, this)
            OverstyrArbeidsgiveropplysningerRiver(it, this)
            InfotrygdendringerRiver(it, this)
            UtbetalingshistorikkEtterInfotrygdendringRiver(it, this)
            DødsmeldingerRiver(it, this)
            ForkastSykmeldingsperioderRiver(it, this)
            AvbruttSøknadRiver(it, this)
            AnmodningOmForkastingRiver(it, this)
            IdentOpphørtRiver(it, this)
            SkjønnsmessigFastsettelseRiver(it, this)
            MinimumSykdomsgradVurdertRiver(it, this)
            EndretVurderingPåSkjæringstidspunktRiver(it, this)
            EndretGrunnlagForBeregningRiver(it, this)
        }
    }

    private var messageRecognized = false
    private val riverErrors = mutableListOf<Pair<String, MessageProblems>>()

    fun beforeRiverHandling() {
        messageRecognized = false
        riverErrors.clear()
    }

    override fun onRecognizedMessage(
        message: HendelseMessage,
        context: MessageContext,
    ) {
        try {
            measureTime {
                val behandlingContext = BehandlingContext(message, utsender, utboksDao)
                messageRecognized = true
                message.logRecognized(log, sikkerLogg)
                if (hendelseRepository.erBehandlet(message.meldingsporing.id)) return message.logDuplikat(sikkerLogg)

                hendelseRepository.lagreMelding(message)
                hendelseMediator.behandle(message, behandlingContext)
                hendelseRepository.markerSomBehandlet(message.meldingsporing.id)
                behandlingContext.sendMeldingerIUtboks()
            }.also { result ->
                val antallSekunder = result.toDouble(DurationUnit.SECONDS)
                val label =
                    when {
                        antallSekunder < 1.0 -> "under ett sekund"
                        antallSekunder <= 2.0 -> "mer enn ett sekund"
                        antallSekunder <= 5.0 -> "mer enn to sekunder"
                        antallSekunder <= 10.0 -> "mer enn fem sekunder"
                        else -> "mer enn 10 sekunder"
                    }
                sikkerLogg.info("brukte $label ($antallSekunder s) på å prosessere meldingen")
            }
        } catch (err: Exception) {
            if (kritiskFeilSomSkalMedføreAtPoddenDør(err, message)) {
                severeErrorHandler(err, message, context)
            } else {
                errorHandler(err, message)
            }
        }
    }

    private fun kritiskFeilSomSkalMedføreAtPoddenDør(
        err: Exception,
        message: HendelseMessage,
    ): Boolean = err.kritiskFeil || message.måMeldingResendesVedFeil

    private val Exception.kritiskFeil get() =
        when (this) {
            is DeserializationException,
            is JsonMigrationException,
            is SQLException,
            -> true
            else -> false
        }

    private val HendelseMessage.måMeldingResendesVedFeil get() =
        when (this) {
            // meldinger som fint kan ignoreres/blir sendt på nytt får en
            // avslappet feilhåndtering
            is AnmodningOmForkastingMessage,
            is AnnulleringMessage,
            is AvstemmingMessage,
            is SimuleringMessage,
            is UtbetalingMessage,
            is FeriepengeutbetalingMessage,
            is UtbetalingsgodkjenningMessage,
            is UtbetalingshistorikkForFeriepengerMessage,
            is UtbetalingshistorikkMessage,
            is VilkårsgrunnlagMessage,
            is YtelserMessage,
            is ForkastSykmeldingsperioderMessage,
            is GrunnbeløpsreguleringMessage,
            is InntektsmeldingerReplayMessage,
            is MigrateMessage,
            is MinimumSykdomsgradVurdertMessage,
            is OverstyrArbeidsforholdMessage,
            is OverstyrArbeidsgiveropplysningerMessage,
            is OverstyrTidslinjeMessage,
            is PersonPåminnelseMessage,
            is PåminnelseMessage,
            is SkjønnsmessigFastsettelseMessage,
            is GjenopptaBehandlingMessage,
            is EndretVurderingPåSkjæringstidspunktMessage,
            is EndretGrunnlagForBeregningMessage,
            is InntektsopplysningerFraLagretInntektsmeldingMessage,
            -> false

            // meldinger som må replayes/sendes på nytt ved feil får
            // en feilhåndtering som medfører at podden går ned
            is UtbetalingshistorikkEtterInfotrygdendringMessage,
            is AvbruttSøknadMessage,
            is DødsmeldingMessage,
            is IdentOpphørtMessage,
            is InfotrygdendringMessage,
            is NavNoInntektsmeldingMessage,
            is NavNoKorrigertInntektsmeldingMessage,
            is NavNoSelvbestemtInntektsmeldingMessage,
            is NyArbeidsledigSøknadMessage,
            is NyArbeidsledigTidligereArbeidstakerSøknadMessage,
            is NyFrilansSøknadMessage,
            is NySelvstendigSøknadMessage,
            is NySøknadMessage,
            is SendtSøknadArbeidsgiverMessage,
            is SendtSøknadArbeidsledigMessage,
            is SendtSøknadArbeidsledigTidligereArbeidstakerMessage,
            is SendtSøknadFrilansMessage,
            is SendtSøknadNavMessage,
            is SendtSøknadFiskerMessage,
            is SendtSøknadAnnetMessage,
            is SendtSøknadSelvstendigMessage,
            -> true
        }

    override fun onRiverError(
        riverName: String,
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        riverErrors.add(riverName to problems)
    }

    fun afterRiverHandling(message: String) {
        if (messageRecognized || riverErrors.isEmpty()) return
        sikkerLogg.error("kunne ikke gjenkjenne melding:\n\t$message\n\nProblemer:\n${riverErrors.joinToString(separator = "\n") { "${it.first}:\n${it.second}" }}")
    }

    private fun MessageContext.sendPåSlack(message: HendelseMessage) {
        val googleUrl = "<https://console.cloud.google.com/logs/query;query=resource.labels.container_name:%22spleis%22%0AjsonPayload.message:%22${message.meldingsporing.id.id}%22;duration=P1D?project=tbd-prod-eacd|${message.navn}>"
        val melding = "\n\nEn $googleUrl får Spleis til å gå ned!!"
        fireAndForget(
            UtgåendeMelding.nyRapidmelding(
                eventName = "slackmelding",
                innhold =
                    mapOf(
                        "melding" to "$melding\n\n - Deres erbødig SPleis :spleis-realistisk:",
                        "level" to "ERROR",
                    ),
            ),
        )
    }

    private fun severeErrorHandler(
        err: Exception,
        message: HendelseMessage,
        context: MessageContext,
    ) {
        errorHandler("kritisk feil (podden dør!!)", err, message.toJson(), message.secureDiagnosticinfo())
        context.sendPåSlack(message)
        throw err
    }

    private fun errorHandler(
        err: Exception,
        message: HendelseMessage,
    ) {
        errorHandler("alvorlig feil", err, message.toJson(), message.secureDiagnosticinfo())
    }

    private fun errorHandler(
        prefix: String,
        err: Exception,
        message: String,
        context: Map<String, String> = emptyMap(),
    ) {
        log.error("$prefix: ${err.message} (se sikkerlogg for melding)", err)
        withMDC(context) { sikkerLogg.error("$prefix: ${err.message}\n\t$message", err) }
    }

    private inner class DelegatedRapid(
        private val rapidsConnection: RapidsConnection,
    ) : RapidsConnection(),
        RapidsConnection.MessageListener {
        init {
            rapidsConnection.register(this)
        }

        override fun onMessage(
            message: String,
            context: MessageContext,
            metadata: MessageMetadata,
            metrics: MeterRegistry,
        ) {
            beforeRiverHandling()
            notifyMessage(message, context, metadata, metrics)
            afterRiverHandling(message)
        }

        override fun publish(message: String) {
            rapidsConnection.publish(message)
        }

        override fun publish(
            key: String,
            message: String,
        ) {
            rapidsConnection.publish(key, message)
        }

        override fun publish(messages: List<OutgoingMessage>): Pair<List<SentMessage>, List<FailedMessage>> = rapidsConnection.publish(messages)

        override fun rapidName() = rapidsConnection.rapidName()

        override fun start() = throw IllegalStateException()

        override fun stop() = throw IllegalStateException()
    }
}

internal interface IMessageMediator {
    fun onRecognizedMessage(
        message: HendelseMessage,
        context: MessageContext,
    )

    fun onRiverError(
        riverName: String,
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    )
}
