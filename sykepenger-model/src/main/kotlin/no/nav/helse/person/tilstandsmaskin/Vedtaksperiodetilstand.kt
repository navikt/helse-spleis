package no.nav.helse.person.tilstandsmaskin

import java.time.LocalDateTime
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode

internal val starttilstander = setOf(ArbeidstakerStart, SelvstendigStart, FrilansStart, ArbeidsledigStart)

// Gang of four State pattern
internal sealed interface Vedtaksperiodetilstand {
    val type: TilstandType
    val erFerdigBehandlet: Boolean get() = false

    fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {}
    fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
        LocalDateTime.MAX

    fun håndterMakstid(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.funksjonellFeil(Varselkode.RV_VT_1)
        vedtaksperiode.forkast(eventBus, påminnelse, aktivitetslogg)
    }

    fun replayUtført(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {}
    fun inntektsmeldingFerdigbehandlet(
        vedtaksperiode: Vedtaksperiode,
        eventBus: EventBus,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
    }

    fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        return null
    }

    fun håndterOverstyrArbeidsgiveropplysninger(
        vedtaksperiode: Vedtaksperiode,
        hendelse: OverstyrArbeidsgiveropplysninger,
        aktivitetslogg: IAktivitetslogg
    ) {
    }

    fun gjenopptaBehandling(
        vedtaksperiode: Vedtaksperiode,
        eventBus: EventBus,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
        aktivitetslogg.info("Tidligere periode ferdigbehandlet, men gjør ingen tilstandsendring.")
    }

    fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {}
}
