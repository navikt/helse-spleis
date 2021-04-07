package no.nav.helse.hendelser

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class InntektsmeldingReplayTest {

    @Test
    fun `trimmer inntektsmelding når ikke relevant`() {
        val inntektsmelding = inntektsmelding(1.januar til 16.januar, 1.mars)
        val vedtaksperiode = UUID.randomUUID()
        val replay = InntektsmeldingReplay(inntektsmelding, vedtaksperiode)

        assertEquals(1.januar til 1.mars, inntektsmelding.periode())
        replay.håndter(vedtaksperiode(), UUID.randomUUID(), 1.januar til 10.januar)
        assertEquals(11.januar til 1.mars, inntektsmelding.periode())
    }

    private val person = Person("aktør", "fnr")
    private fun vedtaksperiode() = Vedtaksperiode(
        person = person,
        arbeidsgiver = Arbeidsgiver(person, "orgnr"),
        id = UUID.randomUUID(),
        aktørId = "aktør",
        fødselsnummer = "fnr",
        organisasjonsnummer = "orgnr"
    )

    private fun inntektsmelding(arbeidsgiverperiode: Periode, førsteFraværsdag: LocalDate) = Inntektsmelding(
        meldingsreferanseId = UUID.randomUUID(),
        refusjon = Inntektsmelding.Refusjon(null, null, emptyList()),
        orgnummer = "orgnr",
        fødselsnummer = "fnr",
        aktørId = "aktør",
        førsteFraværsdag = førsteFraværsdag,
        beregnetInntekt = 25000.månedlig,
        arbeidsgiverperioder = listOf(arbeidsgiverperiode),
        ferieperioder = emptyList(),
        arbeidsforholdId = null,
        begrunnelseForReduksjonEllerIkkeUtbetalt = null,
        harOpphørAvNaturalytelser = false
    )
}
