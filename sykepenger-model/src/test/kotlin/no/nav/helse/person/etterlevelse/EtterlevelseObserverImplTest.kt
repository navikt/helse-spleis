package no.nav.helse.person.etterlevelse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class EtterlevelseObserverImplTest {

    @Test
    fun `enkel vurdering`() {
        val etterlevelseObserver = EtterlevelseObserverImpl()

        etterlevelseObserver.`§8-2 ledd 1`(
            oppfylt = true,
            skjæringstidspunkt = LocalDate.now(),
            tilstrekkeligAntallOpptjeningsdager = 28,
            arbeidsforhold = listOf(),
            antallOpptjeningsdager = 28
        )

        assertEquals(1, etterlevelseObserver.vurderinger().size)
    }

    @Test
    fun `enkel vurdering som gjøres flere ganger innenfor samme hendelse skal dedup'es`() {
        val etterlevelseObserver = EtterlevelseObserverImpl()

        etterlevelseObserver.`§8-2 ledd 1`(true, LocalDate.now(), 28, listOf(), 28)
        etterlevelseObserver.`§8-2 ledd 1`(true, LocalDate.now(), 28, listOf(), 28)

        assertEquals(1, etterlevelseObserver.vurderinger().size)
    }

    @Test
    fun `dedup'er ikke vurderinger som er ulike`() {
        val etterlevelseObserver = EtterlevelseObserverImpl()

        etterlevelseObserver.`§8-2 ledd 1`(true, LocalDate.now(), 28, listOf(), 28)
        etterlevelseObserver.`§8-2 ledd 1`(false, LocalDate.now(), 28, listOf(), 28)

        assertEquals(2, etterlevelseObserver.vurderinger().size)
    }
}

