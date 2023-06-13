package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverperiodeMediator
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.økonomi.Økonomi

internal class InfotrygdUtbetalingstidslinjedekoratør(
    private val other: ArbeidsgiverperiodeMediator,
    private val spleisPeriode: Periode,
    utbetaltIInfotrygd: List<Periode>
) : ArbeidsgiverperiodeMediator by(other) {

    private val ukjenteDager = utbetaltIInfotrygd.filter { it.overlapperMed(spleisPeriode) }.map { it.subset(spleisPeriode) }.flatten().toSet()
    private var forrigeIkkeUkjenteDag: LocalDate? = null
    override fun arbeidsgiverperiodedagNav(dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        leggTil(dato) { other.arbeidsgiverperiodedagNav(dato, økonomi, kilde) }
    }

    override fun fridag(dato: LocalDate) {
        leggTil(dato) { other.fridag(dato) }
    }

    override fun fridagOppholdsdag(dato: LocalDate) {
        leggTil(dato) { other.fridagOppholdsdag(dato) }
    }

    override fun arbeidsdag(dato: LocalDate) {
        leggTil(dato) { other.arbeidsdag(dato) }
    }

    override fun arbeidsgiverperiodedag(dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
       leggTil(dato) { other.arbeidsgiverperiodedag(dato, økonomi, kilde) }
    }

    override fun utbetalingsdag(dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        leggTil(dato) { other.utbetalingsdag(dato, økonomi, kilde) }
    }

    override fun foreldetDag(dato: LocalDate, økonomi: Økonomi) {
        leggTil(dato) { other.foreldetDag(dato, økonomi) }
    }

    override fun avvistDag(dato: LocalDate, begrunnelse: Begrunnelse, økonomi: Økonomi) {
        leggTil(dato) { other.avvistDag(dato, begrunnelse, økonomi) }
    }

    private fun leggTil(dato: LocalDate, block: () -> Unit) {
        if (dato !in spleisPeriode) return // Legger kun til dager innenfor Spleis sin sykdomstidslinje
        if (dato in ukjenteDager) return // Er en dag som er utbetalt i Infotrygd
        leggTilUkjenteDager(dato)
        block()
        forrigeIkkeUkjenteDag = dato
    }

    private fun leggTilUkjenteDager(dato: LocalDate) {
        if (forrigeIkkeUkjenteDag == null) return
        ukjenteDager
            .filter { ukjentDag -> ukjentDag > forrigeIkkeUkjenteDag }
            .filter { ukjentDag -> ukjentDag < dato }
            .forEach { ukjentDag -> other.ukjentDag(ukjentDag) }
    }
}
