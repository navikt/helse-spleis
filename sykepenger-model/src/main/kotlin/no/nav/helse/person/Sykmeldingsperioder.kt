package no.nav.helse.person

import no.nav.helse.dto.SykmeldingsperioderDto
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import java.time.LocalDate

internal class Sykmeldingsperioder(
    private var perioder: List<Periode> = listOf(),
) {
    internal fun perioder() = perioder.toList()

    internal fun lagre(
        sykmelding: Sykmelding,
        aktivitetslogg: IAktivitetslogg,
    ) {
        perioder = sykmelding.oppdaterSykmeldingsperioder(aktivitetslogg, perioder)
    }

    internal fun avventerSøknad(vedtaksperiode: Periode): Boolean = perioder.any { other -> vedtaksperiode.overlapperMed(other) }

    internal fun fjern(periode: Periode) {
        perioder = perioder.flatMap { it.uten(periode.oppdaterFom(LocalDate.MIN)) }
    }

    internal fun dto() = SykmeldingsperioderDto(perioder = this.perioder.map { it.dto() })

    internal companion object {
        fun gjenopprett(dto: SykmeldingsperioderDto): Sykmeldingsperioder =
            Sykmeldingsperioder(
                perioder = dto.perioder.map { Periode.gjenopprett(it) },
            )
    }
}
