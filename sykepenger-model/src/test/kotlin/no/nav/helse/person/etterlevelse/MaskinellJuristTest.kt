package no.nav.helse.person.etterlevelse

import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class MaskinellJuristTest {
    private lateinit var etterlevelseObserver: EtterlevelseObserver

    @BeforeEach
    fun beforeEach() {
        etterlevelseObserver = MaskinellJurist()
    }

    @Test
    fun `enkel vurdering`() {
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
        etterlevelseObserver.`§8-2 ledd 1`(true, 1.januar, 28, listOf(), 28)
        etterlevelseObserver.`§8-2 ledd 1`(true, 1.januar, 28, listOf(), 28)

        assertEquals(1, etterlevelseObserver.vurderinger().size)
    }

    @Test
    fun `dedup'er ikke vurderinger som er ulike`() {
        etterlevelseObserver.`§8-2 ledd 1`(true, 1.januar, 28, listOf(), 28)
        etterlevelseObserver.`§8-2 ledd 1`(false, 1.januar, 28, listOf(), 28)

        assertEquals(2, etterlevelseObserver.vurderinger().size)
    }

    @Test
    fun `Vurderinger på dagnivå blir slått sammen`() {
        repeat(10) {
            etterlevelseObserver.`§8-16 ledd 1`(1.januar.plusDays(it.toLong()), 1.0, 1000.0, 1000.0)
        }
        assertEquals(1, etterlevelseObserver.vurderinger().size)
    }
}

