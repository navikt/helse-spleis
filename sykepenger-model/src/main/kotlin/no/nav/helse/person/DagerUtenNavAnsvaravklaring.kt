package no.nav.helse.person

import no.nav.helse.dto.DagerUtenNavAnsvaravklaringDto
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.periode

data class DagerUtenNavAnsvaravklaring(
    val ferdigAvklart: Boolean,
    val dager: List<Periode>
) {
    val periode = dager.periode()

    fun samme(other: DagerUtenNavAnsvaravklaring): Boolean {
        if (this == other) return true
        if (this.datoer.isEmpty() || other.datoer.isEmpty()) return false
        if (this.avklartIInfotrygd || other.avklartIInfotrygd) return false
        return if (this.datoer.size < other.datoer.size) other.datoer.containsAll(this.datoer)
        else this.datoer.containsAll(other.datoer)
    }

    private val datoer by lazy { dager.flatten() }
    private val avklartIInfotrygd by lazy { ferdigAvklart && datoer.size < 16 }


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
