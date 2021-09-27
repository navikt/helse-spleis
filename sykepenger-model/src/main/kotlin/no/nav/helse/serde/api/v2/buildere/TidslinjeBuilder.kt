package no.nav.helse.serde.api.v2.buildere

import no.nav.helse.hendelser.*
import no.nav.helse.person.SykdomshistorikkVisitor
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.person.UtbetalingVisitor
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VedtaksperiodeVisitor
import no.nav.helse.serde.PersonData
import no.nav.helse.serde.api.*
import no.nav.helse.serde.mapping.SpeilDagtype
import no.nav.helse.serde.mapping.SpeilKildetype
import no.nav.helse.sykdomstidslinje.*
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import java.util.*
import kotlin.math.roundToInt

// Besøker hele sykdomshistorikk-treet
internal class VedtaksperiodeSykdomstidslinjeBuilder(vedtaksperiode: Vedtaksperiode): VedtaksperiodeVisitor {
    private val sykdomstidslinje: MutableList<SykdomstidslinjedagDTO> = mutableListOf()
    init {
        vedtaksperiode.accept(this)
    }

    internal fun build() = sykdomstidslinje.toList()

    override fun postVisitSykdomstidslinje(tidslinje: Sykdomstidslinje, låstePerioder: MutableList<Periode>) {
        this.sykdomstidslinje.addAll(SykdomstidslinjeBuilder(tidslinje).build())
    }
}

internal class SykdomshistorikkBuilder(private val id: UUID, element: Sykdomshistorikk.Element) : SykdomshistorikkVisitor {
    private val beregnetTidslinje: MutableList<SykdomstidslinjedagDTO> = mutableListOf()

    init {
        element.accept(this)
    }

    fun build(): Pair<UUID, List<SykdomstidslinjedagDTO>> = id to beregnetTidslinje.toList()

    override fun preVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) {
        this.beregnetTidslinje.addAll(SykdomstidslinjeBuilder(tidslinje).build())
    }

}

internal class SykdomstidslinjeBuilder(tidslinje: Sykdomstidslinje): SykdomstidslinjeVisitor {
    private val tidslinje = mutableListOf<SykdomstidslinjedagDTO>()
    init {
        tidslinje.accept(this)
    }

    fun build() = tidslinje.toList()

    override fun visitDag(dag: Dag.UkjentDag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) =
        leggTilDag(dag, dato, null, kilde)

    override fun visitDag(dag: Dag.Arbeidsdag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) =
        leggTilDag(dag, dato, null, kilde)

    override fun visitDag(
        dag: Dag.Arbeidsgiverdag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) = leggTilDag(dag, dato, økonomi, kilde)

    override fun visitDag(dag: Dag.Feriedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) =
        leggTilDag(dag, dato, null, kilde)

    override fun visitDag(dag: Dag.FriskHelgedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) =
        leggTilDag(dag, dato, null, kilde)

    override fun visitDag(
        dag: Dag.ArbeidsgiverHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) = leggTilDag(dag, dato, økonomi, kilde)

    override fun visitDag(
        dag: Dag.Sykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) = leggTilDag(dag, dato, økonomi, kilde)

    override fun visitDag(
        dag: Dag.ForeldetSykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) = leggTilDag(dag, dato, økonomi, kilde)

    override fun visitDag(
        dag: Dag.SykHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) = leggTilDag(dag, dato, økonomi, kilde)

    override fun visitDag(dag: Dag.Permisjonsdag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) =
        leggTilDag(dag, dato, null, kilde)

    override fun visitDag(dag: Dag.ProblemDag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde, melding: String) =
        leggTilDag(dag, dato, null, kilde)

    private fun leggTilDag(dag: Dag, dato: LocalDate, økonomi: Økonomi?, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        val dagDto = SykdomstidslinjedagDTO(
            dato,
            dag.toDagtypeDTO(),
            SykdomstidslinjedagDTO.KildeDTO(kilde.toKildetypeDTO(), kilde.meldingsreferanseId()),
            økonomi?.medData { grad, _, _, _, _, _, _, _, _ -> grad }
        )

        tidslinje.add(dagDto)
    }

    private fun SykdomstidslinjeHendelse.Hendelseskilde.toKildetypeDTO() = when {
        erAvType(Inntektsmelding::class) -> SpeilKildetype.Inntektsmelding
        erAvType(Søknad::class) -> SpeilKildetype.Søknad
        erAvType(Sykmelding::class) -> SpeilKildetype.Sykmelding
        erAvType(OverstyrTidslinje::class) -> SpeilKildetype.Saksbehandler
        else -> SpeilKildetype.Ukjent
    }

