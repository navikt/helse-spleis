package no.nav.helse.person.infotrygdhistorikk

import no.nav.helse.utbetalingstidslinje.ArbeidsgiverperiodeMediator
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse

internal class InfotrygdUtbetalingstidslinjedekoratør(
    private val other: ArbeidsgiverperiodeMediator,
    private val førsteDag: LocalDate
) : ArbeidsgiverperiodeMediator by(other) {
    override fun fridag(dato: LocalDate) {
        if (dato < førsteDag) return
        other.fridag(dato)
    }

    override fun arbeidsdag(dato: LocalDate) {
        if (dato < førsteDag) return
        other.arbeidsdag(dato)
    }

    override fun arbeidsgiverperiodedag(
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) {
        if (dato < førsteDag) return
        other.arbeidsgiverperiodedag(dato, økonomi, kilde)
    }

    override fun utbetalingsdag(dato: LocalDate, økonomi: Økonomi, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        if (dato < førsteDag) return
        other.utbetalingsdag(dato, økonomi, kilde)
    }

    override fun foreldetDag(dato: LocalDate, økonomi: Økonomi) {
        if (dato < førsteDag) return
        other.foreldetDag(dato, økonomi)
    }

    override fun avvistDag(dato: LocalDate, begrunnelse: Begrunnelse) {
        if (dato < førsteDag) return
        other.avvistDag(dato, begrunnelse)
    }
}
