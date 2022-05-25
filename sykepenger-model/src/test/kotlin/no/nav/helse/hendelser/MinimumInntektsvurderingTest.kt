package no.nav.helse.hendelser

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Fødselsnummer
import no.nav.helse.april
import no.nav.helse.mai
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.somFødselsnummer
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class MinimumInntektsvurderingTest {

    private companion object {
        private val UNG = "01026000014".somFødselsnummer()
        private val GAMMEL = "01025400065".somFødselsnummer()
    }

    private val aktivitetslogg get() = Aktivitetslogg()

    @Test
    fun `Validering ok hvis inntekt er lik en halv G for ung person`() {
        assertHarMinimumInntekt(
            skjæringstidspunkt = 1.mai(2021),
            beløp = 53199.5.årlig,
            fødselsnummer = UNG
        )
    }

    @Test
    fun `Validering ok hvis inntekt er større enn en halv G for ung person`() {
        assertHarMinimumInntekt(
            skjæringstidspunkt = 1.mai(2021),
            beløp = 53199.6.årlig,
            fødselsnummer = UNG
        )
    }

    @Test
    fun `Valider inntekt høyere enn en halv G for ung person etter nytt grunnbeløp men før virkningstidspunkt for minimuminntekt`() {
        assertHarMinimumInntekt(
            skjæringstidspunkt = 23.mai(2021),
            beløp = 53199.4.årlig,
            fødselsnummer = UNG
        )
    }

    @Test
    fun `Validering feiler hvis inntekt er lavere enn en halv G for ung person etter virkningstidspunkt for minimuminntekt`() {
        assertHarIkkeMinimumInntekt(
            skjæringstidspunkt = 24.mai(2021),
            beløp = 53199.4.årlig,
            fødselsnummer = UNG
        )
    }

    @Test
    fun `Validering ok hvis inntekt er lik en halv G for ung person med gammelt grunnbeløp`() {
        assertHarMinimumInntekt(
            skjæringstidspunkt = 1.april(2021),
            beløp =  50675.5.årlig,
            fødselsnummer = UNG
        )
    }

    @Test
    fun `Validering ok hvis inntekt er større enn en halv G for ung person med gammelt grunnbeløp`() {
        assertHarMinimumInntekt(
            skjæringstidspunkt = 1.april(2021),
            beløp =  50675.6.årlig,
            fødselsnummer = UNG
        )
    }

    @Test
    fun `Validering feiler hvis inntekt er lavere enn en halv G for ung person med gammelt grunnbeløp`() {
        assertHarIkkeMinimumInntekt(
            skjæringstidspunkt = 1.april(2021),
            beløp =  50675.4.årlig,
            fødselsnummer = UNG
        )
    }

    @Test
    fun `Validering ok hvis inntekt er lik to G for gammel person`() {
        assertHarMinimumInntekt(
            skjæringstidspunkt = 1.mai(2021),
            beløp = 212798.0.årlig,
            fødselsnummer = GAMMEL
        )
    }

    @Test
    fun `Validering ok hvis inntekt er større enn to G for gammel person`() {
        assertHarMinimumInntekt(
            skjæringstidspunkt = 1.mai(2021),
            beløp = 212798.1.årlig,
            fødselsnummer = GAMMEL
        )
    }

    @Test
    fun `Validerin inntekt over to G for gammel person etter nytt grunnbeløp men før virkningstidspunkt for minimuminntekt`() {
        assertHarMinimumInntekt(
            skjæringstidspunkt = 23.mai(2021),
            beløp = 212797.9.årlig,
            fødselsnummer = GAMMEL
        )
    }

    @Test
    fun `Validering feiler hvis inntekt er lavere enn to G for gammel person etter virkningstidspunkt for minimuminntekt`() {
        assertHarIkkeMinimumInntekt(
            skjæringstidspunkt = 24.mai(2021),
            beløp = 212797.9.årlig,
            fødselsnummer = GAMMEL
        )
    }

    private fun assertHarMinimumInntekt(
        skjæringstidspunkt: LocalDate,
        beløp: Inntekt,
        fødselsnummer: Fødselsnummer
    ) {
        val aktivitetslogg = this.aktivitetslogg
        assertTrue(
            validerMinimumInntekt(
                aktivitetslogg = aktivitetslogg,
                fødselsnummer = fødselsnummer,
                grunnlagForSykepengegrunnlag = sykepengegrunnlag(skjæringstidspunkt, beløp),
                MaskinellJurist()
            )
        )
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    private fun assertHarIkkeMinimumInntekt(
        skjæringstidspunkt: LocalDate,
        beløp: Inntekt,
        fødselsnummer: Fødselsnummer
    ) {
        val aktivitetslogg = this.aktivitetslogg
        assertFalse(
            validerMinimumInntekt(
                aktivitetslogg = aktivitetslogg,
                fødselsnummer = fødselsnummer,
                grunnlagForSykepengegrunnlag = sykepengegrunnlag(skjæringstidspunkt, beløp),
                MaskinellJurist()
            )
        )
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
    }

    private fun sykepengegrunnlag(
        skjæringstidspunkt: LocalDate,
        beløp: Inntekt
    ) = Sykepengegrunnlag.opprett(
        arbeidsgiverInntektsopplysninger = listOf(
            ArbeidsgiverInntektsopplysning(
                orgnummer = "123456789",
                inntektsopplysning = Inntektshistorikk.Inntektsmelding(
                    id = UUID.randomUUID(),
                    dato = 1.mai(2021),
                    hendelseId = UUID.randomUUID(),
                    beløp = beløp
                )
            )
        ),
        skjæringstidspunkt = skjæringstidspunkt,
        subsumsjonObserver = MaskinellJurist(),
        deaktiverteArbeidsforhold = emptyList()
    )
}
