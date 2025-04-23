package no.nav.helse.person

import java.time.LocalDate
import no.nav.helse.dto.SykmeldingsperioderDto
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal class Sykmeldingsperioder(
    private var perioder: List<Periode> = listOf()
) {

    fun view() = SykmeldingsperioderView(perioder)
    internal fun perioder() = perioder.toList()

    internal fun lagre(sykmelding: Sykmelding, aktivitetslogg: IAktivitetslogg) {
        perioder = sykmelding.oppdaterSykmeldingsperioder(aktivitetslogg, perioder)
    }

    internal fun avventerSøknad(vedtaksperiode: Periode): Boolean {
        return perioder.any { other -> vedtaksperiode.overlapperMed(other) }
    }

    internal fun fjern(periode: Periode) {
        perioder = perioder.flatMap { it.uten(periode.oppdaterFom(LocalDate.MIN)) }
    }

    internal fun dto() = SykmeldingsperioderDto(perioder = this.perioder.map { it.dto() })

    internal companion object {
        fun gjenopprett(dto: SykmeldingsperioderDto): Sykmeldingsperioder {
            return Sykmeldingsperioder(
                perioder = dto.perioder.map { Periode.gjenopprett(it) }
            )
        }
    }
}

internal data class SykmeldingsperioderView(val perioder: List<Periode>)
