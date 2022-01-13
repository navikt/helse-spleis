package no.nav.helse.person.etterlevelse

import no.nav.helse.person.Bokstav
import no.nav.helse.person.Ledd
import no.nav.helse.person.Ledd.Companion.ledd
import no.nav.helse.person.Paragraf
import no.nav.helse.person.Punktum
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class EnkelVurderingTest {

    private lateinit var vurderinger: List<JuridiskVurdering>

    @BeforeEach
    fun beforeEach() {
        vurderinger = emptyList()
    }

    @Test
    fun `enkel vurdering`() {
        nyVurdering()
        assertEquals(1, vurderinger.size)
    }

    @Test
    fun `enkel vurdering som gjøres flere ganger innenfor samme hendelse forekommer kun en gang`() {
        nyVurdering()
        nyVurdering()

        assertEquals(1, vurderinger.size)
    }

    @Test
    fun `vurderinger med ulike data utgjør hvert sitt innslag`() {
        nyVurdering(true)
        nyVurdering(false)

        assertEquals(2, vurderinger.size)
    }

    private fun nyVurdering(
        oppfylt: Boolean = true,
        versjon: LocalDate = LocalDate.MAX,
        paragraf: Paragraf = Paragraf.PARAGRAF_8_2,
        ledd: Ledd = 1.ledd,
        punktum: List<Punktum> = emptyList(),
        bokstaver: List<Bokstav> = emptyList(),
        input: Map<String, Any> = emptyMap(),
        output: Map<String, Any> = emptyMap(),
        kontekster: Map<String, String> = emptyMap()
    ) {
        vurderinger = EnkelVurdering(oppfylt, versjon, paragraf, ledd, punktum, bokstaver, input, output, kontekster).sammenstill(vurderinger)
    }
}
