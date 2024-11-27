package no.nav.helse.spleis

import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import java.sql.SQLException
import kotlin.time.DurationUnit
import kotlin.time.measureTime
import no.nav.helse.serde.DeserializationException
import no.nav.helse.serde.migration.JsonMigrationException
import no.nav.helse.spleis.db.HendelseRepository
import no.nav.helse.spleis.meldinger.AnmodningOmForkastingRiver
import no.nav.helse.spleis.meldinger.AnnullerUtbetalingerRiver
import no.nav.helse.spleis.meldinger.AvbruttSøknadRiver
import no.nav.helse.spleis.meldinger.DødsmeldingerRiver
import no.nav.helse.spleis.meldinger.ForkastSykmeldingsperioderRiver
import no.nav.helse.spleis.meldinger.GrunnbeløpsreguleringRiver
import no.nav.helse.spleis.meldinger.IdentOpphørtRiver
import no.nav.helse.spleis.meldinger.InfotrygdendringerRiver
import no.nav.helse.spleis.meldinger.InntektsmeldingerReplayRiver
import no.nav.helse.spleis.meldinger.InntektsmeldingerRiver
import no.nav.helse.spleis.meldinger.MigrateRiver
import no.nav.helse.spleis.meldinger.MinimumSykdomsgradVurdertRiver
import no.nav.helse.spleis.meldinger.NyeArbeidsledigSøknaderRiver
import no.nav.helse.spleis.meldinger.NyeFrilansSøknaderRiver
import no.nav.helse.spleis.meldinger.NyeSelvstendigSøknaderRiver
import no.nav.helse.spleis.meldinger.NyeSøknaderRiver
import no.nav.helse.spleis.meldinger.OverstyrArbeidsforholdRiver
import no.nav.helse.spleis.meldinger.OverstyrArbeidsgiveropplysningerRiver
import no.nav.helse.spleis.meldinger.OverstyrTidlinjeRiver
import no.nav.helse.spleis.meldinger.PersonAvstemmingRiver
import no.nav.helse.spleis.meldinger.PersonPåminnelserRiver
import no.nav.helse.spleis.meldinger.PåminnelserRiver
import no.nav.helse.spleis.meldinger.SendtArbeidsgiverSøknaderRiver
import no.nav.helse.spleis.meldinger.SendtArbeidsledigSøknaderRiver
import no.nav.helse.spleis.meldinger.SendtFrilansSøknaderRiver
import no.nav.helse.spleis.meldinger.SendtNavSøknaderRiver
import no.nav.helse.spleis.meldinger.SendtSelvstendigSøknaderRiver
import no.nav.helse.spleis.meldinger.SimuleringerRiver
import no.nav.helse.spleis.meldinger.SkjønnsmessigFastsettelseRiver
import no.nav.helse.spleis.meldinger.SykepengegrunnlagForArbeidsgiverRiver
import no.nav.helse.spleis.meldinger.UtbetalingerRiver
import no.nav.helse.spleis.meldinger.UtbetalingpåminnelserRiver
import no.nav.helse.spleis.meldinger.UtbetalingsgodkjenningerRiver
import no.nav.helse.spleis.meldinger.UtbetalingshistorikkEtterInfotrygdendringRiver
import no.nav.helse.spleis.meldinger.UtbetalingshistorikkForFeriepengerRiver
import no.nav.helse.spleis.meldinger.UtbetalingshistorikkRiver
import no.nav.helse.spleis.meldinger.VilkårsgrunnlagRiver
import no.nav.helse.spleis.meldinger.YtelserRiver
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import org.slf4j.LoggerFactory

internal class MessageMediator(
    rapidsConnection: RapidsConnection,
    private val hendelseMediator: IHendelseMediator,
    private val hendelseRepository: HendelseRepository,
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
            SendtArbeidsledigSøknaderRiver(it, this)
            InntektsmeldingerRiver(it, this)
            InntektsmeldingerReplayRiver(it, this)
            UtbetalingshistorikkRiver(it, this)
            UtbetalingshistorikkForFeriepengerRiver(it, this)
            YtelserRiver(it, this)
            SykepengegrunnlagForArbeidsgiverRiver(it, this)
            VilkårsgrunnlagRiver(it, this)
            UtbetalingsgodkjenningerRiver(it, this)
            UtbetalingerRiver(it, this)
            PåminnelserRiver(it, this)
            PersonPåminnelserRiver(it, this)
            UtbetalingpåminnelserRiver(it, this)
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
        }
    }

    private var messageRecognized = false
    private val riverErrors = mutableListOf<Pair<String, MessageProblems>>()

    fun beforeRiverHandling() {
        messageRecognized = false
        riverErrors.clear()
    }

    override fun onRecognizedMessage(message: HendelseMessage, context: MessageContext) {
        try {
            measureTime {
                    messageRecognized = true
                    message.logRecognized(log, sikkerLogg)
                    hendelseRepository.lagreMelding(message)

                    if (
                        message.skalDuplikatsjekkes &&
                            hendelseRepository.erBehandlet(message.meldingsporing.id)
                    ) {
                        message.logDuplikat(sikkerLogg)
                        return
                    }

                    hendelseMediator.behandle(message, context)
                    hendelseRepository.markerSomBehandlet(message.meldingsporing.id)
                }
                .also { result ->
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
        } catch (err: DeserializationException) {
            severeErrorHandler(err, message)
        } catch (err: JsonMigrationException) {
            severeErrorHandler(err, message)
        } catch (err: SQLException) {
            severeErrorHandler(err, message)
        } catch (err: Exception) {
            errorHandler(err, message)
        }
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
        sikkerLogg.warn(
            "kunne ikke gjenkjenne melding:\n\t$message\n\nProblemer:\n${riverErrors.joinToString(separator = "\n") { "${it.first}:\n${it.second}" }}"
        )
    }

    private fun severeErrorHandler(err: Exception, message: HendelseMessage) {
        errorHandler(err, message)
        throw err
    }

    private fun errorHandler(err: Exception, message: HendelseMessage) {
        errorHandler(err, message.toJson(), message.secureDiagnosticinfo())
    }

    private fun errorHandler(
        err: Exception,
        message: String,
        context: Map<String, String> = emptyMap(),
    ) {
        log.error("alvorlig feil: ${err.message} (se sikkerlogg for melding)", err)
        withMDC(context) { sikkerLogg.error("alvorlig feil: ${err.message}\n\t$message", err) }
    }

    private inner class DelegatedRapid(private val rapidsConnection: RapidsConnection) :
        RapidsConnection(), RapidsConnection.MessageListener {

        init {
            rapidsConnection.register(this)
        }

        override fun onMessage(
            message: String,
            context: MessageContext,
            metadata: MessageMetadata,
            meterRegistry: MeterRegistry,
        ) {
            beforeRiverHandling()
            notifyMessage(message, context, metadata, meterRegistry)
            afterRiverHandling(message)
        }

        override fun publish(message: String) {
            rapidsConnection.publish(message)
        }

        override fun publish(key: String, message: String) {
            rapidsConnection.publish(key, message)
        }

        override fun rapidName() = rapidsConnection.rapidName()

        override fun start() = throw IllegalStateException()

        override fun stop() = throw IllegalStateException()
    }
}

internal interface IMessageMediator {
    fun onRecognizedMessage(message: HendelseMessage, context: MessageContext)

    fun onRiverError(
        riverName: String,
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    )
}
