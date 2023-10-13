package no.nav.helse.serde.api.speil.builders

import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Søknad
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.serde.api.dto.AvvistDag
import no.nav.helse.serde.api.dto.BegrunnelseDTO
import no.nav.helse.serde.api.dto.Sykdomstidslinjedag
import no.nav.helse.serde.api.dto.SykdomstidslinjedagKildetype
import no.nav.helse.serde.api.dto.SykdomstidslinjedagType
import no.nav.helse.serde.api.dto.Utbetalingstidslinjedag
import no.nav.helse.serde.api.dto.UtbetalingstidslinjedagType
import no.nav.helse.serde.api.dto.UtbetalingstidslinjedagUtenGrad
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.SykdomshistorikkHendelse.Hendelseskilde
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.UtbetalingVisitor
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Økonomi

internal class SykdomstidslinjeBuilder(tidslinje: Sykdomstidslinje): SykdomstidslinjeVisitor {
    private val tidslinje = mutableListOf<Sykdomstidslinjedag>()
    init {
        tidslinje.accept(this)
    }

    fun build() = tidslinje.toList()

    override fun visitDag(dag: Dag.UkjentDag, dato: LocalDate, kilde: Hendelseskilde) =
        leggTilDag(dag, dato, null, kilde)

    override fun visitDag(dag: Dag.Arbeidsdag, dato: LocalDate, kilde: Hendelseskilde) =
        leggTilDag(dag, dato, null, kilde)

    override fun visitDag(
        dag: Dag.Arbeidsgiverdag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) = leggTilDag(dag, dato, økonomi, kilde)

    override fun visitDag(dag: Dag.Feriedag, dato: LocalDate, kilde: Hendelseskilde) =
        leggTilDag(dag, dato, null, kilde)

    override fun visitDag(
        dag: Dag.ArbeidIkkeGjenopptattDag,
        dato: LocalDate,
        kilde: Hendelseskilde
    ) = leggTilDag(dag, dato, null, kilde)

    override fun visitDag(dag: Dag.FriskHelgedag, dato: LocalDate, kilde: Hendelseskilde) =
        leggTilDag(dag, dato, null, kilde)

    override fun visitDag(
        dag: Dag.ArbeidsgiverHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) = leggTilDag(dag, dato, økonomi, kilde)

    override fun visitDag(
        dag: Dag.Sykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) = leggTilDag(dag, dato, økonomi, kilde)

    override fun visitDag(
        dag: Dag.SykedagNav,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) = leggTilDag(dag, dato, økonomi, kilde)

    override fun visitDag(
        dag: Dag.ForeldetSykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) = leggTilDag(dag, dato, økonomi, kilde)

    override fun visitDag(
        dag: Dag.SykHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) = leggTilDag(dag, dato, økonomi, kilde)

    override fun visitDag(dag: Dag.Permisjonsdag, dato: LocalDate, kilde: Hendelseskilde) =
        leggTilDag(dag, dato, null, kilde)

    override fun visitDag(
        dag: Dag.AndreYtelser,
        dato: LocalDate,
        kilde: Hendelseskilde,
        ytelse: Dag.AndreYtelser.AnnenYtelse
    ) = leggTilDag(dag, dato, null, kilde, when (ytelse) {
        Dag.AndreYtelser.AnnenYtelse.Foreldrepenger -> SykdomstidslinjedagType.ANDRE_YTELSER_FORELDREPENGER
        Dag.AndreYtelser.AnnenYtelse.AAP -> SykdomstidslinjedagType.ANDRE_YTELSER_AAP
        Dag.AndreYtelser.AnnenYtelse.Omsorgspenger -> SykdomstidslinjedagType.ANDRE_YTELSER_OMSORGSPENGER
        Dag.AndreYtelser.AnnenYtelse.Pleiepenger -> SykdomstidslinjedagType.ANDRE_YTELSER_PLEIEPENGER
        Dag.AndreYtelser.AnnenYtelse.Svangerskapspenger -> SykdomstidslinjedagType.ANDRE_YTELSER_SVANGERSKAPSPENGER
        Dag.AndreYtelser.AnnenYtelse.Opplæringspenger -> SykdomstidslinjedagType.ANDRE_YTELSER_OPPLÆRINGSPENGER
        Dag.AndreYtelser.AnnenYtelse.Dagpenger -> SykdomstidslinjedagType.ANDRE_YTELSER_DAGPENGER
    })

    override fun visitDag(
        dag: Dag.ProblemDag,
        dato: LocalDate,
        kilde: Hendelseskilde,
        other: Hendelseskilde?,
        melding: String
    ) =
        leggTilDag(dag, dato, null, kilde)

    private fun leggTilDag(dag: Dag, dato: LocalDate, økonomi: Økonomi?, kilde: Hendelseskilde, dagtype: SykdomstidslinjedagType = dag.toDagtypeDTO()) {
        val dagDto = Sykdomstidslinjedag(
            dato,
            dagtype,
            Sykdomstidslinjedag.SykdomstidslinjedagKilde(kilde.toKildetypeDTO(), kilde.meldingsreferanseId()),
            økonomi?.brukAvrundetGrad { grad -> grad }
        )

        tidslinje.add(dagDto)
    }

