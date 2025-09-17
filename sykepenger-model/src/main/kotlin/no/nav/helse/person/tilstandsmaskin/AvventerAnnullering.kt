package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.utbetalingslinjer.Utbetalingtype

internal data object AvventerAnnullering : Vedtaksperiodetilstand {
    override val type = TilstandType.AVVENTER_ANNULLERING

    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, revurdering: Revurderingseventyr, aktivitetslogg: IAktivitetslogg) {}

    override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        if (vedtaksperiode.behandlinger.utbetales()) {
            aktivitetslogg.info("Stopper gjenoppta behandling pga. pågående utbetaling")
            return
        }
        val sisteUtbetalteUtbetaling = vedtaksperiode.behandlinger.sisteUtbetalteUtbetaling()
        checkNotNull(sisteUtbetalteUtbetaling) { "Fant ikke en utbetalt utbetaling for vedtaksperiode ${vedtaksperiode.id}" }
        val grunnlagsdata = checkNotNull(vedtaksperiode.vilkårsgrunnlag) { "Mangler vilkårsgrunnlag i pågående annullering for vedtaksperiode ${vedtaksperiode.id} etter hendelse ${hendelse.metadata.meldingsreferanseId}, er ikke det litt rart?" }

        val sisteAktiveUtbetalingMedSammeKorrelasjonsId = vedtaksperiode.yrkesaktivitet.sisteAktiveUtbetalingMedSammeKorrelasjonsId(sisteUtbetalteUtbetaling)

        if (sisteAktiveUtbetalingMedSammeKorrelasjonsId != null && sisteAktiveUtbetalingMedSammeKorrelasjonsId.overlapperMed(vedtaksperiode.periode)) {
            val annullering = vedtaksperiode.yrkesaktivitet.lagAnnulleringsutbetaling(hendelse, aktivitetslogg, sisteAktiveUtbetalingMedSammeKorrelasjonsId)
            vedtaksperiode.behandlinger.leggTilAnnullering(annullering, grunnlagsdata, aktivitetslogg)
        } else {
            val tomAnnullering = vedtaksperiode.yrkesaktivitet.lagTomUtbetaling(vedtaksperiode.periode, Utbetalingtype.ANNULLERING)
                .also { it.opprett(aktivitetslogg) }
            vedtaksperiode.behandlinger.leggTilAnnullering(tomAnnullering, grunnlagsdata, aktivitetslogg)
        }
        vedtaksperiode.tilstand(aktivitetslogg, TilAnnullering)
    }
}
