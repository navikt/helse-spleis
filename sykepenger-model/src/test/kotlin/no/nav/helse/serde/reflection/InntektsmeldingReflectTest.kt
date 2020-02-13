package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.september
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class InntektsmeldingReflectTest {
    @Test
    internal fun `kontroller at alle felter er gjort rede for`() {
        assertMembers<Inntektsmelding, InntektsmeldingReflect>(
            skalMappes = listOf(
                "hendelseId",
                "hendelsestype",
                "refusjon",
                "orgnummer",
                "fødselsnummer",
                "aktørId",
                "mottattDato",
                "førsteFraværsdag",
                "beregnetInntekt",
                "arbeidsgiverperioder",
                "ferieperioder",
                "aktivitetslogger",
                "aktivitetslogg"
            )
        )
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    internal fun `mapper Inntektsmelding til map`() {
        val map = InntektsmeldingReflect(inntektsmelding).toMap()

        assertEquals(2, map.size)
        assertEquals("Inntektsmelding", map["type"])
        assertEquals(orgnummer, (map["data"] as Map<String, Any>)["orgnummer"])
    }

    internal companion object {
        internal val inntektsmelding = Inntektsmelding(
            hendelseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(
                opphørsdato = LocalDate.now(),
                beløpPrMåned = 120.0
            ),
            orgnummer = orgnummer,
            fødselsnummer = fnr,
            aktørId = aktørId,
            mottattDato = LocalDateTime.now(),
            førsteFraværsdag = 1.januar,
            beregnetInntekt = 120.0,
            aktivitetslogger = Aktivitetslogger(),
            aktivitetslogg = Aktivitetslogg(),
            arbeidsgiverperioder = listOf(Periode(10.september, 10.september.plusDays(16))),
            ferieperioder = emptyList()
        )
    }
}
