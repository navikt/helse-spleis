package no.nav.helse.person.etterlevelse

import no.nav.helse.person.inntekt.Skatteopplysning

class SkattBuilder(skatt: Skatteopplysning) {
    private lateinit var inntekt: Map<String, Any>

    init {
        skatt.accept { beløp, måned, type, fordel, beskrivelse ->
            inntekt = mapOf(
                "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
                "årMåned" to måned,
                "type" to type,
                "fordel" to fordel,
                "beskrivelse" to beskrivelse
            )
        }
    }

    fun inntekt() = inntekt

    companion object {
        fun Iterable<Skatteopplysning>.subsumsjonsformat(): List<Map<String, Any>> = map { SkattBuilder(it).inntekt() }
    }
}