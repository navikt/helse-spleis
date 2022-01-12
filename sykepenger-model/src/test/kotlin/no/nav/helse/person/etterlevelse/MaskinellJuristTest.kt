package no.nav.helse.person.etterlevelse

import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt
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
    fun `enkel vurdering som gjøres flere ganger innenfor samme hendelse forekommer kun en gang`() {
        etterlevelseObserver.`§8-2 ledd 1`(true, 1.januar, 28, listOf(), 28)
        etterlevelseObserver.`§8-2 ledd 1`(true, 1.januar, 28, listOf(), 28)

        assertEquals(1, etterlevelseObserver.vurderinger().size)
    }

    @Test
    fun `vurderinger med ulike data utgjør hvert sitt innslag`() {
        etterlevelseObserver.`§8-2 ledd 1`(true, 1.januar, 28, listOf(), 28)
        etterlevelseObserver.`§8-2 ledd 1`(false, 1.januar, 28, listOf(), 28)

        assertEquals(2, etterlevelseObserver.vurderinger().size)
    }

    @Test
    fun `Legger til betingede vurderinger`() {
        etterlevelseObserver.`§8-10 ledd 2 punktum 1`(oppfylt = true, funnetRelevant = true, Inntekt.INGEN, 1.januar, Inntekt.INGEN)
        assertEquals(1, etterlevelseObserver.vurderinger().size)
    }

    @Test
    fun `En betinget vurdering erstatter en annen dersom de er like`() {
        etterlevelseObserver.`§8-10 ledd 2 punktum 1`(oppfylt = true, funnetRelevant = true, Inntekt.INGEN, 1.januar, Inntekt.INGEN)
        etterlevelseObserver.`§8-10 ledd 2 punktum 1`(oppfylt = true, funnetRelevant = true, Inntekt.INGEN, 1.januar, Inntekt.INGEN)
        assertEquals(1, etterlevelseObserver.vurderinger().size)
    }

    @Test
    fun `En betinget vurdering blir ikke lagt til dersom betingelsen ikke er oppfylt`() {
        etterlevelseObserver.`§8-10 ledd 2 punktum 1`(oppfylt = true, funnetRelevant = false, Inntekt.INGEN, 1.januar, Inntekt.INGEN)
        assertEquals(0, etterlevelseObserver.vurderinger().size)
    }
}

