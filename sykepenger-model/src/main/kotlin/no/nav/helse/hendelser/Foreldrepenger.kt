package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.AnnenYtelseSomKanOppdatereHistorikk.Companion.HvorforIkkeOppdatereHistorikk.*
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.Ytelser.Companion.familieYtelserPeriode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.SykdomshistorikkHendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje

class Foreldrepenger(
    private val foreldrepengeytelse: List<ForeldrepengerPeriode>
): AnnenYtelseSomKanOppdatereHistorikk() {
    private val perioder get() = foreldrepengeytelse.map { it.periode }

    internal fun overlapper(aktivitetslogg: IAktivitetslogg, sykdomsperiode: Periode, erForlengelse: Boolean): Boolean {
        if (foreldrepengeytelse.isEmpty()) {
            aktivitetslogg.info("Bruker har ingen foreldrepenger")
            return false
        }
        val overlappsperiode = if (erForlengelse) sykdomsperiode else sykdomsperiode.familieYtelserPeriode
        return perioder.any { ytelse -> ytelse.overlapperMed(overlappsperiode) }.also { overlapper ->
            if (!overlapper) aktivitetslogg.info("Bruker har foreldrepenger, men det slår ikke ut på overlappsjekken")
        }
    }

    internal fun avgrensTil(periode: Periode): Foreldrepenger {
        return Foreldrepenger(foreldrepengeytelse.mapNotNull { foreldrepenger ->
            if (foreldrepenger.periode.starterEtter(periode)) null
            else if (periode.starterEtter(foreldrepenger.periode)) null
            else ForeldrepengerPeriode(foreldrepenger.periode.subset(periode), foreldrepenger.grad) })
    }

    override fun sykdomstidslinje(meldingsreferanseId: UUID, registrert: LocalDateTime): Sykdomstidslinje {
        val hendelseskilde = SykdomshistorikkHendelse.Hendelseskilde(Ytelser::class, meldingsreferanseId, registrert)
        val førsteDag = foreldrepengeytelse.map { it.periode }.minOf { it.start }
        val sisteDag = foreldrepengeytelse.map { it.periode }.maxOf { it.endInclusive }
        val sykdomstidslinje = Sykdomstidslinje.andreYtelsedager(førsteDag, sisteDag, hendelseskilde, Dag.AndreYtelser.AnnenYtelse.Foreldrepenger)
        return sykdomstidslinje
    }

    override fun skalOppdatereHistorikk(vedtaksperiode: Periode, vedtaksperiodeRettEtter: Periode?): Pair<Boolean, Companion.HvorforIkkeOppdatereHistorikk?> {
        if (foreldrepengeytelse.isEmpty()) return false to INGEN_YTELSE
        if (vedtaksperiodeRettEtter != null) return false to HAR_VEDTAKSPERIODE_RETT_ETTER
        val sammenhengendePerioder = foreldrepengeytelse.map { it.periode }.grupperSammenhengendePerioder()
        if (sammenhengendePerioder.size > 1) return false to FLERE_IKKE_SAMMENHENGENDE_INNSLAG
        val foreldrepengeperiode = sammenhengendePerioder.single()
        val fullstendigOverlapp = foreldrepengeperiode == vedtaksperiode
        val foreldrepengerIHalen = vedtaksperiode.overlapperMed(foreldrepengeperiode) && foreldrepengeperiode.slutterEtter(vedtaksperiode.endInclusive)
        if (!fullstendigOverlapp && !foreldrepengerIHalen) return false to IKKE_I_HALEN_AV_VEDTAKSPERIODE
        if (foreldrepengeytelse.any { it.grad != 100 }) return false to GRADERT_YTELSE
        return true to null
    }
}
class ForeldrepengerPeriode(internal val periode: Periode, internal val grad: Int)
