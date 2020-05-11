package no.nav.helse.tournament

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.sykdomstidslinje.NyDag
import no.nav.helse.sykdomstidslinje.NyDag.*

internal enum class Turneringsnøkkel {
    Arbeidsdag_SØ,
    Arbeidsdag_IM,
    Sykedag_SM,
    Sykedag_SØ,
    ForeldetSykedag,
    Feriedag_SØ,
    Feriedag_IM,
    Permisjonsdag_AAREG,
    Permisjonsdag_SØ,
    SykHelgedag_SM,
    SykHelgedag_SØ,
    Arbeidsgiverdag_IM,
    Arbeidsgiverdag_SØ,
    Studiedag,
    AnnenInntekt_INNTK,
    AnnenInntekt_SØ,
    Utenlandsdag,
    UkjentDag,
    UbestemtDag;

    companion object {
        fun fraDag(dag: NyDag) = when {
            dag is NyArbeidsdag && dag.kommerFra(Inntektsmelding::class) -> Arbeidsdag_IM
            dag is NyArbeidsdag && dag.kommerFra(Søknad::class) -> Arbeidsdag_SØ
            dag is NyArbeidsgiverdag && dag.kommerFra(Inntektsmelding::class) -> Arbeidsgiverdag_IM
            dag is NyArbeidsgiverdag && dag.kommerFra(Søknad::class) -> Arbeidsgiverdag_SØ
            dag is NyArbeidsgiverHelgedag && dag.kommerFra(Inntektsmelding::class) -> Arbeidsgiverdag_IM
            dag is NyArbeidsgiverHelgedag && dag.kommerFra(Søknad::class) -> Arbeidsgiverdag_SØ
            dag is NyFeriedag && dag.kommerFra(Inntektsmelding::class) -> Feriedag_IM
            dag is NyFeriedag && dag.kommerFra(Søknad::class) -> Feriedag_SØ
            dag is NyFriskHelgedag && dag.kommerFra(Inntektsmelding::class) -> Feriedag_IM
            dag is NyFriskHelgedag && dag.kommerFra(Søknad::class) -> Feriedag_SØ
            dag is NyForeldetSykedag -> ForeldetSykedag
            dag is NyPermisjonsdag && dag.kommerFra(Søknad::class) -> Permisjonsdag_SØ
            dag is NyPermisjonsdag -> Permisjonsdag_AAREG
            dag is ProblemDag -> UbestemtDag
            dag is NyStudiedag -> Studiedag
            dag is NySykedag && dag.kommerFra(Sykmelding::class) -> Sykedag_SM
            dag is NySykedag && dag.kommerFra(Søknad::class) -> Sykedag_SØ
            dag is NySykHelgedag && dag.kommerFra(Sykmelding::class) -> SykHelgedag_SM
            dag is NySykHelgedag && dag.kommerFra(Søknad::class) -> SykHelgedag_SØ
            dag is NyUkjentDag -> UkjentDag
            dag is NyUtenlandsdag -> Utenlandsdag
            else -> throw IllegalArgumentException("Ingen turneringsnøkkel definert for ${dag::class.simpleName}")
        }
    }
}
