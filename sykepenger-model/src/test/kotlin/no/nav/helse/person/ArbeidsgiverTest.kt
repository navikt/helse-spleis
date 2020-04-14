package no.nav.helse.person

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.september
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class ArbeidsgiverTest {
    @Test
    fun `ny inntektsmelding legger på inntekt på inntektHistorie`() {
        val inntektsmelding = Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(
                opphørsdato = LocalDate.now(),
                beløpPrMåned = 120.0
            ),
            orgnummer = "orgnr",
            fødselsnummer = "fnr",
            aktørId = "aktørId",
            førsteFraværsdag = 1.januar,
            beregnetInntekt = 120.0,
            arbeidsgiverperioder = listOf(Periode(10.september, 10.september.plusDays(16))),
            ferieperioder = emptyList(),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        )
        val person = Person("aktørId", "fnr")
        val arbeidsgiver = Arbeidsgiver(person, "12345678")
        arbeidsgiver.håndter(sykmelding(Triple(10.september, 26.september, 100)))
        arbeidsgiver.håndter(inntektsmelding)
        arbeidsgiver.accept(ArbeidsgiverTestVisitor)
        assertEquals(
            120.00.toBigDecimal().setScale(2),
            ArbeidsgiverTestVisitor.inntekthistorikk.inntekt(10.september)!!.setScale(2)
        )
    }

    private fun sykmelding(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>): Sykmelding {
        return Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = "fnr",
            aktørId = "aktørId",
            orgnummer = "orgnr",
            sykeperioder = listOf(*sykeperioder)
        )
    }

    private object ArbeidsgiverTestVisitor : ArbeidsgiverVisitor {
        lateinit var inntekthistorikk: Inntekthistorikk
        override fun preVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {
            this.inntekthistorikk = inntekthistorikk
        }
    }

}
