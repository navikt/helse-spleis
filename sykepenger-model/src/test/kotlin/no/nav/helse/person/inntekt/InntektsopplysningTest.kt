package no.nav.helse.person.inntekt

import java.util.UUID
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.person.inntekt.Inntektsopplysning.Companion.lagre
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class InntektsopplysningTest {
    private companion object {
        private val INNTEKT = 25000.månedlig
    }

    @Test
    fun overstyres() {
        val im1 = Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), INNTEKT)
        val im2 = Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), INNTEKT)
        val saksbehandler1 = Saksbehandler(UUID.randomUUID(), 20.januar, UUID.randomUUID(), 20000.månedlig, "", null)
        val saksbehandler2 = Saksbehandler(UUID.randomUUID(), 20.januar, UUID.randomUUID(), 20000.månedlig, "", null)
        val saksbehandler3 = Saksbehandler(UUID.randomUUID(), 20.januar, UUID.randomUUID(), 30000.månedlig, "", null)
        val saksbehandler4 = Saksbehandler(UUID.randomUUID(), 20.januar, UUID.randomUUID(), INGEN, "", null)
        val ikkeRapportert = IkkeRapportert(UUID.randomUUID(), 1.januar)

        assertEquals(saksbehandler1, im1.overstyres(saksbehandler1))
        assertEquals(saksbehandler1, saksbehandler1.overstyres(saksbehandler2))
        assertEquals(saksbehandler3, saksbehandler1.overstyres(saksbehandler3))
        assertEquals(im1, im1.overstyres(im2))
        assertEquals(saksbehandler4, ikkeRapportert.overstyres(saksbehandler4))
        assertEquals(im1, ikkeRapportert.overstyres(im1))
    }

    @Test
    fun `lagre inntektsopplysninger`() {
        val sykepengegrunnlag1 = Skatteopplysning(UUID.randomUUID(), INNTEKT, januar(2018), Skatteopplysning.Inntekttype.LØNNSINNTEKT, "fordel", "beskrivelse")
        val sykepengegrunnlag2 = Skatteopplysning(UUID.randomUUID(), INNTEKT, februar(2018), Skatteopplysning.Inntekttype.LØNNSINNTEKT, "fordel", "beskrivelse")
        val skattComposite1 = SkattSykepengegrunnlag(UUID.randomUUID(), 1.januar, listOf(sykepengegrunnlag1))
        val skattComposite2 = SkattSykepengegrunnlag(UUID.randomUUID(), 1.januar, listOf(sykepengegrunnlag1, sykepengegrunnlag2))

        var list = listOf<Inntektsopplysning>()
        list = skattComposite1.lagre(list)
        assertEquals(1, list.size)
        assertEquals(skattComposite1, list.single())

        list = skattComposite2.lagre(list)
        assertEquals(1, list.size)
        assertEquals(skattComposite2, list.single())

        val im1 = Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), INNTEKT)
        list = im1.lagre(list)
        assertEquals(2, list.size)
        assertEquals(list.first(), skattComposite2)
        assertEquals(list.last(), im1)
    }

    @Test
    fun `skatt composite erstatter andre`() {
        val sykepengegrunnlag1 = Skatteopplysning(UUID.randomUUID(), INNTEKT, januar(2018), Skatteopplysning.Inntekttype.LØNNSINNTEKT, "fordel", "beskrivelse")
        val sykepengegrunnlag2 = Skatteopplysning(UUID.randomUUID(), INNTEKT, februar(2018), Skatteopplysning.Inntekttype.LØNNSINNTEKT, "fordel", "beskrivelse")
        val skattComposite1 = SkattSykepengegrunnlag(UUID.randomUUID(), 1.januar, listOf(sykepengegrunnlag1))
        val skattComposite2 = SkattSykepengegrunnlag(UUID.randomUUID(), 1.januar, listOf(sykepengegrunnlag1, sykepengegrunnlag2))

        assertTrue(skattComposite1.kanLagres(skattComposite2))
        assertTrue(skattComposite1.skalErstattesAv(skattComposite2))
    }

    @Test
    fun `inntektsmelding-likhet`() {
        val im1 = Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), INNTEKT)
        val im2 = Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), INNTEKT)

        assertEquals(im1, im2)
        assertTrue(im1.skalErstattesAv(im2))
        assertTrue(im2.skalErstattesAv(im1))
        assertFalse(im1.kanLagres(im2))
    }

    @Test
    fun `saksbehandler-likhet`() {
        val saksbehandler1 = Saksbehandler(UUID.randomUUID(), 20.januar, UUID.randomUUID(), 20000.månedlig, "", null)
        val saksbehandler2 = Saksbehandler(UUID.randomUUID(), 20.januar, UUID.randomUUID(), 25000.månedlig, "", null)

        assertNotEquals(saksbehandler1, saksbehandler2)
        assertTrue(saksbehandler1.skalErstattesAv(saksbehandler2))
        assertTrue(saksbehandler2.skalErstattesAv(saksbehandler1))
        assertTrue(saksbehandler1.kanLagres(saksbehandler2))
    }
}
