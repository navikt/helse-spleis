package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.UtbetalingHendelse.Oppdragstatus.AKSEPTERT
import no.nav.helse.hendelser.Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import java.time.LocalDate

internal class FlereArbeidsgivereTest : AbstractEndToEndTest() {

    internal companion object {
        private const val a1 = "arbeidsgiver 1"
        private const val a2 = "arbeidsgiver 2"
        private const val a3 = "arbeidsgiver 3"
        private const val a4 = "arbeidsgiver 4"
    }

    private val a1Inspektør get() = TestArbeidsgiverInspektør(person, a1)
    private val a2Inspektør get() = TestArbeidsgiverInspektør(person, a2)
    private val a3Inspektør get() = TestArbeidsgiverInspektør(person, a3)
    private val a4Inspektør get() = TestArbeidsgiverInspektør(person, a4)

    @Test
    fun `overlappende arbeidsgivere ikke sendt til infotrygd`() {
        gapPeriode(1.januar to 31.januar, a1)
        gapPeriode(15.januar to 15.februar, a2)
        assertNoErrors(a1Inspektør)
        assertNoErrors(a2Inspektør)

        betale(a1)
        assertNoErrors(a1Inspektør)
        assertNoErrors(a2Inspektør)
        assertEquals(AVVENTER_ARBEIDSGIVERE, a1Inspektør.sisteTilstand(0))
        assertEquals(AVVENTER_HISTORIKK, a2Inspektør.sisteTilstand(0))

        assertTilstander(
            a1.id(0),
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_ARBEIDSGIVERE
        )
        assertTilstander(
            a2.id(0),
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK
        )
    }

    @Test
    fun `vedtaksperioder atskilt med betydelig tid`() {
        prosessperiode(1.januar to 31.januar, a1)
        assertNoErrors(a1Inspektør)
        assertEquals(AVSLUTTET, a1Inspektør.sisteTilstand(0))
        assertTilstander(
            a1.id(0),
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        prosessperiode(1.mars to 31.mars, a2)
        assertNoErrors(a2Inspektør)
        assertEquals(AVSLUTTET, a1Inspektør.sisteTilstand(0))
        assertTilstander(
            a2.id(0),
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `Tre overlappende perioder med en ikke-overlappende periode` (){
        gapPeriode(1.januar to 31.januar, a1)
        gapPeriode(15.januar to 15.mars, a2)
        gapPeriode(1.februar to 28.februar, a3)
        gapPeriode(15.april to 15.mai, a4)

        betale(a1)
        assertEquals(AVVENTER_ARBEIDSGIVERE, a1Inspektør.sisteTilstand(0))
        assertEquals(AVVENTER_HISTORIKK, a2Inspektør.sisteTilstand(0))
        assertEquals(AVVENTER_HISTORIKK, a3Inspektør.sisteTilstand(0))
        assertEquals(AVVENTER_HISTORIKK, a4Inspektør.sisteTilstand(0))

        betale(a3)
        assertEquals(AVVENTER_ARBEIDSGIVERE, a1Inspektør.sisteTilstand(0))
        assertEquals(AVVENTER_HISTORIKK, a2Inspektør.sisteTilstand(0))
        assertEquals(AVVENTER_ARBEIDSGIVERE, a3Inspektør.sisteTilstand(0))
        assertEquals(AVVENTER_HISTORIKK, a4Inspektør.sisteTilstand(0))

        betale(a2)
        vedta(a1)
        assertEquals(AVSLUTTET, a1Inspektør.sisteTilstand(0))
        assertEquals(AVVENTER_ARBEIDSGIVERE, a2Inspektør.sisteTilstand(0))
        assertEquals(AVVENTER_HISTORIKK, a3Inspektør.sisteTilstand(0))
        assertEquals(AVVENTER_HISTORIKK, a4Inspektør.sisteTilstand(0))

        betale(a3)  // Rekalkulerer a3
        assertEquals(AVSLUTTET, a1Inspektør.sisteTilstand(0))
        assertEquals(AVVENTER_ARBEIDSGIVERE, a2Inspektør.sisteTilstand(0))
        assertEquals(AVVENTER_SIMULERING, a3Inspektør.sisteTilstand(0))
        assertEquals(AVVENTER_HISTORIKK, a4Inspektør.sisteTilstand(0))

        vedta(a3)
        assertEquals(AVSLUTTET, a1Inspektør.sisteTilstand(0))
        assertEquals(AVVENTER_HISTORIKK, a2Inspektør.sisteTilstand(0))
        assertEquals(AVSLUTTET, a3Inspektør.sisteTilstand(0))
        assertEquals(AVVENTER_HISTORIKK, a4Inspektør.sisteTilstand(0))

        betale(a2)  // Rekalkulerer a2
        assertEquals(AVSLUTTET, a1Inspektør.sisteTilstand(0))
        assertEquals(AVVENTER_SIMULERING, a2Inspektør.sisteTilstand(0))
        assertEquals(AVSLUTTET, a3Inspektør.sisteTilstand(0))
        assertEquals(AVVENTER_HISTORIKK, a4Inspektør.sisteTilstand(0))

        vedta(a2)
        assertEquals(AVSLUTTET, a1Inspektør.sisteTilstand(0))
        assertEquals(AVSLUTTET, a2Inspektør.sisteTilstand(0))
        assertEquals(AVSLUTTET, a3Inspektør.sisteTilstand(0))
        assertEquals(AVVENTER_HISTORIKK, a4Inspektør.sisteTilstand(0))

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
