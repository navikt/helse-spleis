package no.nav.helse.person

import java.time.LocalDate
import java.time.YearMonth
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.aldri
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
        val periode = sykmelding.periode()
        if (periode == aldri) return sykmelding.info("Sykmeldingsperiode har allerede blitt tidligere håndtert, mistenker korrigert sykmelding")
        val (overlappendePerioder, gapPerioder) = perioder.partition { it.overlapperMed(periode) }
        val sammenhengendePerioder = overlappendePerioder + listOf(periode)
        val nyPeriode = sammenhengendePerioder.minOf { it.start } til sammenhengendePerioder.maxOf { it.endInclusive }
        sykmelding.info("Legger til ny periode $nyPeriode i sykmeldingsperioder")
        perioder = (gapPerioder + listOf(nyPeriode)).sortedBy { it.start }
    }

    internal fun harSykmeldingsperiodeI(måned: YearMonth): Boolean =
        perioder.flatten().any { YearMonth.from(it) == måned }

    internal fun harSykmeldingsperiode() = perioder.isNotEmpty()

    internal fun kanFortsetteBehandling(vedtaksperiode: Periode): Boolean {
        val lavesteDato = perioder.minOfOrNull { it.start } ?: return true
        return lavesteDato > vedtaksperiode.endInclusive
    }

    internal fun fjern(tom: LocalDate) {
        perioder = perioder.mapNotNull { it.beholdDagerEtter(tom) }
    }

    fun blirTruffetAv(inntektsmelding: SykdomstidslinjeHendelse) = perioder.any(inntektsmelding::erRelevant)
    internal fun harSykmeldingsperiodeFør(dato: LocalDate) = perioder.any { it.start < dato }

    internal fun beholdReelleSykmeldingsperioder() {
        perioder = perioder.filterNot { it == aldri }
    }
}
internal interface SykmeldingsperioderVisitor {
    fun preVisitSykmeldingsperioder(sykmeldingsperioder: Sykmeldingsperioder) {}
    fun visitSykmeldingsperiode(periode: Periode) {}
    fun postVisitSykmeldingsperioder(sykmeldingsperioder: Sykmeldingsperioder) {}
}
