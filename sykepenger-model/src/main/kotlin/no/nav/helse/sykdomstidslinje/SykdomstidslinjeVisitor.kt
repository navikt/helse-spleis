package no.nav.helse.sykdomstidslinje

import no.nav.helse.sykdomstidslinje.dag.*

internal interface SykdomstidslinjeVisitor {
    fun visitArbeidsdag(arbeidsdag: Arbeidsdag.Inntektsmelding) {}
    fun visitArbeidsdag(arbeidsdag: Arbeidsdag.Søknad) {}
    fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag.Inntektsmelding) {}
    fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag.Søknad) {}
    fun visitFeriedag(feriedag: Feriedag.Inntektsmelding) {}
    fun visitFeriedag(feriedag: Feriedag.Søknad) {}
    fun visitImplisittDag(implisittDag: ImplisittDag) {}
    fun visitPermisjonsdag(permisjonsdag: Permisjonsdag.Søknad) {}
    fun visitPermisjonsdag(permisjonsdag: Permisjonsdag.Aareg) {}
    fun visitStudiedag(studiedag: Studiedag) {}
    fun visitSykHelgedag(sykHelgedag: SykHelgedag) {}
    fun visitSykedag(sykedag: Sykedag.Sykmelding) {}
    fun visitSykedag(sykedag: Sykedag.Søknad) {}
    fun visitUbestemt(ubestemtdag: Ubestemtdag) {}
    fun visitUtenlandsdag(utenlandsdag: Utenlandsdag) {}
    fun preVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) {}
    fun postVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) {}
}
