package no.nav.helse.spleis.e2e

import no.nav.helse.Grunnbeløp
import no.nav.helse.april
import no.nav.helse.august
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.TestPerson
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.februar
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OppgittArbeidgiverperiode
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OppgittInntekt
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OppgittRefusjon
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingstidslinje.Maksdatoresultat
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MaksdatoE2ETest : AbstractDslTest() {

    @Test
    fun `Korrekt maksdato ved mursteinspølser og lave inntekter`() {
        val seks6 = Grunnbeløp.`6G`.beløp(1.januar)
        val a1Inntekt = seks6 * 0.5
        val a2Inntekt = seks6 * 0.2

        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 60.prosent))
        }

        a2 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 60.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = a2Inntekt)
        }

        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = a1Inntekt)
            håndterVilkårsgrunnlag(1.vedtaksperiode, skatteinntekter = listOf(a1 to a1Inntekt, a2 to a2Inntekt))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a1 {
            håndterSøknad(Sykdom(1.februar, 18.februar, 60.prosent))
            håndterSøknad(Sykdom(19.februar, 19.februar, 90.prosent))
            håndterSøknad(Sykdom(20.februar, 28.februar, 60.prosent))
        }

        a2 {
            håndterSøknad(Sykdom(1.februar, 28.februar, 60.prosent))
        }

        a1 {
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
        }

        a2 {
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            inspektør(a2).sisteMaksdato(2.vedtaksperiode).also {
                assertEquals(31, it.antallForbrukteDager)
                assertEquals(217, it.gjenståendeDager)
                assertEquals(28.desember, it.maksdato)
            }
        }

        a1 {
            håndterYtelser(3.vedtaksperiode)
            håndterSimulering(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)
            håndterUtbetalt()
        }

        a1 {
            håndterYtelser(4.vedtaksperiode)
            håndterSimulering(4.vedtaksperiode)
            håndterUtbetalingsgodkjenning(4.vedtaksperiode)
            håndterUtbetalt()
            inspektør(a1).sisteMaksdato(4.vedtaksperiode).also {
                assertEquals(31, it.antallForbrukteDager)
                assertEquals(217, it.gjenståendeDager)
                assertEquals(28.desember, it.maksdato)
            }
        }
    }

    @Test
    fun `hensyntar tidligere arbeidsgivere fra IT`() {
        a2 {
            håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a2, 1.januar, 31.januar))
        }
        a1 {
            nyPeriode(mars, a1)
            håndterArbeidsgiveropplysninger(listOf(1.mars til 16.mars), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            inspektør(a1).sisteMaksdato(1.vedtaksperiode).also {
                assertEquals(33, it.antallForbrukteDager)
                assertEquals(215, it.gjenståendeDager)
                assertEquals(25.januar(2019), it.maksdato)
            }

        }
    }

    @Test
    fun `hensyntar ikke senere arbeidsgivere fra IT`() {
        a2 {
            håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a2, 1.april, 30.april))
        }
        a1 {
            nyPeriode(mars, a1)
            håndterArbeidsgiveropplysninger(listOf(1.mars til 16.mars), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_IT_1, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            inspektør(a1).sisteMaksdato(1.vedtaksperiode).also {
                assertEquals(10, it.antallForbrukteDager)
                assertEquals(238, it.gjenståendeDager)
                assertEquals(27.februar(2019), it.maksdato)
            }
        }
    }

    @Test
    fun `maksdato inntreffer siste utbetalte dag i perioden`() {
        medMaksSykedager(11)
        a1 {
            nyttVedtak(januar)

            assertIngenInfo("Maks antall sykepengedager er nådd i perioden", 1.vedtaksperiode.filter())
            assertInfo("Maksimalt antall sykedager overskrides ikke i perioden", 1.vedtaksperiode.filter())
            assertIngenFunksjonelleFeil()
            assertTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET)
        }
    }

    @Test
    fun `maksdato inntreffer i forlengelsen`() {
        medMaksSykedager(12)
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)

            assertInfo("Maks antall sykepengedager er nådd i perioden", 2.vedtaksperiode.filter())
            assertIngenInfo("Maksimalt antall sykedager overskrides ikke i perioden", 2.vedtaksperiode.filter())
            assertIngenFunksjonelleFeil()
            assertTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET)
        }
    }

    @Test
    fun `maksdato inntreffer på annen arbeidsgiver`() {
        medMaksSykedager(12)
        a1 {
            nyPeriode(januar)
        }
        a2 {
            nyPeriode(31.januar til 28.februar)
            håndterArbeidsgiveropplysninger(1.vedtaksperiode, OppgittInntekt(INNTEKT), OppgittRefusjon(INNTEKT, emptyList()), OppgittArbeidgiverperiode(listOf(31.januar til 15.februar)))
        }
        a1 {
            håndterArbeidsgiveropplysninger(1.vedtaksperiode, OppgittInntekt(INNTEKT), OppgittRefusjon(INNTEKT, emptyList()), OppgittArbeidgiverperiode(listOf(1.januar til 16.januar)))
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertIngenInfo("Maks antall sykepengedager er nådd i perioden", 1.vedtaksperiode.filter())
            assertInfo("Maksimalt antall sykedager overskrides ikke i perioden", 1.vedtaksperiode.filter())
            assertIngenFunksjonelleFeil()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)

            assertInfo("Maks antall sykepengedager er nådd i perioden", 1.vedtaksperiode.filter())
            assertIngenInfo("Maksimalt antall sykedager overskrides ikke i perioden", 1.vedtaksperiode.filter())
            assertIngenFunksjonelleFeil()
        }
    }

    @Test
    fun `sammenhengende sykdom etter 26 uker fra maksdato - under 67 år`() {
        medMaksSykedager(10)
        a1 {
            nyttVedtak(januar)
            // setter opp vedtaksperioder 182 dager etter maksdato
            forlengVedtakUtenUtbetaling(februar)
            forlengVedtakUtenUtbetaling(mars)
            forlengVedtakUtenUtbetaling(april)
            forlengVedtakUtenUtbetaling(mai)
            forlengVedtakUtenUtbetaling(juni)
            forlengVedtakUtenUtbetaling(juli)

            assertEquals(Maksdatoresultat.Bestemmelse.ORDINÆR_RETT, inspektør(1.vedtaksperiode).maksdatoer.last().bestemmelse)
            val oppholdsdagerJuli = inspektør(7.vedtaksperiode).maksdatoer.last().oppholdsdager
            assertEquals(182, oppholdsdagerJuli.sumOf { it.count() })
            assertEquals(31.juli, oppholdsdagerJuli.last().endInclusive)

            // oppretter forlengelse fom 182 dager etter maksdato
            nyPeriode(august)
            håndterYtelser(8.vedtaksperiode)

            assertInfo("Maks antall sykepengedager er nådd i perioden", 8.vedtaksperiode.filter())
            assertIngenInfo("Maksimalt antall sykedager overskrides ikke i perioden", 8.vedtaksperiode.filter())
            assertFunksjonellFeil(Varselkode.RV_VV_9, 8.vedtaksperiode.filter())
            assertSisteTilstand(8.vedtaksperiode, TIL_INFOTRYGD) {
                "Disse periodene skal kastes ut pr nå"
            }
        }
    }

    @Test
    fun `sammenhengende sykdom etter 26 uker fra maksdato - over 67 år`() {
        val PERSON_67_ÅR_11_JANUAR_2018 = 11.januar(1951)
        medMaksSykedager(248, maksSykedagerOver67 = 10, fødselsdato = PERSON_67_ÅR_11_JANUAR_2018)
        a1 {
            nyttVedtak(januar)
            // setter opp vedtaksperioder 182 dager etter maksdato
            forlengVedtakUtenUtbetaling(februar)
            forlengVedtakUtenUtbetaling(mars)
            forlengVedtakUtenUtbetaling(april)
            forlengVedtakUtenUtbetaling(mai)
            forlengVedtakUtenUtbetaling(juni)
            forlengVedtakUtenUtbetaling(juli)

            assertEquals(Maksdatoresultat.Bestemmelse.BEGRENSET_RETT, inspektør(1.vedtaksperiode).maksdatoer.last().bestemmelse)
            val oppholdsdagerJuli = inspektør(7.vedtaksperiode).maksdatoer.last().oppholdsdager
            assertEquals(182, oppholdsdagerJuli.sumOf { it.count() })
            assertEquals(31.juli, oppholdsdagerJuli.last().endInclusive)

            // oppretter forlengelse fom 182 dager etter maksdato
            nyPeriode(august)
            håndterYtelser(8.vedtaksperiode)

            assertInfo("Maks antall sykepengedager er nådd i perioden", 8.vedtaksperiode.filter())
            assertIngenInfo("Maksimalt antall sykedager overskrides ikke i perioden", 8.vedtaksperiode.filter())
            assertFunksjonellFeil(Varselkode.RV_VV_9, 8.vedtaksperiode.filter())
            assertSisteTilstand(8.vedtaksperiode, TIL_INFOTRYGD) {
                "Disse periodene skal kastes ut pr nå"
            }
        }
    }

    private fun TestPerson.TestArbeidsgiver.forlengVedtakUtenUtbetaling(periode: Periode) {
        val vedtaksperiode = nyPeriode(periode)
        håndterYtelser(vedtaksperiode)
        håndterUtbetalingsgodkjenning(vedtaksperiode)
    }
}