    private fun Hendelseskilde.toKildetypeDTO() = when {
        erAvType("Inntektsmelding") -> SykdomstidslinjedagKildetype.Inntektsmelding
        erAvType(Søknad::class) -> SykdomstidslinjedagKildetype.Søknad
        erAvType(OverstyrTidslinje::class) -> SykdomstidslinjedagKildetype.Saksbehandler
        else -> SykdomstidslinjedagKildetype.Ukjent
    }

    private fun Dag.toDagtypeDTO() = when (this) {
        is Dag.Sykedag -> SykdomstidslinjedagType.SYKEDAG
        is Dag.UkjentDag -> SykdomstidslinjedagType.ARBEIDSDAG
        is Dag.Arbeidsdag -> SykdomstidslinjedagType.ARBEIDSDAG
        is Dag.Arbeidsgiverdag -> SykdomstidslinjedagType.ARBEIDSGIVERDAG
        is Dag.Feriedag -> SykdomstidslinjedagType.FERIEDAG
        is Dag.ArbeidIkkeGjenopptattDag -> SykdomstidslinjedagType.ARBEID_IKKE_GJENOPPTATT_DAG
        is Dag.FriskHelgedag -> SykdomstidslinjedagType.FRISK_HELGEDAG
        is Dag.ArbeidsgiverHelgedag -> SykdomstidslinjedagType.ARBEIDSGIVERDAG
        is Dag.ForeldetSykedag -> SykdomstidslinjedagType.FORELDET_SYKEDAG
        is Dag.SykHelgedag -> SykdomstidslinjedagType.SYK_HELGEDAG
        is Dag.Permisjonsdag -> SykdomstidslinjedagType.PERMISJONSDAG
        is Dag.ProblemDag -> SykdomstidslinjedagType.UBESTEMTDAG
        is Dag.SykedagNav -> SykdomstidslinjedagType.SYKEDAG
        is Dag.AndreYtelser -> error("denne må settes manuelt")
    }
}

// Besøker hele utbetaling-treet
internal class UtbetalingstidslinjeBuilder(utbetalingstidslinje: Utbetalingstidslinje): UtbetalingVisitor {
    private val utbetalingstidslinje: MutableList<Utbetalingstidslinjedag> = mutableListOf()

    init {
        utbetalingstidslinje.accept(this)
    }

    internal fun build() = utbetalingstidslinje.toList()

    override fun visit(
        dag: Utbetalingsdag.Arbeidsdag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        utbetalingstidslinje.add(UtbetalingstidslinjedagUtenGrad(type = UtbetalingstidslinjedagType.Arbeidsdag, dato = dato))
    }

    override fun visit(
        dag: Utbetalingsdag.ArbeidsgiverperiodeDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        utbetalingstidslinje.add(UtbetalingstidslinjedagUtenGrad(type = UtbetalingstidslinjedagType.ArbeidsgiverperiodeDag, dato = dato))
    }

    private fun leggTilUtbetalingsdag(dato: LocalDate, økonomi: Økonomi, type: UtbetalingstidslinjedagType) {
        utbetalingstidslinje.add(UtbetalingsdagDTOBuilder(økonomi, type, dato).build())
    }

    override fun visit(dag: Utbetalingsdag.ArbeidsgiverperiodedagNav, dato: LocalDate, økonomi: Økonomi) {
        leggTilUtbetalingsdag(dato, økonomi, UtbetalingstidslinjedagType.ArbeidsgiverperiodeDag)
    }

    override fun visit(dag: Utbetalingsdag.NavDag, dato: LocalDate, økonomi: Økonomi) {
        leggTilUtbetalingsdag(dato, økonomi, UtbetalingstidslinjedagType.NavDag)
    }

    override fun visit(
        dag: Utbetalingsdag.NavHelgDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        utbetalingstidslinje.add(UtbetalingstidslinjedagUtenGrad(type = UtbetalingstidslinjedagType.NavHelgDag, dato = dato))
    }

    override fun visit(
        dag: Utbetalingsdag.Fridag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        utbetalingstidslinje.add(
            UtbetalingstidslinjedagUtenGrad(
                type = if (dato.erHelg()) UtbetalingstidslinjedagType.Helgedag else UtbetalingstidslinjedagType.Feriedag,
                dato = dato
            )
        )
    }

    override fun visit(
        dag: Utbetalingsdag.UkjentDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        utbetalingstidslinje.add(
            UtbetalingstidslinjedagUtenGrad(
                type = UtbetalingstidslinjedagType.UkjentDag,
                dato = dato
            )
        )
    }

    override fun visit(
        dag: Utbetalingsdag.AvvistDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        økonomi.brukTotalGrad { totalGrad->
            utbetalingstidslinje.add(
                AvvistDag(
                    type = UtbetalingstidslinjedagType.AvvistDag,
                    dato = dato,
                    begrunnelser = dag.begrunnelser.map { BegrunnelseDTO.fraBegrunnelse(it) },
                    totalGrad = totalGrad
                )
            )
        }
    }

    override fun visit(
        dag: Utbetalingsdag.ForeldetDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        utbetalingstidslinje.add(UtbetalingstidslinjedagUtenGrad(type = UtbetalingstidslinjedagType.ForeldetDag, dato = dato))
    }
}
