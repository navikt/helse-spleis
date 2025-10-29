package no.nav.helse.person.tilstandsmaskin

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

        val sisteAktiveUtbetalingMedSammeKorrelasjonsId = vedtaksperiode.yrkesaktivitet.sisteAktiveUtbetalingMedSammeKorrelasjonsId(sisteUtbetalteUtbetaling)

        if (sisteAktiveUtbetalingMedSammeKorrelasjonsId != null && sisteAktiveUtbetalingMedSammeKorrelasjonsId.overlapperMed(vedtaksperiode.periode)) {
            val annullering = vedtaksperiode.yrkesaktivitet.lagAnnulleringsutbetaling(eventBus, hendelse, aktivitetslogg, sisteAktiveUtbetalingMedSammeKorrelasjonsId)
            vedtaksperiode.behandlinger.leggTilAnnullering(eventBus, annullering, aktivitetslogg)
        } else {
            val tomAnnullering = vedtaksperiode.yrkesaktivitet.lagTomUtbetaling(vedtaksperiode.periode, Utbetalingtype.ANNULLERING)
                .also { it.opprett(with (vedtaksperiode.yrkesaktivitet) { eventBus.utbetalingEventBus }, aktivitetslogg) }
            vedtaksperiode.behandlinger.leggTilAnnullering(eventBus, tomAnnullering, aktivitetslogg)
        }
        vedtaksperiode.tilstand(eventBus, aktivitetslogg, TilAnnullering)
    }
}
