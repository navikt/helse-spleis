package no.nav.helse.person.infotrygdhistorikk

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.InfotrygdhistorikkVisitor
import no.nav.helse.sykdomstidslinje.Dag.Companion.replace
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate
import java.util.*

abstract class Infotrygdperiode(private val periode: Periode) : ClosedRange<LocalDate> by(periode), Iterable<LocalDate> by(periode) {
    internal open fun sykdomstidslinje(kilde: SykdomstidslinjeHendelse.Hendelseskilde): Sykdomstidslinje = Sykdomstidslinje()
    internal open fun utbetalingstidslinje(): Utbetalingstidslinje = Utbetalingstidslinje()

    internal abstract fun accept(visitor: InfotrygdhistorikkVisitor)
    internal open fun valider(aktivitetslogg: IAktivitetslogg, periode: Periode) {}
    internal open fun validerOverlapp(aktivitetslogg: IAktivitetslogg, periode: Periode) {}

    internal fun historikkFor(orgnummer: String, sykdomstidslinje: Sykdomstidslinje, kilde: SykdomstidslinjeHendelse.Hendelseskilde): Sykdomstidslinje {
        if (!gjelder(orgnummer)) return sykdomstidslinje
        return sykdomstidslinje(kilde).merge(sykdomstidslinje, replace)
    }

    internal fun overlapperMed(other: Periode) = periode.overlapperMed(other)
    internal open fun gjelder(orgnummer: String) = true
    internal open fun utbetalingEtter(orgnumre: List<String>, dato: LocalDate) = false

    override fun hashCode() = Objects.hash(this::class, periode)
    override fun equals(other: Any?): Boolean {
        if (other !is Infotrygdperiode) return false
        if (this::class != other::class) return false
        return this.periode == other.periode
    }
}
