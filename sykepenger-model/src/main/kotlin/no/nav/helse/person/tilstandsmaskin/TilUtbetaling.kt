package no.nav.helse.person.tilstandsmaskin

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.hendelser.til
import no.nav.helse.person.Behandlinger
import no.nav.helse.person.EventBus
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Utbetalingslinje

internal data object TilUtbetaling : Vedtaksperiodetilstand {
    override val type = TilstandType.TIL_UTBETALING

    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        trengerUtbetaling(vedtaksperiode, eventBus, aktivitetslogg)
    }

    override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Stopper gjenoppta behandling pga. pågående utbetaling")
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        trengerUtbetaling(vedtaksperiode, eventBus, aktivitetslogg)
        return null
    }
}

internal fun trengerUtbetaling(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg, medMaksdato: Boolean = true) {
    // Når du står i TilUtbetaling så anses behandlingen som lukket siden vedtaket er fattet (dog ikke avsluttet!)
    // Derfor er det her __forrigeBehandling__, ikke den åpne behandlignen.
    val forrigeBehandling = vedtaksperiode.behandlinger.forrigeBehandling

    return trengerUtbetaling(
        aktivitetslogg = aktivitetslogg.kontekst(forrigeBehandling),
        eventBus = eventBus,
        maksdato = forrigeBehandling.maksdato.maksdato.takeIf { medMaksdato },
        vedtaksperiodeId = vedtaksperiode.id,
        forrigeBehandling = forrigeBehandling,
        yrkesaktivitetssporing = vedtaksperiode.yrkesaktivitet.yrkesaktivitetstype
    )
}

private fun trengerUtbetaling(
    aktivitetslogg: IAktivitetslogg,
    eventBus: EventBus,
    maksdato: LocalDate?,
    vedtaksperiodeId: UUID,
    forrigeBehandling: Behandlinger.Behandling,
    yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet
) {
    val utbetaling = checkNotNull(forrigeBehandling.utbetaling()) { "forventer utbetaling" }
    val saksbehandler = checkNotNull(utbetaling.vurdering) { "forventer vurdering" }.ident

    val aktivitetsloggMedUtbetalingkontekst = aktivitetslogg.kontekst(utbetaling)

    oppdragsdetaljer(utbetaling.arbeidsgiverOppdrag, maksdato)?.let {
        eventBus.utbetal(
            yrkesaktivitetssporing = yrkesaktivitetssporing,
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = forrigeBehandling.id,
            utbetalingId = utbetaling.id,
            oppdragsdetaljer = it,
            saksbehandler = saksbehandler
        )
        val aktivitetsloggMedOppdragkontekst = aktivitetsloggMedUtbetalingkontekst.kontekst(utbetaling.arbeidsgiverOppdrag)
        aktivitetsloggMedOppdragkontekst.info("Sender ut event om at det skal utbetales til arbeidsgiver")
    }

    oppdragsdetaljer(utbetaling.personOppdrag, maksdato)?.let {
        eventBus.utbetal(
            yrkesaktivitetssporing = yrkesaktivitetssporing,
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = forrigeBehandling.id,
            utbetalingId = utbetaling.id,
            oppdragsdetaljer = it,
            saksbehandler = saksbehandler
        )
        val aktivitetsloggMedOppdragkontekst = aktivitetsloggMedUtbetalingkontekst.kontekst(utbetaling.personOppdrag)
        aktivitetsloggMedOppdragkontekst.info("Sender ut event om at det skal utbetales til sykmeldt")
    }
}

internal fun oppdragsdetaljer(oppdrag: Oppdrag, maksdato: LocalDate?): EventSubscription.Oppdragsdetaljer? {
    when (oppdrag.endringskode) {
        Endringskode.UEND -> return null
        Endringskode.NY,
        Endringskode.ENDR -> when (oppdrag.status) {
            Oppdragstatus.AKSEPTERT,
            Oppdragstatus.AKSEPTERT_MED_FEIL,
            Oppdragstatus.FEIL -> return null

            Oppdragstatus.AVVIST,
            Oppdragstatus.OVERFØRT,
            null -> {
                val linjerMedEndring = oppdrag.linjerMedEndring().takeIf { it.isNotEmpty() } ?: return null
                return EventSubscription.Oppdragsdetaljer(
                    mottaker = oppdrag.mottaker,
                    fagområde = oppdrag.fagområde.verdi,
                    linjer = linjerMedEndring.map(Utbetalingslinje::oppdragsdetaljerLinje),
                    fagsystemId = oppdrag.fagsystemId,
                    endringskode = oppdrag.endringskode.toString(),
                    maksdato = maksdato
                )
            }
        }
    }
}

private fun Utbetalingslinje.oppdragsdetaljerLinje() = EventSubscription.Oppdragsdetaljer.Linje(
    periode = fom til tom,
    sats = beløp,
    grad = grad,
    stønadsdager = stønadsdager(),
    totalbeløp = totalbeløp(),
    endringskode = endringskode.toString(),
    delytelseId = delytelseId,
    refDelytelseId = refDelytelseId,
    refFagsystemId = refFagsystemId,
    statuskode = statuskode,
    datoStatusFom = datoStatusFom,
    klassekode = klassekode.verdi
)
