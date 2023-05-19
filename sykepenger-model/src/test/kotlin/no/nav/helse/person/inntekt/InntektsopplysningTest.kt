package no.nav.helse.person.inntekt

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class InntektsopplysningTest {
    private companion object {
        private val INNTEKT = 25000.månedlig
    }

    @Test
    fun overstyres() {
        val im1 = Inntektsmelding(1.januar, UUID.randomUUID(), INNTEKT, LocalDateTime.now())
        val im2 = Inntektsmelding(1.januar, UUID.randomUUID(), INNTEKT, LocalDateTime.now())
        val saksbehandler1 = im1.overstyresAv(Saksbehandler(20.januar, UUID.randomUUID(), 20000.månedlig, "", null, LocalDateTime.now()))
        val saksbehandler2 = Saksbehandler(20.januar, UUID.randomUUID(), 20000.månedlig, "", null, LocalDateTime.now())
        val saksbehandler3 = Saksbehandler(20.januar, UUID.randomUUID(), 30000.månedlig, "", null, LocalDateTime.now())
        val saksbehandler4 = Saksbehandler(20.januar, UUID.randomUUID(), INGEN, "", null, LocalDateTime.now())
        val ikkeRapportert = IkkeRapportert(1.januar, UUID.randomUUID(), LocalDateTime.now())

        assertEquals(saksbehandler1, im1.overstyresAv(saksbehandler1))
        assertEquals(saksbehandler1, saksbehandler1.overstyresAv(saksbehandler2))
        assertEquals(saksbehandler3, saksbehandler1.overstyresAv(saksbehandler3))
        assertEquals(im1, im1.overstyresAv(im2))
        assertEquals(saksbehandler4, ikkeRapportert.overstyresAv(saksbehandler4))
        assertEquals(im1, ikkeRapportert.overstyresAv(im1))
    }


    @Test
    fun `inntektsmelding-likhet`() {
        val hendelsesId = UUID.randomUUID()
        val im1 = Inntektsmelding(1.januar, hendelsesId, INNTEKT, LocalDateTime.now())
        val im2 = Inntektsmelding(1.januar, hendelsesId, INNTEKT, LocalDateTime.now())

        assertEquals(im1, im2)
        assertFalse(im1.kanLagres(im2))
        assertFalse(im2.kanLagres(im1))
    }


    @Test
    fun `inntektsmelding-ulikhet`() {
        val im1 = Inntektsmelding(1.januar, UUID.randomUUID(), INNTEKT, LocalDateTime.now())
        val im2 = Inntektsmelding(2.januar, UUID.randomUUID(), INNTEKT, LocalDateTime.now())

        assertNotEquals(im1, im2)
        assertTrue(im1.kanLagres(im2))
        assertTrue(im2.kanLagres(im1))
    }

    @Test
    fun `saksbehandler-likhet`() {
        val saksbehandler1 = Saksbehandler(20.januar, UUID.randomUUID(), 20000.månedlig, "", null, LocalDateTime.now())
        val saksbehandler2 = Saksbehandler(20.januar, UUID.randomUUID(), 25000.månedlig, "", null, LocalDateTime.now())

        assertNotEquals(saksbehandler1, saksbehandler2)
    }

    @Test
    fun `turnering - inntektsmelding vs inntektsmelding`() {
        val im1 = Inntektsmelding(1.januar, UUID.randomUUID(), INNTEKT, LocalDateTime.now())
        val im2 = Inntektsmelding(1.januar, UUID.randomUUID(), INNTEKT, LocalDateTime.now().plusSeconds(1))

        assertEquals(im2, im1.beste(im2))
        assertEquals(im2, im2.beste(im1))
    }

    @Test
    fun `turnering - skatt vs inntektsmelding`() {
        val im = Inntektsmelding(10.februar, UUID.randomUUID(), INNTEKT, LocalDateTime.now())
        val skatt1 = SkattSykepengegrunnlag(UUID.randomUUID(), 1.februar, emptyList(), emptyList())
        val skatt2 = SkattSykepengegrunnlag(UUID.randomUUID(), 31.januar, emptyList(), emptyList())

        assertEquals(im, im.beste(skatt1))
        assertEquals(im, skatt1.beste(im))

        assertEquals(skatt2, im.beste(skatt2))
        assertEquals(skatt2, skatt2.beste(im))
    }

    @Test
    fun `turnering - ikkeRapportert vs inntektsmelding`() {
        val im = Inntektsmelding(10.februar, UUID.randomUUID(), INNTEKT, LocalDateTime.now())
        val ikkeRapportert1 = IkkeRapportert(1.februar, UUID.randomUUID(), LocalDateTime.now())
        val ikkeRapportert2 = IkkeRapportert(31.januar, UUID.randomUUID(), LocalDateTime.now())

        assertEquals(im, im.beste(ikkeRapportert1))
        assertEquals(im, ikkeRapportert1.beste(im))

        assertEquals(ikkeRapportert2, ikkeRapportert2.beste(im))
        assertEquals(ikkeRapportert2, im.beste(ikkeRapportert2))
    }

    @Test
    fun `turnering - ikkeRapportert vs skatt`() {
        val skatt = SkattSykepengegrunnlag(UUID.randomUUID(), 1.februar, emptyList(), emptyList())
        val ikkeRapportert = IkkeRapportert(31.januar, UUID.randomUUID(), LocalDateTime.now())

        assertThrows<IllegalStateException> { assertEquals(skatt, skatt.beste(ikkeRapportert)) }
        assertThrows<IllegalStateException> { assertEquals(skatt, ikkeRapportert.beste(skatt)) }
    }

    @Test
    fun `turnering - skatt vs skatt`() {
        val skatt1 = SkattSykepengegrunnlag(UUID.randomUUID(), 1.februar, emptyList(), emptyList())
        val skatt2 = SkattSykepengegrunnlag(UUID.randomUUID(), 1.februar, emptyList(), emptyList())

        assertThrows<IllegalStateException> { skatt1.beste(skatt2) }
        assertThrows<IllegalStateException> { skatt2.beste(skatt1) }
    }

    @Test
    fun `turnering - ikkeRapportert vs ikkeRapportert`() {
        val ikkeRapportert1 = IkkeRapportert(31.januar, UUID.randomUUID(), LocalDateTime.now())
        val ikkeRapportert2 = IkkeRapportert(31.januar, UUID.randomUUID(), LocalDateTime.now())

        assertThrows<IllegalStateException> { ikkeRapportert1.beste(ikkeRapportert2) }
        assertThrows<IllegalStateException> { ikkeRapportert2.beste(ikkeRapportert1) }
    }
}
