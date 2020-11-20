package no.nav.helse.serde.api

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.serde.mapping.SpeilDagtype
import no.nav.helse.serde.mapping.SpeilKildetype
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.serde.reflection.ReflectInstance.Companion.maybe
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Økonomi

internal fun tilSpeilSykdomstidslinjedag(
    dag: Dag,
    kilde: SykdomstidslinjeHendelse.Hendelseskilde
) =
    SykdomstidslinjedagDTO(
        dag["dato"],
        dag.toSpeilDagtype(),
        SykdomstidslinjedagDTO.KildeDTO(kilde.toSpeilKildetype(), kilde.meldingsreferanseId()),
        dag.maybe<Økonomi?>("økonomi")?.get<Prosentdel>("grad")?.toDouble()
    )

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
    is Dag.Studiedag -> SpeilDagtype.STUDIEDAG
    is Dag.Utenlandsdag -> SpeilDagtype.UTENLANDSDAG
    is Dag.ProblemDag -> SpeilDagtype.UBESTEMTDAG
}
