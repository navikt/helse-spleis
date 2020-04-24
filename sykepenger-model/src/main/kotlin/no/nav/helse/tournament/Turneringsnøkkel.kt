package no.nav.helse.tournament

import no.nav.helse.sykdomstidslinje.dag.*

internal enum class Turneringsnøkkel {
    ImplisittDag,
    Arbeidsdag_SØ,
    Arbeidsdag_IM,
    Sykedag_SM,
    Sykedag_SØ,
    Kun_arbeidsgiverdag,
    Feriedag_SØ,
    Feriedag_IM,
    Permisjonsdag_AAREG,
    Permisjonsdag_SØ,
    SykHelgedag_SM,
    SykHelgedag_SØ,
    Egenmeldingsdag_IM,
    Egenmeldingsdag_SØ,
    Studiedag,
    AnnenInntekt_INNTK,
    AnnenInntekt_SØ,
    Utenlandsdag,
    UbestemtDag;

    companion object {
        fun fraDag(dag: Dag) = when (dag) {
            is Arbeidsdag.Inntektsmelding -> Arbeidsdag_IM
            is Arbeidsdag.Søknad -> Arbeidsdag_SØ
            is Egenmeldingsdag.Inntektsmelding -> Egenmeldingsdag_IM
            is Egenmeldingsdag.Søknad -> Egenmeldingsdag_SØ
            is Feriedag.Inntektsmelding -> Feriedag_IM
            is Feriedag.Søknad -> Feriedag_SØ
            is FriskHelgedag.Inntektsmelding -> Feriedag_IM
            is FriskHelgedag.Søknad -> Feriedag_SØ
            is no.nav.helse.sykdomstidslinje.dag.ImplisittDag -> ImplisittDag
            is ForeldetSykedag -> Kun_arbeidsgiverdag
            is Permisjonsdag.Søknad -> Permisjonsdag_SØ
            is Permisjonsdag.Aareg -> Permisjonsdag_AAREG
            is no.nav.helse.sykdomstidslinje.dag.Studiedag -> Studiedag
            is SykHelgedag.Sykmelding -> SykHelgedag_SM
            is SykHelgedag.Søknad -> SykHelgedag_SØ
            is Sykedag.Sykmelding -> Sykedag_SM
            is Sykedag.Søknad -> Sykedag_SØ
            is Ubestemtdag -> UbestemtDag
            is no.nav.helse.sykdomstidslinje.dag.Utenlandsdag -> Utenlandsdag
            else -> throw IllegalArgumentException("Finner ikke turneringsnøkkel for ${dag::class.simpleName}")
        }
    }
}
