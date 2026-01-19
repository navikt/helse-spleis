package no.nav.helse.person

import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.tilstandsmaskin.TilstandType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class EventBus {
    private val observers = mutableListOf<EventSubscription>()
    private val _events = mutableListOf<EventSubscription.Event>()

    // eksponerer ikke en mutable list
    val events get() = _events.toList()

    fun register(observer: EventSubscription) {
        observers.add(observer)
    }

    internal fun overlappendeInfotrygdperioder(event: EventSubscription.OverlappendeInfotrygdperioder) {
        _events.add(event)
        observers.forEach { it.overlappendeInfotrygdperioder(event) }
    }

    internal fun sykefraværstilfelleIkkeFunnet(skjæringstidspunkt: LocalDate) {
        val event = EventSubscription.SykefraværstilfelleIkkeFunnet(
            skjæringstidspunkt = skjæringstidspunkt
        )
        _events.add(event)
        observers.forEach { it.sykefraværstilfelleIkkeFunnet(event) }
    }

    internal fun annullert(event: EventSubscription.UtbetalingAnnullertEvent) {
        _events.add(event)
        observers.forEach { it.annullering(event) }
    }

    internal fun vedtaksperiodePåminnet(
        vedtaksperiodeId: UUID,
        behandlingsporing: Behandlingsporing.Yrkesaktivitet,
        tilstand: TilstandType,
        antallGangerPåminnet: Int,
        tilstandsendringstidspunkt: LocalDateTime,
        påminnelsestidspunkt: LocalDateTime,
        nestePåminnelsestidspunkt: LocalDateTime
    ) {
        val event = EventSubscription.VedtaksperiodePåminnetEvent(
            vedtaksperiodeId = vedtaksperiodeId,
            yrkesaktivitetssporing = behandlingsporing,
            tilstand = tilstand,
            antallGangerPåminnet = antallGangerPåminnet,
            tilstandsendringstidspunkt = tilstandsendringstidspunkt,
            påminnelsestidspunkt = påminnelsestidspunkt,
            nestePåminnelsestidspunkt = nestePåminnelsestidspunkt
        )
        _events.add(event)
        observers.forEach { it.vedtaksperiodePåminnet(event) }
    }

    internal fun vedtaksperiodeIkkePåminnet(vedtaksperiodeId: UUID, yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet, tilstandType: TilstandType) {
        val event = EventSubscription.VedtaksperiodeIkkePåminnetEvent(vedtaksperiodeId, yrkesaktivitetssporing, tilstandType)
        _events.add(event)
        observers.forEach { it.vedtaksperiodeIkkePåminnet(event) }
    }

    internal fun vedtaksperiodeForkastet(event: EventSubscription.VedtaksperiodeForkastetEvent) {
        _events.add(event)
        observers.forEach { it.vedtaksperiodeForkastet(event) }
    }

    internal fun vedtaksperiodeEndret(event: EventSubscription.VedtaksperiodeEndretEvent) {
        _events.add(event)
        observers.forEach { it.vedtaksperiodeEndret(event) }
    }

    internal fun inntektsmeldingReplay(opplysninger: EventSubscription.TrengerArbeidsgiveropplysninger) {
        val event = EventSubscription.TrengerInntektsmeldingReplayEvent(opplysninger = opplysninger)
        _events.add(event)
        observers.forEach { it.inntektsmeldingReplay(event) }
    }

    internal fun trengerArbeidsgiveropplysninger(opplysninger: EventSubscription.TrengerArbeidsgiveropplysninger) {
        val event = EventSubscription.TrengerArbeidsgiveropplysningerEvent(opplysninger)
        _events.add(event)
        observers.forEach { it.trengerArbeidsgiveropplysninger(event) }
    }

    internal fun trengerIkkeArbeidsgiveropplysninger(event: EventSubscription.TrengerIkkeArbeidsgiveropplysningerEvent) {
        _events.add(event)
        observers.forEach { it.trengerIkkeArbeidsgiveropplysninger(event) }
    }

    internal fun utbetalingUtbetalt(event: EventSubscription.UtbetalingUtbetaltEvent) {
        _events.add(event)
        observers.forEach { it.utbetalingUtbetalt(event) }
    }

    internal fun utbetalingUtenUtbetaling(event: EventSubscription.UtbetalingUtenUtbetalingEvent) {
        _events.add(event)
        observers.forEach { it.utbetalingUtenUtbetaling(event) }
    }

    internal fun utbetalingEndret(event: EventSubscription.UtbetalingEndretEvent) {
        _events.add(event)
        observers.forEach { it.utbetalingEndret(event) }
    }

    internal fun avsluttetUtenVedtak(event: EventSubscription.AvsluttetUtenVedtakEvent) {
        _events.add(event)
        observers.forEach { it.avsluttetUtenVedtak(event) }
    }

    internal fun avsluttetMedVedtak(avsluttetMedVedtakEvent: EventSubscription.AvsluttetMedVedtakEvent) {
        _events.add(avsluttetMedVedtakEvent)
        observers.forEach { it.avsluttetMedVedtak(avsluttetMedVedtakEvent) }
    }

    internal fun analytiskDatapakke(analytiskDatapakkeEvent: EventSubscription.AnalytiskDatapakkeEvent) {
        _events.add(analytiskDatapakkeEvent)
        observers.forEach { it.analytiskDatapakke(analytiskDatapakkeEvent) }
    }

    internal fun behandlingLukket(behandlingLukketEvent: EventSubscription.BehandlingLukketEvent) {
        _events.add(behandlingLukketEvent)
        observers.forEach { it.behandlingLukket(behandlingLukketEvent) }
    }

    internal fun behandlingForkastet(behandlingForkastetEvent: EventSubscription.BehandlingForkastetEvent) {
        _events.add(behandlingForkastetEvent)
        observers.forEach { it.behandlingForkastet(behandlingForkastetEvent) }
    }

    internal fun nyBehandling(event: EventSubscription.BehandlingOpprettetEvent) {
        _events.add(event)
        observers.forEach { it.nyBehandling(event) }
    }

    internal fun utkastTilVedtak(event: EventSubscription.UtkastTilVedtakEvent) {
        _events.add(event)
        observers.forEach { it.utkastTilVedtak(event) }
    }

    fun planlagtAnnullering(planlagtAnnullering: EventSubscription.PlanlagtAnnulleringEvent) {
        _events.add(planlagtAnnullering)
        observers.forEach { it.planlagtAnnullering(planlagtAnnullering) }
    }

    internal fun emitOverstyringIgangsattEvent(event: EventSubscription.OverstyringIgangsatt) {
        _events.add(event)
        observers.forEach { it.overstyringIgangsatt(event) }
    }

    internal fun feriepengerUtbetalt(feriepengerUtbetaltEvent: EventSubscription.FeriepengerUtbetaltEvent) {
        _events.add(feriepengerUtbetaltEvent)
        observers.forEach { it.feriepengerUtbetalt(feriepengerUtbetaltEvent) }
    }

    internal fun emitInntektsmeldingFørSøknadEvent(
        meldingsreferanseId: UUID,
        arbeidstaker: Behandlingsporing.Yrkesaktivitet.Arbeidstaker
    ) {
        val event = EventSubscription.InntektsmeldingFørSøknadEvent(meldingsreferanseId, arbeidstaker)
        _events.add(event)
        observers.forEach { it.inntektsmeldingFørSøknad(event) }
    }

    internal fun emitInntektsmeldingIkkeHåndtert(meldingsreferanseId: MeldingsreferanseId, organisasjonsnummer: String, speilrelatert: Boolean) {
        val event = EventSubscription.InntektsmeldingIkkeHåndtertEvent(meldingsreferanseId.id, Behandlingsporing.Yrkesaktivitet.Arbeidstaker(organisasjonsnummer), speilrelatert)
        _events.add(event)
        observers.forEach { it.inntektsmeldingIkkeHåndtert(event) }
    }

    internal fun emitArbeidsgiveropplysningerIkkeHåndtert(meldingsreferanseId: MeldingsreferanseId, organisasjonsnummer: String) =
        emitInntektsmeldingIkkeHåndtert(meldingsreferanseId, organisasjonsnummer, true)

    internal fun emitInntektsmeldingHåndtert(meldingsreferanseId: UUID, vedtaksperiodeId: UUID, organisasjonsnummer: String) {
        val event = EventSubscription.InntektsmeldingHåndtertEvent(meldingsreferanseId, vedtaksperiodeId, Behandlingsporing.Yrkesaktivitet.Arbeidstaker(organisasjonsnummer))
        _events.add(event)
        observers.forEach { it.inntektsmeldingHåndtert(event) }
    }

    internal fun sendSkatteinntekterLagtTilGrunn(skatteinntekterLagtTilGrunnEvent: EventSubscription.SkatteinntekterLagtTilGrunnEvent) {
        _events.add(skatteinntekterLagtTilGrunnEvent)
        observers.forEach { it.skatteinntekterLagtTilGrunn(skatteinntekterLagtTilGrunnEvent) }
    }

    internal fun emitSøknadHåndtert(meldingsreferanseId: UUID, vedtaksperiodeId: UUID, yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet) {
        val event = EventSubscription.SøknadHåndtertEvent(meldingsreferanseId, vedtaksperiodeId, yrkesaktivitetssporing)
        _events.add(event)
        observers.forEach { it.søknadHåndtert(event) }
    }

    internal fun vedtaksperiodeVenter(eventer: List<EventSubscription.VedtaksperiodeVenterEvent>) {
        val event = EventSubscription.VedtaksperioderVenterEvent(eventer)
        _events.add(event)
        observers.forEach { it.vedtaksperioderVenter(event) }
    }

    internal fun behandlingUtført() {
        observers.forEach { it.behandlingUtført() }
    }

    internal fun nyVedtaksperiodeUtbetaling(yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet, utbetalingId: UUID, vedtaksperiodeId: UUID) {
        val event = EventSubscription.VedtaksperiodeNyUtbetalingEvent(yrkesaktivitetssporing, utbetalingId, vedtaksperiodeId)
        _events.add(event)
        observers.forEach { it.nyVedtaksperiodeUtbetaling(event) }
    }

    internal fun vedtaksperiodeOpprettet(vedtaksperiodeId: UUID, yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet, periode: Periode, skjæringstidspunkt: LocalDate, opprettet: LocalDateTime) {
        val event = EventSubscription.VedtaksperiodeOpprettet(vedtaksperiodeId, yrkesaktivitetssporing, periode, skjæringstidspunkt, opprettet)
        _events.add(event)
        observers.forEach { it.vedtaksperiodeOpprettet(event) }
    }

    internal fun vedtaksperiodeAnnullert(vedtaksperiodeAnnullertEvent: EventSubscription.VedtaksperiodeAnnullertEvent) {
        _events.add(vedtaksperiodeAnnullertEvent)
        observers.forEach { it.vedtaksperiodeAnnullert(vedtaksperiodeAnnullertEvent) }
    }
}
