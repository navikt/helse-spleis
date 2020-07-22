package no.nav.helse.serde.reflection

import no.nav.helse.person.Inntekthistorikk
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import java.time.LocalDate
import java.util.*

internal class InntektReflect(inntekt: Inntekthistorikk.Inntekt) {
    private val fom: LocalDate = inntekt["fom"]
    private val hendelseId: UUID = inntekt["hendelseId"]
    private val beløp: Double = inntekt["beløp"]
    private val kilde: Inntekthistorikk.Inntekt.Kilde = inntekt["kilde"]

    internal fun toMap(): Map<String, Any?> = mapOf(
        "fom" to fom,
        "hendelseId" to hendelseId,
        "beløp" to beløp,
        "kilde" to kilde.toString()
    )
}
