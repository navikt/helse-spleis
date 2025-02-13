package no.nav.helse.person

import java.util.*
import no.nav.helse.hendelser.MeldingsreferanseId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class DokumentsporingTest {

    @Test
    fun `to like sporinger i et set`() {
        val søknad = MeldingsreferanseId(UUID.randomUUID())
        val sporing1 = Dokumentsporing.søknad(søknad)
        val sporing2 = Dokumentsporing.søknad(søknad)
        val set = setOf(sporing1, sporing2)
        assertEquals(sporing1, sporing2)
        assertEquals(sporing1.hashCode(), sporing2.hashCode())
        assertEquals(1, set.size)
    }
}
