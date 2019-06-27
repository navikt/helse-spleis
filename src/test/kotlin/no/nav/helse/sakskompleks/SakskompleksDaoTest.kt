package no.nav.helse.sakskompleks

import no.nav.helse.sakskompleks.domain.Sakskompleks
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class SakskompleksDaoTest {

    @Test
    fun `skal finne sak for bruker`() {
        val sakForBruker = Sakskompleks(
                id = UUID.randomUUID(),
                aktørId = "123456789",
                sykmeldinger = emptyList(),
                søknader = emptyList()
        )
        val sakForAnnenBruker = Sakskompleks(
                id = UUID.randomUUID(),
                aktørId = "987654321",
                sykmeldinger = emptyList(),
                søknader = emptyList()
        )

        val saker = listOf(sakForBruker, sakForAnnenBruker)

        val dao = SakskompleksDao(saker)
        val sakerForBruker = dao.finnSaker("123456789")

        assertEquals(1, sakerForBruker.size)
        assertEquals(sakForBruker, sakerForBruker[0])
    }
}
