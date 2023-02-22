package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import no.nav.helse.person.InfotrygdhistorikkVisitor
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Økonomi

class Friperiode(fom: LocalDate, tom: LocalDate) : Infotrygdperiode(fom, tom) {
    override fun sykdomstidslinje(kilde: SykdomstidslinjeHendelse.Hendelseskilde): Sykdomstidslinje {
        return Sykdomstidslinje.feriedager(periode.start, periode.endInclusive, kilde)
    }

    override fun utbetalingstidslinje(): Utbetalingstidslinje {
        return Utbetalingstidslinje.Builder().apply {
            periode.forEach { dag -> addFridag(dag, Økonomi.ikkeBetalt()) }
        }.build()
    }

    override fun accept(visitor: InfotrygdhistorikkVisitor) {
        visitor.visitInfotrygdhistorikkFerieperiode(this, periode.start, periode.endInclusive)
    }
}
