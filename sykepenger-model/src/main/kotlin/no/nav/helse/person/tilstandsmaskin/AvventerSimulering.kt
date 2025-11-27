package no.nav.helse.person.tilstandsmaskin

import java.time.LocalDate
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.utbetalingslinjer.Oppdrag

internal data object AvventerSimulering : Vedtaksperiodetilstand {
    override val type: TilstandType = TilstandType.AVVENTER_SIMULERING
    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        trengerSimulering(vedtaksperiode, aktivitetslogg)
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        trengerSimulering(vedtaksperiode, aktivitetslogg)
        return null
    }
}

private const val systemident = "SPLEIS"
internal fun trengerSimulering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
    val utbetaling = checkNotNull(vedtaksperiode.behandlinger.utbetaling)
    val aktivitetsloggMedUtbetalingkontekst = aktivitetslogg.kontekst(utbetaling)

    simuleringsbehov(aktivitetsloggMedUtbetalingkontekst, utbetaling.arbeidsgiverOppdrag, vedtaksperiode.behandlinger.maksdato.maksdato)
    simuleringsbehov(aktivitetsloggMedUtbetalingkontekst, utbetaling.personOppdrag, vedtaksperiode.behandlinger.maksdato.maksdato)
}

private fun simuleringsbehov(aktivitetslogg: IAktivitetslogg, oppdrag: Oppdrag, maksdato: LocalDate) {
    utbetalingsbehovdetaljer(oppdrag, systemident, maksdato)?.also {
        aktivitetslogg
            .kontekst(oppdrag)
            .behov(Behovtype.Simulering, "Trenger simulering fra Oppdragssystemet", it)
    }
}
