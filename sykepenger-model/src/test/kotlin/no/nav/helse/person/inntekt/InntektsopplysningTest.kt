package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.person.inntekt.Inntektsmeldinginntekt.Companion.finnInntektsmeldingForSkjæringstidspunkt
import no.nav.helse.person.inntekt.Skatteopplysning.Inntekttype.LØNNSINNTEKT
import no.nav.helse.yearMonth
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class InntektsopplysningTest {
    private companion object {
        private val INNTEKT = 25000.månedlig
    }

    @Test
    fun `inntektsmelding-likhet`() {
        val hendelsesId = UUID.randomUUID()
        val im1 = Inntektsmeldinginntekt(1.januar, hendelsesId, INNTEKT)
        val im2 = Inntektsmeldinginntekt(1.januar, hendelsesId, INNTEKT)

        assertTrue(im1.funksjoneltLik(im2))
        assertFalse(im1.kanLagres(im2))
        assertFalse(im2.kanLagres(im1))
    }

    @Test
    fun `inntektsmelding-ulikhet`() {
        val im1 = Inntektsmeldinginntekt(1.januar, UUID.randomUUID(), INNTEKT)
        val im2 = Inntektsmeldinginntekt(2.januar, UUID.randomUUID(), INNTEKT)

        assertFalse(im1.funksjoneltLik(im2))
        assertTrue(im1.kanLagres(im2))
        assertTrue(im2.kanLagres(im1))
    }

    @Test
    fun `saksbehandler-likhet`() {
        val ikkeRapportert = IkkeRapportert(1.januar, UUID.randomUUID())
        val saksbehandler1 = Saksbehandler(UUID.randomUUID(), Inntektsdata(UUID.randomUUID(), 20.januar, 20000.månedlig, LocalDateTime.now()), ikkeRapportert)
        val saksbehandler2 = Saksbehandler(UUID.randomUUID(), Inntektsdata(UUID.randomUUID(), 20.januar, 25000.månedlig, LocalDateTime.now()), ikkeRapportert)

        assertFalse(saksbehandler1.funksjoneltLik(saksbehandler2))
    }

    @Test
    fun `turnering - inntektsmelding vs inntektsmelding`() {
        val im1 = Inntektsmeldinginntekt(1.januar, UUID.randomUUID(), INNTEKT, Inntektsmeldinginntekt.Kilde.Arbeidsgiver, LocalDateTime.now())
        val im2 = Inntektsmeldinginntekt(1.januar, UUID.randomUUID(), INNTEKT, Inntektsmeldinginntekt.Kilde.Arbeidsgiver, LocalDateTime.now().plusSeconds(1))

        assertSame(im2, listOf(im1, im2).finnInntektsmeldingForSkjæringstidspunkt(1.januar, null))
    }

    @Test
    fun `turnering - skatt vs inntektsmelding`() {
        val im = Inntektsmeldinginntekt(10.februar, UUID.randomUUID(), INNTEKT)
        val skatt1 = SkatteopplysningerForSykepengegrunnlag(
            arbeidsgiver = "orgnr",
            hendelseId = UUID.randomUUID(),
            skjæringstidspunkt = 1.februar,
            inntektsopplysninger = listOf(
                Skatteopplysning(
                    hendelseId = UUID.randomUUID(),
                    beløp = 25000.månedlig,
                    måned = 1.januar.yearMonth,
                    type = LØNNSINNTEKT,
                    fordel = "",
                    beskrivelse = "",
                    tidsstempel = LocalDateTime.now()
                )
            ),
            ansattPerioder = listOf(
                SkatteopplysningerForSykepengegrunnlag.AnsattPeriode(LocalDate.EPOCH, null),
            ),
            tidsstempel = LocalDateTime.now()
        )
        val skatt2 = SkatteopplysningerForSykepengegrunnlag(
            arbeidsgiver = "orgnr",
            hendelseId = UUID.randomUUID(),
            skjæringstidspunkt = 31.januar,
            inntektsopplysninger = listOf(
                Skatteopplysning(
                    hendelseId = UUID.randomUUID(),
                    beløp = 25000.månedlig,
                    måned = 1.desember(2017).yearMonth,
                    type = LØNNSINNTEKT,
                    fordel = "",
                    beskrivelse = "",
                    tidsstempel = LocalDateTime.now()
                )
            ),
            ansattPerioder = listOf(
                SkatteopplysningerForSykepengegrunnlag.AnsattPeriode(LocalDate.EPOCH, null),
            ),
            tidsstempel = LocalDateTime.now()
        )

        assertSame(im, im.avklarSykepengegrunnlag(skatt1.arbeidstakerInntektsgrunnlag()))
        assertInstanceOf(SkattSykepengegrunnlag::class.java, im.avklarSykepengegrunnlag(skatt2.arbeidstakerInntektsgrunnlag()))
    }
}
