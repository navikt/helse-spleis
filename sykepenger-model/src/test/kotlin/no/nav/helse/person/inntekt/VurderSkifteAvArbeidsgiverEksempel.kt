package no.nav.helse.person.inntekt

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.januar
import no.nav.helse.person.ArbeidstakerOpptjening
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.vurderSkifteAvArbeidsgiver
import no.nav.helse.person.inntekt.Arbeidstakerinntektskilde.Arbeidsgiver
import no.nav.helse.økonomi.Inntekt.Companion.månedlig

/*
    dette er mer "hva i all verden skjer i denne funksjonen da", mer enn en faktisk test av
    korrekt oppførsel. Jeg vet ikke hva korrekt oppførsel er.
 */
fun main() {
    listOf(
        Triple("a1", "a1", "a1"),
        Triple("a1", "a1", "a2"),
        Triple("a1", "a2", "a1"),
        Triple("a1", "a2", "a2"),
    ).forEach { (orgForInntektsopplysning, orgForOpptjeningsgrunnlag, orgForVurderingAvSkifte) ->
        println("($orgForInntektsopplysning, $orgForOpptjeningsgrunnlag, $orgForVurderingAvSkifte) => ${kjørr(orgForInntektsopplysning, orgForOpptjeningsgrunnlag, orgForVurderingAvSkifte)}")
    }
}

private fun kjørr(orgForInntektsopplysning: String, orgForOpptjeningsgrunnlag: String, orgForVurderingAvSkifte: String): Boolean {
    val logg = Aktivitetslogg()
    val opplysninger = lagEnArbeidsgiverInntektsopplysning(orgForInntektsopplysning)
    opplysninger.vurderSkifteAvArbeidsgiver(
        aktivitetslogg = logg, opptjening = lagOpptjening(orgForOpptjeningsgrunnlag), orgnummer = orgForVurderingAvSkifte
    )
    return logg.harVarslerEllerVerre()
}

private fun lagOpptjening(orgForOpptjeningsgrunnlag: String): ArbeidstakerOpptjening = ArbeidstakerOpptjening.nyOpptjening(
    grunnlag = listOf(
        ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag(
            orgForOpptjeningsgrunnlag, listOf(
            ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag.Arbeidsforhold(
                ansattFom = 1.januar(1889), null, deaktivert = false
            )
        )
        )
    ), skjæringstidspunkt = 1.januar
)

private fun lagEnArbeidsgiverInntektsopplysning(orgForInntektsopplysning: String): List<ArbeidsgiverInntektsopplysning> {
    val opplysninger = listOf(
        ArbeidsgiverInntektsopplysning(
            orgnummer = orgForInntektsopplysning, faktaavklartInntekt = ArbeidstakerFaktaavklartInntekt(
            id = UUID.randomUUID(), inntektsdata = Inntektsdata(
            hendelseId = MeldingsreferanseId(UUID.randomUUID()), dato = 1.januar, 30000.månedlig, LocalDateTime.now()
        ), inntektsopplysningskilde = Arbeidsgiver
        ), korrigertInntekt = null, skjønnsmessigFastsatt = null
        )
    )
    return opplysninger
}
