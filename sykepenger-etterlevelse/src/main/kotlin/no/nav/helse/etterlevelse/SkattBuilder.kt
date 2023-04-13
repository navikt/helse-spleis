package no.nav.helse.etterlevelse

import java.time.YearMonth
import no.nav.helse.økonomi.Inntekt

class SkattBuilder(skatt: SkatteopplysningPort) {
    private lateinit var inntekt: Map<String, Any>

    init {
        skatt.accept { beløp, måned, type, fordel, beskrivelse ->
            inntekt = mapOf(
                "beløp" to beløp.månedlig,
                "årMåned" to måned,
                "type" to type,
                "fordel" to fordel,
                "beskrivelse" to beskrivelse
            )
        }
    }

    fun inntekt() = inntekt

    companion object {
        fun Iterable<SkatteopplysningPort>.subsumsjonsformat(): List<Map<String, Any>> = map { SkattBuilder(it).inntekt() }
    }
}

interface SkatteopplysningPort {
    fun <T> accept(pseudoVisitor: (beløp: Inntekt, måned: YearMonth, type: String, fordel: String, beskrivelse: String) -> T)

}