package no.nav.helse.person.inntekt

import java.util.UUID
import no.nav.helse.økonomi.Inntekt

sealed interface FaktaavklartInntekt {
    val id: UUID
    val inntektsdata: Inntektsdata
}

sealed interface FaktaavklartInntektView {
    val hendelseId: UUID
    val beløp: Inntekt
}
