package no.nav.helse.spleis.e2e

import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ManglerVilkårsgrunnlagE2ETest : AbstractDslTest() {

    @Test
    fun `Inntektsmelding opplyser om endret arbeidsgiverperiode - AUU periode inneholder utbetalingsdag`() {
        a1 {
            nyPeriode(2.januar til 17.januar, a1)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertEquals(2.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode))

            nyPeriode(22.januar til 31.januar, a1)
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                førsteFraværsdag = 22.januar
            )

            assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode))

            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                førsteFraværsdag = 1.januar
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `Infotrygd utbetaler periode i forkant - Skjæringstidspunktet flytter seg`() {
        a1 {
            nyttVedtak(februar)
            assertEquals(1.februar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertNotNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode))

            håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar))
            nyPeriode(10.mars til 31.mars, a1)
            håndterArbeidsgiveropplysninger(
                arbeidsgiverperioder = emptyList(),
                vedtaksperiodeId = 2.vedtaksperiode
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertVarsler(listOf(Varselkode.RV_VV_2, Varselkode.RV_IT_14), 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterVilkårsgrunnlag(2.vedtaksperiode)

            assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(setOf(1.januar, 10.mars), inspektør.vilkårsgrunnlaghistorikk().aktiveSpleisSkjæringstidspunkt)

            assertEquals(emptyList<Periode>(), inspektør.arbeidsgiverperiode(1.vedtaksperiode))
            assertEquals(emptyList<Periode>(), inspektør.arbeidsgiverperiode(2.vedtaksperiode))

            håndterYtelser(2.vedtaksperiode)
            inspektør.utbetaling(1).also { utbetalinginspektør ->
                assertEquals(1, utbetalinginspektør.arbeidsgiverOppdrag.size)
                assertEquals(1.februar til 28.februar, utbetalinginspektør.arbeidsgiverOppdrag[0].inspektør.periode)
            }
            inspektør.utbetaling(2).also { utbetalinginspektør ->
                assertEquals(1, utbetalinginspektør.arbeidsgiverOppdrag.size)
                assertEquals(10.mars til 30.mars, utbetalinginspektør.arbeidsgiverOppdrag[0].inspektør.periode)
            }
        }
    }

    @Test
    fun `søknad omgjør paddet arbeidsdager til syk`() {
        a1 {
            håndterSøknad(1.januar til 3.januar)
            håndterSøknad(31.januar til 5.februar)
            // perioden 4. til 9.januar er paddet arbeidsdager; perioden 23.januar til 30.januar er "implisitte arbeidsdager" (ukjentdager på sykdomtsidslinjen)
            håndterInntektsmelding(
                listOf(1.januar til 3.januar, 10.januar til 22.januar),
                førsteFraværsdag = 31.januar
            )
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            inspektør.sykdomstidslinje.inspektør.also { sykdomstidslinjeInspektør ->
                assertInstanceOf(Dag.Arbeidsdag::class.java, sykdomstidslinjeInspektør[4.januar])
                assertInstanceOf(Dag.Arbeidsdag::class.java, sykdomstidslinjeInspektør[9.januar])
                assertInstanceOf(Dag.UkjentDag::class.java, sykdomstidslinjeInspektør[23.januar])
                assertInstanceOf(Dag.UkjentDag::class.java, sykdomstidslinjeInspektør[30.januar])
            }

            observatør.vedtaksperiodeVenter.clear()

            håndterSøknad(10.januar til 26.januar)
            assertEquals(listOf(31.januar, 10.januar), inspektør.skjæringstidspunkter(2.vedtaksperiode))
            assertVarsel(Varselkode.RV_IV_11, 2.vedtaksperiode.filter())

            inspektør.sykdomstidslinje.inspektør.also { sykdomstidslinjeInspektør ->
                assertInstanceOf(Dag.Arbeidsdag::class.java, sykdomstidslinjeInspektør[4.januar])
                assertInstanceOf(Dag.Arbeidsdag::class.java, sykdomstidslinjeInspektør[9.januar])
                assertInstanceOf(Dag.Sykedag::class.java, sykdomstidslinjeInspektør[23.januar])
                assertInstanceOf(Dag.Sykedag::class.java, sykdomstidslinjeInspektør[26.januar])
            }

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertEquals(31.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))

        }
    }

    @Test
    fun `Inntektsmelding sletter vilkårsgrunnlag og trekker tilbake penger`() {
        medJSONPerson("/personer/infotrygdforlengelse.json")

        a1 {
            håndterUtbetalingshistorikkEtterInfotrygdendring(utbetalinger = listOf(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar)))
            assertEquals(januar, testperson.person.infotrygdhistorikk.betaltePerioder().single())
            val førsteVedtaksperiode = inspektør.vedtaksperioder(1.vedtaksperiode)
            assertEquals(februar, førsteVedtaksperiode.periode)

            val assertTilstandFørInntektsmeldingHensyntas: () -> Unit = {
                val førsteUtbetalingsdagIInfotrygd = 1.januar
                assertEquals(førsteUtbetalingsdagIInfotrygd, førsteVedtaksperiode.skjæringstidspunkt)
                assertNotNull(testperson.person.vilkårsgrunnlagFor(førsteVedtaksperiode.skjæringstidspunkt))
                assertTrue(testperson.person.vilkårsgrunnlagFor(førsteVedtaksperiode.skjæringstidspunkt)!!.inspektør.infotrygd)
            }

            assertTilstandFørInntektsmeldingHensyntas()

            håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars))
            // Arbeidsgiver sender inntektsmelding for forlengelse i Mars _før_ vi møttar søknad.
            // Så lenge det ikke treffer noen vedtaksperiode i Spleis skjer det ingenting.
            // Personen vært frisk 1. & 2.Mars, så er nytt skjæringstidspunkt, men samme arbeidsgiverperiode
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(16.desember(2017) til 31.desember(2017)),
                førsteFraværsdag = 5.mars
            )
            assertInfo("Inntektsmelding før søknad - er relevant for sykmeldingsperioder [05-03-2018 til 31-03-2018]")
            assertTilstandFørInntektsmeldingHensyntas()

            // Når søknaden kommer replayes Inntektsmelding og nå puttes plutselig info fra Inntektsmlding på
            // arbeidsgiver, også lengre tilbake i tid enn vedtaksperioden som blir truffet.
            håndterSøknad(5.mars til 31.mars)
            assertVarsel(Varselkode.RV_IM_3, 2.vedtaksperiode.filter())
            assertTilstandFørInntektsmeldingHensyntas()
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_SIMULERING)
        }
    }

    @Test
    fun `korrigert arbeidsgiverperiode under pågående revurdering`() {
        a1 {
            nyttVedtak(januar, 100.prosent)
            forlengVedtak(februar, 100.prosent)
            håndterSøknad(Sykdom(1.januar, 31.januar, 80.prosent))

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
            nullstillTilstandsendringer()
            håndterInntektsmelding(
                listOf(1.februar til 16.februar),
                førsteFraværsdag = 1.februar
            )
            assertVarsler(listOf(Varselkode.RV_IM_24), 2.vedtaksperiode.filter())
            assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(1.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))

            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)

        }
    }

    @Test
    fun `korrigert arbeidsgiverperiode under pågående revurdering - korrigert søknad for februar`() {
        a1 {
            nyttVedtak(januar, 100.prosent)
            forlengVedtak(februar, 100.prosent)
            håndterSøknad(Sykdom(1.februar, 28.februar, 80.prosent))

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            nullstillTilstandsendringer()
            håndterInntektsmelding(
                listOf(1.februar til 16.februar),
                førsteFraværsdag = 1.februar
            )
            assertVarsler(listOf(Varselkode.RV_IM_24), 2.vedtaksperiode.filter())
            assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(1.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))

            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)

            assertVarsel(Varselkode.RV_UT_23, 2.vedtaksperiode.filter())
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)
        }
    }
}
