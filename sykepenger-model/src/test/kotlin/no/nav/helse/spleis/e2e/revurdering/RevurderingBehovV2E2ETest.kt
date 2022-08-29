package no.nav.helse.spleis.e2e.revurdering

import java.time.LocalDate
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype.Feriedag
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.personLogg
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.sisteBehov
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertIngenVarsler
import no.nav.helse.spleis.e2e.assertVarsler
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterOverstyrInntekt
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyeVedtak
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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
    fun `Henter ytelser for alle periodene på skjæringstidspunktet`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        nyttVedtak(1.mai, 31.mai)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(20.mars, Feriedag)))
        håndterYtelser(3.vedtaksperiode)
        assertYtelser(1.januar til 31.mars)
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
    fun `Sjekker overlapp mot overtrygd`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar, besvart = LocalDate.EPOCH.atStartOfDay())
        forlengVedtak(1.mars, 31.mars, besvart = LocalDate.EPOCH.atStartOfDay())
        håndterOverstyrInntekt(30000.månedlig, skjæringstidspunkt = 1.januar)
        val utbetalinger = listOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar,  31.januar, 100.prosent, INNTEKT))
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true))
        assertIngenVarsler()
        håndterYtelser(3.vedtaksperiode, utbetalinger = utbetalinger.toTypedArray(), inntektshistorikk = inntektshistorikk)
        assertYtelser(1.januar til 31.mars)
        assertVarsler()
    }

    @Test
    fun `Sjekker overlapp mot pleiepenger`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        håndterOverstyrInntekt(30000.månedlig, skjæringstidspunkt = 1.januar)
        assertIngenVarsler()
        håndterYtelser(3.vedtaksperiode, pleiepenger = listOf(1.januar til 31.januar))
        assertYtelser(1.januar til 31.mars)
        assertVarsler()
    }

    @Test
    fun `henter ytelser for periode som strekker seg fra skjæringstidsunkt til siste tom`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1, a2)
        forlengVedtak(1.mars, 31.mars, a2)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)), a1)
        assertYtelser(1.januar til 31.mars)
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