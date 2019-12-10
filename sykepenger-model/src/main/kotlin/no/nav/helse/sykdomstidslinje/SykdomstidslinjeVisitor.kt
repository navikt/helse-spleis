package no.nav.helse.sykdomstidslinje

import no.nav.helse.sak.Arbeidsgiver
import no.nav.helse.sak.Sak
import no.nav.helse.sak.Vedtaksperiode
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
    fun preVisitSak(sak: Sak) {}
    fun postVisitSak(sak: Sak) {}
    fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver) {}
    fun postVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver) {}
    fun preVisitVedtaksperiode(periode: Vedtaksperiode) {}
    fun postVisitVedtaksperiode(periode: Vedtaksperiode) {}
}
