package no.nav.helse.person

import no.nav.helse.hendelser.Periode

internal class Sykmeldingsperioder(
    private val perioder: MutableList<Periode> = mutableListOf()
) {

    internal fun accept(visitor: Visitor) {
        visitor.preVisitSykmeldingsperioder(this)
        perioder.forEach { visitor.visitSykmeldingsperiode(it) }
        visitor.postVisitSykmeldingsperioder(this)
    }

    internal fun lagre(periode: Periode) {
        perioder.add(periode)
    }

    interface Visitor {
        fun preVisitSykmeldingsperioder(sykmeldingsperioder: Sykmeldingsperioder) {}
        fun visitSykmeldingsperiode(periode: Periode) {}
        fun postVisitSykmeldingsperioder(sykmeldingsperioder: Sykmeldingsperioder) {}
    }
}
