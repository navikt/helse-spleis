package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import no.nav.helse.dto.InfotrygdFerieperiodeDto
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.InfotrygdperiodeVisitor
import no.nav.helse.person.PersonObserver
import no.nav.helse.sykdomstidslinje.SykdomshistorikkHendelse.Hendelseskilde
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Økonomi

class Friperiode(fom: LocalDate, tom: LocalDate) : Infotrygdperiode(fom, tom) {
    override fun sykdomstidslinje(kilde: Hendelseskilde): Sykdomstidslinje {
        return Sykdomstidslinje.feriedager(periode.start, periode.endInclusive, kilde)
    }

    override fun somOverlappendeInfotrygdperiode(): PersonObserver.OverlappendeInfotrygdperiodeEtterInfotrygdendring.Infotrygdperiode {
        return PersonObserver.OverlappendeInfotrygdperiodeEtterInfotrygdendring.Infotrygdperiode(
            fom = this.periode.start,
            tom = this.periode.endInclusive,
            type = "FRIPERIODE",
            orgnummer = null
        )

    }

    override fun utbetalingstidslinje(): Utbetalingstidslinje {
        return Utbetalingstidslinje.Builder().apply {
            periode.forEach { dag -> addFridag(dag, Økonomi.ikkeBetalt()) }
        }.build()
    }

    override fun accept(visitor: InfotrygdperiodeVisitor) {
        visitor.visitInfotrygdhistorikkFerieperiode(this, periode.start, periode.endInclusive)
    }

    internal fun dto() = InfotrygdFerieperiodeDto(periode.dto())

    internal companion object {
        internal fun gjenopprett(dto: InfotrygdFerieperiodeDto): Friperiode {
            val periode = Periode.gjenopprett(dto.periode)
            return Friperiode(
                fom = periode.start,
                tom = periode.endInclusive
            )
        }
    }
}
