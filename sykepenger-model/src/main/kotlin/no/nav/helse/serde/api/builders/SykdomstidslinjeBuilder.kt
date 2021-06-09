package no.nav.helse.serde.api.builders

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.serde.api.SykdomstidslinjedagDTO
import no.nav.helse.serde.mapping.SpeilDagtype
import no.nav.helse.serde.mapping.SpeilKildetype
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal class SykdomstidslinjeBuilder(private val sykdomstidslinjeListe: MutableList<SykdomstidslinjedagDTO> = mutableListOf()) :
    BuilderState() {

    internal fun build() = sykdomstidslinjeListe.toList()

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
            dag.toSpeilDagtype(),
            SykdomstidslinjedagDTO.KildeDTO(kilde.toSpeilKildetype(), kilde.meldingsreferanseId()),
            økonomi?.reflection { grad, _, _, _, _, _, _, _, _ -> grad }
        )

        sykdomstidslinjeListe.add(dagDto)
    }

    private fun SykdomstidslinjeHendelse.Hendelseskilde.toSpeilKildetype() = when {
        erAvType(Inntektsmelding::class) -> SpeilKildetype.Inntektsmelding
        erAvType(Søknad::class) -> SpeilKildetype.Søknad
        erAvType(Sykmelding::class) -> SpeilKildetype.Sykmelding
        erAvType(OverstyrTidslinje::class) -> SpeilKildetype.Saksbehandler
        else -> SpeilKildetype.Ukjent
    }

    private fun Dag.toSpeilDagtype() = when (this) {
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

    override fun postVisitSykdomstidslinje(tidslinje: Sykdomstidslinje) {
        popState()
    }
}
