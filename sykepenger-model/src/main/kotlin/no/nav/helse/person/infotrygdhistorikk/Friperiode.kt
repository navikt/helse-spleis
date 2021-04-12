package no.nav.helse.person.infotrygdhistorikk

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.InfotrygdhistorikkVisitor
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Økonomi

class Friperiode(periode: Periode) : Infotrygdperiode(periode) {
    override fun sykdomstidslinje(kilde: SykdomstidslinjeHendelse.Hendelseskilde): Sykdomstidslinje {
        return Sykdomstidslinje.feriedager(start, endInclusive, kilde)
    }

    override fun utbetalingstidslinje(): Utbetalingstidslinje {
        return Utbetalingstidslinje().also {
            forEach { dag -> it.addFridag(dag, Økonomi.ikkeBetalt().inntekt(Inntekt.INGEN, skjæringstidspunkt = dag)) }
        }
    }

    override fun accept(visitor: InfotrygdhistorikkVisitor) {
        visitor.visitInfotrygdhistorikkFerieperiode(this)
    }
}
