package no.nav.helse.hendelser

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Ytelser.Companion.familieYtelserPeriode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.SykdomshistorikkHendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje

class Foreldrepenger(
    private val foreldrepengeytelse: List<ForeldrepengerPeriode>
) {
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

    internal fun tilOgMed(dato: LocalDate): Foreldrepenger {
        return Foreldrepenger(foreldrepengeytelse.mapNotNull { foreldrepenger ->
            if (foreldrepenger.periode.starterEtter(dato.somPeriode())) null else ForeldrepengerPeriode(foreldrepenger.periode.start til minOf(foreldrepenger.periode.endInclusive, dato), foreldrepenger.grad) })
    }

    internal fun sykdomshistorikkElement(
        meldingsreferanseId: UUID,
        hendelseskilde: SykdomshistorikkHendelse.Hendelseskilde
    ): Sykdomshistorikk.Element {
        val førsteDag = foreldrepengeytelse.map { it.periode }.minOf { it.start }
        val sisteDag = foreldrepengeytelse.map { it.periode }.maxOf { it.endInclusive }
        val sykdomstidslinje = Sykdomstidslinje.andreYtelsedager(førsteDag, sisteDag, hendelseskilde, Dag.AndreYtelser.AnnenYtelse.Foreldrepenger)
        return Sykdomshistorikk.Element.opprett(meldingsreferanseId, sykdomstidslinje)
    }

    internal fun skalOppdatereHistorikk(periode: Periode, periodeRettEtter: Periode? = null): Boolean {
        if (foreldrepengeytelse.isEmpty()) return false
        if (foreldrepengeytelse.size > 1) return false
        if (periodeRettEtter != null) return false
        val foreldrepengeperiode = foreldrepengeytelse.first().periode
        val fullstendigOverlapp = foreldrepengeperiode == periode
        val foreldrepengerIHalen = periode.overlapperMed(foreldrepengeperiode) && foreldrepengeperiode.slutterEtter(periode.endInclusive)
        return fullstendigOverlapp || foreldrepengerIHalen
    }
}
class ForeldrepengerPeriode(internal val periode: Periode, internal val grad: Int)
