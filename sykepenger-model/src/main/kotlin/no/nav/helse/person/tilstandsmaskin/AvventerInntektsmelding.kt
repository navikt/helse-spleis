package no.nav.helse.person.tilstandsmaskin

import java.time.LocalDateTime
import java.time.Period
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.DagerFraInntektsmelding
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object AvventerInntektsmelding : Vedtaksperiodetilstand {
    override val type: TilstandType = TilstandType.AVVENTER_INNTEKTSMELDING
    override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
        tilstandsendringstidspunkt.plusDays(180)

    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        check(vedtaksperiode.yrkesaktivitet.yrkesaktivitetstype is Behandlingsporing.Yrkesaktivitet.Arbeidstaker) { "Forventer kun arbeidstakere her" }
        vedtaksperiode.trengerInntektsmeldingReplay(eventBus)
    }

    override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        check(vedtaksperiode.behandlinger.harIkkeUtbetaling()) {
            "hæ?! vedtaksperiodens behandling er ikke uberegnet!"
        }
    }

    override fun skalHåndtereDager(
        vedtaksperiode: Vedtaksperiode,
        dager: DagerFraInntektsmelding,
        aktivitetslogg: IAktivitetslogg
    ): Boolean {
        return vedtaksperiode.skalHåndtereDagerAvventerInntektsmelding(dager, aktivitetslogg)
    }

    override fun håndterKorrigerendeInntektsmelding(
        vedtaksperiode: Vedtaksperiode,
        eventBus: EventBus,
        dager: DagerFraInntektsmelding,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterDager(eventBus, dager, aktivitetslogg)
        if (aktivitetslogg.harFunksjonelleFeil()) return vedtaksperiode.forkast(eventBus, dager.hendelse, aktivitetslogg)
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        if (vurderOmKanGåVidere(vedtaksperiode, eventBus, aktivitetslogg)) {
            aktivitetslogg.info("Gikk videre fra AvventerInntektsmelding til ${vedtaksperiode.tilstand::class.simpleName} som følge av en vanlig påminnelse.")
        }

        if (påminnelse.når(Påminnelse.Predikat.Flagg("trengerReplay"))) return vedtaksperiode.trengerInntektsmeldingReplay(eventBus)
        if (vurderOmInntektsmeldingAldriKommer(påminnelse)) return vedtaksperiode.tilstand(eventBus, aktivitetslogg, AvventerAOrdningen)
        vedtaksperiode.sendTrengerArbeidsgiveropplysninger(eventBus)
    }

    private fun vurderOmInntektsmeldingAldriKommer(påminnelse: Påminnelse): Boolean {
        if (påminnelse.når(Påminnelse.Predikat.Flagg("ønskerInntektFraAOrdningen"))) return true
        val ventetMinst3Måneder = påminnelse.når(Påminnelse.Predikat.VentetMinst(Period.ofDays(90)))
        return ventetMinst3Måneder
    }

    override fun gjenopptaBehandling(
        vedtaksperiode: Vedtaksperiode,
        eventBus: EventBus,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
        vurderOmKanGåVidere(vedtaksperiode, eventBus, aktivitetslogg)
    }

    override fun replayUtført(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.sendTrengerArbeidsgiveropplysninger(eventBus)
        vurderOmKanGåVidere(vedtaksperiode, eventBus, aktivitetslogg)
    }

    override fun inntektsmeldingFerdigbehandlet(
        vedtaksperiode: Vedtaksperiode,
        eventBus: EventBus,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
        vurderOmKanGåVidere(vedtaksperiode, eventBus, aktivitetslogg)
    }

    fun vurderOmKanGåVidere(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg): Boolean {
        vedtaksperiode.videreførEksisterendeOpplysninger(eventBus, aktivitetslogg)

        if (vedtaksperiode.måInnhenteInntektEllerRefusjon()) {
            if (vedtaksperiode.behandlinger.børBrukeSkatteinntekterDirekte()) {
                vedtaksperiode.tilstand(eventBus, aktivitetslogg, AvventerAOrdningen)
                return true
            }
            return false
        }
        vedtaksperiode.tilstand(eventBus, aktivitetslogg, AvventerBlokkerendePeriode)
        return true
    }
}
