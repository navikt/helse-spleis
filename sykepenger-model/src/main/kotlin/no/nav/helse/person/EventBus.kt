package no.nav.helse.person

import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.tilstandsmaskin.TilstandType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class EventBus {
    private val observers = mutableListOf<EventSubscription>()

    fun register(observer: EventSubscription) {
        observers.add(observer)
    }

    internal fun overlappendeInfotrygdperioder(event: EventSubscription.OverlappendeInfotrygdperioder) {
        observers.forEach { it.overlappendeInfotrygdperioder(event) }
    }

    internal fun sykefraværstilfelleIkkeFunnet(skjæringstidspunkt: LocalDate) {
        val event = EventSubscription.SykefraværstilfelleIkkeFunnet(
            skjæringstidspunkt = skjæringstidspunkt
        )
        observers.forEach { it.sykefraværstilfelleIkkeFunnet(event) }
    }

    internal fun annullert(event: EventSubscription.UtbetalingAnnullertEvent) {
        observers.forEach { it.annullering(event) }
    }

    internal fun vedtaksperiodePåminnet(vedtaksperiodeId: UUID, organisasjonsnummer: String, påminnelse: Påminnelse) {
        observers.forEach { it.vedtaksperiodePåminnet(vedtaksperiodeId, organisasjonsnummer, påminnelse) }
    }

    internal fun vedtaksperiodeIkkePåminnet(vedtaksperiodeId: UUID, organisasjonsnummer: String, tilstandType: TilstandType) {
        observers.forEach { it.vedtaksperiodeIkkePåminnet(vedtaksperiodeId, organisasjonsnummer, tilstandType) }
    }

    internal fun vedtaksperiodeForkastet(event: EventSubscription.VedtaksperiodeForkastetEvent) {
        observers.forEach { it.vedtaksperiodeForkastet(event) }
    }

    internal fun vedtaksperiodeEndret(event: EventSubscription.VedtaksperiodeEndretEvent) {
        observers.forEach { it.vedtaksperiodeEndret(event) }
    }

    internal fun inntektsmeldingReplay(event: EventSubscription.TrengerArbeidsgiveropplysningerEvent) {
        observers.forEach { it.inntektsmeldingReplay(event) }
    }

    internal fun trengerArbeidsgiveropplysninger(event: EventSubscription.TrengerArbeidsgiveropplysningerEvent) {
        observers.forEach { it.trengerArbeidsgiveropplysninger(event) }
    }

    internal fun trengerIkkeArbeidsgiveropplysninger(event: EventSubscription.TrengerIkkeArbeidsgiveropplysningerEvent) {
        observers.forEach { it.trengerIkkeArbeidsgiveropplysninger(event) }
    }

    internal fun utbetalingUtbetalt(event: EventSubscription.UtbetalingUtbetaltEvent) {
        observers.forEach { it.utbetalingUtbetalt(event) }
    }

    internal fun utbetalingUtenUtbetaling(event: EventSubscription.UtbetalingUtbetaltEvent) {
        observers.forEach { it.utbetalingUtenUtbetaling(event) }
    }

    internal fun utbetalingEndret(event: EventSubscription.UtbetalingEndretEvent) {
        observers.forEach { it.utbetalingEndret(event) }
    }

    internal fun avsluttetUtenVedtak(event: EventSubscription.AvsluttetUtenVedtakEvent) {
        observers.forEach { it.avsluttetUtenVedtak(event) }
    }

    internal fun avsluttetMedVedtak(avsluttetMedVedtakEvent: EventSubscription.AvsluttetMedVedtakEvent) {
        observers.forEach { it.avsluttetMedVedtak(avsluttetMedVedtakEvent) }
    }

    internal fun analytiskDatapakke(analytiskDatapakkeEvent: EventSubscription.AnalytiskDatapakkeEvent) {
        observers.forEach { it.analytiskDatapakke(analytiskDatapakkeEvent) }
    }

    internal fun behandlingLukket(behandlingLukketEvent: EventSubscription.BehandlingLukketEvent) {
        observers.forEach { it.behandlingLukket(behandlingLukketEvent) }
    }

    internal fun behandlingForkastet(behandlingForkastetEvent: EventSubscription.BehandlingForkastetEvent) {
        observers.forEach { it.behandlingForkastet(behandlingForkastetEvent) }
    }

    internal fun nyBehandling(event: EventSubscription.BehandlingOpprettetEvent) {
        observers.forEach { it.nyBehandling(event) }
    }

    internal fun utkastTilVedtak(event: EventSubscription.UtkastTilVedtakEvent) {
        observers.forEach { it.utkastTilVedtak(event) }
    }

    fun planlagtAnnullering(planlagtAnnullering: EventSubscription.PlanlagtAnnulleringEvent) {
        observers.forEach { it.planlagtAnnullering(planlagtAnnullering) }
    }

    internal fun emitOverstyringIgangsattEvent(event: EventSubscription.OverstyringIgangsatt) {
        observers.forEach { it.overstyringIgangsatt(event) }
    }

    internal fun feriepengerUtbetalt(feriepengerUtbetaltEvent: EventSubscription.FeriepengerUtbetaltEvent) {
        observers.forEach { it.feriepengerUtbetalt(feriepengerUtbetaltEvent) }
    }

    internal fun emitInntektsmeldingFørSøknadEvent(
        meldingsreferanseId: UUID,
        yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet
    ) {
        observers.forEach {
            it.inntektsmeldingFørSøknad(EventSubscription.InntektsmeldingFørSøknadEvent(meldingsreferanseId, yrkesaktivitetssporing))
        }
    }

    internal fun emitInntektsmeldingIkkeHåndtert(meldingsreferanseId: MeldingsreferanseId, organisasjonsnummer: String, speilrelatert: Boolean) {
        observers.forEach {
            it.inntektsmeldingIkkeHåndtert(meldingsreferanseId.id, organisasjonsnummer, speilrelatert)
        }
    }

    internal fun emitArbeidsgiveropplysningerIkkeHåndtert(meldingsreferanseId: MeldingsreferanseId, organisasjonsnummer: String) =
        emitInntektsmeldingIkkeHåndtert(meldingsreferanseId, organisasjonsnummer, true)

    internal fun emitInntektsmeldingHåndtert(meldingsreferanseId: UUID, vedtaksperiodeId: UUID, organisasjonsnummer: String) {
        observers.forEach {
            it.inntektsmeldingHåndtert(meldingsreferanseId, vedtaksperiodeId, organisasjonsnummer)
        }
    }

    internal fun sendSkatteinntekterLagtTilGrunn(skatteinntekterLagtTilGrunnEvent: EventSubscription.SkatteinntekterLagtTilGrunnEvent) {
        observers.forEach {
            it.skatteinntekterLagtTilGrunn(skatteinntekterLagtTilGrunnEvent)
        }
    }

    internal fun emitSøknadHåndtert(meldingsreferanseId: UUID, vedtaksperiodeId: UUID, organisasjonsnummer: String) {
        observers.forEach {
            it.søknadHåndtert(meldingsreferanseId, vedtaksperiodeId, organisasjonsnummer)
        }
    }

    internal fun vedtaksperiodeVenter(eventer: List<EventSubscription.VedtaksperiodeVenterEvent>) {
        observers.forEach { it.vedtaksperioderVenter(eventer) }
    }

    internal fun behandlingUtført() {
        observers.forEach { it.behandlingUtført() }
    }

    internal fun nyVedtaksperiodeUtbetaling(organisasjonsnummer: String, utbetalingId: UUID, vedtaksperiodeId: UUID) {
        observers.forEach { it.nyVedtaksperiodeUtbetaling(organisasjonsnummer, utbetalingId, vedtaksperiodeId) }
    }

    internal fun vedtaksperiodeOpprettet(vedtaksperiodeId: UUID, yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet, periode: Periode, skjæringstidspunkt: LocalDate, opprettet: LocalDateTime) {
        val event = EventSubscription.VedtaksperiodeOpprettet(vedtaksperiodeId, yrkesaktivitetssporing, periode, skjæringstidspunkt, opprettet)
        observers.forEach { it.vedtaksperiodeOpprettet(event) }
    }

    internal fun vedtaksperiodeAnnullert(vedtaksperiodeAnnullertEvent: EventSubscription.VedtaksperiodeAnnullertEvent) {
        observers.forEach { it.vedtaksperiodeAnnullert(vedtaksperiodeAnnullertEvent) }
    }
}
