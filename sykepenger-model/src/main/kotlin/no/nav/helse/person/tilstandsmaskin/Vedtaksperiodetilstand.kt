package no.nav.helse.person.tilstandsmaskin

import java.time.LocalDateTime
import no.nav.helse.hendelser.DagerFraInntektsmelding
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode

// Gang of four State pattern
internal sealed interface Vedtaksperiodetilstand {
    val type: TilstandType
    val erFerdigBehandlet: Boolean get() = false

    fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {}
    fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
        LocalDateTime.MAX

    fun håndterMakstid(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.funksjonellFeil(Varselkode.RV_VT_1)
        vedtaksperiode.forkast(påminnelse, aktivitetslogg)
    }

    fun replayUtført(vedtaksperiode: Vedtaksperiode, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {}
    fun inntektsmeldingFerdigbehandlet(
        vedtaksperiode: Vedtaksperiode,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
    }

    fun skalHåndtereDager(
        vedtaksperiode: Vedtaksperiode,
        dager: DagerFraInntektsmelding,
        aktivitetslogg: IAktivitetslogg
    ) =
        dager.skalHåndteresAv(vedtaksperiode.periode)

    fun håndterKorrigerendeInntektsmelding(vedtaksperiode: Vedtaksperiode, dager: DagerFraInntektsmelding, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.håndterKorrigerendeInntektsmelding(dager, aktivitetslogg)
    }

    fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {}

    fun håndterUtbetalingHendelse(vedtaksperiode: Vedtaksperiode, hendelse: UtbetalingHendelse, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Forventet ikke utbetaling i %s".format(type.name))
    }

    fun håndterOverstyrArbeidsgiveropplysninger(
        vedtaksperiode: Vedtaksperiode,
        hendelse: OverstyrArbeidsgiveropplysninger,
        aktivitetslogg: IAktivitetslogg
    ) {
    }

    fun nyAnnullering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {}
    fun gjenopptaBehandling(
        vedtaksperiode: Vedtaksperiode,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
        aktivitetslogg.info("Tidligere periode ferdigbehandlet, men gjør ingen tilstandsendring.")
    }

    fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    )

    fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {}
}
