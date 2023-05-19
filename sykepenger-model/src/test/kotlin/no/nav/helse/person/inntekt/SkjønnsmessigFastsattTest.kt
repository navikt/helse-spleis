package no.nav.helse.person.inntekt

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class SkjønnsmessigFastsattTest {
    
    @Test
    fun `fastsatt årsinntekt - inntektsmelding`() {
        val skjønnsmessigFastsattBeløp = 5000.daglig
        val originalBeløp = 2500.daglig
        val original = Inntektsmelding(1.januar, UUID.randomUUID(), originalBeløp)
        val skjønnsmessigFastsatt = original.overstyresAv(SkjønnsmessigFastsatt(1.januar, UUID.randomUUID(), skjønnsmessigFastsattBeløp, "", null, LocalDateTime.now()))

        assertEquals(skjønnsmessigFastsattBeløp, skjønnsmessigFastsatt.fastsattÅrsinntekt())
        assertEquals(originalBeløp, skjønnsmessigFastsatt.omregnetÅrsinntekt())
    }

    @Test
    fun `fastsatt årsinntekt - saksbehandler`() {
        val skjønnsmessigFastsattBeløp = 5000.daglig
        val originalBeløp = 2500.daglig
        val saksbehandlerbeløp = 2600.daglig
        val original = Inntektsmelding(1.januar, UUID.randomUUID(), originalBeløp)
        val saksbehandler = original.overstyresAv(Saksbehandler(1.januar, UUID.randomUUID(), saksbehandlerbeløp, "", null, LocalDateTime.now()))
        val skjønnsmessigFastsatt = saksbehandler.overstyresAv(SkjønnsmessigFastsatt(1.januar, UUID.randomUUID(), skjønnsmessigFastsattBeløp, "", null, LocalDateTime.now()))

        assertTrue(skjønnsmessigFastsatt is SkjønnsmessigFastsatt)
        assertTrue(saksbehandler is Saksbehandler)
        assertEquals(skjønnsmessigFastsattBeløp, skjønnsmessigFastsatt.fastsattÅrsinntekt())
        assertEquals(saksbehandlerbeløp, skjønnsmessigFastsatt.omregnetÅrsinntekt())
    }
}