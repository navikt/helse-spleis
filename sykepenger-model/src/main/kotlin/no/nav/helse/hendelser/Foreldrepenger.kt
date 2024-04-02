package no.nav.helse.hendelser

import java.util.UUID
import no.nav.helse.hendelser.Ytelser.Companion.familieYtelserPeriode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.SykdomshistorikkHendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje

class Foreldrepenger(
    private val foreldrepengeytelse: List<Periode>
) {

    internal fun overlapper(aktivitetslogg: IAktivitetslogg, sykdomsperiode: Periode, erForlengelse: Boolean): Boolean {
        if (foreldrepengeytelse.isEmpty()) {
            aktivitetslogg.info("Bruker har ingen foreldrepenger")
            return false
        }
        val overlappsperiode = if (erForlengelse) sykdomsperiode else sykdomsperiode.familieYtelserPeriode
        return foreldrepengeytelse.any { ytelse -> ytelse.overlapperMed(overlappsperiode) }.also { overlapper ->
            if (!overlapper) aktivitetslogg.info("Bruker har foreldrepenger, men det slår ikke ut på overlappsjekken")
        }
    }

    internal fun sykdomshistorikkElement(
        meldingsreferanseId: UUID,
        hendelseskilde: SykdomshistorikkHendelse.Hendelseskilde
    ): Sykdomshistorikk.Element {
        val førsteDag = foreldrepengeytelse.minOf { it.start }
        val sisteDag = foreldrepengeytelse.maxOf { it.endInclusive }
        val sykdomstidslinje = Sykdomstidslinje.andreYtelsedager(førsteDag, sisteDag, hendelseskilde, Dag.AndreYtelser.AnnenYtelse.Foreldrepenger)
        return Sykdomshistorikk.Element.opprett(meldingsreferanseId, sykdomstidslinje)
    }

}
