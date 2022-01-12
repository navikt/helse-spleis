package no.nav.helse.person.etterlevelse

import no.nav.helse.person.Ledd.Companion.ledd
import no.nav.helse.person.Paragraf
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class GrupperbarVurderingTest {

    @Test
    fun `Grupperer bare vurderinger som ligger inntil hverandre som kommer Out-Of-Order`() {
        var vurderinger = emptyList<JuridiskVurdering>()

        vurderinger = grupperbarVurdering(4.januar).sammenstill(vurderinger)
        vurderinger = grupperbarVurdering(1.januar).sammenstill(vurderinger)
        vurderinger = grupperbarVurdering(2.januar).sammenstill(vurderinger)
        assertEquals(2, vurderinger.size)
    }

    @Test
    fun `Grupperer alle vurderingersom ligger inntil hverandre selv om de kommer Out-Of-Order`() {
        var vurderinger = emptyList<JuridiskVurdering>()

        vurderinger = grupperbarVurdering(2.januar).sammenstill(vurderinger)
        vurderinger = grupperbarVurdering(4.januar).sammenstill(vurderinger)
        vurderinger = grupperbarVurdering(1.januar).sammenstill(vurderinger)
        vurderinger = grupperbarVurdering(3.januar).sammenstill(vurderinger)
        assertEquals(1, vurderinger.size)
    }

    @Test
    fun `Grupperer vurderinger som ligger inntil hverandre med helg `() {
        var vurderinger = emptyList<JuridiskVurdering>()

        vurderinger = grupperbarVurdering(2.januar).sammenstill(vurderinger)
        vurderinger = grupperbarVurdering(4.januar).sammenstill(vurderinger)
        vurderinger = grupperbarVurdering(1.januar).sammenstill(vurderinger)
        vurderinger = grupperbarVurdering(3.januar).sammenstill(vurderinger)

        assertEquals(1, vurderinger.size)
    }

    private fun grupperbarVurdering(dato: LocalDate) = GrupperbarVurdering(dato, mapOf(), mapOf(), true, LocalDate.MAX, Paragraf.PARAGRAF_8_2, 1.ledd)

}
