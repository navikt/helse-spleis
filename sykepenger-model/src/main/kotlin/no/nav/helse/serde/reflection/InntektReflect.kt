package no.nav.helse.serde.reflection

import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Inntekthistorikk
import java.math.BigDecimal
import java.time.LocalDate
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get

internal class InntektReflect(inntekt: Inntekthistorikk.Inntekt) {
    private val fom: LocalDate = inntekt["fom"]
    private val hendelse: ArbeidstakerHendelse = inntekt["hendelse"]
    private val beløp: BigDecimal = inntekt["beløp"]

    internal fun toMap():Map<String, Any?> = mapOf(
        "fom" to fom,
        "hendelse" to hendelse.hendelseId(),
        "beløp" to beløp
    )
}
