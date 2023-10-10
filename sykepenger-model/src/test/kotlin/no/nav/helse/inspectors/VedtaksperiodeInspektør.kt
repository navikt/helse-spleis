package no.nav.helse.inspectors

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VedtaksperiodeVisitor
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

internal val Vedtaksperiode.inspektør get() = VedtaksperiodeInspektør(this)

internal class VedtaksperiodeInspektør(vedtaksperiode: Vedtaksperiode) : VedtaksperiodeVisitor {

    init {
        vedtaksperiode.accept(this)
    }

    internal lateinit var id: UUID
        private set
    internal lateinit var periode: Periode
        private set
    internal lateinit var oppdatert: LocalDateTime
        private set
    internal lateinit var skjæringstidspunkt: LocalDate
    internal lateinit var utbetalingIdTilVilkårsgrunnlagId: Pair<UUID, UUID>
    internal lateinit var utbetalingstidslinje: Utbetalingstidslinje

    override fun preVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime,
        periode: Periode,
        opprinneligPeriode: Periode,
        skjæringstidspunkt: () -> LocalDate,
        hendelseIder: Set<Dokumentsporing>
    ) {
        this.id = id
        this.periode = periode
        this.oppdatert = oppdatert
        this.skjæringstidspunkt = skjæringstidspunkt()
    }

    override fun preVisitVedtaksperiodeUtbetaling(
        grunnlagsdata: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement,
        utbetaling: Utbetaling,
        sykdomstidslinje: Sykdomstidslinje
    ) {
        val vilkårsgrunnlagId = grunnlagsdata.inspektør.vilkårsgrunnlagId
        val utbetalingId = utbetaling.inspektør.utbetalingId
        utbetalingIdTilVilkårsgrunnlagId = utbetalingId to vilkårsgrunnlagId
    }

    override fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje, gjeldendePeriode: Periode?) {
        this.utbetalingstidslinje = tidslinje
    }
}
