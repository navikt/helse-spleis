package no.nav.helse.dto

import java.time.LocalDate

data class RefusjonsservitørDto(val refusjonstidslinjer: Map<LocalDate, BeløpstidslinjeDto>)
