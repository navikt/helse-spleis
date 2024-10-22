package no.nav.helse.person.inntekt

import no.nav.helse.dto.NyInntektUnderveisDto
import no.nav.helse.person.beløp.Beløpstidslinje

data class NyInntektUnderveis(
    val orgnummer: String,
    val beløpstidslinje: Beløpstidslinje
) {
    fun dto() = NyInntektUnderveisDto(
        orgnummer = orgnummer,
        beløpstidslinje = beløpstidslinje.dto()
    )
    companion object {
        fun gjenopprett(dto: NyInntektUnderveisDto) = NyInntektUnderveis(
            orgnummer = dto.orgnummer,
            beløpstidslinje = Beløpstidslinje.gjenopprett(dto.beløpstidslinje)
        )
    }
}
