package no.nav.helse.person.tilstandsmaskin

import java.time.LocalDateTime
import java.time.Period
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.DagerFraInntektsmelding
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.behandlingkilde

internal data object AvventerInntektsmelding : Vedtaksperiodetilstand {
    override val type: TilstandType = TilstandType.AVVENTER_INNTEKTSMELDING
    override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
        tilstandsendringstidspunkt.plusDays(180)

    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        check(vedtaksperiode.yrkesaktivitet.yrkesaktivitetstype is Behandlingsporing.Yrkesaktivitet.Arbeidstaker) { "Forventer kun arbeidstakere her" }
        vedtaksperiode.trengerInntektsmeldingReplay()
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
        dager: DagerFraInntektsmelding,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterDager(dager, aktivitetslogg)
        if (aktivitetslogg.harFunksjonelleFeilEllerVerre()) return vedtaksperiode.forkast(dager.hendelse, aktivitetslogg)
    }

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        if (vurderOmKanGåVidere(vedtaksperiode, revurdering.hendelse, aktivitetslogg)) return
        vedtaksperiode.sendTrengerArbeidsgiveropplysninger()
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        if (vurderOmKanGåVidere(vedtaksperiode, påminnelse, aktivitetslogg)) {
            aktivitetslogg.info("Gikk videre fra AvventerInntektsmelding til ${vedtaksperiode.tilstand::class.simpleName} som følge av en vanlig påminnelse.")
        }

        if (påminnelse.når(Påminnelse.Predikat.Flagg("trengerReplay"))) return vedtaksperiode.trengerInntektsmeldingReplay()
        if (vurderOmInntektsmeldingAldriKommer(påminnelse)) return vedtaksperiode.tilstand(aktivitetslogg, AvventerAOrdningen)
        vedtaksperiode.sendTrengerArbeidsgiveropplysninger()
    }

    private fun vurderOmInntektsmeldingAldriKommer(påminnelse: Påminnelse): Boolean {
        if (påminnelse.når(Påminnelse.Predikat.Flagg("ønskerInntektFraAOrdningen"))) return true
        val ventetMinst3Måneder = påminnelse.når(Påminnelse.Predikat.VentetMinst(Period.ofDays(90)))
        return ventetMinst3Måneder
    }

    override fun gjenopptaBehandling(
        vedtaksperiode: Vedtaksperiode,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
        vurderOmKanGåVidere(vedtaksperiode, hendelse, aktivitetslogg)
    }

    override fun replayUtført(vedtaksperiode: Vedtaksperiode, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.sendTrengerArbeidsgiveropplysninger()
        vurderOmKanGåVidere(vedtaksperiode, hendelse, aktivitetslogg)
    }

    override fun inntektsmeldingFerdigbehandlet(
        vedtaksperiode: Vedtaksperiode,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
        vurderOmKanGåVidere(vedtaksperiode, hendelse, aktivitetslogg)
    }

    private fun vurderOmKanGåVidere(
        vedtaksperiode: Vedtaksperiode,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ): Boolean {
        if (vedtaksperiode.manglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag()) {
            aktivitetslogg.funksjonellFeil(Varselkode.RV_SV_2)
            vedtaksperiode.forkast(hendelse, aktivitetslogg)
            return true
        }
        vedtaksperiode.videreførEksisterendeOpplysninger(hendelse.metadata.behandlingkilde, aktivitetslogg)

        if (vedtaksperiode.måInnhenteInntektEllerRefusjon()) return false
        vedtaksperiode.tilstand(aktivitetslogg, AvventerBlokkerendePeriode)
        return true
    }
}
