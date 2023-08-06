package no.nav.helse.etterlevelse

import java.time.YearMonth

class Inntektsubsumsjon(
    private val beløp: Double,
    private val årMåned: YearMonth,
    private val type: String,
    private val fordel: String,
    private val beskrivelse: String
) {

    fun subsumsjonsformat() = mapOf(
        "beløp" to beløp,
        "årMåned" to årMåned,
        "type" to type,
        "fordel" to fordel,
        "beskrivelse" to beskrivelse
    )

    companion object {
        fun Iterable<Inntektsubsumsjon>.subsumsjonsformat() = map { it.subsumsjonsformat() }
    }
}