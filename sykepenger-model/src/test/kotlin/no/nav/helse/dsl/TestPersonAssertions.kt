package no.nav.helse.dsl

import no.nav.helse.inspectors.PersonInspektør
import no.nav.helse.inspectors.SubsumsjonInspektør
import no.nav.helse.inspectors.inspektør
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.LocalDate

internal class TestPersonAssertions(
    private val personInspektør: PersonInspektør,
    private val jurist: SubsumsjonsListLog,
) {
    internal fun assertArbeidsgivereISykepengegrunnlag(
        skjæringstidspunkt: LocalDate,
        vararg arbeidsgivere: String,
    ) {
        assertEquals(
            arbeidsgivere.toSet(),
            personInspektør.vilkårsgrunnlagHistorikk
                .grunnlagsdata(
                    skjæringstidspunkt,
                ).inspektør.inntektsgrunnlag.inspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.keys,
        )
    }

    internal fun <R> assertSubsumsjoner(block: SubsumsjonInspektør.() -> R): R = block(SubsumsjonInspektør(jurist))
}
