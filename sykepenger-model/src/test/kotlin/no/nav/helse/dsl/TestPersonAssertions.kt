package no.nav.helse.dsl

import java.time.LocalDate
import no.nav.helse.inspectors.PersonInspektør
import no.nav.helse.inspectors.inspektør
import org.junit.jupiter.api.Assertions.assertEquals

internal class TestPersonAssertions(private val personInspektør: PersonInspektør) {
    internal fun assertArbeidsgivereISykepengegrunnlag(skjæringstidspunkt: LocalDate, vararg arbeidsgivere: String) {
        assertEquals(
            arbeidsgivere.toSet(),
            personInspektør.grunnlagsdata(skjæringstidspunkt).inspektør.sykepengegrunnlag.inspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.keys
        )
    }
}