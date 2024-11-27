package no.nav.helse.inspectors

import no.nav.helse.person.inntekt.InntektshistorikkView
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate

internal class InntektshistorikkInspektør(
    inntektshistorikk: InntektshistorikkView
) {
    private val inntektsopplysninger = inntektshistorikk.inntekter.map { Opplysning(it.dato, it.beløp) }
    internal val size get() = inntektsopplysninger.size
    internal val inntektsdatoer get() = inntektsopplysninger.map { it.dato }

    class Opplysning(
        val dato: LocalDate,
        val sykepengegrunnlag: Inntekt
    )

    internal fun omregnetÅrsinntekt(dato: LocalDate) = inntektsopplysninger.firstOrNull { it.dato == dato }
}
