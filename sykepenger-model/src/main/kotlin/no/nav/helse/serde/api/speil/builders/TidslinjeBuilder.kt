package no.nav.helse.serde.api.speil.builders

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.dto.serialisering.UtbetalingsdagUtDto
import no.nav.helse.dto.serialisering.UtbetalingstidslinjeUtDto
import no.nav.helse.dto.serialisering.ØkonomiUtDto
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
import no.nav.helse.serde.api.dto.UtbetalingsdagDTO
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
import kotlin.math.roundToInt

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
        is Dag.SykedagNav -> SykdomstidslinjedagType.SYKEDAG_NAV
        is Dag.AndreYtelser -> error("denne må settes manuelt")
    }
}

internal class UtbetalingstidslinjeBuilder(private val dto: UtbetalingstidslinjeUtDto) {
    private val utbetalingstidslinje by lazy {
        dto.dager.map {
            when (it) {
                is UtbetalingsdagUtDto.ArbeidsdagDto -> UtbetalingstidslinjedagUtenGrad(type = UtbetalingstidslinjedagType.Arbeidsdag, dato = it.dato)
                is UtbetalingsdagUtDto.ArbeidsgiverperiodeDagDto -> UtbetalingstidslinjedagUtenGrad(type = UtbetalingstidslinjedagType.ArbeidsgiverperiodeDag, dato = it.dato)
                is UtbetalingsdagUtDto.ArbeidsgiverperiodeDagNavDto -> mapUtbetalingsdag(it.dato, UtbetalingstidslinjedagType.ArbeidsgiverperiodeDag, it.økonomi)
                is UtbetalingsdagUtDto.AvvistDagDto -> AvvistDag(
                    type = UtbetalingstidslinjedagType.AvvistDag,
                    dato = it.dato,
                    begrunnelser = it.begrunnelser.map { BegrunnelseDTO.fraBegrunnelse(it) },
                    totalGrad = it.økonomi.totalGrad.prosent.toInt()
                )

                is UtbetalingsdagUtDto.ForeldetDagDto -> UtbetalingstidslinjedagUtenGrad(type = UtbetalingstidslinjedagType.ForeldetDag, dato = it.dato)
                is UtbetalingsdagUtDto.FridagDto -> UtbetalingstidslinjedagUtenGrad(
                    type = if (it.dato.erHelg()) UtbetalingstidslinjedagType.Helgedag else UtbetalingstidslinjedagType.Feriedag,
                    dato = it.dato
                )
                is UtbetalingsdagUtDto.NavDagDto -> mapUtbetalingsdag(it.dato, UtbetalingstidslinjedagType.NavDag, it.økonomi)
                is UtbetalingsdagUtDto.NavHelgDagDto -> UtbetalingstidslinjedagUtenGrad(type = UtbetalingstidslinjedagType.NavHelgDag, dato = it.dato)
                is UtbetalingsdagUtDto.UkjentDagDto -> UtbetalingstidslinjedagUtenGrad(
                    type = UtbetalingstidslinjedagType.UkjentDag,
                    dato = it.dato
                )

            }
        }
    }

    private fun mapUtbetalingsdag(dato: LocalDate, type: UtbetalingstidslinjedagType, økonomi: ØkonomiUtDto): UtbetalingsdagDTO {
        return UtbetalingsdagDTO(
            type = type,
            dato = dato,
            arbeidsgiverbeløp = økonomi.arbeidsgiverbeløp!!.dagligInt.beløp,
            personbeløp = økonomi.personbeløp!!.dagligInt.beløp,
            totalGrad = økonomi.totalGrad.prosent.roundToInt()
        )
    }

    internal fun build() = utbetalingstidslinje.toList()
}
