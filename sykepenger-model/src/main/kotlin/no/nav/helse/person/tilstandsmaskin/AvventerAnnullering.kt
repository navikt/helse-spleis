package no.nav.helse.person.tilstandsmaskin

import java.time.LocalDateTime
import no.nav.helse.hendelser.AnnullerUtbetaling
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetalingkladd
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

        val annullering = lagAnnulleringsutbetaling(eventBus, vedtaksperiode, aktivitetslogg)

        val vurdering = (hendelse as? AnnullerUtbetaling)?.vurdering
            ?: Utbetaling.Vurdering(true, "Automatisk behandlet", "tbd@nav.no", LocalDateTime.now(), true)

        vedtaksperiode.behandlinger.leggTilAnnullering(with (vedtaksperiode.yrkesaktivitet) { eventBus.utbetalingEventBus }, annullering, vurdering, aktivitetslogg)

        if (!vedtaksperiode.behandlinger.erAvsluttet())
            return vedtaksperiode.tilstand(eventBus, aktivitetslogg, TilAnnullering)

        vedtaksperiode.vedtakAnnullert(eventBus, hendelse, aktivitetslogg)
    }

    private fun lagAnnulleringsutbetaling(eventBus: EventBus, vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg): Utbetaling {
        return (annullerAktivUtbetalingForVedtaksperiode(vedtaksperiode, aktivitetslogg) ?: lagTomAnnulleringsutbetaling(vedtaksperiode)).also {
            vedtaksperiode.yrkesaktivitet.leggTilNyUtbetaling(eventBus, aktivitetslogg, it)
        }
    }

    private fun annullerAktivUtbetalingForVedtaksperiode(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg): Utbetaling? {
        // for nye ting så er det tilstrekkelig å annullere forrige utbetaling på behandlingen,
        // men vi må støtte at oppdrag ble delt mellom flere vedtaksperioder på eldre ting
        return vedtaksperiode
            .yrkesaktivitet
            .aktiveUtbetalingerForPeriode(vedtaksperiode.periode)
            .let {
                when {
                    it.size <= 1 -> it.firstOrNull()
                    else -> error("Finner flere aktive utbetalinger som overlapper med vedtaksperioden")
                }
            }
            ?.lagAnnulleringsutbetaling(aktivitetslogg)
    }

    private fun lagTomAnnulleringsutbetaling(vedtaksperiode: Vedtaksperiode): Utbetaling {
        return Utbetaling.lagTomUtbetaling(
            vedtaksperiodekladd = Utbetalingkladd(
                arbeidsgiveroppdrag = Oppdrag(mottaker = vedtaksperiode.yrkesaktivitet.organisasjonsnummer, fagområde = Fagområde.SykepengerRefusjon),
                personoppdrag = Oppdrag(mottaker = vedtaksperiode.person.fødselsnummer, fagområde = Fagområde.Sykepenger)
            ),
            periode = vedtaksperiode.periode,
            type = Utbetalingtype.ANNULLERING
        )
    }
}
