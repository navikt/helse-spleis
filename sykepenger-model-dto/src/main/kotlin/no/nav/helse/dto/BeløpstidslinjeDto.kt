package no.nav.helse.dto

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class BeløpstidslinjeDto(val dager: List<BeløpstidslinjedagDto>) {

    data class BeløpstidslinjedagDto(val dato: LocalDate, val dagligBeløp: Double, val kilde: BeløpstidslinjedagKildeDto)
    data class BeløpstidslinjedagKildeDto(val meldingsreferanseId: UUID, val avsender: AvsenderDto, val tidsstempel: LocalDateTime)
}
