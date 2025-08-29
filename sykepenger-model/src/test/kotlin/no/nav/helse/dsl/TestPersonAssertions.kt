package no.nav.helse.dsl

import java.time.LocalDate
import no.nav.helse.inspectors.PersonInspektør
import no.nav.helse.inspectors.SubsumsjonInspektør
import no.nav.helse.person.ArbeidstakerOpptjeningView
import no.nav.helse.testhelpers.assertNotNull
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertFalse

internal class TestPersonAssertions(private val personInspektør: PersonInspektør, private val jurist: SubsumsjonsListLog) {

    internal fun assertHarIkkeArbeidsforhold(skjæringstidspunkt: LocalDate, orgnummer: String) {
        val vilkårsgrunnlag = personInspektør.vilkårsgrunnlagHistorikk.grunnlagsdata(skjæringstidspunkt)
        assertNotNull(vilkårsgrunnlag)
        assertFalse((vilkårsgrunnlag.opptjening!! as ArbeidstakerOpptjeningView).arbeidsforhold.any { it.orgnummer == orgnummer })
    }

    internal fun assertHarArbeidsforhold(skjæringstidspunkt: LocalDate, orgnummer: String) {
        val vilkårsgrunnlag = personInspektør.vilkårsgrunnlagHistorikk.grunnlagsdata(skjæringstidspunkt)
        assertNotNull(vilkårsgrunnlag)
        Assertions.assertTrue((vilkårsgrunnlag.opptjening!! as ArbeidstakerOpptjeningView).arbeidsforhold.any { it.orgnummer == orgnummer })
    }

    internal fun <R> assertSubsumsjoner(block: SubsumsjonInspektør.() -> R): R {
        return block(SubsumsjonInspektør(jurist))
    }
}
