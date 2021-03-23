package no.nav.helse.person.infotrygdhistorikk

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.InfotrygdhistorikkVisitor
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.Historie
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate
import java.util.*

internal abstract class Infotrygdperiode(private val periode: Periode) : ClosedRange<LocalDate> by(periode), Iterable<LocalDate> by(periode) {
    open fun sykdomstidslinje(kilde: SykdomstidslinjeHendelse.Hendelseskilde): Sykdomstidslinje = Sykdomstidslinje()
    open fun utbetalingstidslinje(): Utbetalingstidslinje = Utbetalingstidslinje()
    open fun append(bøtte: Historie.Historikkbøtte, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {}

    abstract fun accept(visitor: InfotrygdhistorikkVisitor)
    open fun valider(aktivitetslogg: IAktivitetslogg, periode: Periode) {}
    open fun validerOverlapp(aktivitetslogg: IAktivitetslogg, periode: Periode) {}

    fun overlapperMed(other: Periode) = periode.overlapperMed(other)
    open fun gjelder(orgnummer: String) = true
    override fun hashCode() = Objects.hash(this::class, periode)

    override fun equals(other: Any?): Boolean {
        if (other !is Infotrygdperiode) return false
        if (this::class != other::class) return false
        return this.periode == other.periode
    }
}
