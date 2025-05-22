package no.nav.helse.dsl

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.inspectors.ArbeidsgiverInntektsopplysningInspektør
import no.nav.helse.inspectors.GrunnlagsdataInspektør
import no.nav.helse.inspectors.SelvstendigInntektsopplysningInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.Arbeidstakerinntektskilde.AOrdningen
import no.nav.helse.person.inntekt.Arbeidstakerinntektskilde.Arbeidsgiver
import no.nav.helse.person.inntekt.Arbeidstakerinntektskilde.Infotrygd
import no.nav.helse.person.inntekt.InntektsgrunnlagView
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.fail

internal fun ArbeidsgiverInntektsopplysning.assertArbeidsgiverInntektsopplysning(
    forventetFaktaavklartInntekt: Inntekt,
    forventetOmregnetÅrsinntekt: Inntekt = forventetFaktaavklartInntekt,
    forventetFastsattÅrsinntekt: Inntekt = forventetOmregnetÅrsinntekt,
    forventetKorrigertInntekt: Inntekt? = null,
    forventetkilde: Arbeidstakerkilde = Arbeidstakerkilde.Arbeidsgiver
) {
    assertArbeidsgiverInntektsopplysning(inspektør, forventetFaktaavklartInntekt, forventetOmregnetÅrsinntekt, forventetFastsattÅrsinntekt, forventetKorrigertInntekt, forventetkilde)
}

internal fun TestPerson.TestArbeidsgiver.assertInntektsgrunnlag(
    skjæringstidspunkt: LocalDate,
    forventetAntallArbeidsgivere: Int,
    assertBlock: InntektsgrunnlagAssert.() -> Unit
) {
    assertInntektsgrunnlag(inspektør.vilkårsgrunnlag(skjæringstidspunkt)!!.inspektør, forventetAntallArbeidsgivere, assertBlock)
}

internal fun AbstractEndToEndTest.assertInntektsgrunnlag(
    skjæringstidspunkt: LocalDate,
    forventetAntallArbeidsgivere: Int,
    assertBlock: InntektsgrunnlagAssert.() -> Unit
) {
    val grunnlagsdataInspektør = person.inspektør
        .vilkårsgrunnlagHistorikk
        .vilkårsgrunnlagHistorikkInnslag()
        .firstOrNull()
        ?.vilkårsgrunnlag
        ?.firstOrNull { it.skjæringstidspunkt == skjæringstidspunkt }
        ?.inspektør ?: fail { "finner ikke aktivt skjæringstidspunkt $skjæringstidspunkt" }
    assertInntektsgrunnlag(grunnlagsdataInspektør, forventetAntallArbeidsgivere, assertBlock)
}

private fun assertInntektsgrunnlag(
    inspektør: GrunnlagsdataInspektør,
    forventetAntallArbeidsgivere: Int,
    assertBlock: InntektsgrunnlagAssert.() -> Unit
) {
    val inntektsgrunnlag = inspektør.inntektsgrunnlag
    assertEquals(forventetAntallArbeidsgivere, inntektsgrunnlag.arbeidsgiverInntektsopplysninger.size + inntektsgrunnlag.deaktiverteArbeidsforhold.size)
    InntektsgrunnlagAssert(inntektsgrunnlag).apply(assertBlock).assert()
}

internal data class InntektsgrunnlagAssert(val inntektsgrunnlag: InntektsgrunnlagView) {
    internal fun assertBeregningsgrunnlag(beløp: Inntekt) {
        assertEquals(beløp, inntektsgrunnlag.beregningsgrunnlag) { "feil beregningsgrunnlag" }
    }

    internal fun assertSykepengegrunnlag(beløp: Inntekt) {
        assertEquals(beløp, inntektsgrunnlag.sykepengegrunnlag) { "feil sykepengegrunnlag" }
    }

    private var fastsatteÅrsinntekter = mutableListOf<Inntekt>()

