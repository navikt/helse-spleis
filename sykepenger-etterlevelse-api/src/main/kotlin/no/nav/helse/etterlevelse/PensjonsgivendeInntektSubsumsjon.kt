package no.nav.helse.etterlevelse

import java.time.Year

data class PensjonsgivendeInntektSubsumsjon(
    val pensjonsgivendeInntekt: Double,
    val årstall: Year,
    val gjennomsnittligG: Double
)

fun List<PensjonsgivendeInntektSubsumsjon>.subsumsjonsformat() = this.map {
    mapOf(
        "pensjonsgivendeInntekt" to it.pensjonsgivendeInntekt,
        "årstall" to it.årstall,
        "gjennomsnittligG" to it.gjennomsnittligG,
    )
}
