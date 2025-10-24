package no.nav.helse.person.inntekt

import java.util.UUID

sealed interface FaktaavklartInntekt {
    val id: UUID
    val inntektsdata: Inntektsdata
}

sealed interface FaktaavklartInntektView {
    val id: UUID
}
