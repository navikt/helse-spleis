package no.nav.helse.person

import java.time.YearMonth
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til

internal class Sykmeldingsperioder(
    private var perioder: List<Periode> = listOf()
) {

    internal fun accept(visitor: SykmeldingsperioderVisitor) {
        visitor.preVisitSykmeldingsperioder(this)
        perioder.forEach { visitor.visitSykmeldingsperiode(it) }
        visitor.postVisitSykmeldingsperioder(this)
    }

    internal fun lagre(periode: Periode) {
        val (overlappendePerioder, gapPerioder) = perioder.partition { it.overlapperMed(periode) }
        val sammenhengendePerioder = overlappendePerioder + listOf(periode)
        val nyPeriode = sammenhengendePerioder.minOf { it.start } til sammenhengendePerioder.maxOf { it.endInclusive }
        perioder = (gapPerioder + listOf(nyPeriode)).sortedBy { it.start }
    }

    internal fun harSykmeldingsperiodeI(måned: YearMonth): Boolean =
        perioder.flatten().any { YearMonth.from(it) == måned }

    internal fun harSykmeldingsperiode() = perioder.isNotEmpty()

    internal fun kanFortsetteBehandling(vedtaksperiode: Periode): Boolean {
        val lavesteDato = perioder.minOfOrNull { it.start } ?: return true
        return lavesteDato > vedtaksperiode.endInclusive
    }

    internal fun fjern(søknadsperiode: Periode) {
        perioder = perioder.mapNotNull { it.beholdDagerEtter(søknadsperiode.endInclusive) }
    }
}
internal interface SykmeldingsperioderVisitor {
    fun preVisitSykmeldingsperioder(sykmeldingsperioder: Sykmeldingsperioder) {}
    fun visitSykmeldingsperiode(periode: Periode) {}
    fun postVisitSykmeldingsperioder(sykmeldingsperioder: Sykmeldingsperioder) {}
}
