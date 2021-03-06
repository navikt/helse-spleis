package no.nav.helse.tournament

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.*

internal enum class Turneringsnøkkel {
    Arbeidsdag_SØ,
    Arbeidsdag_IM,
    Sykedag_SM,
    Sykedag_SØ,
    ForeldetSykedag,
    Feriedag_SØ,
    Feriedag_IM,
    Permisjonsdag_SØ,
    SykHelgedag_SM,
    SykHelgedag_SØ,
    Arbeidsgiverdag_IM,
    ArbeidsgiverHelgedag_IM,
    Arbeidsgiverdag_SØ,
    ArbeidsgiverHelgedag_SØ,
    AnnenInntekt_INNTK,
    AnnenInntekt_SØ,
    AvslåttDag,
    Saksbehandlerdag,
    UkjentDag,
    UbestemtDag;

    companion object {
        fun fraDag(dag: Dag) = when {
            dag.kommerFra(OverstyrTidslinje::class) -> Saksbehandlerdag
            dag is Arbeidsdag && dag.kommerFra(Inntektsmelding::class) -> Arbeidsdag_IM
            dag is Arbeidsdag && dag.kommerFra(Søknad::class) -> Arbeidsdag_SØ
            dag is Arbeidsgiverdag && dag.kommerFra(Inntektsmelding::class) -> Arbeidsgiverdag_IM
            dag is Arbeidsgiverdag && dag.kommerFra(Søknad::class) -> Arbeidsgiverdag_SØ
            dag is ArbeidsgiverHelgedag && dag.kommerFra(Inntektsmelding::class) -> ArbeidsgiverHelgedag_IM
            dag is ArbeidsgiverHelgedag && dag.kommerFra(Søknad::class) -> ArbeidsgiverHelgedag_SØ
            dag is Feriedag && dag.kommerFra(Inntektsmelding::class) -> Feriedag_IM
            dag is Feriedag && dag.kommerFra(Søknad::class) -> Feriedag_SØ
            dag is FriskHelgedag && dag.kommerFra(Inntektsmelding::class) -> Feriedag_IM
            dag is FriskHelgedag && dag.kommerFra(Søknad::class) -> Feriedag_SØ
            dag is Dag.ForeldetSykedag -> ForeldetSykedag
            dag is Permisjonsdag -> Permisjonsdag_SØ
            dag is ProblemDag -> UbestemtDag
            dag is Sykedag && dag.kommerFra(Sykmelding::class) -> Sykedag_SM
            dag is Sykedag && dag.kommerFra(Søknad::class) -> Sykedag_SØ
            dag is SykHelgedag && dag.kommerFra(Sykmelding::class) -> SykHelgedag_SM
            dag is SykHelgedag && dag.kommerFra(Søknad::class) -> SykHelgedag_SØ
            dag is Dag.UkjentDag -> UkjentDag
            else -> throw IllegalArgumentException("Ingen turneringsnøkkel definert for ${dag::class.simpleName}")
        }
    }
}
