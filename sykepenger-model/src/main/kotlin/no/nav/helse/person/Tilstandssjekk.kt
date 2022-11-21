package no.nav.helse.person

import no.nav.helse.person.Vedtaksperiode.Avsluttet
import no.nav.helse.person.Vedtaksperiode.AvsluttetUtenUtbetaling
import no.nav.helse.person.Vedtaksperiode.AvventerBlokkerendePeriode
import no.nav.helse.person.Vedtaksperiode.AvventerGjennomførtRevurdering
import no.nav.helse.person.Vedtaksperiode.AvventerGodkjenning
import no.nav.helse.person.Vedtaksperiode.AvventerGodkjenningRevurdering
import no.nav.helse.person.Vedtaksperiode.AvventerHistorikk
import no.nav.helse.person.Vedtaksperiode.AvventerHistorikkRevurdering
import no.nav.helse.person.Vedtaksperiode.AvventerInntektsmeldingEllerHistorikk
import no.nav.helse.person.Vedtaksperiode.AvventerRevurdering
import no.nav.helse.person.Vedtaksperiode.AvventerSimulering
import no.nav.helse.person.Vedtaksperiode.AvventerSimuleringRevurdering
import no.nav.helse.person.Vedtaksperiode.AvventerVilkårsprøving
import no.nav.helse.person.Vedtaksperiode.AvventerVilkårsprøvingRevurdering
import no.nav.helse.person.Vedtaksperiode.RevurderingFeilet
import no.nav.helse.person.Vedtaksperiode.Start
import no.nav.helse.person.Vedtaksperiode.TilInfotrygd
import no.nav.helse.person.Vedtaksperiode.TilUtbetaling
import no.nav.helse.person.Vedtaksperiode.UtbetalingFeilet
import no.nav.helse.person.Vedtaksperiode.Vedtaksperiodetilstand

internal abstract class Tilstandssjekk(
    private val enAv: Set<Vedtaksperiodetilstand>,
    ikkeEnAv: Set<Vedtaksperiodetilstand>
): Iterable<Vedtaksperiodetilstand> by enAv {
    init {
        val har = enAv.map { it::class } + ikkeEnAv.map { it::class }
        val mangler = Vedtaksperiodetilstand::class.sealedSubclasses.minus(har.toSet())
        check(mangler.isEmpty()) { "Tilstandssjekken '${this::class.simpleName}' har ikke tatt stilling til tilstandene ${mangler.map { it.simpleName }}" }
    }

    internal object AktivRevurdering: Tilstandssjekk(
        enAv = setOf(
            AvventerHistorikkRevurdering,
            AvventerSimuleringRevurdering,
            AvventerGodkjenningRevurdering,
            AvventerGjennomførtRevurdering,
            AvventerVilkårsprøvingRevurdering,
        ),
        ikkeEnAv = setOf(
            Start,
            Avsluttet,
            TilInfotrygd,
            TilUtbetaling,
            UtbetalingFeilet,
            AvventerHistorikk,
            RevurderingFeilet,
            AvventerSimulering,
            AvventerRevurdering,
            AvventerGodkjenning,
            AvventerVilkårsprøving,
            AvsluttetUtenUtbetaling,
            AvventerBlokkerendePeriode,
            AvventerInntektsmeldingEllerHistorikk,
        )
    )
    internal object PågåendeRevurdering: Tilstandssjekk(
        enAv = setOf(
            TilUtbetaling,
            UtbetalingFeilet,
            AvventerHistorikkRevurdering,
            AvventerSimuleringRevurdering,
            AvventerGodkjenningRevurdering
        ),
        ikkeEnAv = setOf(
            Start,
            Avsluttet,
            TilInfotrygd,
            RevurderingFeilet,
            AvventerHistorikk,
            AvventerSimulering,
            AvventerRevurdering,
            AvventerGodkjenning,
            AvventerVilkårsprøving,
            AvsluttetUtenUtbetaling,
            AvventerBlokkerendePeriode,
            AvventerGjennomførtRevurdering,
            AvventerVilkårsprøvingRevurdering,
            AvventerInntektsmeldingEllerHistorikk
        )
    )
}