package no.nav.helse.dsl

import java.time.LocalDate
import no.nav.helse.inspectors.ArbeidsgiverInntektsopplysningInspektør
import no.nav.helse.inspectors.GrunnlagsdataInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.AbstractPersonTest
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.Arbeidsgiverinntekt
import no.nav.helse.person.inntekt.Infotrygd
import no.nav.helse.person.inntekt.SkattSykepengegrunnlag
import no.nav.helse.økonomi.Inntekt
import org.junit.jupiter.api.Assertions.assertEquals

internal fun TestPerson.TestArbeidsgiver.assertInntektsgrunnlag(
    skjæringstidspunkt: LocalDate,
    orgnummer: String,
    forventetFaktaavklartInntekt: Inntekt,
    forventetOmregnetÅrsinntekt: Inntekt = forventetFaktaavklartInntekt,
    forventetFastsattÅrsinntekt: Inntekt = forventetOmregnetÅrsinntekt,
    forventetKorrigertInntekt: Inntekt? = null,
    forventetkilde: Arbeidsgiverinntekt.Kilde = Arbeidsgiverinntekt.Kilde.Arbeidsgiver
) {
    assertInntektsgrunnlag(inspektør.vilkårsgrunnlag(skjæringstidspunkt)!!.inspektør, orgnummer, forventetFaktaavklartInntekt, forventetOmregnetÅrsinntekt, forventetFastsattÅrsinntekt, forventetKorrigertInntekt, forventetkilde)
}
internal fun AbstractDslTest.assertInntektsgrunnlag(
    skjæringstidspunkt: LocalDate,
    orgnummer: String,
    forventetFaktaavklartInntekt: Inntekt,
    forventetOmregnetÅrsinntekt: Inntekt = forventetFaktaavklartInntekt,
    forventetFastsattÅrsinntekt: Inntekt = forventetOmregnetÅrsinntekt,
    forventetKorrigertInntekt: Inntekt? = null,
    forventetkilde: Arbeidsgiverinntekt.Kilde = Arbeidsgiverinntekt.Kilde.Arbeidsgiver
) {
    assertInntektsgrunnlag(inspektør(orgnummer).vilkårsgrunnlag(skjæringstidspunkt)!!.inspektør, orgnummer, forventetFaktaavklartInntekt, forventetOmregnetÅrsinntekt, forventetFastsattÅrsinntekt, forventetKorrigertInntekt, forventetkilde)
}
internal fun AbstractPersonTest.assertInntektsgrunnlag(
    skjæringstidspunkt: LocalDate,
    orgnummer: String,
    forventetFaktaavklartInntekt: Inntekt,
    forventetOmregnetÅrsinntekt: Inntekt = forventetFaktaavklartInntekt,
    forventetFastsattÅrsinntekt: Inntekt = forventetOmregnetÅrsinntekt,
    forventetKorrigertInntekt: Inntekt? = null,
    forventetkilde: Arbeidsgiverinntekt.Kilde = Arbeidsgiverinntekt.Kilde.Arbeidsgiver
) {
    assertInntektsgrunnlag(inspektør.vilkårsgrunnlag(skjæringstidspunkt)!!.inspektør, orgnummer, forventetFaktaavklartInntekt, forventetOmregnetÅrsinntekt, forventetFastsattÅrsinntekt, forventetKorrigertInntekt, forventetkilde)
}

internal fun VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement.assertInntektsgrunnlag(
    orgnummer: String,
    forventetFaktaavklartInntekt: Inntekt,
    forventetOmregnetÅrsinntekt: Inntekt = forventetFaktaavklartInntekt,
    forventetFastsattÅrsinntekt: Inntekt = forventetOmregnetÅrsinntekt,
    forventetKorrigertInntekt: Inntekt? = null,
    forventetkilde: Arbeidsgiverinntekt.Kilde = Arbeidsgiverinntekt.Kilde.Arbeidsgiver
) {
    assertInntektsgrunnlag(inspektør, orgnummer, forventetFaktaavklartInntekt, forventetOmregnetÅrsinntekt, forventetFastsattÅrsinntekt, forventetKorrigertInntekt, forventetkilde)
}
internal fun ArbeidsgiverInntektsopplysning.assertInntektsgrunnlag(
    forventetFaktaavklartInntekt: Inntekt,
    forventetOmregnetÅrsinntekt: Inntekt = forventetFaktaavklartInntekt,
    forventetFastsattÅrsinntekt: Inntekt = forventetOmregnetÅrsinntekt,
    forventetKorrigertInntekt: Inntekt? = null,
    forventetkilde: Arbeidsgiverinntekt.Kilde = Arbeidsgiverinntekt.Kilde.Arbeidsgiver
) {
    assertInntektsgrunnlag(inspektør, forventetFaktaavklartInntekt, forventetOmregnetÅrsinntekt, forventetFastsattÅrsinntekt, forventetKorrigertInntekt, forventetkilde)
}

internal fun assertInntektsgrunnlag(
    inspektør: GrunnlagsdataInspektør,
    orgnummer: String,
    forventetFaktaavklartInntekt: Inntekt,
    forventetOmregnetÅrsinntekt: Inntekt = forventetFaktaavklartInntekt,
    forventetFastsattÅrsinntekt: Inntekt = forventetOmregnetÅrsinntekt,
    forventetKorrigertInntekt: Inntekt? = null,
    forventetkilde: Arbeidsgiverinntekt.Kilde = Arbeidsgiverinntekt.Kilde.Arbeidsgiver
) {
    val actual = inspektør
        .inntektsgrunnlag
        .arbeidsgiverInntektsopplysninger
        .single { it.orgnummer == orgnummer }

    assertInntektsgrunnlag(actual.inspektør, forventetFaktaavklartInntekt, forventetOmregnetÅrsinntekt, forventetFastsattÅrsinntekt, forventetKorrigertInntekt, forventetkilde)
}


internal fun assertInntektsgrunnlag(
    inspektør: ArbeidsgiverInntektsopplysningInspektør,
    forventetFaktaavklartInntekt: Inntekt,
    forventetOmregnetÅrsinntekt: Inntekt = forventetFaktaavklartInntekt,
    forventetFastsattÅrsinntekt: Inntekt = forventetOmregnetÅrsinntekt,
    forventetKorrigertInntekt: Inntekt? = null,
    forventetkilde: Arbeidsgiverinntekt.Kilde = Arbeidsgiverinntekt.Kilde.Arbeidsgiver
) {
    assertEquals(forventetFaktaavklartInntekt, inspektør.faktaavklartInntekt.inntektsdata.beløp) { "faktaavklart inntekt er feil" }
    assertEquals(forventetOmregnetÅrsinntekt, inspektør.omregnetÅrsinntekt) { "omregnet årsinntekt er feil" }
    assertEquals(forventetFastsattÅrsinntekt, inspektør.fastsattÅrsinntekt) { "fastsatt årsinntekt er feil" }
    assertEquals(forventetKorrigertInntekt, inspektør.korrigertInntekt?.inntektsdata?.beløp) { "korrigert inntekt er feil" }
    assertEquals(forventetkilde, when (inspektør.faktaavklartInntekt.inntektsopplysning) {
        is Arbeidsgiverinntekt -> inspektør.faktaavklartInntekt.inntektsopplysning.kilde
        Infotrygd -> Arbeidsgiverinntekt.Kilde.Arbeidsgiver
        is SkattSykepengegrunnlag -> Arbeidsgiverinntekt.Kilde.AOrdningen
    })
}
