package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import java.util.Objects
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.InfotrygdhistorikkVisitor
import no.nav.helse.sykdomstidslinje.Dag.Companion.replace
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

abstract class Infotrygdperiode(fom: LocalDate, tom: LocalDate) {
    protected val periode = fom til tom

    internal open fun sykdomstidslinje(kilde: SykdomstidslinjeHendelse.Hendelseskilde): Sykdomstidslinje = Sykdomstidslinje()
    internal open fun utbetalingstidslinje(): Utbetalingstidslinje = Utbetalingstidslinje()

    internal abstract fun accept(visitor: InfotrygdhistorikkVisitor)

    internal fun historikkFor(orgnummer: String, sykdomstidslinje: Sykdomstidslinje, kilde: SykdomstidslinjeHendelse.Hendelseskilde): Sykdomstidslinje {
        if (!gjelder(orgnummer)) return sykdomstidslinje
        return sykdomstidslinje.merge(sykdomstidslinje(kilde), replace)
    }

    internal open fun gjelder(orgnummer: String) = true

    override fun hashCode() = Objects.hash(this::class, periode)
    override fun equals(other: Any?): Boolean {
        if (other !is Infotrygdperiode) return false
        if (this::class != other::class) return false
        return this.periode == other.periode
    }

    internal companion object {
        internal fun sorter(perioder: List<Infotrygdperiode>) =
            perioder.sortedWith(compareBy( { it.periode.start }, { it.hashCode() }))

        internal fun List<Infotrygdperiode>.utbetalingsperioder(organisasjonsnummer: String? = null) =  this
            .filterIsInstance<Utbetalingsperiode>()
            .filter { organisasjonsnummer == null || it.gjelder(organisasjonsnummer) }
            .map { it.periode }

        internal fun List<Infotrygdperiode>.harBetaltRettFør(other: Periode) = this
            .any {
                it.periode.erRettFør(other)
            }
    }
}
