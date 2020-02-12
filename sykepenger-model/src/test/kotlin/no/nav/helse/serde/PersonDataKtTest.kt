package no.nav.helse.serde

import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandTypeGammelOgNy
import org.junit.jupiter.api.Test

internal class PersonDataKtTest {


    @Test
    fun `parser alle tilstander vi bruker i prod`() {
        TilstandType.values()
            .forEach { parseTilstand(TilstandTypeGammelOgNy.valueOf(it.name)) }
    }
}
