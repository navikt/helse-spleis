package no.nav.helse.arbeidsgiver

import no.nav.helse.TestConstants
import no.nav.helse.fixtures.januar
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.testhelpers.InntektsmeldingHendelseWrapper
import no.nav.helse.testhelpers.inntektsmelding
import no.nav.helse.person.InntektHistorie
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*

internal class ArbeidsgiverTest {
        private val uuid = UUID.randomUUID()

    @Test
    fun `restoring av arbeidsgiver gir samme objekt`() {
        val arbeidsgiverMemento = Arbeidsgiver.Memento(uuid, "2", emptyList(), InntektHistorie.Memento(listOf()))
        val arbeidsgiverString = arbeidsgiverMemento.state()
        val arbeidsgiverMementoFromString = Arbeidsgiver.Memento.fromString(arbeidsgiverString)
        val restoredArbeidsgiver = Arbeidsgiver.restore(arbeidsgiverMementoFromString)

        assertEquals(uuid, restoredArbeidsgiver.memento().id)
        assertEquals("2", restoredArbeidsgiver.memento().organisasjonsnummer)
        assertEquals(0, restoredArbeidsgiver.memento().inntektHistorie.inntekter.size)
        assertEquals(0, restoredArbeidsgiver.memento().perioder.size)
    }

    @Test
    fun `restoring av arbeidsgiver med inntektHistorie gir samme objekt`() {
        val arbeidsgiverMemento = Arbeidsgiver.Memento(uuid, "2", emptyList(), InntektHistorie.Memento(listOf(
            InntektHistorie.Memento.Inntekt(1.januar, TestConstants.inntektsmeldingHendelse(), 100.00.toBigDecimal()))))
        val arbeidsgiverString = arbeidsgiverMemento.state()
        val arbeidsgiverMementoFromString = Arbeidsgiver.Memento.fromString(arbeidsgiverString)
        val restoredArbeidsgiver = Arbeidsgiver.restore(arbeidsgiverMementoFromString)

        assertEquals(uuid, restoredArbeidsgiver.memento().id)
        assertEquals(1, restoredArbeidsgiver.memento().inntektHistorie.inntekter.size)
        assertEquals(100.0.toBigDecimal(), restoredArbeidsgiver.memento().inntektHistorie.inntekter.first().beløp)
    }

    @Test
    fun `ny inntektsmelding legger på inntekt på inntektHistorie`() {
        val inntektsmelding = inntektsmelding(InntektsmeldingHendelseWrapper) {
            this.beregnetInntekt = 120.0
            this.førsteFraværsdag = 1.januar
        }
        val arbeidsgiver = Arbeidsgiver("12345678")
        arbeidsgiver.håndter(inntektsmelding)

        assertEquals(1, arbeidsgiver.memento().inntektHistorie.inntekter.size)
        assertEquals(120.00.toBigDecimal().setScale(2), arbeidsgiver.memento().inntektHistorie.inntekter.first().beløp)
        assertEquals(1.januar, arbeidsgiver.memento().inntektHistorie.inntekter.first().fom)
    }


}
