package no.nav.helse.person

import no.nav.helse.hendelser.Periode

internal class Sykmeldingsperioder() {

    internal fun accept(visitor: Visitor) {
        visitor.preVisitSykmeldingsperioder(this)
        //visitor.visitSykmeldingsperiode()
        visitor.postVisitSykmeldingsperioder(this)
    }

    internal fun lagre(periode: Periode) : Unit = TODO()

    interface Visitor {
        fun preVisitSykmeldingsperioder(sykmeldingsperioder: Sykmeldingsperioder) {}
        fun visitSykmeldingsperiode(periode: Periode) {}
        fun postVisitSykmeldingsperioder(sykmeldingsperioder: Sykmeldingsperioder) {}
    }
}
