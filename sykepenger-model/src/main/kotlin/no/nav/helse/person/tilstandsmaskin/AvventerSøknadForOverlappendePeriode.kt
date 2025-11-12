package no.nav.helse.person.tilstandsmaskin

import java.time.Period
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode

internal data object AvventerSøknadForOverlappendePeriode : Vedtaksperiodetilstand {
    override val type: TilstandType = TilstandType.AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE

    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        bekreftAtPeriodenSkalBehandlesISpeilOgHarNokInformasjon(vedtaksperiode)
        check(vedtaksperiode.person.avventerSøknad(vedtaksperiode.periode)) { "forventer å vente annen søknad" }
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        if (vedtaksperiode.person.avventerSøknad(vedtaksperiode.periode)) {
            return aktivitetslogg.info("Gjenopptar ikke behandling fordi minst én arbeidsgiver venter på søknad for sykmelding som er før eller overlapper med vedtaksperioden")
        }
        gåVidere(vedtaksperiode, eventBus, aktivitetslogg)
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        val ventetMinstTreMåneder = påminnelse.når(Påminnelse.Predikat.VentetMinst(Period.ofMonths(3)))
        val forkasteOverlappendeSykmeldingsperidoer = påminnelse.når(Påminnelse.Predikat.Flagg("forkastOverlappendeSykmeldingsperioderAndreArbeidsgivere"))
        if (ventetMinstTreMåneder || forkasteOverlappendeSykmeldingsperidoer) {
            aktivitetslogg.varsel(Varselkode.RV_SY_4)
            vedtaksperiode.person.fjernSykmeldingsperiode(vedtaksperiode.periode)
            gåVidere(vedtaksperiode, eventBus, aktivitetslogg)
        }
    }

    private fun gåVidere(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.tilstand(eventBus, aktivitetslogg, AvventerBlokkerendePeriode)
    }
}
