package no.nav.helse.person

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.september
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class ArbeidsgiverTest {
    @Test
    fun `ny inntektsmelding legger på inntekt på inntektHistorie`() {
        val inntektsmelding = Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(
                opphørsdato = null,
                inntekt = 12000.månedlig
            ),
            orgnummer = "orgnr",
            fødselsnummer = "fnr",
            aktørId = "aktørId",
            førsteFraværsdag = 1.januar,
            beregnetInntekt = 12000.månedlig,
            arbeidsgiverperioder = listOf(Periode(10.september, 10.september.plusDays(16))),
            ferieperioder = emptyList(),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        )
        val person = Person("aktørId", "fnr")
        val arbeidsgiver = Arbeidsgiver(person, "12345678")
        arbeidsgiver.håndter(sykmelding(Sykmeldingsperiode(10.september, 26.september, 100.prosent)))
        arbeidsgiver.håndter(inntektsmelding)
        assertEquals(
            12000.månedlig,
            arbeidsgiver.grunnlagForSykepengegrunnlag(10.september, 10.september)
        )
    }

    private fun sykmelding(vararg sykeperioder: Sykmeldingsperiode): Sykmelding {
        return Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = "fnr",
            aktørId = "aktørId",
            orgnummer = "orgnr",
            sykeperioder = sykeperioder.toList(),
            opprettet = Sykmeldingsperiode.periode(sykeperioder.toList())?.start?.atStartOfDay() ?: LocalDateTime.now()
        )
    }
}
