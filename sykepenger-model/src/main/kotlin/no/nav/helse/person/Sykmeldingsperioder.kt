package no.nav.helse.person

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.til
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse

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

    internal fun avventerSøknad(skjæringstidspunkt: LocalDate): Boolean {
        val måned = skjæringstidspunkt.withDayOfMonth(1) til skjæringstidspunkt.withDayOfMonth(skjæringstidspunkt.lengthOfMonth())
        return perioder.any(måned::overlapperMed)
    }

    internal fun avventerSøknad(vedtaksperiode: Periode): Boolean {
        val lavesteDato = perioder.minOfOrNull { it.start } ?: return false
        return lavesteDato <= vedtaksperiode.endInclusive
    }

    internal fun fjern(tom: LocalDate) {
        perioder = perioder.mapNotNull { it.beholdDagerEtter(tom) }
    }

    fun blirTruffetAv(inntektsmelding: SykdomstidslinjeHendelse) = perioder.any(inntektsmelding::erRelevant)
    internal fun harSykmeldingsperiodeFør(dato: LocalDate) = perioder.any { it.start < dato }
}
internal interface SykmeldingsperioderVisitor {
    fun preVisitSykmeldingsperioder(sykmeldingsperioder: Sykmeldingsperioder) {}
    fun visitSykmeldingsperiode(periode: Periode) {}
    fun postVisitSykmeldingsperioder(sykmeldingsperioder: Sykmeldingsperioder) {}
}
