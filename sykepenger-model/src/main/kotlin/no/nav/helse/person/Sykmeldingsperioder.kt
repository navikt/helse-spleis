package no.nav.helse.person

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.inntektsmelding.DagerFraInntektsmelding
import no.nav.helse.dto.SykmeldingsperioderDto

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

    internal fun overlappendePerioder(dager: DagerFraInntektsmelding) =
        dager.overlappendeSykmeldingsperioder(perioder)

    internal fun perioderInnenfor16Dager(dager: DagerFraInntektsmelding) =
        dager.perioderInnenfor16Dager(perioder)

    internal fun dto() = SykmeldingsperioderDto(perioder = this.perioder.map { it.dto() })

    internal companion object {
        fun gjenopprett(dto: SykmeldingsperioderDto): Sykmeldingsperioder {
            return Sykmeldingsperioder(
                perioder = dto.perioder.map { Periode.gjenopprett(it) }
            )
        }
    }
}

internal interface SykmeldingsperioderVisitor {
    fun preVisitSykmeldingsperioder(sykmeldingsperioder: Sykmeldingsperioder) {}
    fun visitSykmeldingsperiode(periode: Periode) {}
    fun postVisitSykmeldingsperioder(sykmeldingsperioder: Sykmeldingsperioder) {}
}
