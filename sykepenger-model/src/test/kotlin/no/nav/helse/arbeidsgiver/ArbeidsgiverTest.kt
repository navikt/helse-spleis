package no.nav.helse.arbeidsgiver

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.TestConstants
import no.nav.helse.fixtures.januar
import no.nav.helse.hendelser.ModelInntektsmelding
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.InntektHistorie
import no.nav.helse.september
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
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
        val arbeidsgiverMemento = Arbeidsgiver.Memento(
            uuid, "2", emptyList(), InntektHistorie.Memento(
                listOf(
                    InntektHistorie.Memento.Inntekt(
                        1.januar,
                        TestConstants.inntektsmeldingHendelse(),
                        100.00.toBigDecimal()
                    )
                )
            )
        )
        val arbeidsgiverString = arbeidsgiverMemento.state()
        val arbeidsgiverMementoFromString = Arbeidsgiver.Memento.fromString(arbeidsgiverString)
        val restoredArbeidsgiver = Arbeidsgiver.restore(arbeidsgiverMementoFromString)

        assertEquals(uuid, restoredArbeidsgiver.memento().id)
        assertEquals(1, restoredArbeidsgiver.memento().inntektHistorie.inntekter.size)
        assertEquals(100.0.toBigDecimal(), restoredArbeidsgiver.memento().inntektHistorie.inntekter.first().beløp)
    }

    @Test
    fun `ny inntektsmelding legger på inntekt på inntektHistorie`() {
        val inntektsmelding = ModelInntektsmelding(
            hendelseId = UUID.randomUUID(),
            refusjon = ModelInntektsmelding.Refusjon(
                opphørsdato = LocalDate.now(),
                beløpPrMåned = 120.0,
                endringerIRefusjon = null
            ),
            orgnummer = "orgnr",
            fødselsnummer = "fnr",
            aktørId = "aktørId",
            mottattDato = LocalDateTime.now(),
            førsteFraværsdag = 1.januar,
            beregnetInntekt = 120.0,
            aktivitetslogger = Aktivitetslogger(),
            originalJson = "{}",
            arbeidsgiverperioder = listOf(10.september..10.september.plusDays(16)),
            ferieperioder = emptyList()
        )

        val arbeidsgiver = Arbeidsgiver("12345678")

        assertThrows<Aktivitetslogger.AktivitetException> {
            arbeidsgiver.håndter(inntektsmelding)
        }

        assertEquals(1, arbeidsgiver.memento().inntektHistorie.inntekter.size)
        assertEquals(120.00.toBigDecimal().setScale(2), arbeidsgiver.memento().inntektHistorie.inntekter.first().beløp.setScale(2))
        assertEquals(1.januar, arbeidsgiver.memento().inntektHistorie.inntekter.first().fom)
    }

    @Test
    fun `restoring av arbeidsgiver uten inntekstHistorie legger på tom inntekstHistorie`() {

        val arbeidsgiverMemento = Arbeidsgiver.Memento(
            uuid, "2", emptyList(), InntektHistorie.Memento(
                listOf(
                    InntektHistorie.Memento.Inntekt(
                        1.januar,
                        TestConstants.inntektsmeldingHendelse(),
                        100.00.toBigDecimal()
                    )
                )
            )
        )
        val arbeidsgiverString = arbeidsgiverMemento.state()

        val json = jacksonObjectMapper().readTree(arbeidsgiverString) as ObjectNode

        json.remove("inntektHistorie")

        val arbeidsgiverMementoFromString = Arbeidsgiver.Memento.fromString(json.toString())

        assertTrue(arbeidsgiverMementoFromString.inntektHistorie.inntekter.isEmpty())

    }

}
