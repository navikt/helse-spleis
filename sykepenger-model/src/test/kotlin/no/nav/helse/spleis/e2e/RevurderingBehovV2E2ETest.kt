package no.nav.helse.spleis.e2e

import java.time.LocalDate
import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype.Feriedag
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.personLogg
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.sisteBehov
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.NyRevurdering::class)
internal class RevurderingBehovV2E2ETest : AbstractEndToEndTest() {

    @Test
    fun `Henter ytelser for hele revurderingsperioden`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.januar, Feriedag)))
        håndterYtelser(3.vedtaksperiode)
        assertYtelser(1.januar til 31.mars)
    }

    @Test
    fun `Henter ytelser for kun de periodene som blir revurdert`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(20.mars, Feriedag)))
        håndterYtelser(3.vedtaksperiode)
        assertYtelser(1.mars til 31.mars)
    }

    @Test
    fun `Henter ytelser for alle perioder som blir revurdert`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        håndterOverstyrInntekt(30000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(3.vedtaksperiode)
        assertYtelser(1.januar til 31.mars)
    }

    @Test
    fun `Ping Pong - henter ytelser for første periode isolert, deretter for andre periode isolert`() {
        nyttVedtak(1.januar, 31.januar)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent))
        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(a1, 1.februar, 28.februar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(
                Inntektsopplysning(a1, 1.februar, INNTEKT, true),
            )
        )
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        assertYtelser(1.januar til 31.januar)

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        assertYtelser(1.mars til 31.mars)
    }

    private fun assertYtelser(periode: Periode) {
        assertArbeidsavklaringspengerdetaljer(periode.start.minusMonths(6), periode.endInclusive)
        assertDagpengerdetaljer(periode.start.minusMonths(6), periode.endInclusive)
        assertInstitusjonsoppholddetaljer(periode.start, periode.endInclusive)
        assertOmsorgspengerdetaljer(periode.start, periode.endInclusive)
        assertOpplæringspengerdetaljer(periode.start, periode.endInclusive)
        assertPleiepengerdetaljer(periode.start, periode.endInclusive)
    }

    private fun assertArbeidsavklaringspengerdetaljer(fom: LocalDate, tom: LocalDate) {
        val behov = person.personLogg.sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Arbeidsavklaringspenger).detaljer()
        assertEquals(fom, (LocalDate.parse(behov["periodeFom"] as String)))
        assertEquals(tom, (LocalDate.parse(behov["periodeTom"] as String)))
    }

    private fun assertDagpengerdetaljer(fom: LocalDate, tom: LocalDate) {
        val behov = person.personLogg.sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Dagpenger).detaljer()
        assertEquals(fom, (LocalDate.parse(behov["periodeFom"] as String)))
        assertEquals(tom, (LocalDate.parse(behov["periodeTom"] as String)))
    }

    private fun assertInstitusjonsoppholddetaljer(fom: LocalDate, tom: LocalDate) {
        val behov = person.personLogg.sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Institusjonsopphold).detaljer()
        assertEquals(fom, (LocalDate.parse(behov["institusjonsoppholdFom"] as String)))
        assertEquals(tom, (LocalDate.parse(behov["institusjonsoppholdTom"] as String)))
    }

    private fun assertOmsorgspengerdetaljer(fom: LocalDate, tom: LocalDate) {
        val behov = person.personLogg.sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Omsorgspenger).detaljer()
        assertEquals(fom, (LocalDate.parse(behov["omsorgspengerFom"] as String)))
        assertEquals(tom, (LocalDate.parse(behov["omsorgspengerTom"] as String)))
    }

    private fun assertOpplæringspengerdetaljer(fom: LocalDate, tom: LocalDate) {
        val behov = person.personLogg.sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Opplæringspenger).detaljer()
        assertEquals(fom, (LocalDate.parse(behov["opplæringspengerFom"] as String)))
        assertEquals(tom, (LocalDate.parse(behov["opplæringspengerTom"] as String)))
    }

    private fun assertPleiepengerdetaljer(fom: LocalDate, tom: LocalDate) {
        val behov = person.personLogg.sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Pleiepenger).detaljer()
        assertEquals(fom, (LocalDate.parse(behov["pleiepengerFom"] as String)))
        assertEquals(tom, (LocalDate.parse(behov["pleiepengerTom"] as String)))
    }

}