package no.nav.helse.spleis.e2e.refusjon

import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.Avsender.ARBEIDSGIVER
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.assertBeløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.beløpstidslinje
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RefusjonsopplysningerE2ETest : AbstractDslTest() {

    @Test
    fun `Trenger nye refusjonsopplysninger ved oppholdsdager på arbeidsgiver`() {
        listOf(a1, a2).nyeVedtak(januar)
        a2 { forlengVedtak(februar) }
        a1 {
            håndterSøknad(Sykdom(2.februar, 28.februar, 100.prosent), Ferie(2.februar, 2.februar))
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `første fraværsdag oppgitt til dagen etter arbeidsgiverperioden`() {
        a1 {
            nyttVedtak(førsteFraværsdag = 17.januar, arbeidsgiverperiode = listOf(1.januar til 16.januar), periode = januar)
        }
    }

    @Test
    fun `første fraværsdag oppgitt til dagen etter arbeidsgiverperioden over helg`() {
        a1 {
            nyttVedtak(førsteFraværsdag = 22.januar, arbeidsgiverperiode = listOf(4.januar til 19.januar), periode = 4.januar til 31.januar)
        }
    }

    @Test
    fun `lager nytt innslag i vilkårsgrunnlaghistorikken med oppdaterte refusjonsopplysninger ved ny inntektsmelding`() {
        a1 {
            val arbeidsgiverperiode = listOf(1.januar til 16.januar)
            nyttVedtak(januar, arbeidsgiverperiode = arbeidsgiverperiode, førsteFraværsdag = 1.januar, refusjon = Inntektsmelding.Refusjon(INNTEKT, opphørsdato = null))
            assertBeløpstidslinje(ARBEIDSGIVER.beløpstidslinje(januar, INNTEKT), inspektør.refusjon(1.vedtaksperiode), ignoreMeldingsreferanseId = true)
            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            håndterInntektsmelding(arbeidsgiverperioder = arbeidsgiverperiode, førsteFraværsdag = 22.januar, refusjon = Inntektsmelding.Refusjon(beløp = INGEN, opphørsdato = null))
            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
            assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            assertBeløpstidslinje(
                ARBEIDSGIVER.beløpstidslinje(1.januar til 21.januar, INNTEKT) + ARBEIDSGIVER.beløpstidslinje(22.januar til 31.januar, INGEN),
                inspektør.refusjon(1.vedtaksperiode),
                ignoreMeldingsreferanseId = true
            )
        }
    }

    @Test
    fun `Duplikat innhold i ny inntektsmelding`() {
        a1 {
            val arbeidsgiverperiode = listOf(1.januar til 16.januar)
            nyttVedtak(januar, arbeidsgiverperiode = arbeidsgiverperiode, førsteFraværsdag = 1.januar, refusjon = Inntektsmelding.Refusjon(INNTEKT, opphørsdato = null))
            assertBeløpstidslinje(ARBEIDSGIVER.beløpstidslinje(januar, INNTEKT), inspektør.refusjon(1.vedtaksperiode), ignoreMeldingsreferanseId = true)
            håndterInntektsmelding(arbeidsgiverperioder = arbeidsgiverperiode, førsteFraværsdag = 1.januar, refusjon = Inntektsmelding.Refusjon(beløp = INNTEKT, opphørsdato = null))
            assertBeløpstidslinje(ARBEIDSGIVER.beløpstidslinje(januar, INNTEKT), inspektør.refusjon(1.vedtaksperiode), ignoreMeldingsreferanseId = true)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `Duplikat inntektsmelding`() {
        a1 {
            val arbeidsgiverperiode = listOf(1.januar til 16.januar)
            håndterSykmelding(januar)
            håndterSøknad(januar)
            val inntektsmeldingId = håndterInntektsmelding(arbeidsgiverperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertBeløpstidslinje(ARBEIDSGIVER.beløpstidslinje(januar, INNTEKT), inspektør.refusjon(1.vedtaksperiode), ignoreMeldingsreferanseId = true)
            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)

            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
            håndterSøknad(februar)
            håndterInntektsmelding(
                id = inntektsmeldingId,
                arbeidsgiverperioder = arbeidsgiverperiode,
            )
            assertBeløpstidslinje(ARBEIDSGIVER.beløpstidslinje(januar, INNTEKT), inspektør.refusjon(1.vedtaksperiode), ignoreMeldingsreferanseId = true)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `godtar refusjonsopplysninger selv med oppholdsdager i snuten`() {
        listOf(a1, a2).nyeVedtak(1.desember(2017) til 28.desember(2017))

        a2 {
            forlengVedtak(29.desember(2017) til 10.januar)
        }
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(1.januar, 1.januar))
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            håndterInntektsmelding(listOf(), førsteFraværsdag = 1.januar)
        }
        a2 {
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        }
        a1 {
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
        }
    }

    @Test
    fun `godtar refusjonsopplysninger selv med oppholdsdager i snuten take two`() {
        listOf(a1, a2).nyeVedtak(1.desember(2017) til 28.desember(2017))

        a2 {
            forlengVedtak(29.desember(2017) til 10.januar)
        }
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(1.januar, 1.januar))
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            håndterInntektsmelding(listOf(1.desember(2017) til 16.desember(2017)), førsteFraværsdag = 2.januar)

            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)

            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        }
        a1 {
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
        }
    }
}
