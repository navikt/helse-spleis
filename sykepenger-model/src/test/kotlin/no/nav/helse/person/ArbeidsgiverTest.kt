package no.nav.helse.person

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.ModelInntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.september
import no.nav.helse.testhelpers.januar
import no.nav.helse.toJson
import no.nav.inntektsmeldingkontrakt.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.inntektsmeldingkontrakt.Periode as InntektsmeldingPeriode

internal class ArbeidsgiverTest {
    private val uuid = UUID.randomUUID()

    @Test
    fun `restoring av arbeidsgiver gir samme objekt`() {
        val arbeidsgiverMemento = Arbeidsgiver.Memento(uuid, "2", emptyList(), Inntekthistorikk.Memento(listOf()))
        val arbeidsgiverString = arbeidsgiverMemento.state()
        val arbeidsgiverMementoFromString = Arbeidsgiver.Memento.fromString(arbeidsgiverString)
        val restoredArbeidsgiver = Arbeidsgiver.restore(arbeidsgiverMementoFromString)

        assertEquals(uuid, restoredArbeidsgiver.memento().id)
        assertEquals("2", restoredArbeidsgiver.memento().organisasjonsnummer)
        assertEquals(0, restoredArbeidsgiver.memento().inntekthistorikk.inntekter.size)
        assertEquals(0, restoredArbeidsgiver.memento().perioder.size)
    }

    @Test
    fun `restoring av arbeidsgiver med inntektHistorie gir samme objekt`() {
        val arbeidsgiverMemento = Arbeidsgiver.Memento(
            uuid, "2", emptyList(), Inntekthistorikk.Memento(
                listOf(
                    Inntekthistorikk.Memento.Inntekt(
                        1.januar,
                        inntektsmelding(),
                        100.00.toBigDecimal()
                    )
                )
            )
        )
        val arbeidsgiverString = arbeidsgiverMemento.state()
        val arbeidsgiverMementoFromString = Arbeidsgiver.Memento.fromString(arbeidsgiverString)
        val restoredArbeidsgiver = Arbeidsgiver.restore(arbeidsgiverMementoFromString)

        assertEquals(uuid, restoredArbeidsgiver.memento().id)
        assertEquals(1, restoredArbeidsgiver.memento().inntekthistorikk.inntekter.size)
        assertEquals(100.0.toBigDecimal(), restoredArbeidsgiver.memento().inntekthistorikk.inntekter.first().beløp)
    }


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
            originalJson = "{}",
            arbeidsgiverperioder = listOf(Periode(10.september, 10.september.plusDays(16))),
            ferieperioder = emptyList()
        )

        val arbeidsgiver = Arbeidsgiver("12345678")
        arbeidsgiver.håndter(inntektsmelding, arbeidsgiver)
        assertTrue(inntektsmelding.hasErrors())
        assertEquals(1, arbeidsgiver.memento().inntekthistorikk.inntekter.size)
        assertEquals(
            120.00.toBigDecimal().setScale(2),
            arbeidsgiver.memento().inntekthistorikk.inntekter.first().beløp.setScale(2)
        )
        assertEquals(1.januar, arbeidsgiver.memento().inntekthistorikk.inntekter.first().fom)
    }

    @Test
    fun `restoring av arbeidsgiver uten inntekstHistorie legger på tom inntekstHistorie`() {

        val arbeidsgiverMemento = Arbeidsgiver.Memento(
            uuid, "2", emptyList(), Inntekthistorikk.Memento(
                listOf(
                    Inntekthistorikk.Memento.Inntekt(
                        1.januar,
                        inntektsmelding(),
                        100.00.toBigDecimal()
                    )
                )
            )
        )
        val arbeidsgiverString = arbeidsgiverMemento.state()

        val json = jacksonObjectMapper().readTree(arbeidsgiverString) as ObjectNode

        json.remove("inntektHistorie")

        val arbeidsgiverMementoFromString = Arbeidsgiver.Memento.fromString(json.toString())

        assertTrue(arbeidsgiverMementoFromString.inntekthistorikk.inntekter.isEmpty())

    }

    private fun inntektsmelding(): ModelInntektsmelding {
        return ModelInntektsmelding(
            hendelseId = UUID.randomUUID(),
            refusjon = ModelInntektsmelding.Refusjon(null, 1.0),
            orgnummer = "orgnummer",
            fødselsnummer = "fnr",
            aktørId = "aktør",
            mottattDato = LocalDateTime.now(),
            førsteFraværsdag = LocalDate.now(),
            beregnetInntekt = 1.0,
            originalJson = Inntektsmelding(
                inntektsmeldingId = "",
                arbeidstakerFnr = "fødselsnummer",
                arbeidstakerAktorId = "aktørId",
                virksomhetsnummer = "virksomhetsnummer",
                arbeidsgiverFnr = null,
                arbeidsgiverAktorId = null,
                arbeidsgivertype = Arbeidsgivertype.VIRKSOMHET,
                arbeidsforholdId = null,
                beregnetInntekt = BigDecimal.ONE,
                refusjon = Refusjon(beloepPrMnd = BigDecimal.ONE, opphoersdato = LocalDate.now()),
                endringIRefusjoner = listOf(EndringIRefusjon(endringsdato = LocalDate.now(), beloep = BigDecimal.ONE)),
                opphoerAvNaturalytelser = emptyList(),
                gjenopptakelseNaturalytelser = emptyList(),
                arbeidsgiverperioder = listOf(InntektsmeldingPeriode(fom = LocalDate.now(), tom = LocalDate.now())),
                status = Status.GYLDIG,
                arkivreferanse = "",
                ferieperioder = listOf(InntektsmeldingPeriode(fom = LocalDate.now(), tom = LocalDate.now())),
                foersteFravaersdag = LocalDate.now(),
                mottattDato = LocalDateTime.now()
            ).toJson(),
            arbeidsgiverperioder = listOf(Periode(1.januar, 2.januar)),
            ferieperioder = emptyList(),
            aktivitetslogger = Aktivitetslogger()
        )
    }

}
