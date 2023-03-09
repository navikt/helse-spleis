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
        return perioder.any { other -> vedtaksperiode.overlapperMed(other) }
    }

    internal fun fjern(søknad: Periode) {
        perioder = perioder.flatMap { it.trim(søknad.oppdaterFom(LocalDate.MIN)) }
    }

    internal fun overlappendePerioder(inntektsmelding: Inntektsmelding) =
        inntektsmelding.overlappendeSykmeldingsperioder(perioder)
}

internal interface SykmeldingsperioderVisitor {
    fun preVisitSykmeldingsperioder(sykmeldingsperioder: Sykmeldingsperioder) {}
    fun visitSykmeldingsperiode(periode: Periode) {}
    fun postVisitSykmeldingsperioder(sykmeldingsperioder: Sykmeldingsperioder) {}
}
