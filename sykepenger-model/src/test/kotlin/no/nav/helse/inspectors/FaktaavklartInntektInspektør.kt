package no.nav.helse.inspectors

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.inntekt.ArbeidstakerFaktaavklartInntekt
import no.nav.helse.person.inntekt.FaktaavklartInntekt
import no.nav.helse.person.inntekt.SelvstendigFaktaavklartInntekt
import no.nav.helse.økonomi.Inntekt

internal val ArbeidstakerFaktaavklartInntekt.inspektør get() = FaktaavklartInntektInspektør(this)

internal class FaktaavklartInntektInspektør(inntekt: ArbeidstakerFaktaavklartInntekt) {
    val beløp: Inntekt = inntekt.inntektsdata.beløp
    val hendelseId: UUID = inntekt.inntektsdata.hendelseId.id
    val tidsstempel: LocalDateTime = inntekt.inntektsdata.tidsstempel
    val opplysningstype = inntekt.inntektsopplysningskilde
}

// Speiler beløpet slik det ble presentert av det tidligere view-laget: for arbeidstakere er det
// den faktaavklarte inntekten, for selvstendig næringsdrivende er det normalinntekten.
internal val FaktaavklartInntekt.beløp: Inntekt
    get() = when (this) {
        is ArbeidstakerFaktaavklartInntekt -> inntektsdata.beløp
        is SelvstendigFaktaavklartInntekt -> normalinntekt
    }

internal val FaktaavklartInntekt.hendelseId: UUID get() = inntektsdata.hendelseId.id
