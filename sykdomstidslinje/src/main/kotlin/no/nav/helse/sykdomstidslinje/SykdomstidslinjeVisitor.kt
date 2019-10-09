package no.nav.helse.sykdomstidslinje

import no.nav.helse.sykdomstidslinje.dag.*

interface SykdomstidslinjeVisitor {
    fun visitArbeidsdag(arbeidsdag: Arbeidsdag) {}
    fun visitFylldag(fylldag: Fylldag) {}
    fun visitFeriedag(feriedag: Feriedag) {}
    fun visitHelgedag(helgedag: Helgedag) {}
    fun visitSykedag(sykedag: Sykedag) {}
    fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag) {}
    fun visitSykHelgedag(sykHelgedag: SykHelgedag) {}
    fun visitUtenlandsdag(utenlandsdag: Utenlandsdag) {}
    fun visitUbestemt(ubestemtdag: Ubestemtdag) {}
    fun visitStudiedag(studiedag: Studiedag) {}
    fun visitPermisjonsdag(permisjonsdag: Permisjonsdag) {}
    fun preVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) {}
    fun postVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) {}

}
