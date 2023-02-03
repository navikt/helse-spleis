package no.nav.helse.person.etterlevelse

import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.person.SkatteopplysningVisitor
import no.nav.helse.person.inntekt.Skatteopplysning
import no.nav.helse.økonomi.Inntekt

internal class SkattBuilder(skatt: Skatteopplysning) : SkatteopplysningVisitor {
    private lateinit var inntekt: Map<String, Any>

    init {
        skatt.accept(this)
    }

    fun inntekt() = inntekt

    override fun visitSkatteopplysning(
        skatteopplysning: Skatteopplysning,
        hendelseId: UUID,
        beløp: Inntekt,
        måned: YearMonth,
        type: Skatteopplysning.Inntekttype,
        fordel: String,
        beskrivelse: String,
        tidsstempel: LocalDateTime
    ) {
        inntekt = mapOf(
            "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
            "årMåned" to måned,
            "type" to type.fromInntekttype(),
            "fordel" to fordel,
            "beskrivelse" to beskrivelse
        )
    }

    private fun Skatteopplysning.Inntekttype.fromInntekttype() = when (this) {
        Skatteopplysning.Inntekttype.LØNNSINNTEKT -> "LØNNSINNTEKT"
        Skatteopplysning.Inntekttype.NÆRINGSINNTEKT -> "NÆRINGSINNTEKT"
        Skatteopplysning.Inntekttype.PENSJON_ELLER_TRYGD -> "PENSJON_ELLER_TRYGD"
        Skatteopplysning.Inntekttype.YTELSE_FRA_OFFENTLIGE -> "YTELSE_FRA_OFFENTLIGE"
    }
}