package no.nav.helse.serde.reflection

import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Inntekthistorikk
import java.math.BigDecimal
import java.time.LocalDate

internal class InntektReflect(inntekt: Inntekthistorikk.Inntekt) {
    private val fom: LocalDate = inntekt.getProp("fom")
    private val hendelse: ArbeidstakerHendelse = inntekt.getProp("hendelse")
    private val beløp: BigDecimal = inntekt.getProp("beløp")

    internal fun toMap():Map<String, Any?> = mapOf(
        "fom" to fom,
        "hendelse" to hendelse.hendelseId(),
        "beløp" to beløp
    )
}
