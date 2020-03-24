package no.nav.helse.sykdomstidslinje

import no.nav.helse.sykdomstidslinje.dag.*

internal interface NySykdomstidslinjeVisitor {
    fun preVisitSykdomstidslinje(tidslinje: NySykdomstidslinje) {}
    fun visitArbeidsdag(dag: Arbeidsdag.Inntektsmelding) {}
    fun visitArbeidsdag(dag: Arbeidsdag.Søknad) {}
    fun visitEgenmeldingsdag(dag: Egenmeldingsdag.Inntektsmelding) {}
    fun visitEgenmeldingsdag(dag: Egenmeldingsdag.Søknad) {}
    fun visitFeriedag(dag: Feriedag.Inntektsmelding) {}
    fun visitFeriedag(dag: Feriedag.Søknad) {}
    fun visitImplisittDag(dag: ImplisittDag) {}
    fun visitKunArbeidsgiverSykedag(dag: KunArbeidsgiverSykedag) {}
    fun visitPermisjonsdag(dag: Permisjonsdag.Søknad) {}
    fun visitPermisjonsdag(dag: Permisjonsdag.Aareg) {}
    fun visitStudiedag(dag: Studiedag) {}
    fun visitSykHelgedag(dag: SykHelgedag.Sykmelding) {}
    fun visitSykHelgedag(dag: SykHelgedag.Søknad) {}
    fun visitSykedag(dag: Sykedag.Sykmelding) {}
    fun visitSykedag(dag: Sykedag.Søknad) {}
    fun visitUbestemt(dag: Ubestemtdag) {}
    fun visitUtenlandsdag(dag: Utenlandsdag) {}
    fun postVisitSykdomstidslinje(tidslinje: NySykdomstidslinje) {}
}
