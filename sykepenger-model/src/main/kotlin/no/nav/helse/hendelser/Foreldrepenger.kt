package no.nav.helse.hendelser

import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.Ytelser.Companion.familieYtelserPeriode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class Foreldrepenger(
    private val foreldrepengeytelse: List<GradertPeriode>
) : AnnenYtelseSomKanOppdatereHistorikk() {
    private val perioder get() = foreldrepengeytelse.map { it.periode }

    internal fun valider(
        aktivitetslogg: IAktivitetslogg,
        sykdomsperiode: Periode,
        erForlengelse: Boolean
    ) {
        if (foreldrepengeytelse.isEmpty()) return aktivitetslogg.info("Bruker har ingen foreldrepenger")
        varselHvisOverlapperMedForeldrepenger(aktivitetslogg, erForlengelse, sykdomsperiode)
        varselHvisForlengerForeldrepengerMerEnn14Dager(aktivitetslogg, sykdomsperiode)
    }

    private fun varselHvisOverlapperMedForeldrepenger(
        aktivitetslogg: IAktivitetslogg,
        erForlengelse: Boolean,
        sykdomsperiode: Periode
    ) {
        val overlappsperiode = if (erForlengelse) sykdomsperiode else sykdomsperiode.familieYtelserPeriode
        val overlapperMedForeldrepenger = perioder.any { ytelse -> ytelse.overlapperMed(overlappsperiode) }
        if (overlapperMedForeldrepenger) {
            aktivitetslogg.varsel(Varselkode.`Overlapper med foreldrepenger`)
        } else {
            aktivitetslogg.info("Bruker har foreldrepenger, men det slår ikke ut på overlappsjekken")
        }
    }

    private fun varselHvisForlengerForeldrepengerMerEnn14Dager(
        aktivitetslogg: IAktivitetslogg,
        sykdomsperiode: Periode
    ) {
        val foreldrepengeperiodeFør = foreldrepengeytelse.lastOrNull { it.periode.endInclusive < sykdomsperiode.start } ?: return
        val harForeldrepengerAlleDager = foreldrepengeperiodeFør.periode.erRettFør(sykdomsperiode) && foreldrepengeperiodeFør.periode.count() > 14 && foreldrepengeperiodeFør.grad == 100
        if (!harForeldrepengerAlleDager) return
        aktivitetslogg.varsel(Varselkode.`Forlenger foreldrepenger med mer enn 14 dager`)
    }

    override fun sykdomstidslinje(
        meldingsreferanseId: UUID,
        registrert: LocalDateTime
    ): Sykdomstidslinje {
        if (foreldrepengeytelse.isEmpty()) return Sykdomstidslinje()
        require(foreldrepengeytelse.map { it.periode }.grupperSammenhengendePerioder().size == 1) { "Ikke trygt å kalle sykdomstidslinjen til ${this.javaClass.simpleName} når det er huller i ytelser" }
        val hendelseskilde = SykdomshistorikkHendelse.Hendelseskilde(Ytelser::class, meldingsreferanseId, registrert)
        val førsteDag = foreldrepengeytelse.map { it.periode }.minOf { it.start }
        val sisteDag = foreldrepengeytelse.map { it.periode }.maxOf { it.endInclusive }
        val sykdomstidslinje = Sykdomstidslinje.andreYtelsedager(førsteDag, sisteDag, hendelseskilde, Dag.AndreYtelser.AnnenYtelse.Foreldrepenger)
        return sykdomstidslinje
    }

    override fun skalOppdatereHistorikk(
        vedtaksperiode: Periode,
        skjæringstidspunkt: LocalDate,
        vedtaksperiodeRettEtter: Periode?
    ): Pair<Boolean, Companion.HvorforIkkeOppdatereHistorikk?> = foreldrepengeytelse.skalOppdatereHistorikkIHalen(vedtaksperiode, skjæringstidspunkt, vedtaksperiodeRettEtter)

    internal fun perioder(): List<Periode> = foreldrepengeytelse.map { it.periode }
}
