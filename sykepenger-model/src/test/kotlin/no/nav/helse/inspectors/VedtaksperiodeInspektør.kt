package no.nav.helse.inspectors

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.inspectors.BehandlingInspektør.Behandling
import no.nav.helse.person.Behandlinger
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VedtaksperiodeVisitor
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Maksdatoresultat
import no.nav.helse.utbetalingstidslinje.Maksdatoresultat.Bestemmelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

internal val Vedtaksperiode.inspektør get() = VedtaksperiodeInspektør(this)

internal class VedtaksperiodeInspektør(vedtaksperiode: Vedtaksperiode) : VedtaksperiodeVisitor {
    private val view = vedtaksperiode.view()

    internal val id = view.id
    internal val periode = view.periode
    internal val oppdatert = view.oppdatert
    internal val skjæringstidspunkt = view.skjæringstidspunkt

    internal val utbetalingstidslinje: Utbetalingstidslinje get() = behandlinger.last().endringer.last().utbetalingstidslinje
    internal val behandlinger = mutableListOf<Behandling>()
    internal var egenmeldingsperioder = listOf<Periode>()

    internal val arbeidsgiverperiode get() = behandlinger.last().endringer.last().arbeidsgiverperiode

    internal val sykdomstidslinje get() = behandlinger.last().endringer.last().sykdomstidslinje

    internal val maksdatoer = mutableListOf<Maksdatoresultat>()

    internal val utbetalinger = mutableListOf<Utbetaling>()

    internal lateinit var hendelser: Set<Dokumentsporing>
        private set

    internal val hendelseIder get() = hendelser.map { it.id }.toSet()

    init {
        vedtaksperiode.accept(this)
    }

    override fun preVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime,
        periode: Periode,
        opprinneligPeriode: Periode,
        skjæringstidspunkt: LocalDate,
        hendelseIder: Set<Dokumentsporing>,
        egenmeldingsperioder: List<Periode>
    ) {
        this.egenmeldingsperioder = egenmeldingsperioder
        this.hendelser = hendelseIder
    }

    override fun preVisitBehandlinger(behandlinger: List<Behandlinger.Behandling>) {
        this.behandlinger.addAll(behandlinger.map { it.inspektør.behandling })
    }

    override fun visitBehandlingendring(
        id: UUID,
        tidsstempel: LocalDateTime,
        sykmeldingsperiode: Periode,
        periode: Periode,
        grunnlagsdata: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement?,
        utbetaling: Utbetaling?,
        dokumentsporing: Dokumentsporing,
        sykdomstidslinje: Sykdomstidslinje,
        skjæringstidspunkt: LocalDate,
        arbeidsgiverperiode: List<Periode>,
        utbetalingstidslinje: Utbetalingstidslinje,
        maksdatoresultat: Maksdatoresultat
    ) {
        if (utbetaling != null) utbetalinger.add(utbetaling)

        if (maksdatoresultat.bestemmelse == Bestemmelse.IKKE_VURDERT) return
        maksdatoer.add(maksdatoresultat)
    }
}