    private fun Dag.toDagtypeDTO() = when (this) {
        is Dag.Sykedag -> SpeilDagtype.SYKEDAG
        is Dag.UkjentDag -> SpeilDagtype.ARBEIDSDAG
        is Dag.Arbeidsdag -> SpeilDagtype.ARBEIDSDAG
        is Dag.Arbeidsgiverdag -> SpeilDagtype.ARBEIDSGIVERDAG
        is Dag.Feriedag -> SpeilDagtype.FERIEDAG
        is Dag.FriskHelgedag -> SpeilDagtype.FRISK_HELGEDAG
        is Dag.ArbeidsgiverHelgedag -> SpeilDagtype.ARBEIDSGIVERDAG
        is Dag.ForeldetSykedag -> SpeilDagtype.FORELDET_SYKEDAG
        is Dag.SykHelgedag -> SpeilDagtype.SYK_HELGEDAG
        is Dag.Permisjonsdag -> SpeilDagtype.PERMISJONSDAG
        is Dag.ProblemDag -> SpeilDagtype.UBESTEMTDAG
        is Dag.AvslåttDag -> SpeilDagtype.AVSLÅTT
    }
}

// Besøker hele utbetaling-treet
internal class UtbetalingstidslinjeBuilder(utbetaling: Utbetaling): UtbetalingVisitor {
    private val utbetalingstidslinje: MutableList<UtbetalingstidslinjedagDTO> = mutableListOf()

    init {
        utbetaling.accept(this)
    }

    internal fun build() = utbetalingstidslinje.toList()

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        økonomi.medAvrundetData { _, aktuellDagsinntekt ->
            utbetalingstidslinje.add(
                UtbetalingsdagDTO(
                    type = TypeDataDTO.Arbeidsdag,
                    inntekt = aktuellDagsinntekt!!,
                    dato = dato
                )
            )
        }
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        økonomi.medAvrundetData { _, aktuellDagsinntekt ->
            utbetalingstidslinje.add(
                UtbetalingsdagDTO(
                    type = TypeDataDTO.ArbeidsgiverperiodeDag,
                    inntekt = aktuellDagsinntekt!!,
                    dato = dato
                )
            )
        }
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.NavDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        økonomi.medData { grad, _, _, _, totalGrad, aktuellDagsinntekt, arbeidsgiverbeløp, _, _ ->
            utbetalingstidslinje.add(
                NavDagDTO(
                    type = TypeDataDTO.NavDag,
                    inntekt = aktuellDagsinntekt!!.roundToInt(),
                    dato = dato,
                    utbetaling = arbeidsgiverbeløp!!.roundToInt(),
                    grad = grad,
                    totalGrad = totalGrad
                )
            )
        }
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        økonomi.medData { grad, _ ->
            utbetalingstidslinje.add(
                UtbetalingsdagMedGradDTO(
                    type = TypeDataDTO.NavHelgDag,
                    inntekt = 0,   // Speil needs zero here
                    dato = dato,
                    grad = grad
                )
            )
        }
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.Fridag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        utbetalingstidslinje.add(
            UtbetalingsdagDTO(
                type = if (dato.erHelg()) TypeDataDTO.Helgedag else TypeDataDTO.Feriedag,
                inntekt = 0,    // Speil needs zero here
                dato = dato
            )
        )
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        utbetalingstidslinje.add(
            UtbetalingsdagDTO(
                type = TypeDataDTO.UkjentDag,
                inntekt = 0,    // Speil needs zero here
                dato = dato
            )
        )
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        utbetalingstidslinje.add(
            AvvistDagDTO(
                type = TypeDataDTO.AvvistDag,
                inntekt = 0,    // Speil needs zero here
                dato = dato,
                begrunnelser = dag.begrunnelser.map { BegrunnelseDTO.valueOf(PersonData.UtbetalingstidslinjeData.BegrunnelseData.fraBegrunnelse(it).name) },
                grad = 0.0 // Speil wants zero here
            )
        )
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.ForeldetDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        utbetalingstidslinje.add(
            UtbetalingsdagDTO(
                type = TypeDataDTO.ForeldetDag,
                inntekt = 0,    // Speil needs zero here
                dato = dato
            )
        )
    }
}
