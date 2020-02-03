package no.nav.helse.person

import no.nav.helse.hendelser.ModelInntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.september
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class ArbeidsgiverTest {
    @Test
    fun `ny inntektsmelding legger på inntekt på inntektHistorie`() {
        val inntektsmelding = ModelInntektsmelding(
            hendelseId = UUID.randomUUID(),
            refusjon = ModelInntektsmelding.Refusjon(
                opphørsdato = LocalDate.now(),
                beløpPrMåned = 120.0
            ),
            orgnummer = "orgnr",
            fødselsnummer = "fnr",
            aktørId = "aktørId",
            mottattDato = LocalDateTime.now(),
            førsteFraværsdag = 1.januar,
            beregnetInntekt = 120.0,
            aktivitetslogger = Aktivitetslogger(),
            arbeidsgiverperioder = listOf(Periode(10.september, 10.september.plusDays(16))),
            ferieperioder = emptyList()
        )
        val person = Person("aktørId", "fnr")
        val arbeidsgiver = Arbeidsgiver("12345678")
        arbeidsgiver.håndter(inntektsmelding, person)
        assertTrue(inntektsmelding.hasErrors())

        arbeidsgiver.accept(ArbeidsgiverTestVisitor)
        assertEquals(
            120.00.toBigDecimal().setScale(2),
            ArbeidsgiverTestVisitor.inntekthistorikk.inntekt(10.september)!!.setScale(2)
        )
    }

    private object ArbeidsgiverTestVisitor : ArbeidsgiverVisitor {
        lateinit var inntekthistorikk: Inntekthistorikk
        override fun preVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {
            this.inntekthistorikk = inntekthistorikk
        }
    }

}
