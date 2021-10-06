package no.nav.helse.person

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.somFødselsnummer
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.september
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class ArbeidsgiverTest {
    private companion object {
        const val ORGNUMMER = "888888888"
    }
    @Test
    fun `ny inntektsmelding legger på inntekt på inntektHistorie`() {
        val inntektsmelding = Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(
                beløp = 12000.månedlig,
                opphørsdato = null
            ),
            orgnummer = ORGNUMMER,
            fødselsnummer = "fnr",
            aktørId = "aktørId",
            førsteFraværsdag = 1.januar,
            beregnetInntekt = 12000.månedlig,
            arbeidsgiverperioder = listOf(Periode(10.september, 10.september.plusDays(16))),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null,
            mottatt = LocalDateTime.now()
        )
        val person = Person("aktørId", "01010112345".somFødselsnummer())
        person.håndter(sykmelding(Sykmeldingsperiode(10.september, 26.september, 100.prosent)))
        person.håndter(inntektsmelding)
        assertEquals(
            12000.månedlig,
            person.arbeidsgiver(ORGNUMMER).grunnlagForSykepengegrunnlag(10.september, 10.september)
        )
    }

    private fun sykmelding(vararg sykeperioder: Sykmeldingsperiode): Sykmelding {
        val periode = Sykmeldingsperiode.periode(sykeperioder.toList())
        return Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = "fnr",
            aktørId = "aktørId",
            orgnummer = ORGNUMMER,
            sykeperioder = sykeperioder.toList(),
            sykmeldingSkrevet = periode?.start!!.atStartOfDay(),
            mottatt = periode.endInclusive.atStartOfDay()
        )
    }
}
