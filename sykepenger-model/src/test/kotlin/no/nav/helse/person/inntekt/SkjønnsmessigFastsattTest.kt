package no.nav.helse.person.inntekt

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SkjønnsmessigFastsattTest {
    
    @Test
    fun `inntektsmelding blir overskrevet av skjønnsmessig fastsettelse`() {
        val skjønnsmessigFastsattBeløp = 5000.daglig
        val originalBeløp = 2500.daglig
        val original = Inntektsmelding(1.januar, UUID.randomUUID(), originalBeløp)
        val skjønnsmessigFastsatt = original.overstyresAv(SkjønnsmessigFastsatt(1.januar, UUID.randomUUID(), skjønnsmessigFastsattBeløp, LocalDateTime.now()))

        assertEquals(skjønnsmessigFastsattBeløp, skjønnsmessigFastsatt.fastsattÅrsinntekt())
        assertEquals(originalBeløp, skjønnsmessigFastsatt.omregnetÅrsinntekt())
    }

    @Test
    fun `saksbehandlerinntekt blir overskrevet av skjønnsmessig fastsettelse`() {
        val skjønnsmessigFastsattBeløp = 5000.daglig
        val originalBeløp = 2500.daglig
        val saksbehandlerbeløp = 2600.daglig
        val original = Inntektsmelding(1.januar, UUID.randomUUID(), originalBeløp)
        val saksbehandler = original.overstyresAv(Saksbehandler(1.januar, UUID.randomUUID(), saksbehandlerbeløp, "", null, LocalDateTime.now()))
        val skjønnsmessigFastsatt = saksbehandler.overstyresAv(SkjønnsmessigFastsatt(1.januar, UUID.randomUUID(), skjønnsmessigFastsattBeløp, LocalDateTime.now()))

        assertTrue(skjønnsmessigFastsatt is SkjønnsmessigFastsatt)
        assertTrue(saksbehandler is Saksbehandler)
        assertEquals(skjønnsmessigFastsattBeløp, skjønnsmessigFastsatt.fastsattÅrsinntekt())
        assertEquals(saksbehandlerbeløp, skjønnsmessigFastsatt.omregnetÅrsinntekt())
    }

    @Test
    fun `skjønnsmessig fastsettelse blir overskrevet av skjønnsmessig fastsettelse`() {
        val skjønnsmessigFastsattBeløp1 = 5000.daglig
        val skjønnsmessigFastsattBeløp2 = 5001.daglig
        val originalBeløp = 2500.daglig
        val original = Inntektsmelding(1.januar, UUID.randomUUID(), originalBeløp)
        val skjønnsmessigFastsatt1 = original.overstyresAv(SkjønnsmessigFastsatt(1.januar, UUID.randomUUID(), skjønnsmessigFastsattBeløp1, LocalDateTime.now()))
        val skjønnsmessigFastsatt2 = skjønnsmessigFastsatt1.overstyresAv(SkjønnsmessigFastsatt(1.januar, UUID.randomUUID(), skjønnsmessigFastsattBeløp2, LocalDateTime.now()))

        assertTrue(skjønnsmessigFastsatt1 is SkjønnsmessigFastsatt)
        assertTrue(skjønnsmessigFastsatt2 is SkjønnsmessigFastsatt)
        assertEquals(skjønnsmessigFastsattBeløp2, skjønnsmessigFastsatt2.fastsattÅrsinntekt())
        assertEquals(originalBeløp, skjønnsmessigFastsatt2.omregnetÅrsinntekt())
    }
}