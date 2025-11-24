package no.nav.helse.person

import java.time.LocalDate
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.EventSubscription.UtbetalingEndretEvent.OppdragEventDetaljer
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.UtbetalingObserver
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingslinjer.Utbetalingtype

internal class BehandlingEventBus(
    private val eventBus: EventBus,
    private val yrkesaktivitetstype: Behandlingsporing.Yrkesaktivitet,
    private val vedtaksperiodeId: UUID,
    private val tidligereSøknadIder: Set<MeldingsreferanseId>
) : UtbetalingObserver {
    fun behandlingLukket(
        behandlingId: UUID
    ) {
        eventBus.behandlingLukket(
            EventSubscription.BehandlingLukketEvent(
                yrkesaktivitetssporing = yrkesaktivitetstype,
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = behandlingId
            )
        )
    }

    fun behandlingForkastet(
        behandlingId: UUID,
        automatiskBehandling: Boolean
    ) {
        eventBus.behandlingForkastet(
            EventSubscription.BehandlingForkastetEvent(
                yrkesaktivitetssporing = yrkesaktivitetstype,
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = behandlingId,
                automatiskBehandling = automatiskBehandling
            )
        )
    }

    fun nyBehandling(
        id: UUID,
        periode: Periode,
        meldingsreferanseId: MeldingsreferanseId,
        innsendt: LocalDateTime,
        registert: LocalDateTime,
        avsender: Avsender,
        type: EventSubscription.BehandlingOpprettetEvent.Type,
        søknadIder: Set<MeldingsreferanseId>
    ) {
        val event = EventSubscription.BehandlingOpprettetEvent(
            yrkesaktivitetssporing = yrkesaktivitetstype,
            vedtaksperiodeId = vedtaksperiodeId,
            søknadIder = (tidligereSøknadIder + søknadIder).map { it.id }.toSet(),
            behandlingId = id,
            fom = periode.start,
            tom = periode.endInclusive,
            type = type,
            kilde = EventSubscription.BehandlingOpprettetEvent.Kilde(meldingsreferanseId.id, innsendt, registert, avsender)
        )
        eventBus.nyBehandling(event)
    }

    internal fun utbetalingAnnullert(
        id: UUID,
        korrelasjonsId: UUID,
        periode: Periode,
        personFagsystemId: String,
        godkjenttidspunkt: LocalDateTime,
        saksbehandlerEpost: String,
        saksbehandlerIdent: String,
        arbeidsgiverFagsystemId: String
    ) {
        eventBus.annullert(
            EventSubscription.UtbetalingAnnullertEvent(
                yrkesaktivitetssporing = yrkesaktivitetstype,
                korrelasjonsId = korrelasjonsId,
                arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
                personFagsystemId = personFagsystemId,
                utbetalingId = id,
                fom = periode.start,
                tom = periode.endInclusive,
                annullertAvSaksbehandler = godkjenttidspunkt,
                saksbehandlerEpost = saksbehandlerEpost,
                saksbehandlerIdent = saksbehandlerIdent
            )
        )
    }

    internal fun utbetalingUtbetalt(
        id: UUID,
        korrelasjonsId: UUID,
        type: Utbetalingtype,
        periode: Periode,
        maksdato: LocalDate,
        forbrukteSykedager: Int,
        gjenståendeSykedager: Int,
        stønadsdager: Int,
        arbeidsgiverOppdrag: Oppdrag,
        personOppdrag: Oppdrag,
        epost: String,
        tidspunkt: LocalDateTime,
        automatiskBehandling: Boolean,
        utbetalingstidslinje: List<EventSubscription.Utbetalingsdag>,
        ident: String
    ) {
        eventBus.utbetalingUtbetalt(
            EventSubscription.UtbetalingUtbetaltEvent(
                yrkesaktivitetssporing = yrkesaktivitetstype,
                utbetalingId = id,
                type = type.name,
                korrelasjonsId = korrelasjonsId,
                fom = periode.start,
                tom = periode.endInclusive,
                maksdato = maksdato,
                forbrukteSykedager = forbrukteSykedager,
                gjenståendeSykedager = gjenståendeSykedager,
                stønadsdager = stønadsdager,
                epost = epost,
                tidspunkt = tidspunkt,
                automatiskBehandling = automatiskBehandling,
                arbeidsgiverOppdrag = EventSubscription.OppdragEventDetaljer.mapOppdrag(arbeidsgiverOppdrag),
                personOppdrag = EventSubscription.OppdragEventDetaljer.mapOppdrag(personOppdrag),
                utbetalingsdager = utbetalingstidslinje,
                ident = ident
            )
        )
    }

    internal fun utbetalingUtenUtbetaling(
        id: UUID,
        korrelasjonsId: UUID,
        type: Utbetalingtype,
        periode: Periode,
        maksdato: LocalDate,
        forbrukteSykedager: Int,
        gjenståendeSykedager: Int,
        stønadsdager: Int,
        personOppdrag: Oppdrag,
        ident: String,
        arbeidsgiverOppdrag: Oppdrag,
        tidspunkt: LocalDateTime,
        automatiskBehandling: Boolean,
        utbetalingstidslinje: List<EventSubscription.Utbetalingsdag>,
        epost: String
    ) {
        eventBus.utbetalingUtenUtbetaling(
            EventSubscription.UtbetalingUtenUtbetalingEvent(
                yrkesaktivitetssporing = yrkesaktivitetstype,
                utbetalingId = id,
                type = type.name,
                fom = periode.start,
                tom = periode.endInclusive,
                maksdato = maksdato,
                forbrukteSykedager = forbrukteSykedager,
                gjenståendeSykedager = gjenståendeSykedager,
                stønadsdager = stønadsdager,
                epost = epost,
                tidspunkt = tidspunkt,
                automatiskBehandling = automatiskBehandling,
                arbeidsgiverOppdrag = EventSubscription.OppdragEventDetaljer.mapOppdrag(arbeidsgiverOppdrag),
                personOppdrag = EventSubscription.OppdragEventDetaljer.mapOppdrag(personOppdrag),
                utbetalingsdager = utbetalingstidslinje,
                ident = ident,
                korrelasjonsId = korrelasjonsId
            )
        )
    }

    override fun utbetalingEndret(
        id: UUID,
        type: Utbetalingtype,
        arbeidsgiverOppdrag: Oppdrag,
        personOppdrag: Oppdrag,
        forrigeTilstand: Utbetalingstatus,
        nesteTilstand: Utbetalingstatus,
        korrelasjonsId: UUID
    ) {
        eventBus.utbetalingEndret(
            EventSubscription.UtbetalingEndretEvent(
                yrkesaktivitetssporing = yrkesaktivitetstype,
                utbetalingId = id,
                type = type.name,
                forrigeStatus = forrigeTilstand.name,
                gjeldendeStatus = nesteTilstand.name,
                arbeidsgiverOppdrag = OppdragEventDetaljer.mapOppdrag(arbeidsgiverOppdrag),
                personOppdrag = OppdragEventDetaljer.mapOppdrag(personOppdrag),
                korrelasjonsId = korrelasjonsId
            )
        )
    }
}
