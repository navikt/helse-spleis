package no.nav.helse.sykdomstidslinje

import no.nav.helse.sykdomstidslinje.dag.*

internal interface SykdomstidslinjeVisitor {
    fun visitArbeidsdag(arbeidsdag: Arbeidsdag) {}
    fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag) {}
    fun visitFeriedag(feriedag: Feriedag) {}
    fun visitImplisittDag(implisittDag: ImplisittDag) {}
    fun visitPermisjonsdag(permisjonsdag: Permisjonsdag) {}
    fun visitStudiedag(studiedag: Studiedag) {}
    fun visitSykHelgedag(sykHelgedag: SykHelgedag) {}
    fun visitSykedag(sykedag: Sykedag) {}
    fun visitUbestemt(ubestemtdag: Ubestemtdag) {}
    fun visitUtenlandsdag(utenlandsdag: Utenlandsdag) {}
    fun preVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) {}
    fun postVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) {}
    fun preVisitArbeidsgiver(arbeidsgiverSykdomstidslinje: ArbeidsgiverSykdomstidslinje) {}
    fun postVisitArbeidsgiver(arbeidsgiverSykdomstidslinje: ArbeidsgiverSykdomstidslinje) {}
}