    internal fun assertInntektsgrunnlag(
        orgnummer: String,
        forventetFaktaavklartInntekt: Inntekt,
        forventetOmregnetÅrsinntekt: Inntekt = forventetFaktaavklartInntekt,
        forventetFastsattÅrsinntekt: Inntekt = forventetOmregnetÅrsinntekt,
        forventetKorrigertInntekt: Inntekt? = null,
        forventetkilde: Arbeidstakerkilde = Arbeidstakerkilde.Arbeidsgiver,
        forventetKildeId: UUID? = null,
        deaktivert: Boolean = false
    ) {
        if (!deaktivert) fastsatteÅrsinntekter.add(forventetFastsattÅrsinntekt)

        val aktiv = inntektsgrunnlag
            .arbeidsgiverInntektsopplysninger
            .singleOrNull { it.orgnummer == orgnummer }
        val deaktiv = inntektsgrunnlag
            .deaktiverteArbeidsgiverInntektsopplysninger
            .singleOrNull { it.orgnummer == orgnummer }

        val actual = aktiv ?: deaktiv
        assertNotNull(actual)
        assertEquals(deaktivert, aktiv == null) { "forventet at inntekten er ${if (deaktivert) "deaktivert" else "aktivert"}" }
        assertEquals(deaktivert, deaktiv != null) { "forventet at inntekten er ${if (deaktivert) "deaktivert" else "aktivert"}" }
        assertArbeidsgiverInntektsopplysning(actual.inspektør, forventetFaktaavklartInntekt, forventetOmregnetÅrsinntekt, forventetFastsattÅrsinntekt, forventetKorrigertInntekt, forventetkilde, forventetKildeId)
    }

    internal fun assertSelvstendigInntektsgrunnlag(
        forventetFaktaavklartInntekt: Inntekt,
        forventetOmregnetÅrsinntekt: Inntekt = forventetFaktaavklartInntekt,
        forventetFastsattÅrsinntekt: Inntekt = forventetOmregnetÅrsinntekt,
    ) {
        fastsatteÅrsinntekter.add(forventetFastsattÅrsinntekt)

        val selvstendigInntektsopplysning = inntektsgrunnlag
            .selvstendigInntektsopplysning

        val actual = selvstendigInntektsopplysning
        assertNotNull(actual)
        assertSelvstendigInntektsopplysning(
            inspektør = actual.inspektør,
            forventetFaktaavklartInntekt = forventetFaktaavklartInntekt,
            forventetOmregnetÅrsinntekt = forventetOmregnetÅrsinntekt,
            forventetFastsattÅrsinntekt = forventetFastsattÅrsinntekt,
        )
    }

    internal fun assert() {
        assertBeregningsgrunnlag(fastsatteÅrsinntekter.summer())
    }
}

internal enum class Arbeidstakerkilde {
    Arbeidsgiver,
    AOrdningen
}

private fun assertArbeidsgiverInntektsopplysning(
    inspektør: ArbeidsgiverInntektsopplysningInspektør,
    forventetFaktaavklartInntekt: Inntekt,
    forventetOmregnetÅrsinntekt: Inntekt = forventetFaktaavklartInntekt,
    forventetFastsattÅrsinntekt: Inntekt = forventetOmregnetÅrsinntekt,
    forventetKorrigertInntekt: Inntekt? = null,
    forventetkilde: Arbeidstakerkilde = Arbeidstakerkilde.Arbeidsgiver,
    forventetKildeId: UUID? = null
) {
    assertEquals(forventetFaktaavklartInntekt, inspektør.faktaavklartInntekt.inntektsdata.beløp) { "faktaavklart inntekt er feil" }
    assertEquals(forventetOmregnetÅrsinntekt, inspektør.omregnetÅrsinntekt) { "omregnet årsinntekt er feil" }
    assertEquals(forventetFastsattÅrsinntekt, inspektør.fastsattÅrsinntekt) { "fastsatt årsinntekt er feil" }
    assertEquals(forventetKorrigertInntekt, inspektør.korrigertInntekt?.inntektsdata?.beløp) { "korrigert inntekt er feil" }
    assertEquals(
        forventetkilde,
        when (inspektør.faktaavklartInntekt.inntektsopplysningskilde) {
            is Arbeidsgiver -> Arbeidstakerkilde.Arbeidsgiver
            Infotrygd -> Arbeidstakerkilde.Arbeidsgiver
            is AOrdningen -> Arbeidstakerkilde.AOrdningen
        }
    )

    if (forventetKildeId != null) assertEquals(forventetKildeId, inspektør.faktaavklartInntekt.inntektsdata.hendelseId.id)
}

private fun assertSelvstendigInntektsopplysning(
    inspektør: SelvstendigInntektsopplysningInspektør,
    forventetFaktaavklartInntekt: Inntekt,
    forventetOmregnetÅrsinntekt: Inntekt = forventetFaktaavklartInntekt,
    forventetFastsattÅrsinntekt: Inntekt = forventetOmregnetÅrsinntekt
) {
    assertEquals(forventetFaktaavklartInntekt, inspektør.faktaavklartInntekt.inntektsdata.beløp) { "faktaavklart inntekt er feil" }
    assertEquals(forventetOmregnetÅrsinntekt, inspektør.omregnetÅrsinntekt) { "omregnet årsinntekt er feil" }
    assertEquals(forventetFastsattÅrsinntekt, inspektør.fastsattÅrsinntekt) { "fastsatt årsinntekt er feil" }
}

