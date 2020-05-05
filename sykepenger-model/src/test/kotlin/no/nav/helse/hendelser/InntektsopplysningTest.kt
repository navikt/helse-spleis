package no.nav.helse.hendelser

import no.nav.helse.person.Inntekthistorikk
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class InntektsopplysningTest {

    private companion object {
        private val HENDELSE = UUID.randomUUID()
        private const val ORGNR = "123456789"
        private val DATO = 1.januar
    }

    private lateinit var inntekthistorikk: Inntekthistorikk

    @BeforeEach
    fun setup() {
        inntekthistorikk = Inntekthistorikk()
    }

    @Test
    fun `legger til inntekter for samme arbeidsgiver`() {
        inntektsopplysning(DATO, ORGNR).addInntekter(HENDELSE, ORGNR, inntekthistorikk)
        assertNotNull(inntekthistorikk.inntekt(DATO.minusDays(1)))
    }

    @Test
    fun `legger ikke til inntekter for annen arbeidsgiver`() {
        inntektsopplysning(DATO, "987654321").addInntekter(HENDELSE, ORGNR, inntekthistorikk)
        assertNull(inntekthistorikk.inntekt(DATO.minusDays(1)))
    }

    private fun inntektsopplysning(dato: LocalDate, orgnr: String) =
        Utbetalingshistorikk.Inntektsopplysning(dato, 1000, orgnr, true)
}
