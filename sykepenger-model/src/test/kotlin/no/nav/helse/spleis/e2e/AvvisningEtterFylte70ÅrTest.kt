package no.nav.helse.spleis.e2e

import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.erHelg
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.mars
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class AvvisningEtterFylte70ÅrTest : AbstractDslTest() {
    companion object {
        private val FYLLER_70_TIENDE_JANUAR_FØDSELSDATO = 10.januar(1948)
        private val FYLLER_70_FJORTENDE_JANUAR_FØDSELSDATO = 14.januar(1948)
        private val FYLLER_70_TOOGTYVENDE_JANUAR_FØDSELSDATO = 22.januar(1948)
    }

    @Test
    fun `Person over 70 får alle dager etter AGP avvist med Over70 som begrunnelse`() {
        medFødselsdato(FYLLER_70_FJORTENDE_JANUAR_FØDSELSDATO)
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)

            val avvisteDager = observatør.utbetalingUtenUtbetalingEventer.first().utbetalingsdager.filter { it.type == EventSubscription.Utbetalingsdag.Dagtype.AvvistDag }
            val arbeidsgiverperiodedager = observatør.utbetalingUtenUtbetalingEventer.first().utbetalingsdager.filter { it.type == EventSubscription.Utbetalingsdag.Dagtype.ArbeidsgiverperiodeDag }
            val navDager = observatør.utbetalingUtenUtbetalingEventer.first().utbetalingsdager.filter { it.type == EventSubscription.Utbetalingsdag.Dagtype.NavDag }
            val navHelgedager = observatør.utbetalingUtenUtbetalingEventer.first().utbetalingsdager.filter { it.type == EventSubscription.Utbetalingsdag.Dagtype.NavHelgDag }

            assertEquals(11, avvisteDager.size)
            assertEquals(16, arbeidsgiverperiodedager.size)
            assertEquals(0, navDager.size)
            assertEquals(4, navHelgedager.size)
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_HISTORIKK,
                AVVENTER_GODKJENNING,
                AVSLUTTET
            )
        }
    }

    @Test
    fun `Får ikke plutselig penger igjen etter 26 uker hvis over 70 år`() {
        medFødselsdato(FYLLER_70_TIENDE_JANUAR_FØDSELSDATO)
        a1 {
            håndterSykmelding(Sykmeldingsperiode(20.desember(2017), 9.januar))
            håndterSøknad(20.desember(2017) til 9.januar)
            håndterArbeidsgiveropplysninger(
                listOf(20.desember(2017) til 4.januar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterSykmelding(Sykmeldingsperiode(11.juli, 31.juli))
            håndterSøknad(11.juli til 31.juli)
            håndterArbeidsgiveropplysninger(
                listOf(11.juli til 26.juli),
                vedtaksperiodeId = 2.vedtaksperiode
            )
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)

            val avvisteDager = observatør.utbetalingUtenUtbetalingEventer.first().utbetalingsdager.filter { it.type == EventSubscription.Utbetalingsdag.Dagtype.AvvistDag }
            val arbeidsgiverperiodedager = observatør.utbetalingUtenUtbetalingEventer.first().utbetalingsdager.filter { it.type == EventSubscription.Utbetalingsdag.Dagtype.ArbeidsgiverperiodeDag }

            assertEquals(3, avvisteDager.size)
            assertEquals(16, arbeidsgiverperiodedager.size)
            assertTrue(avvisteDager.all { it.begrunnelser == listOf(EventSubscription.Utbetalingsdag.EksternBegrunnelseDTO.Over70) })
            assertTrue(arbeidsgiverperiodedager.all { it.begrunnelser == null })
        }
    }

    @Test
    fun `Skal kun få avvist pga 70, selv om man også ville fått avvist pga MinimumInntektOver67 om man var 69`() {
        medFødselsdato(FYLLER_70_TIENDE_JANUAR_FØDSELSDATO)
        a1 {
            håndterSykmelding(februar)
            håndterSøknad(februar)
            håndterArbeidsgiveropplysninger(
                arbeidsgiverperioder = listOf(1.februar til 16.februar),
                beregnetInntekt = 10000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)

            val avvisteDager = observatør.utbetalingUtenUtbetalingEventer.first().utbetalingsdager.filter { it.type == EventSubscription.Utbetalingsdag.Dagtype.AvvistDag }

            assertEquals(8, avvisteDager.size)

            val periodenEtterAGP = 17.februar til 28.februar
            periodenEtterAGP.filter { !it.erHelg() }.forEach { dato ->
                assertEquals(listOf(
                    EventSubscription.Utbetalingsdag.EksternBegrunnelseDTO.MinimumInntektOver67,
                    EventSubscription.Utbetalingsdag.EksternBegrunnelseDTO.Over70
                ), avvisteDager.first { it.dato == dato }.begrunnelser)
            }

            assertVarsler(1.vedtaksperiode, Varselkode.RV_SV_1)
        }
    }

    @Test
    fun `Skal kun få avvist pga 70 for siste del av perioden, selv om man også har fått avvist for MinimumInntektOver67 ved skjæringstidspunktet`() {
        medFødselsdato(FYLLER_70_TIENDE_JANUAR_FØDSELSDATO)
        a1 {
            håndterSykmelding(15.desember(2017) til 15.januar)
            håndterSøknad(15.desember(2017) til 15.januar)
            håndterArbeidsgiveropplysninger(
                arbeidsgiverperioder = listOf(15.desember(2017) til 30.desember(2017)),
                beregnetInntekt = 10000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)

            val avvisteDager = observatør.utbetalingUtenUtbetalingEventer.first().utbetalingsdager.filter { it.type == EventSubscription.Utbetalingsdag.Dagtype.AvvistDag }

            assertEquals(11, avvisteDager.size)

            val før70 = 31.desember(2017) til 9.januar
            val etter70 = 10.januar til 15.januar

            avvisteDager.filter { it.dato in før70 }

            før70.filter { !it.erHelg() }.forEach { dato ->
                assertEquals(listOf(
                    EventSubscription.Utbetalingsdag.EksternBegrunnelseDTO.MinimumInntektOver67,
                ), avvisteDager.first { it.dato == dato }.begrunnelser)
            }

            etter70.filter { !it.erHelg() }.forEach { dato ->
                assertEquals(listOf(
                    EventSubscription.Utbetalingsdag.EksternBegrunnelseDTO.MinimumInntektOver67,
                    EventSubscription.Utbetalingsdag.EksternBegrunnelseDTO.Over70
                ), avvisteDager.first { it.dato == dato }.begrunnelser)
            }

            assertVarsler(1.vedtaksperiode, Varselkode.RV_SV_1)
        }
    }

    @Test
    fun `Maksdato settes til dagen før 70årsdagen uavhengig av opptjente sykedager dersom person fyller 70 i perioden`() {
        medFødselsdato(FYLLER_70_TIENDE_JANUAR_FØDSELSDATO)
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)

            assertEquals(9.januar, inspektør.sisteMaksdato(1.vedtaksperiode).maksdato)
        }
    }

    @Test
    fun `Maksdato settes til dagen før 70årsdagen også for perioder etter fylte 70`() {
        medFødselsdato(FYLLER_70_TIENDE_JANUAR_FØDSELSDATO)
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(mars)
            håndterArbeidsgiveropplysninger(listOf(1.mars til 16.mars), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)

            assertEquals(9.januar, inspektør.sisteMaksdato(1.vedtaksperiode).maksdato)
        }
    }

    @Test
    fun `Maksdato settes til virkedagen før 70årsdagen hvis bursdagen er på en søndag`() {
        medFødselsdato(FYLLER_70_FJORTENDE_JANUAR_FØDSELSDATO)
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)

            assertEquals(12.januar, inspektør.sisteMaksdato(1.vedtaksperiode).maksdato)
        }
    }

    @Test
    fun `Maksdato settes til virkedagen før 70årsdagen hvis bursdagen er på en mandag`() {
        medFødselsdato(FYLLER_70_TOOGTYVENDE_JANUAR_FØDSELSDATO)
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)

            assertEquals(19.januar, inspektør.sisteMaksdato(1.vedtaksperiode).maksdato)
        }
    }
}
