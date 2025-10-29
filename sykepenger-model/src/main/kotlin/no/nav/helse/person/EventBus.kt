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
    private val observers = mutableListOf<PersonObserver>()

    fun register(observer: PersonObserver) {
        observers.add(observer)
    }

    internal fun overlappendeInfotrygdperioder(event: PersonObserver.OverlappendeInfotrygdperioder) {
        observers.forEach { it.overlappendeInfotrygdperioder(event) }
    }

    internal fun sykefraværstilfelleIkkeFunnet(skjæringstidspunkt: LocalDate) {
        val event = PersonObserver.SykefraværstilfelleIkkeFunnet(
            skjæringstidspunkt = skjæringstidspunkt
        )
        observers.forEach { it.sykefraværstilfelleIkkeFunnet(event) }
    }

    internal fun annullert(event: PersonObserver.UtbetalingAnnullertEvent) {
        observers.forEach { it.annullering(event) }
    }

    internal fun vedtaksperiodePåminnet(vedtaksperiodeId: UUID, organisasjonsnummer: String, påminnelse: Påminnelse) {
        observers.forEach { it.vedtaksperiodePåminnet(vedtaksperiodeId, organisasjonsnummer, påminnelse) }
    }

    internal fun vedtaksperiodeIkkePåminnet(vedtaksperiodeId: UUID, organisasjonsnummer: String, tilstandType: TilstandType) {
        observers.forEach { it.vedtaksperiodeIkkePåminnet(vedtaksperiodeId, organisasjonsnummer, tilstandType) }
    }

    internal fun vedtaksperiodeForkastet(event: PersonObserver.VedtaksperiodeForkastetEvent) {
        observers.forEach { it.vedtaksperiodeForkastet(event) }
    }

    internal fun vedtaksperiodeEndret(event: PersonObserver.VedtaksperiodeEndretEvent) {
        observers.forEach { it.vedtaksperiodeEndret(event) }
    }

    internal fun inntektsmeldingReplay(event: PersonObserver.TrengerArbeidsgiveropplysningerEvent) {
        observers.forEach { it.inntektsmeldingReplay(event) }
    }

    internal fun trengerArbeidsgiveropplysninger(event: PersonObserver.TrengerArbeidsgiveropplysningerEvent) {
        observers.forEach { it.trengerArbeidsgiveropplysninger(event) }
    }

    internal fun trengerIkkeArbeidsgiveropplysninger(event: PersonObserver.TrengerIkkeArbeidsgiveropplysningerEvent) {
        observers.forEach { it.trengerIkkeArbeidsgiveropplysninger(event) }
    }

    internal fun utbetalingUtbetalt(event: PersonObserver.UtbetalingUtbetaltEvent) {
        observers.forEach { it.utbetalingUtbetalt(event) }
    }

    internal fun utbetalingUtenUtbetaling(event: PersonObserver.UtbetalingUtbetaltEvent) {
        observers.forEach { it.utbetalingUtenUtbetaling(event) }
    }

    internal fun utbetalingEndret(event: PersonObserver.UtbetalingEndretEvent) {
        observers.forEach { it.utbetalingEndret(event) }
    }

    internal fun avsluttetUtenVedtak(event: PersonObserver.AvsluttetUtenVedtakEvent) {
        observers.forEach { it.avsluttetUtenVedtak(event) }
    }

    internal fun avsluttetMedVedtak(avsluttetMedVedtakEvent: PersonObserver.AvsluttetMedVedtakEvent) {
        observers.forEach { it.avsluttetMedVedtak(avsluttetMedVedtakEvent) }
    }

    internal fun analytiskDatapakke(analytiskDatapakkeEvent: PersonObserver.AnalytiskDatapakkeEvent) {
        observers.forEach { it.analytiskDatapakke(analytiskDatapakkeEvent) }
    }

    internal fun behandlingLukket(behandlingLukketEvent: PersonObserver.BehandlingLukketEvent) {
        observers.forEach { it.behandlingLukket(behandlingLukketEvent) }
    }

    internal fun behandlingForkastet(behandlingForkastetEvent: PersonObserver.BehandlingForkastetEvent) {
        observers.forEach { it.behandlingForkastet(behandlingForkastetEvent) }
    }

    internal fun nyBehandling(event: PersonObserver.BehandlingOpprettetEvent) {
        observers.forEach { it.nyBehandling(event) }
    }

    internal fun utkastTilVedtak(event: PersonObserver.UtkastTilVedtakEvent) {
        observers.forEach { it.utkastTilVedtak(event) }
    }

    fun planlagtAnnullering(planlagtAnnullering: PersonObserver.PlanlagtAnnulleringEvent) {
        observers.forEach { it.planlagtAnnullering(planlagtAnnullering) }
    }

    internal fun emitOverstyringIgangsattEvent(event: PersonObserver.OverstyringIgangsatt) {
        observers.forEach { it.overstyringIgangsatt(event) }
    }

    internal fun feriepengerUtbetalt(feriepengerUtbetaltEvent: PersonObserver.FeriepengerUtbetaltEvent) {
        observers.forEach { it.feriepengerUtbetalt(feriepengerUtbetaltEvent) }
    }

    internal fun emitInntektsmeldingFørSøknadEvent(
        meldingsreferanseId: UUID,
        yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet
    ) {
        observers.forEach {
            it.inntektsmeldingFørSøknad(PersonObserver.InntektsmeldingFørSøknadEvent(meldingsreferanseId, yrkesaktivitetssporing))
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

    internal fun sendSkatteinntekterLagtTilGrunn(skatteinntekterLagtTilGrunnEvent: PersonObserver.SkatteinntekterLagtTilGrunnEvent) {
        observers.forEach {
            it.skatteinntekterLagtTilGrunn(skatteinntekterLagtTilGrunnEvent)
        }
    }

    internal fun emitSøknadHåndtert(meldingsreferanseId: UUID, vedtaksperiodeId: UUID, organisasjonsnummer: String) {
        observers.forEach {
            it.søknadHåndtert(meldingsreferanseId, vedtaksperiodeId, organisasjonsnummer)
        }
    }

    internal fun vedtaksperiodeVenter(eventer: List<PersonObserver.VedtaksperiodeVenterEvent>) {
        observers.forEach { it.vedtaksperioderVenter(eventer) }
    }

    internal fun behandlingUtført() {
        observers.forEach { it.behandlingUtført() }
    }

    internal fun nyVedtaksperiodeUtbetaling(organisasjonsnummer: String, utbetalingId: UUID, vedtaksperiodeId: UUID) {
        observers.forEach { it.nyVedtaksperiodeUtbetaling(organisasjonsnummer, utbetalingId, vedtaksperiodeId) }
    }

    internal fun vedtaksperiodeOpprettet(vedtaksperiodeId: UUID, yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet, periode: Periode, skjæringstidspunkt: LocalDate, opprettet: LocalDateTime) {
        val event = PersonObserver.VedtaksperiodeOpprettet(vedtaksperiodeId, yrkesaktivitetssporing, periode, skjæringstidspunkt, opprettet)
        observers.forEach { it.vedtaksperiodeOpprettet(event) }
    }

    internal fun vedtaksperiodeAnnullert(vedtaksperiodeAnnullertEvent: PersonObserver.VedtaksperiodeAnnullertEvent) {
        observers.forEach { it.vedtaksperiodeAnnullert(vedtaksperiodeAnnullertEvent) }
    }
}
