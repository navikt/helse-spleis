package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object AvventerSimulering : Vedtaksperiodetilstand {
    override val type: TilstandType = TilstandType.AVVENTER_SIMULERING
    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        trengerSimulering(vedtaksperiode, eventBus, aktivitetslogg)
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        trengerSimulering(vedtaksperiode, eventBus, aktivitetslogg)
        return null
    }
}

internal fun trengerSimulering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
    val utbetaling = checkNotNull(vedtaksperiode.behandlinger.utbetaling)
    val aktivitetsloggMedUtbetalingkontekst = aktivitetslogg.kontekst(utbetaling)

    oppdragsdetaljer(utbetaling.arbeidsgiverOppdrag, vedtaksperiode.behandlinger.maksdato.maksdato)?.let {
        eventBus.simuler(
            yrkesaktivitetssporing = vedtaksperiode.yrkesaktivitet.yrkesaktivitetstype,
            vedtaksperiodeId = vedtaksperiode.id,
            behandlingId = vedtaksperiode.behandlinger.sisteBehandlingId,
            utbetalingId = utbetaling.id,
            oppdragsdetaljer = it
        )
        val aktivitetsloggMedOppdragkontekst = aktivitetsloggMedUtbetalingkontekst.kontekst(utbetaling.arbeidsgiverOppdrag)
        aktivitetsloggMedOppdragkontekst.info("Sender ut event om at utbetalingen til arbeidsgiver skal simuleres")
    }

    oppdragsdetaljer(utbetaling.personOppdrag, vedtaksperiode.behandlinger.maksdato.maksdato)?.let {
        eventBus.simuler(
            yrkesaktivitetssporing = vedtaksperiode.yrkesaktivitet.yrkesaktivitetstype,
            vedtaksperiodeId = vedtaksperiode.id,
            behandlingId = vedtaksperiode.behandlinger.sisteBehandlingId,
            utbetalingId = utbetaling.id,
            oppdragsdetaljer = it
        )
        val aktivitetsloggMedOppdragkontekst = aktivitetsloggMedUtbetalingkontekst.kontekst(utbetaling.personOppdrag)
        aktivitetsloggMedOppdragkontekst.info("Sender ut event om at utbetalingen til sykmeldt skal simuleres")
    }
}
