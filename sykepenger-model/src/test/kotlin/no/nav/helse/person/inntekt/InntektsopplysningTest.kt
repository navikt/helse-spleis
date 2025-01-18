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
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
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
    fun overstyres() {
        val im1 = Inntektsmeldinginntekt(1.januar, UUID.randomUUID(), INNTEKT)
        val im2 = Inntektsmeldinginntekt(1.januar, UUID.randomUUID(), INNTEKT)
        val saksbehandler1 = im1.overstyresAv(Saksbehandler(20.januar, UUID.randomUUID(), 20000.månedlig, LocalDateTime.now()))
        val saksbehandler2 = Saksbehandler(20.januar, UUID.randomUUID(), 20000.månedlig, LocalDateTime.now())
        val saksbehandler3 = Saksbehandler(20.januar, UUID.randomUUID(), 30000.månedlig, LocalDateTime.now())
        val saksbehandler4 = Saksbehandler(20.januar, UUID.randomUUID(), INGEN, LocalDateTime.now())
        val ikkeRapportert = IkkeRapportert(1.januar, UUID.randomUUID())

        assertTrue(saksbehandler1.funksjoneltLik(im1.overstyresAv(saksbehandler1)))
        assertSame(saksbehandler1, saksbehandler1.overstyresAv(saksbehandler2))
        assertTrue(saksbehandler3.funksjoneltLik(saksbehandler1.overstyresAv(saksbehandler3)))
        assertTrue(im1.funksjoneltLik(im1.overstyresAv(im2)))
        assertSame(ikkeRapportert, ikkeRapportert.overstyresAv(saksbehandler4))
        assertTrue(saksbehandler1.funksjoneltLik(ikkeRapportert.overstyresAv(saksbehandler1)))
        assertSame(im1, ikkeRapportert.overstyresAv(im1))
    }

    @Test
    fun `opphøre, gjøre om, avvikle, tilbakestille eller kansellere`() {
        val im = Inntektsmeldinginntekt(1.januar, UUID.randomUUID(), INNTEKT)
        val saksbehandler = im.overstyresAv(Saksbehandler(20.januar, UUID.randomUUID(), 20000.månedlig, LocalDateTime.now()))
        val skjønnsmessigFastsatt1 = im.overstyresAv(SkjønnsmessigFastsatt(1.januar, UUID.randomUUID(), 20000.månedlig, LocalDateTime.now()))
        val skjønnsmessigFastsatt2 = saksbehandler.overstyresAv(SkjønnsmessigFastsatt(1.januar, UUID.randomUUID(), 20000.månedlig, LocalDateTime.now()))
        val skjønnsmessigFastsatt3 = skjønnsmessigFastsatt2.overstyresAv(SkjønnsmessigFastsatt(1.januar, UUID.randomUUID(), 20000.månedlig, LocalDateTime.now()))
        val ikkeRapportert = IkkeRapportert(1.januar, UUID.randomUUID())
        val skatt = skattSykepengegrunnlag(UUID.randomUUID(), 1.februar, emptyList(), emptyList())

        assertSame(im, im.omregnetÅrsinntekt())
        assertSame(saksbehandler, saksbehandler.omregnetÅrsinntekt())
        assertSame(im, skjønnsmessigFastsatt1.omregnetÅrsinntekt())
        assertSame(saksbehandler, skjønnsmessigFastsatt2.omregnetÅrsinntekt())
        assertSame(saksbehandler, skjønnsmessigFastsatt3.omregnetÅrsinntekt())
        assertSame(ikkeRapportert, ikkeRapportert.omregnetÅrsinntekt())
        assertSame(skatt, skatt.omregnetÅrsinntekt())
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
        val saksbehandler1 = Saksbehandler(20.januar, UUID.randomUUID(), 20000.månedlig, LocalDateTime.now())
        val saksbehandler2 = Saksbehandler(20.januar, UUID.randomUUID(), 25000.månedlig, LocalDateTime.now())

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

    private fun skattSykepengegrunnlag(
        hendelseId: UUID,
        dato: LocalDate,
        inntektsopplysninger: List<Skatteopplysning>,
        ansattPerioder: List<AnsattPeriode>
    ) =
        SkattSykepengegrunnlag(UUID.randomUUID(), Inntektsdata(hendelseId, dato, Skatteopplysning.omregnetÅrsinntekt(inntektsopplysninger), LocalDateTime.now()), inntektsopplysninger, ansattPerioder)
}
