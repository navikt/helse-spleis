package no.nav.helse.person

import no.nav.helse.person.Vedtaksperiode.Vedtaksperiodetilstand
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.Utbetalingslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.UtbetalingsdagVisitor

internal interface PersonVisitor: ArbeidsgiverVisitor {
    fun preVisitPerson(person: Person) {}
    fun postVisitPerson(person: Person) {}
}

internal interface ArbeidsgiverVisitor: UtbetalingsdagVisitor, VedtaksperiodeVisitor {
    fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver) {}
    fun postVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver) {}
}

internal interface VedtaksperiodeVisitor: SykdomstidslinjeVisitor {
    fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode) {}
    fun visitUtbetalingslinje(linje: Utbetalingslinje)
    fun visitTilstand(tilstand: Vedtaksperiodetilstand)
    fun postVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode) {}
}


