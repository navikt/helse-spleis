package no.nav.helse.person.tilstandsmaskin

import java.time.LocalDate
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.Behandlinger
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Utbetalingslinje

internal data object TilUtbetaling : Vedtaksperiodetilstand {
    override val type = TilstandType.TIL_UTBETALING

    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        trengerUtbetaling(vedtaksperiode, aktivitetslogg)
    }

    override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Stopper gjenoppta behandling pga. pågående utbetaling")
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        trengerUtbetaling(vedtaksperiode, aktivitetslogg)
        return null
    }
}

internal fun trengerUtbetaling(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg, medMaksdato: Boolean = true) {
    val forrigeBehandling = vedtaksperiode.behandlinger.forrigeBehandling
    val aktivitetsloggMedForrigeBehandlingkontekst = aktivitetslogg.kontekst(forrigeBehandling)
    return overførUtbetaling(aktivitetsloggMedForrigeBehandlingkontekst, forrigeBehandling, forrigeBehandling.maksdato.maksdato.takeIf { medMaksdato })
}

private fun overførUtbetaling(aktivitetslogg: IAktivitetslogg, forrigeBehandling: Behandlinger.Behandling, maksdato: LocalDate?) {
    val utbetaling = checkNotNull(forrigeBehandling.utbetaling()) { "forventer utbetaling" }
    val saksbehandler = checkNotNull(utbetaling.vurdering) { "forventer vurdering" }.ident

    val aktivitetsloggMedUtbetalingkontekst = aktivitetslogg.kontekst(utbetaling)

    utbetalingsbehov(aktivitetsloggMedUtbetalingkontekst, utbetaling.arbeidsgiverOppdrag, saksbehandler, maksdato)
    utbetalingsbehov(aktivitetsloggMedUtbetalingkontekst, utbetaling.personOppdrag, saksbehandler, maksdato)
}

private fun utbetalingsbehov(aktivitetslogg: IAktivitetslogg, oppdrag: Oppdrag, saksbehandler: String, maksdato: LocalDate?) {
    val utbetalingstype = when (oppdrag.fagområde) {
        Fagområde.SykepengerRefusjon -> "arbeidsgiverutbetaling"
        Fagområde.Sykepenger -> "personutbetaling"
    }

    utbetalingsbehovdetaljer(oppdrag, saksbehandler, maksdato)?.also {
        aktivitetslogg
            .kontekst(oppdrag)
            .behov(Behovtype.Utbetaling, "Trenger å sende $utbetalingstype til Oppdrag", it)
    }
}

internal fun utbetalingsbehovdetaljer(oppdrag: Oppdrag, saksbehandler: String, maksdato: LocalDate?): Map<String, Any>? {
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
                val maksdatomap = maksdato?.let { mapOf("maksdato" to maksdato.toString()) } ?: emptyMap()
                return mapOf(
                    "mottaker" to oppdrag.mottaker,
                    "fagområde" to oppdrag.fagområde.verdi,
                    "linjer" to linjerMedEndring.map(Utbetalingslinje::behovdetaljer),
                    "fagsystemId" to oppdrag.fagsystemId,
                    "endringskode" to "${oppdrag.endringskode}",
                    "saksbehandler" to saksbehandler
                ) + maksdatomap
            }
        }
    }
}

private fun Utbetalingslinje.behovdetaljer() = mapOf<String, Any?>(
    "fom" to fom.toString(),
    "tom" to tom.toString(),
    "satstype" to "DAG",
    "sats" to beløp,
    "grad" to grad.toDouble(), // backwards-compatibility mot andre systemer som forventer double: må gjennomgås
    "stønadsdager" to stønadsdager(),
    "totalbeløp" to totalbeløp(),
    "endringskode" to endringskode.toString(),
    "delytelseId" to delytelseId,
    "refDelytelseId" to refDelytelseId,
    "refFagsystemId" to refFagsystemId,
    "statuskode" to statuskode,
    "datoStatusFom" to datoStatusFom?.toString(),
    "klassekode" to klassekode.verdi,
    "datoKlassifikFom" to fom.toString(),
)
