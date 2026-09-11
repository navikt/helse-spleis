package no.nav.helse.inspectors

import java.time.LocalDate
import no.nav.helse.person.inntekt.Inntektshistorikk
import no.nav.helse.økonomi.Inntekt

internal class InntektshistorikkInspektør(inntektshistorikk: Inntektshistorikk) {

    private val inntektsopplysninger = inntektshistorikk.historikk.map { Opplysning(it.inntektsdata.dato, it.inntektsdata.beløp) }
    internal val size get() = inntektsopplysninger.size
    internal val inntektsdatoer get() = inntektsopplysninger.map { it.dato }

    class Opplysning(
        val dato: LocalDate,
        val sykepengegrunnlag: Inntekt,
    )

    internal fun omregnetÅrsinntekt(dato: LocalDate) = inntektsopplysninger.firstOrNull { it.dato == dato }
}
