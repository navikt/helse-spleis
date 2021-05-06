package no.nav.helse.person.infotrygdhistorikk

import no.nav.helse.person.InfotrygdhistorikkVisitor
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

class Friperiode(fom: LocalDate, tom: LocalDate) : Infotrygdperiode(fom, tom) {
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
