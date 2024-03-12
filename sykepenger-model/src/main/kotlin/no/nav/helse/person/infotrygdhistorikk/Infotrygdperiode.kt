package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.InfotrygdperiodeVisitor
import no.nav.helse.person.PersonObserver
import no.nav.helse.sykdomstidslinje.Dag.Companion.sammenhengendeSykdom
import no.nav.helse.sykdomstidslinje.SykdomshistorikkHendelse.Hendelseskilde
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

abstract class Infotrygdperiode(fom: LocalDate, tom: LocalDate) {
    protected val periode = fom til tom

    internal open fun sykdomstidslinje(kilde: Hendelseskilde): Sykdomstidslinje = Sykdomstidslinje()
    internal open fun utbetalingstidslinje(): Utbetalingstidslinje = Utbetalingstidslinje()

    internal abstract fun accept(visitor: InfotrygdperiodeVisitor)

    internal fun historikkFor(orgnummer: String, sykdomstidslinje: Sykdomstidslinje, kilde: Hendelseskilde): Sykdomstidslinje {
        if (!gjelder(orgnummer)) return sykdomstidslinje
        return sykdomstidslinje.merge(sykdomstidslinje(kilde), sammenhengendeSykdom)
    }

    internal fun overlapperMed(other: Periode) = periode.overlapperMed(other)

    internal open fun gjelder(orgnummer: String) = true

    internal open fun funksjoneltLik(other: Infotrygdperiode): Boolean {
        if (this::class != other::class) return false
        return this.periode == other.periode
    }

    abstract fun somOverlappendeInfotrygdperiode(): PersonObserver.OverlappendeInfotrygdperiodeEtterInfotrygdendring.Infotrygdperiode

    internal companion object {
        internal fun sorter(perioder: List<Infotrygdperiode>) =
            perioder.sortedWith(compareBy( { it.periode.start }, { it.periode.endInclusive }, { it::class.simpleName }))

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
