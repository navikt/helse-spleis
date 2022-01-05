package no.nav.helse.person.etterlevelse

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class EtterlevelseObserverImplTest{



    @Test
    fun `enkel vurdering`(){

        val etterlevelseObserver = EtterlevelseObserverImpl()

        etterlevelseObserver.`§8-2 ledd 1`(
            oppfylt = true,
            skjæringstidspunkt = LocalDate.now(),
            tilstrekkeligAntallOpptjeningsdager = 28,
            arbeidsforhold = listOf(),
            antallOpptjeningsdager = 28
        )

        assertEquals(1,etterlevelseObserver.vurderinger().size)


    }
}

