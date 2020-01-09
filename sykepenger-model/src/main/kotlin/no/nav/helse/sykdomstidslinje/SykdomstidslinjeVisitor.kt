package no.nav.helse.sykdomstidslinje

import no.nav.helse.sykdomstidslinje.dag.*

internal interface SykdomstidslinjeVisitor {
    fun visitArbeidsdag(arbeidsdag: Arbeidsdag) {}
    fun visitImplisittDag(implisittDag: ImplisittDag) {}
    fun visitFeriedag(feriedag: Feriedag) {}
    fun visitSykedag(sykedag: Sykedag) {}
    fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag) {}
    fun visitSykHelgedag(sykHelgedag: SykHelgedag) {}
    fun visitUtenlandsdag(utenlandsdag: Utenlandsdag) {}
    fun visitUbestemt(ubestemtdag: Ubestemtdag) {}
    fun visitStudiedag(studiedag: Studiedag) {}
    fun visitPermisjonsdag(permisjonsdag: Permisjonsdag) {}
    fun preVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) {}
    fun postVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) {}
    fun preVisitSak(personSykdomstidslinje: PersonSykdomstidslinje) {}
    fun postVisitSak(personSykdomstidslinje: PersonSykdomstidslinje) {}
    fun preVisitArbeidsgiver(arbeidsgiverSykdomstidslinje: ArbeidsgiverSykdomstidslinje) {}
    fun postVisitArbeidsgiver(arbeidsgiverSykdomstidslinje: ArbeidsgiverSykdomstidslinje) {}
}
