package no.nav.helse.person

import java.util.UUID
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.person.Inntektshistorikk.Inntektsopplysning.Companion.lagre
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class InntektsopplysningTest {
    private companion object {
        private val INNTEKT = 25000.månedlig
    }

    @Test
    fun `lagre inntektsopplysninger`() {
        val sykepengegrunnlag1 = Inntektshistorikk.Skatt.Sykepengegrunnlag(1.januar, UUID.randomUUID(), INNTEKT, januar(2018), Inntektshistorikk.Skatt.Inntekttype.LØNNSINNTEKT, "fordel", "beskrivelse")
        val sykepengegrunnlag2 = Inntektshistorikk.Skatt.Sykepengegrunnlag(1.februar, UUID.randomUUID(), INNTEKT, februar(2018), Inntektshistorikk.Skatt.Inntekttype.LØNNSINNTEKT, "fordel", "beskrivelse")
        val skattComposite1 = Inntektshistorikk.SkattComposite(UUID.randomUUID(), listOf(sykepengegrunnlag1))
        val skattComposite2 = Inntektshistorikk.SkattComposite(UUID.randomUUID(), listOf(sykepengegrunnlag1, sykepengegrunnlag2))

        var list = listOf<Inntektshistorikk.Inntektsopplysning>()
        list = skattComposite1.lagre(list)
        assertEquals(1, list.size)
        assertEquals(skattComposite1, list.single())

        list = skattComposite2.lagre(list)
        assertEquals(1, list.size)
        assertEquals(skattComposite2, list.single())

        val im1 = Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), INNTEKT)
        list = im1.lagre(list)
        assertEquals(2, list.size)
        assertEquals(list.first(), skattComposite2)
        assertEquals(list.last(), im1)
    }

    @Test
    fun `skatt composite erstatter andre`() {
        val sykepengegrunnlag1 = Inntektshistorikk.Skatt.Sykepengegrunnlag(1.januar, UUID.randomUUID(), INNTEKT, januar(2018), Inntektshistorikk.Skatt.Inntekttype.LØNNSINNTEKT, "fordel", "beskrivelse")
        val sykepengegrunnlag2 = Inntektshistorikk.Skatt.Sykepengegrunnlag(1.februar, UUID.randomUUID(), INNTEKT, februar(2018), Inntektshistorikk.Skatt.Inntekttype.LØNNSINNTEKT, "fordel", "beskrivelse")
        val skattComposite1 = Inntektshistorikk.SkattComposite(UUID.randomUUID(), listOf(sykepengegrunnlag1))
        val skattComposite2 = Inntektshistorikk.SkattComposite(UUID.randomUUID(), listOf(sykepengegrunnlag1, sykepengegrunnlag2))

        assertTrue(skattComposite1.kanLagres(skattComposite2))
        assertTrue(sykepengegrunnlag1.skalErstattesAv(sykepengegrunnlag1))
        assertTrue(skattComposite1.skalErstattesAv(skattComposite2))
    }

    @Test
    fun `kan ha samme skatteinntekt i en skattcomposite`() {
        val innslag = Inntektshistorikk.InnslagBuilder().build {
            addSkattSykepengegrunnlag(1.januar, UUID.randomUUID(), INNTEKT, januar(2018), Inntektshistorikk.Skatt.Inntekttype.LØNNSINNTEKT, "fordel", "beskrivelse")
            addSkattSykepengegrunnlag(1.januar, UUID.randomUUID(), INNTEKT, januar(2018), Inntektshistorikk.Skatt.Inntekttype.LØNNSINNTEKT, "fordel", "beskrivelse")
        }

        val forventet = Inntektshistorikk.Innslag(listOf(
            Inntektshistorikk.SkattComposite(UUID.randomUUID(), listOf(
                Inntektshistorikk.Skatt.Sykepengegrunnlag(1.januar, UUID.randomUUID(), INNTEKT, januar(2018), Inntektshistorikk.Skatt.Inntekttype.LØNNSINNTEKT, "fordel", "beskrivelse"),
                Inntektshistorikk.Skatt.Sykepengegrunnlag(1.januar, UUID.randomUUID(), INNTEKT, januar(2018), Inntektshistorikk.Skatt.Inntekttype.LØNNSINNTEKT, "fordel", "beskrivelse"),
            )
        )))

        assertEquals(innslag, forventet)
        assertNull(innslag + forventet) { "skal gi null når innslagene er identiske" }
    }

    @Test
    fun `inntektsmelding-likhet`() {
        val im1 = Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), INNTEKT)
        val im2 = Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), INNTEKT)

        assertEquals(im1, im2)
        assertTrue(im1.skalErstattesAv(im2))
        assertTrue(im2.skalErstattesAv(im1))
        assertFalse(im1.kanLagres(im2))
    }

    @Test
    fun `saksbehandler-likhet`() {
        val saksbehandler1 = Inntektshistorikk.Saksbehandler(UUID.randomUUID(), 20.januar, UUID.randomUUID(), 20000.månedlig)
        val saksbehandler2 = Inntektshistorikk.Saksbehandler(UUID.randomUUID(), 20.januar, UUID.randomUUID(), 25000.månedlig)

        assertNotEquals(saksbehandler1, saksbehandler2)
        assertTrue(saksbehandler1.skalErstattesAv(saksbehandler2))
        assertTrue(saksbehandler2.skalErstattesAv(saksbehandler1))
        assertTrue(saksbehandler1.kanLagres(saksbehandler2))
    }
}
