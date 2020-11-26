package no.nav.helse.hendelser

import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mai
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

internal class GrunnbeløpsreguleringTest {

    private companion object {

        private const val FNR = "12312313"
        private const val AKTØRID = "123123123"
        private const val ORGNR = "123123"
        private val id = UUID.randomUUID()
        private const val ARBEIDSGIVERFAGSYSTEMID = "123123"
        private const val PERSONFAGSYSTEMID = "321321"
        private val GRUNNBELØP_GYLDIG_FRA = 1.mai(2020)
    }

    @Test
    fun `fagsystemId må være relevant for regulering`() {
        Grunnbeløpsregulering(id, AKTØRID, FNR, ORGNR, GRUNNBELØP_GYLDIG_FRA, ARBEIDSGIVERFAGSYSTEMID).also { grunnbeløpsregulering ->
            assertTrue(grunnbeløpsregulering.erRelevant(ARBEIDSGIVERFAGSYSTEMID, PERSONFAGSYSTEMID, GRUNNBELØP_GYLDIG_FRA))
        }
        Grunnbeløpsregulering(id, AKTØRID, FNR, ORGNR, GRUNNBELØP_GYLDIG_FRA, PERSONFAGSYSTEMID).also { grunnbeløpsregulering ->
            assertTrue(grunnbeløpsregulering.erRelevant(ARBEIDSGIVERFAGSYSTEMID, PERSONFAGSYSTEMID, GRUNNBELØP_GYLDIG_FRA))
        }
        Grunnbeløpsregulering(id, AKTØRID, FNR, ORGNR, GRUNNBELØP_GYLDIG_FRA, "noe annet").also { grunnbeløpsregulering ->
            assertFalse(grunnbeløpsregulering.erRelevant(ARBEIDSGIVERFAGSYSTEMID, PERSONFAGSYSTEMID, GRUNNBELØP_GYLDIG_FRA))
        }
    }

    @Test
    fun `skjæringstidspunkt kan ikke være eldre enn gyldighetsdato`() {
        Grunnbeløpsregulering(id, AKTØRID, FNR, ORGNR, GRUNNBELØP_GYLDIG_FRA, ARBEIDSGIVERFAGSYSTEMID).also { grunnbeløpsregulering ->
            assertFalse(grunnbeløpsregulering.erRelevant(ARBEIDSGIVERFAGSYSTEMID, PERSONFAGSYSTEMID, GRUNNBELØP_GYLDIG_FRA.minusDays(1)))
        }
    }

    @Test
    fun `skal bare håndteres én gang`() {
        val grunnbeløpsregulering = Grunnbeløpsregulering(id, AKTØRID, FNR, ORGNR, 1.januar, ARBEIDSGIVERFAGSYSTEMID)
        assertFalse(grunnbeløpsregulering.håndtert())
        assertTrue(grunnbeløpsregulering.håndtert())
        assertTrue(grunnbeløpsregulering.håndtert())
    }
}
