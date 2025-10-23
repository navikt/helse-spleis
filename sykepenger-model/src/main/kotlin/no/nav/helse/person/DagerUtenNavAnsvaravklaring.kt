package no.nav.helse.person

import no.nav.helse.dto.DagerUtenNavAnsvaravklaringDto
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.periode

data class DagerUtenNavAnsvaravklaring(
    val ferdigAvklart: Boolean,
    val dager: List<Periode>
) {
    val periode = dager.periode()

    fun dto() = DagerUtenNavAnsvaravklaringDto(
        ferdigAvklart = ferdigAvklart,
        dager = dager.map { it.dto() }
    )

    companion object {
        fun gjenopprett(dto: DagerUtenNavAnsvaravklaringDto) =
            DagerUtenNavAnsvaravklaring(
                ferdigAvklart = dto.ferdigAvklart,
                dager = dto.dager.map { Periode.gjenopprett(it) }
            )
    }
}
