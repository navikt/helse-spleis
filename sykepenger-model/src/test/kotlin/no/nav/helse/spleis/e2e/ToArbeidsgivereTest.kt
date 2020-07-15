package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.UtbetalingHendelse.Oppdragstatus.AKSEPTERT
import no.nav.helse.hendelser.Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
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
    fun `overlappende arbeidsgivere ikke sendt til infotrygd`() {
        gapPeriode(1.januar to 31.januar, rød)
        gapPeriode(15.januar to 15.februar, blå)
        assertNoErrors(rødInspektør)
        assertNoErrors(blåInspektør)

        betale(rød)
        assertNoErrors(rødInspektør)
        assertNoErrors(blåInspektør)
        assertEquals(AVVENTER_ARBEIDSGIVERE, rødInspektør.sisteTilstand(0))
        assertEquals(AVVENTER_HISTORIKK, blåInspektør.sisteTilstand(0))

        assertTilstander(
            rød.id(0),
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_ARBEIDSGIVERE
        )
        assertTilstander(
            blå.id(0),
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK
        )
    }

    @Disabled
    @Test
    fun `vedtaksperioder atskilt med betydelig tid`() {
        prosessperiode(1.januar to 31.januar, rød)
        assertNoErrors(rødInspektør)
        assertEquals(AVSLUTTET, rødInspektør.sisteTilstand(0))
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
        assertEquals(AVSLUTTET, rødInspektør.sisteTilstand(0))
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

    private fun prosessperiode(periode: Periode, orgnummer: String, sykedagstelling: Int = 0) {
        gapPeriode(periode, orgnummer)
        betale(orgnummer, sykedagstelling)
        vedta(orgnummer)
    }

    private fun forlengelsePeriode(periode: Periode, orgnummer: String) {
        nyPeriode(periode, orgnummer)
    }

    private fun gapPeriode(periode: Periode, orgnummer: String) {
        nyPeriode(periode, orgnummer)
        person.håndter(inntektsmelding(
            listOf(Periode(periode.start, periode.start.plusDays(15))),
            førsteFraværsdag = periode.start,
            orgnummer = orgnummer
        ))
        person.håndter(vilkårsgrunnlag(orgnummer.id(0), INNTEKT, orgnummer = orgnummer))
    }

    private fun nyPeriode(periode: Periode, orgnummer: String) {
        person.håndter(
            sykmelding(
                Sykmeldingsperiode(periode.start, periode.endInclusive, 100),
                orgnummer = orgnummer
            )
        )
        person.håndter(
            søknad(
                Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100),
                orgnummer = orgnummer
            )
        )
    }

    private fun betale(orgnummer: String, sykedagstelling: Int = 0) {
        person.håndter(
            ytelser(
                orgnummer.id(0),
                utbetalinger = utbetalinger(sykedagstelling, orgnummer),
                orgnummer = orgnummer
            )
        )
    }

    private fun vedta(orgnummer: String) {
        person.håndter(simulering(orgnummer.id(0), orgnummer = orgnummer))
        person.håndter(utbetalingsgodkjenning(orgnummer.id(0), true, orgnummer = orgnummer))
        person.håndter(utbetaling(orgnummer.id(0), AKSEPTERT, orgnummer = orgnummer))
    }

    private fun utbetalinger(dagTeller: Int, orgnummer: String): List<RefusjonTilArbeidsgiver> {
        if (dagTeller == 0) return emptyList()
        val førsteDato = 2.desember(2017).minusDays((
            (dagTeller / 5 * 7) + dagTeller % 5
            ).toLong())
        return listOf(
            RefusjonTilArbeidsgiver(
                førsteDato,
                1.desember(2017),
                100,
                100,
                orgnummer
            )
        )
    }

    private infix fun LocalDate.to(other: LocalDate) = Periode(this, other)
}
