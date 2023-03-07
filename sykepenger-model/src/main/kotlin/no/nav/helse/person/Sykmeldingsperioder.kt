package no.nav.helse.person

import java.time.LocalDate
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmelding

internal class Sykmeldingsperioder(
    private var perioder: List<Periode> = listOf()
) {

    internal fun accept(visitor: SykmeldingsperioderVisitor) {
        visitor.preVisitSykmeldingsperioder(this)
        perioder.forEach { visitor.visitSykmeldingsperiode(it) }
        visitor.postVisitSykmeldingsperioder(this)
    }

    internal fun lagre(sykmelding: Sykmelding) {
        perioder = sykmelding.oppdaterSykmeldingsperioder(perioder)
    }

    internal fun avventerSøknad(vedtaksperiode: Periode): Boolean {
        val lavesteDato = perioder.minOfOrNull { it.start } ?: return false
        return lavesteDato <= vedtaksperiode.endInclusive
    }

    internal fun fjern(tom: LocalDate) {
        perioder = perioder.mapNotNull { it.beholdDagerEtter(tom) }
    }

    internal fun blirTruffetAv(inntektsmelding: Inntektsmelding) = perioder.any { periode ->
        inntektsmelding.overlapperMed(periode)
    }
    internal fun harSykmeldingsperiodeFør(dato: LocalDate) = perioder.any { it.start < dato }
}
internal interface SykmeldingsperioderVisitor {
    fun preVisitSykmeldingsperioder(sykmeldingsperioder: Sykmeldingsperioder) {}
    fun visitSykmeldingsperiode(periode: Periode) {}
    fun postVisitSykmeldingsperioder(sykmeldingsperioder: Sykmeldingsperioder) {}
}
