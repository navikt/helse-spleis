package no.nav.helse.spleis.testhelpers

import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.person.EventSubscription

internal class Behovssamler: EventSubscription {

    private val behov = mutableListOf<EventSubscription.Event>()

    override fun trengerGodkjenning(event: EventSubscription.GodkjenningEvent) { behov.add(event) }

    override fun simuler(event: EventSubscription.SimuleringEvent) { behov.add(event) }

    override fun utbetal(event: EventSubscription.UtbetalingEvent) { behov.add(event) }

    override fun trengerInformasjonTilBeregning(event: EventSubscription.TrengerInformasjonTilBeregningEvent) { behov.add(event) }

    override fun trengerInformasjonTilVilkårsprøving(event: EventSubscription.TrengerInformasjonTilVilkårsprøvingEvent) { behov.add(event) }

    override fun trengerInitiellHistorikkFraInfotrygd(event: EventSubscription.TrengerInitiellHistorikkFraInfotrygdEvent) { behov.add(event) }

    internal inline fun <reified R: EventSubscription.Event>sisteEventuelle() : R? {
        val behovet = behov.filterIsInstance<R>().lastOrNull() ?: return null
        behov.remove(behovet)
        return behovet
    }

    // Det kan være to utbetalingsbehov & simuleringsbehov, et per oppdrag (Refusjon/ Sykmeldt) - derfor spesialhåndtert
    internal fun utbetalingsbehov(): Pair<EventSubscription.UtbetalingEvent?, EventSubscription.UtbetalingEvent?> {
        val førsteBehov = sisteEventuelle<EventSubscription.UtbetalingEvent>() ?: return null to null
        val andreBehov = behov.filterIsInstance<EventSubscription.UtbetalingEvent>().lastOrNull { it.utbetalingId == førsteBehov.utbetalingId && it.oppdragsdetaljer.fagområde != førsteBehov.oppdragsdetaljer.fagområde }
        andreBehov?.let { behov.remove(it) }
        return førsteBehov to andreBehov
    }

    internal fun simuleringsbehov(): Pair<EventSubscription.SimuleringEvent?, EventSubscription.SimuleringEvent?> {
        val førsteBehov = sisteEventuelle<EventSubscription.SimuleringEvent>() ?: return null to null
        val andreBehov = behov.filterIsInstance<EventSubscription.SimuleringEvent>().lastOrNull { it.utbetalingId == førsteBehov.utbetalingId && it.oppdragsdetaljer.fagområde != førsteBehov.oppdragsdetaljer.fagområde }
        andreBehov?.let { behov.remove(it) }
        return førsteBehov to andreBehov
    }

    companion object {
        fun Behandlingsporing.Yrkesaktivitet.yrkesaktivitetstypeOgOrgnummer() = when (this) {
            is Behandlingsporing.Yrkesaktivitet.Arbeidstaker -> "ARBEIDSTAKER" to organisasjonsnummer
            else -> somOrganisasjonsnummer.let { it to it }
        }
    }
}
