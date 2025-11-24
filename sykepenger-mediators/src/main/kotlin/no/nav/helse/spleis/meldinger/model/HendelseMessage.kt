package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.db.HendelseRepository
import org.slf4j.Logger

internal sealed class HendelseMessage(private val packet: JsonMessage) : Aktivitetskontekst {
    abstract val meldingsporing: Meldingsporing
    internal val navn = packet["@event_name"].asText()
    protected val opprettet = packet["@opprettet"].asLocalDateTime()

    internal abstract fun behandle(mediator: IHendelseMediator, context: MessageContext)

    final override fun toSpesifikkKontekst() =
        SpesifikkKontekst(kontekstnavn, mapOf(
            "meldingsreferanseId" to meldingsporing.id.id.toString()
        ))

    internal fun lagreMelding(repository: HendelseRepository) {
        repository.lagreMelding(this, Personidentifikator(meldingsporing.fødselsnummer), meldingsporing.id, toJson())
    }

    internal fun logReplays(logger: Logger, size: Int) {
        logger.info("som følge av $navn id=${meldingsporing.id} sendes $size meldinger for replay for fnr=${meldingsporing.fødselsnummer}")
    }

    internal fun logOutgoingMessages(logger: Logger, size: Int) {
        logger.info("som følge av $navn id=${meldingsporing.id} sendes $size meldinger på rapid for fnr=${meldingsporing.fødselsnummer}")
    }

    internal fun logRecognized(insecureLog: Logger, safeLog: Logger) {
        insecureLog.info("gjenkjente {} med id={}", this::class.simpleName, meldingsporing.id)
        safeLog.info("gjenkjente {} med id={} for fnr={}:\n{}", this::class.simpleName, meldingsporing.id, meldingsporing.fødselsnummer, toJson())
    }

    internal fun logDuplikat(logger: Logger) {
        logger.warn("Har mottatt duplikat {} med id={} for fnr={}", this::class.simpleName, meldingsporing.id, meldingsporing.fødselsnummer)
    }

    internal fun secureDiagnosticinfo() = mapOf(
        "fødselsnummer" to meldingsporing.fødselsnummer
    )

    internal fun tracinginfo() = additionalTracinginfo(packet) + mapOf(
        "event_name" to navn,
        "id" to meldingsporing.id,
        "opprettet" to opprettet
    )

    protected open fun additionalTracinginfo(packet: JsonMessage): Map<String, Any> = emptyMap()

    internal fun toJson() = packet.toJson()
}

internal fun asPeriode(jsonNode: JsonNode): Periode {
    val fom = jsonNode.path("fom").asLocalDate()
    val tom = jsonNode.path("tom").asLocalDate().takeUnless { it < fom } ?: fom
    return Periode(fom, tom)
}

private val HendelseMessage.kontekstnavn
    get() = when (this) {
        is AnmodningOmForkastingMessage -> "AnmodningOmForkasting"
        is AnnulleringMessage -> "AnnullerUtbetaling"

        is AvbruttSøknadMessage -> "AvbruttSøknad"

        is AvstemmingMessage -> "Avstemming"
        is SimuleringMessage -> "Simulering"
        is UtbetalingMessage -> "UtbetalingHendelse"
        is FeriepengeutbetalingMessage -> "Feriepengeutbetaling"
        is UtbetalingsgodkjenningMessage -> "Utbetalingsgodkjenning"
        is UtbetalingshistorikkEtterInfotrygdendringMessage -> "UtbetalingshistorikkEtterInfotrygdendring"
        is UtbetalingshistorikkForFeriepengerMessage -> "UtbetalingshistorikkForFeriepenger"
        is UtbetalingshistorikkMessage -> "Utbetalingshistorikk"
        is VilkårsgrunnlagMessage -> "Vilkårsgrunnlag"
        is YtelserMessage -> "Ytelser"
        is DødsmeldingMessage -> "Dødsmelding"
        is ForkastSykmeldingsperioderMessage -> "ForkastSykmeldingsperioder"
        is GrunnbeløpsreguleringMessage -> "Grunnbeløpsregulering"
        is IdentOpphørtMessage -> "IdentOpphørt"
        is InfotrygdendringMessage -> "Infotrygdendring"
        is InntektsendringerMessage -> "Inntektsendringer"
        is InntektsmeldingMessage -> "Inntektsmelding"
        is NavNoInntektsmeldingMessage -> "NavNoInntektsmelding"
        is NavNoKorrigertInntektsmeldingMessage -> "NavNoKorrigertInntektsmelding"
        is NavNoSelvbestemtInntektsmeldingMessage -> "NavNoSelvbestemtInntektsmelding"
        is InntektsmeldingerReplayMessage -> "InntektsmeldingerReplay"
        is MigrateMessage -> "Migrate"
        is MinimumSykdomsgradVurdertMessage -> "MinimumSykdomsgradsvurderingMelding"
        is OverstyrArbeidsforholdMessage -> "OverstyrArbeidsforhold"
        is OverstyrArbeidsgiveropplysningerMessage -> "OverstyrArbeidsgiveropplysninger"
        is OverstyrTidslinjeMessage -> "OverstyrTidslinje"
        is PersonPåminnelseMessage -> "PersonPåminnelse"
        is PåminnelseMessage -> "Påminnelse"
        is SkjønnsmessigFastsettelseMessage -> "SkjønnsmessigFastsettelse"
        is GjenopptaBehandlingMessage -> "GjenopptaBehandling"

        is NyArbeidsledigSøknadMessage,
        is NyArbeidsledigTidligereArbeidstakerSøknadMessage,
        is NyFrilansSøknadMessage,
        is NyJordbrukerSøknadMessage,
        is NySelvstendigSøknadMessage,
        is NySøknadMessage -> "Sykmelding"

        is SendtSøknadArbeidsgiverMessage,
        is SendtSøknadArbeidsledigMessage,
        is SendtSøknadArbeidsledigTidligereArbeidstakerMessage,
        is SendtSøknadFrilansMessage,
        is SendtSøknadNavMessage,
        is SendtSøknadFiskerMessage,
        is SendtSøknadJordbrukerMessage,
        is SendtSøknadAnnetMessage,
        is SendtSøknadSelvstendigMessage -> "Søknad"

    }
