package no.nav.helse.person.infotrygdhistorikk

import no.nav.helse.person.Aktivitetslogg
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class NødnummerTest {

    @Test
    fun `bruk av nødnummer støttes ikke`() {
        val aktivitetslogg = Aktivitetslogg()
        Nødnummer.Sykepenger.valider(aktivitetslogg, "973626108")
        assertTrue(aktivitetslogg.hasErrorsOrWorse())
    }
}
