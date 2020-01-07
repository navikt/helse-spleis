package no.nav.helse.arbeidsgiver

import no.nav.helse.fixtures.januar
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.testhelpers.InntektsmeldingHendelseWrapper
import no.nav.helse.testhelpers.inntektsmelding
import no.nav.helse.utbetalingstidslinje.InntektHistorie
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
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
            InntektHistorie.Memento.Inntekt(1.januar, "Kilde", 100))))
        val arbeidsgiverString = arbeidsgiverMemento.state()
        val arbeidsgiverMementoFromString = Arbeidsgiver.Memento.fromString(arbeidsgiverString)
        val restoredArbeidsgiver = Arbeidsgiver.restore(arbeidsgiverMementoFromString)

        assertEquals(uuid, restoredArbeidsgiver.memento().id)
        assertEquals(1, restoredArbeidsgiver.memento().inntektHistorie.inntekter.size)
        assertEquals(100, restoredArbeidsgiver.memento().inntektHistorie.inntekter.first().dagsats)
    }

    @Test
    fun `ny inntektsmelding legger på inntekt på inntektHistorie`() {
        val inntektsmelding = inntektsmelding(InntektsmeldingHendelseWrapper) {
            this.beregnetInntekt = 120
            this.førsteFraværsdag = 1.januar
        }
        val arbeidsgiver = Arbeidsgiver("12345678")
        arbeidsgiver.håndter(inntektsmelding)

        assertEquals(1, arbeidsgiver.memento().inntektHistorie.inntekter.size)
        assertEquals(6, arbeidsgiver.memento().inntektHistorie.inntekter.first().dagsats)
        assertEquals(1.januar, arbeidsgiver.memento().inntektHistorie.inntekter.first().fom)
    }


}
