package no.nav.helse.person

import no.nav.helse.dto.ArbeidsgiverperiodeavklaringDto
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.periode

data class Arbeidsgiverperiodeavklaring(
    val ferdigAvklart: Boolean,
    val dager: List<Periode>
) {
    val periode = dager.periode()

    fun dto() = ArbeidsgiverperiodeavklaringDto(
        ferdigAvklart = ferdigAvklart,
        dager = dager.map { it.dto() }
    )

    companion object {
        fun gjenopprett(dto: ArbeidsgiverperiodeavklaringDto) =
            Arbeidsgiverperiodeavklaring(
                ferdigAvklart = dto.ferdigAvklart,
                dager = dto.dager.map { Periode.gjenopprett(it) }
            )
    }
}
