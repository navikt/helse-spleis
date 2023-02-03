package no.nav.helse.dsl

import java.time.LocalDate
import no.nav.helse.inspectors.PersonInspektør
import no.nav.helse.inspectors.SubsumsjonInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.etterlevelse.MaskinellJurist
import org.junit.jupiter.api.Assertions.assertEquals

internal class TestPersonAssertions(private val personInspektør: PersonInspektør, private val jurist: MaskinellJurist) {
    internal fun assertArbeidsgivereISykepengegrunnlag(skjæringstidspunkt: LocalDate, vararg arbeidsgivere: String) {
        assertEquals(
            arbeidsgivere.toSet(),
            personInspektør.vilkårsgrunnlagHistorikk.inspektør.grunnlagsdata(skjæringstidspunkt).inspektør.sykepengegrunnlag.inspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.keys
        )
    }

    internal fun <R> assertSubsumsjoner(block: SubsumsjonInspektør.() -> R): R {
        return block(SubsumsjonInspektør(jurist))
    }
}