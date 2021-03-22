package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class InntektsopplysningTest {

    private companion object {
        private const val ORGNR = "123456789"
        private val DATO = 1.januar
        private val PERIODE = Periode(1.februar, 28.februar)
    }

    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    fun setup() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `refusjon opphører før perioden`() {
        inntektsopplysning(1.januar).valider(aktivitetslogg, PERIODE, DATO)
        assertTrue(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `refusjon opphører i perioden`() {
        inntektsopplysning(15.februar).valider(aktivitetslogg, PERIODE, DATO)
        assertTrue(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `refusjon opphører etter perioden`() {
        inntektsopplysning(1.mars).valider(aktivitetslogg, PERIODE, DATO)
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    private fun inntektsopplysning(refusjonTom: LocalDate? = null) =
        Utbetalingshistorikk.Inntektsopplysning(DATO, 1000.månedlig, ORGNR, true, refusjonTom)
}
