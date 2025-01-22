package no.nav.helse.inspectors

import java.time.LocalDateTime
import java.util.*
import no.nav.helse.person.inntekt.FaktaavklartInntekt
import no.nav.helse.økonomi.Inntekt

internal val FaktaavklartInntekt.inspektør get() = FaktaavklartInntektInspektør(this)

internal class FaktaavklartInntektInspektør(inntekt: FaktaavklartInntekt) {
    val beløp: Inntekt = inntekt.inntektsdata.beløp
    val hendelseId: UUID = inntekt.inntektsdata.hendelseId
    val tidsstempel: LocalDateTime = inntekt.inntektsdata.tidsstempel
    val opplysningstype = inntekt.inntektsopplysning
}
