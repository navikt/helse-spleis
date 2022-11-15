package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverperiodeMediator
import no.nav.helse.økonomi.Økonomi

internal class InfotrygdUtbetalingstidslinjedekoratør(
    private val other: ArbeidsgiverperiodeMediator,
    private val spleisPeriode: Periode,
    private val utbetaltIInfotrygd: List<Periode>
) : ArbeidsgiverperiodeMediator by(other) {

    override fun utbetalingsdag(dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        if (utbetaltIInfotrygd.any { dato in it }) return other.ukjentDag(dato)
        other.utbetalingsdag(dato, økonomi, kilde)
    }
}
