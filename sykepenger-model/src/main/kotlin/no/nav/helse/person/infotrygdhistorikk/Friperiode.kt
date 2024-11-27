package no.nav.helse.person.infotrygdhistorikk

import no.nav.helse.dto.InfotrygdFerieperiodeDto
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.SykdomshistorikkHendelse.Hendelseskilde
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

class Friperiode(
    fom: LocalDate,
    tom: LocalDate
) : Infotrygdperiode(fom, tom) {
    override fun sykdomstidslinje(kilde: Hendelseskilde): Sykdomstidslinje = Sykdomstidslinje.feriedager(periode.start, periode.endInclusive, kilde)

    override fun utbetalingstidslinje(): Utbetalingstidslinje =
        Utbetalingstidslinje
            .Builder()
            .apply {
                periode.forEach { dag -> addFridag(dag, Økonomi.ikkeBetalt()) }
            }.build()

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
