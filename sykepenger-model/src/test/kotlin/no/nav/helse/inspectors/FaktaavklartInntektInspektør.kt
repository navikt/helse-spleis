package no.nav.helse.inspectors

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.inntekt.ArbeidstakerFaktaavklartInntekt
import no.nav.helse.økonomi.Inntekt

internal val ArbeidstakerFaktaavklartInntekt.inspektør get() = FaktaavklartInntektInspektør(this)

internal class FaktaavklartInntektInspektør(inntekt: ArbeidstakerFaktaavklartInntekt) {
    val beløp: Inntekt = inntekt.inntektsdata.beløp
    val hendelseId: UUID = inntekt.inntektsdata.hendelseId.id
    val tidsstempel: LocalDateTime = inntekt.inntektsdata.tidsstempel
    val opplysningstype = inntekt.inntektsopplysningskilde
}
