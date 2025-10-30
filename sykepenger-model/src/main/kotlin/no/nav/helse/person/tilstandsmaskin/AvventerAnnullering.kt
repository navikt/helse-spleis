package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.AnnullerTomUtbetaling
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.utbetalingslinjer.Utbetalingtype

internal data object AvventerAnnullering : Vedtaksperiodetilstand {
    override val type = TilstandType.AVVENTER_ANNULLERING

    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, revurdering: Revurderingseventyr, aktivitetslogg: IAktivitetslogg) {}

    override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        if (vedtaksperiode.behandlinger.utbetales()) {
            aktivitetslogg.info("Stopper gjenoppta behandling pga. pågående utbetaling")
            return
        }
        val sisteUtbetalteUtbetaling = vedtaksperiode.behandlinger.sisteUtbetalteUtbetaling()
        checkNotNull(sisteUtbetalteUtbetaling) { "Fant ikke en utbetalt utbetaling for vedtaksperiode ${vedtaksperiode.id}" }

        // for nye ting så er det tilstrekkelig å annullere forrige utbetaling på behandlingen,
        // men vi må støtte at oppdrag ble delt mellom flere vedtaksperioder på eldre ting
        val annullering = vedtaksperiode
            .yrkesaktivitet
            .aktiveUtbetalingerForPeriode(sisteUtbetalteUtbetaling, vedtaksperiode.periode)
            .let {
                when {
                    it.size <= 1 -> it.firstOrNull()
                    else -> error("Finner flere aktive utbetalinger som overlapper med vedtaksperioden")
                }
            }
            ?.let { utbetalingSomSkalAnnulleres ->
                vedtaksperiode.yrkesaktivitet.lagAnnulleringsutbetaling(eventBus, hendelse, aktivitetslogg, utbetalingSomSkalAnnulleres)
            } ?: vedtaksperiode.yrkesaktivitet.lagTomUtbetaling(vedtaksperiode.periode, Utbetalingtype.ANNULLERING)
                .also { it.opprett(with (vedtaksperiode.yrkesaktivitet) { eventBus.utbetalingEventBus }, aktivitetslogg) }

        vedtaksperiode.behandlinger.leggTilAnnullering(eventBus, with (vedtaksperiode.yrkesaktivitet) { eventBus.utbetalingEventBus }, annullering, aktivitetslogg)

        if (!vedtaksperiode.behandlinger.erAvsluttet()) {
            vedtaksperiode.tilstand(eventBus, aktivitetslogg, TilAnnullering)
        } else {
            vedtaksperiode.forkast(eventBus, AnnullerTomUtbetaling(vedtaksperiode.yrkesaktivitet.yrkesaktivitetstype), aktivitetslogg)
        }
    }
}
