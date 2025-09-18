package no.nav.helse.spleis.e2e

import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.november
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.somOrganisasjonsnummer
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class UtbetalingTest : AbstractDslTest() {
    val ANNET_ORGNUMMER = "foo"

    @Test
    fun `Utbetaling endret får rett organisasjonsnummer ved overlappende sykemelding`() {
        ANNET_ORGNUMMER {
            håndterUtbetalingshistorikkEtterInfotrygdendring(
                ArbeidsgiverUtbetalingsperiode(this.orgnummer, 1.januar(2016), 31.januar(2016))
            )
        }
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            håndterSykmelding(Sykmeldingsperiode(2.februar, 28.februar))
            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
            håndterSøknad(2.februar til 28.februar)
            håndterSøknad(februar)

            assertEquals(a1, observatør.utbetaltEndretEventer.last().yrkesaktivitetssporing.somOrganisasjonsnummer)
        }
    }

    @Test
    fun `grad rundes av`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent, 80.prosent))
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertEquals(20, inspektør.utbetaling(0).arbeidsgiverOppdrag[0].grad)

        }
    }

    @Test
    fun `første periode er kun arbeidsgiverperiode og ferie`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(4.januar, 22.januar))
            håndterSøknad(Sykdom(4.januar, 22.januar, 100.prosent), Søknad.Søknadsperiode.Ferie(20.januar, 22.januar))
            håndterInntektsmelding(listOf(4.januar til 19.januar))
            håndterSykmelding(Sykmeldingsperiode(23.januar, 31.januar))
            håndterSøknad(23.januar til 31.januar)

            assertEquals(4.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(4.januar til 22.januar, inspektør.periode(1.vedtaksperiode))
            assertEquals(listOf(4.januar til 19.januar), inspektør.arbeidsgiverperiode(1.vedtaksperiode))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)

            assertEquals(4.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
            assertEquals(23.januar til 31.januar, inspektør.periode(2.vedtaksperiode))
            assertEquals(listOf(4.januar til 19.januar), inspektør.arbeidsgiverperiode(2.vedtaksperiode))
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)

            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)

            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            assertEquals(2, inspektør.antallUtbetalinger)
            assertEquals(1, inspektør.utbetalinger(1.vedtaksperiode).size)
            assertEquals(1, inspektør.utbetalinger(2.vedtaksperiode).size)

        }
    }

    @Test
    fun `Utbetaling med stort gap kobles ikke sammen med forrige utbetaling -- når snutete egenmeldingsdager og denne utbetalingen ikke har penger`() {
        a1 {
            nyttVedtak(januar)

            håndterSøknad(Sykdom(1.desember, 31.desember, 10.prosent))

            håndterArbeidsgiveropplysninger(
                listOf(13.november til 14.november, 1.desember til 14.desember),
                vedtaksperiodeId = 2.vedtaksperiode
            )
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)

            assertVarsel(Varselkode.RV_VV_4, 2.vedtaksperiode.filter())

            // Arbeidsgiverperioden blir beregnet riktig
            assertEquals(listOf(1.januar til 16.januar), inspektør(a1).arbeidsgiverperiode(1.vedtaksperiode))
            assertEquals(listOf(1.desember til 16.desember), inspektør(a1).arbeidsgiverperiode(2.vedtaksperiode))

            assertEquals(2, inspektør(a1).antallUtbetalinger)
            assertEquals(1.januar til 31.januar, inspektør(a1).utbetaling(0).periode)
            assertEquals(13.november til 31.desember, inspektør(a1).utbetaling(1).periode)
            assertNotEquals(inspektør(a1).utbetaling(0).korrelasjonsId, inspektør(a1).utbetaling(1).korrelasjonsId)

        }
    }
}
