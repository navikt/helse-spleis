package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import no.nav.helse.memento.InfotrygdFerieperiodeMemento
import no.nav.helse.person.InfotrygdperiodeVisitor
import no.nav.helse.sykdomstidslinje.SykdomshistorikkHendelse.Hendelseskilde
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Økonomi

class Friperiode(fom: LocalDate, tom: LocalDate) : Infotrygdperiode(fom, tom) {
    override fun sykdomstidslinje(kilde: Hendelseskilde): Sykdomstidslinje {
        return Sykdomstidslinje.feriedager(periode.start, periode.endInclusive, kilde)
    }

    override fun utbetalingstidslinje(): Utbetalingstidslinje {
        return Utbetalingstidslinje.Builder().apply {
            periode.forEach { dag -> addFridag(dag, Økonomi.ikkeBetalt()) }
        }.build()
    }

    override fun accept(visitor: InfotrygdperiodeVisitor) {
        visitor.visitInfotrygdhistorikkFerieperiode(this, periode.start, periode.endInclusive)
    }

    internal fun memento() = InfotrygdFerieperiodeMemento(periode.memento())
}
