package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.UtbetalingHendelse.Oppdragstatus.AKSEPTERT
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ToArbeidsgivereTest : AbstractEndToEndTest() {

    internal companion object {
        private const val rød = "rød"
        private const val blå = "blå"
    }

    private val rødInspektør get() = TestArbeidsgiverInspektør(person, rød)
    private val blåInspektør get() = TestArbeidsgiverInspektør(person, blå)

    @Test
    fun `overlappende arbeidsgivere sendt til infotrygd`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100), orgnummer = rød))
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100), orgnummer = blå))

        person.håndter(inntektsmelding(listOf(Periode(1.januar, 16.januar)), orgnummer = rød))
        person.håndter(søknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100), orgnummer = rød))
        person.håndter(vilkårsgrunnlag(rød.id(0), INNTEKT, orgnummer = rød))
        assertNoErrors(rødInspektør)

        person.håndter(søknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100), orgnummer = blå))
        person.håndter(inntektsmelding(listOf(Periode(1.januar, 16.januar)), orgnummer = blå))
        person.håndter(vilkårsgrunnlag(blå.id(0), INNTEKT, orgnummer = blå))
        assertNoErrors(blåInspektør)

        person.håndter(ytelser(rød.id(0), orgnummer = rød))
        assertErrors(rødInspektør)
        assertErrors(blåInspektør)
        assertTilstander(
            rød.id(0),
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD
        )
        assertTilstander(
            blå.id(0),
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `vedtaksperioder atskilt med betydelig tid`() {
        prosessperiode(1.januar to 31.januar, rød)
        assertNoErrors(rødInspektør)
        assertTilstander(
            rød.id(0),
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        prosessperiode(1.mars to 31.mars, blå)
        assertNoErrors(blåInspektør)
        assertTilstander(
            blå.id(0),
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    private fun prosessperiode(periode: Periode, orgnummer: String) {
        person.håndter(sykmelding(
            Sykmeldingsperiode(periode.start, periode.endInclusive, 100),
            orgnummer = orgnummer
        ))
        person.håndter(inntektsmelding(
            listOf(Periode(periode.start, periode.start.plusDays(15))),
            førsteFraværsdag = periode.start,
            orgnummer = orgnummer
        ))
        person.håndter(søknad(
            Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100),
            orgnummer = orgnummer
        ))
        person.håndter(vilkårsgrunnlag(orgnummer.id(0), INNTEKT, orgnummer = orgnummer))
        person.håndter(ytelser(orgnummer.id(0), orgnummer = orgnummer))
        person.håndter(simulering(orgnummer.id(0), orgnummer = orgnummer))
        person.håndter(utbetalingsgodkjenning(orgnummer.id(0), true, orgnummer = orgnummer))
        person.håndter(utbetaling(orgnummer.id(0), AKSEPTERT, orgnummer = orgnummer))
    }

    private infix fun LocalDate.to(other: LocalDate) = Periode(this, other)
}
