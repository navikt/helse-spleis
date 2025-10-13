package no.nav.helse.dto

import java.time.LocalDate
import java.time.LocalDateTime

data class BeløpstidslinjeDto(val perioder: List<BeløpstidslinjeperiodeDto>) {

    data class BeløpstidslinjeperiodeDto(val fom: LocalDate, val tom: LocalDate, val dagligBeløp: Double, val kilde: BeløpstidslinjedagKildeDto) {
        fun kanUtvidesAv(other: BeløpstidslinjeperiodeDto) =
            this.tom.plusDays(1) == other.fom &&
                this.kilde == other.kilde &&
                this.dagligBeløp == other.dagligBeløp
    }

    data class BeløpstidslinjedagKildeDto(
        val meldingsreferanseId: MeldingsreferanseDto,
        val avsender: AvsenderDto,
        val tidsstempel: LocalDateTime
    )
}
