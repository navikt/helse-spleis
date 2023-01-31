package no.nav.helse.person.inntekt

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.januar
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
        val im1 = Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), INNTEKT, LocalDateTime.now())
        val im2 = Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), INNTEKT, LocalDateTime.now())
        val saksbehandler1 = Saksbehandler(20.januar, UUID.randomUUID(), 20000.månedlig, "", null, LocalDateTime.now())
        val saksbehandler2 = Saksbehandler(20.januar, UUID.randomUUID(), 20000.månedlig, "", null, LocalDateTime.now())
        val saksbehandler3 = Saksbehandler(20.januar, UUID.randomUUID(), 30000.månedlig, "", null, LocalDateTime.now())
        val saksbehandler4 = Saksbehandler(20.januar, UUID.randomUUID(), INGEN, "", null, LocalDateTime.now())
        val ikkeRapportert = IkkeRapportert(UUID.randomUUID(), 1.januar, LocalDateTime.now())

        assertEquals(saksbehandler1, im1.overstyres(saksbehandler1))
        assertEquals(saksbehandler1, saksbehandler1.overstyres(saksbehandler2))
        assertEquals(saksbehandler3, saksbehandler1.overstyres(saksbehandler3))
        assertEquals(im1, im1.overstyres(im2))
        assertEquals(saksbehandler4, ikkeRapportert.overstyres(saksbehandler4))
        assertEquals(im1, ikkeRapportert.overstyres(im1))
    }


    @Test
    fun `inntektsmelding-likhet`() {
        val im1 = Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), INNTEKT, LocalDateTime.now())
        val im2 = Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), INNTEKT, LocalDateTime.now())

        assertEquals(im1, im2)
        assertFalse(im1.kanLagres(im2))
        assertFalse(im2.kanLagres(im1))
    }


    @Test
    fun `inntektsmelding-ulikhet`() {
        val im1 = Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), INNTEKT, LocalDateTime.now())
        val im2 = Inntektsmelding(UUID.randomUUID(), 2.januar, UUID.randomUUID(), INNTEKT, LocalDateTime.now())

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
}
