package no.nav.helse.spleis.e2e.arbeidsgiveropplysninger

import java.util.*
import no.nav.helse.Toggle
import no.nav.helse.april
import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.februar
import no.nav.helse.fredag
import no.nav.helse.hendelser.Arbeidsgiveropplysning.Begrunnelse.ManglerOpptjening
import no.nav.helse.hendelser.Arbeidsgiveropplysning.Begrunnelse.StreikEllerLockout
import no.nav.helse.hendelser.Arbeidsgiveropplysning.IkkeNyArbeidsgiverperiode
import no.nav.helse.hendelser.Arbeidsgiveropplysning.IkkeUtbetaltArbeidsgiverperiode
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OppgittArbeidgiverperiode
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OppgittInntekt
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OppgittRefusjon
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OpphørAvNaturalytelser
import no.nav.helse.hendelser.Arbeidsgiveropplysning.RedusertUtbetaltBeløpIArbeidsgiverperioden
import no.nav.helse.hendelser.Arbeidsgiveropplysning.UtbetaltDelerAvArbeidsgiverperioden
import no.nav.helse.hendelser.Avsender.ARBEIDSGIVER
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mandag
import no.nav.helse.person.DokumentType
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonObserver.Inntekt
import no.nav.helse.person.PersonObserver.Refusjon
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_24
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_25
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_OO_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_2
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.arbeidsgiver
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.assertBeløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.beløpstidslinje
import no.nav.helse.person.inntekt.Inntektsmeldinginntekt
import no.nav.helse.person.inntekt.SkattSykepengegrunnlag
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ArbeidsgiveropplysningerTest : AbstractDslTest() {

    @Test
    fun `tøysete egenmeldingsdag skaper loop av forespørsler`() {
        a1 {
            nyttVedtak(1.januar til fredag(19.januar))
            håndterSøknad(Sykdom(15.februar, 20.februar, 100.prosent), egenmeldinger = listOf(mandag(5.februar).somPeriode()))
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            val forespørselPgaEgenmeldingsdager = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.single { it.vedtaksperiodeId == 2.vedtaksperiode }
            assertEquals(listOf(5.februar.somPeriode()), inspektør.vedtaksperioder(2.vedtaksperiode).egenmeldingsperioder)
            assertEquals(listOf(Inntekt, Refusjon), forespørselPgaEgenmeldingsdager.forespurteOpplysninger)
            observatør.trengerArbeidsgiveropplysningerVedtaksperioder.clear()
            håndterArbeidsgiveropplysninger(2.vedtaksperiode, OppgittInntekt(INNTEKT), OppgittRefusjon(INNTEKT, emptyList()))
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            val forespørselPgaEgenmeldingsdager2 = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.single { it.vedtaksperiodeId == 2.vedtaksperiode }
            assertEquals(listOf(5.februar.somPeriode()), inspektør.vedtaksperioder(2.vedtaksperiode).egenmeldingsperioder)
            assertEquals(listOf(Inntekt, Refusjon), forespørselPgaEgenmeldingsdager2.forespurteOpplysninger)
        }
    }

    @Test
    fun `Mange auuer hvor de to siste vil utbetales om egenmeldingsdagene er rett på auuen i snuten`() {
        a1 {
            håndterSøknad(Sykdom(5.januar, 10.januar, 100.prosent), egenmeldinger = listOf(1.januar til 4.januar))
            håndterSøknad(Sykdom(11.januar, 16.januar, 100.prosent))
            håndterSøknad(Sykdom(17.januar, 17.januar, 100.prosent))
            håndterSøknad(Sykdom(18.januar, 18.januar, 100.prosent))

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(4.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

            assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
            assertNotNull(observatør.trengerArbeidsgiveropplysningerVedtaksperioder.single { it.vedtaksperiodeId == 3.vedtaksperiode })
            assertNotNull(observatør.trengerArbeidsgiveropplysningerVedtaksperioder.single { it.vedtaksperiodeId == 4.vedtaksperiode })
        }
    }

    @Test
    fun `En egenmeldingsdag som oppgis etter gjennomført arbeidsgiverperiode`() {
        a1 {
            håndterSøknad(1.januar til 16.januar)
            håndterSøknad(Sykdom(4.februar, 13.februar, 100.prosent), egenmeldinger = listOf(20.januar.somPeriode()))
            håndterSøknad(14.februar til 19.februar)

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

            // Litt tøysete forespørsler
            assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
            assertNotNull(observatør.trengerArbeidsgiveropplysningerVedtaksperioder.single { it.vedtaksperiodeId == 2.vedtaksperiode })
            assertNotNull(observatør.trengerArbeidsgiveropplysningerVedtaksperioder.single { it.vedtaksperiodeId == 3.vedtaksperiode })
        }
    }

    @Test
    fun `korrigerende opplysninger på periode med kun arbeid`() {
        a1 {
            håndterSøknad(januar)
            håndterSøknad(februar)
            håndterArbeidsgiveropplysninger(
                1.vedtaksperiode,
                OppgittArbeidgiverperiode(listOf(1.februar til 16.februar)),
                OppgittInntekt(INNTEKT),
                OppgittRefusjon(INNTEKT, emptyList())
            )
            assertEquals("AAAAARR AAAAARR AAAAARR AAAAARR AAA", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            håndterKorrigerteArbeidsgiveropplysninger(
                1.vedtaksperiode,
                OppgittArbeidgiverperiode(listOf(1.januar til 16.januar))
            )
            assertVarsler(1.vedtaksperiode, RV_IM_24)
        }
    }

    @Test
    fun `oppgir refusjonopplysninger frem i tid, og så ombestemmer de seg`() {
        a1 {
            håndterSøknad(januar)
            val arbeidsgiver1 = håndterArbeidsgiveropplysninger(
                1.vedtaksperiode,
                OppgittArbeidgiverperiode(listOf(1.januar til 16.januar)),
                OppgittInntekt(25_000.månedlig),
                OppgittRefusjon(25_000.månedlig, listOf(OppgittRefusjon.Refusjonsendring(1.februar, INGEN)))
            )
            assertBeløpstidslinje(Beløpstidslinje.fra(januar, 25_000.månedlig, arbeidsgiver1.arbeidsgiver), inspektør.refusjon(1.vedtaksperiode))
            assertBeløpstidslinje(Beløpstidslinje.fra(1.februar.somPeriode(), INGEN, arbeidsgiver1.arbeidsgiver), inspektør.ubrukteRefusjonsopplysninger.refusjonstidslinjer.values.single())

            val arbeidsgiver2 = håndterKorrigerteArbeidsgiveropplysninger(
                1.vedtaksperiode,
                OppgittRefusjon(25_000.månedlig, emptyList())
            )

            assertBeløpstidslinje(Beløpstidslinje.fra(januar, 25_000.månedlig, arbeidsgiver2.arbeidsgiver), inspektør.refusjon(1.vedtaksperiode))
            assertBeløpstidslinje(Beløpstidslinje.fra(1.februar.somPeriode(), 25_000.månedlig, arbeidsgiver2.arbeidsgiver), inspektør.ubrukteRefusjonsopplysninger.refusjonstidslinjer.values.single())
        }
    }

    @Test
    fun `oppgir inntekt når vi allerede har skatt i inntektsgrunnlaget - syk fra ghost samme måned`() {
        listOf(a1).nyeVedtak(januar, inntekt = 20_000.månedlig, ghosts = listOf(a2))
        a1 {
            assertVarsler(1.vedtaksperiode, RV_VV_2)
        }
        a2 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(1.vedtaksperiode, OppgittInntekt(25_000.månedlig), OppgittRefusjon(25_000.månedlig, emptyList()))
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
            inspektør.inntekt(1.vedtaksperiode).let {
                assertTrue(it is Inntektsmeldinginntekt)
                assertEquals(25_000.månedlig, it.inntektsdata.beløp)
            }
            assertBeløpstidslinje(ARBEIDSGIVER.beløpstidslinje(januar, 25_000.månedlig), inspektør.refusjon(1.vedtaksperiode), ignoreMeldingsreferanseId = true)
        }
    }

    @Test
    fun `oppgir inntekt når vi allerede har skatt i inntektsgrunnlaget - syk fra ghost annen måned`() {
        listOf(a1).nyeVedtak(januar, inntekt = 20_000.månedlig, ghosts = listOf(a2))
        a1 {
            assertVarsler(1.vedtaksperiode, RV_VV_2)
        }
        a2 {
            håndterSøknad(februar)
            assertTrue(inspektør.inntekt(1.januar) is SkattSykepengegrunnlag)
            håndterArbeidsgiveropplysninger(1.vedtaksperiode, OppgittInntekt(25_000.månedlig), OppgittRefusjon(25_000.månedlig, emptyList()))
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
            inspektør.inntekt(1.vedtaksperiode).let {
                assertTrue(it is Inntektsmeldinginntekt)
                assertEquals(25_000.månedlig, it.inntektsdata.beløp)
            }
            assertBeløpstidslinje(ARBEIDSGIVER.beløpstidslinje(februar, 25_000.månedlig), inspektør.refusjon(1.vedtaksperiode), ignoreMeldingsreferanseId = true)
        }
    }

    @Test
    fun `oppgir at det ikke er noen ny arbeidsgiverperiode på lang periode`() {
        a1 {
            håndterSøknad(januar)
            assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            håndterArbeidsgiveropplysninger(1.vedtaksperiode, OppgittInntekt(INNTEKT), OppgittRefusjon(INNTEKT, emptyList()), IkkeNyArbeidsgiverperiode)
            assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertVarsler(listOf(RV_IM_25), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `oppgir at det ikke er noen ny arbeidsgiverperiode på kort periode`() {
        a1 {
            håndterSøknad(Sykdom(3.januar, 18.januar, 50.prosent), egenmeldinger = listOf(1.januar til 2.januar))
            assertEquals("SSSHH SSSSSHH SSSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            håndterArbeidsgiveropplysninger(1.vedtaksperiode, OppgittInntekt(INNTEKT), OppgittRefusjon(INNTEKT, emptyList()), IkkeNyArbeidsgiverperiode)
            assertEquals("NSSHH SSSSSHH SSSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            assertEquals(50.prosent, (inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje[3.januar] as Dag.SykedagNav).økonomi.grad)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertVarsler(listOf(RV_IM_25), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `oppgir at de ikke har utbetalt arbeidsgiverperioden på lang periode`() {
        a1 {
            håndterSøknad(januar)
            assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            håndterArbeidsgiveropplysninger(1.vedtaksperiode, OppgittInntekt(INNTEKT), OppgittRefusjon(INNTEKT, emptyList()), IkkeUtbetaltArbeidsgiverperiode(ManglerOpptjening))
            assertEquals("NNNNNHH NNNNNHH NNSSSHH SSSSSHH SSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertVarsler(listOf(RV_IM_8), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `oppgir at de ikke har utbetalt arbeidsgiverperioden på kort periode`() {
        a1 {
            håndterSøknad(Sykdom(3.januar, 18.januar, 69.prosent), egenmeldinger = listOf(1.januar til 2.januar))
            assertEquals("SSSHH SSSSSHH SSSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            håndterArbeidsgiveropplysninger(1.vedtaksperiode, OppgittInntekt(INNTEKT), OppgittRefusjon(INNTEKT, emptyList()), IkkeUtbetaltArbeidsgiverperiode(ManglerOpptjening))
            assertEquals("NNNHH NNNNNHH NNNN", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            assertTrue(inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.filterIsInstance<Dag.SykedagNav>().all { it.økonomi.grad == 69.prosent })
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertVarsler(listOf(RV_IM_8), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `oppgir at de ikke har utbetalt arbeidsgiverperioden når arbeidsgiverperiode er i flere perioder`() {
        a1 {
            håndterSøknad(1.januar til 10.januar)
            håndterSøknad(11.januar til 31.januar)
            assertEquals("SSSSSHH SSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            assertEquals("SSHH SSSSSHH SSSSSHH SSS", inspektør.vedtaksperioder(2.vedtaksperiode).sykdomstidslinje.toShortString())
            håndterArbeidsgiveropplysninger(2.vedtaksperiode, OppgittInntekt(INNTEKT), OppgittRefusjon(INNTEKT, emptyList()), IkkeUtbetaltArbeidsgiverperiode(ManglerOpptjening))
            assertEquals("SSSSSHH SSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            assertEquals("NNHH NNSSSHH SSSSSHH SSS", inspektør.vedtaksperioder(2.vedtaksperiode).sykdomstidslinje.toShortString())
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertVarsler(listOf(RV_IM_8), 2.vedtaksperiode.filter())
        }
    }

    @Test
    fun `oppgir at de ikke har utbetalt arbeidsgiverperioden når arbeidsgiverperiode i sin helhet ligger i perioden før`() {
        a1 {
            håndterSøknad(1.januar til 16.januar)
            håndterSøknad(17.januar til 31.januar)
            assertEquals("SSSSSHH SSSSSHH SS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            assertEquals("SSSHH SSSSSHH SSS", inspektør.vedtaksperioder(2.vedtaksperiode).sykdomstidslinje.toShortString())
            håndterArbeidsgiveropplysninger(2.vedtaksperiode, OppgittInntekt(INNTEKT), OppgittRefusjon(INNTEKT, emptyList()), IkkeUtbetaltArbeidsgiverperiode(ManglerOpptjening))
            assertEquals("SSSSSHH SSSSSHH SS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            assertEquals("SSSHH SSSSSHH SSS", inspektør.vedtaksperioder(2.vedtaksperiode).sykdomstidslinje.toShortString())
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertVarsler(listOf(RV_IM_8), 2.vedtaksperiode.filter())
        }
    }

    @Test
    fun `oppgir at de har utbetalt redusert beløp i hullete arbeidsgiverperiode`() {
        a1 {
            håndterSøknad(januar)
            assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            håndterArbeidsgiveropplysninger(
                1.vedtaksperiode,
                OppgittInntekt(INNTEKT),
                OppgittRefusjon(INNTEKT, emptyList()),
                OppgittArbeidgiverperiode(listOf(1.januar til 6.januar, 10.januar til 15.januar, 20.januar til 23.januar)),
                RedusertUtbetaltBeløpIArbeidsgiverperioden(ManglerOpptjening)
            )
            assertEquals("NNNNNHR AANNNHH NAAAAHH NNSSSHH SSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            assertEquals(20.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(listOf(20.januar, 10.januar, 1.januar), inspektør.skjæringstidspunkter(1.vedtaksperiode))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertVarsler(listOf(RV_IM_8), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `oppgir at de kun har utbetalt deler av hullete arbeidsgiverperiode`() {
        a1 {
            håndterSøknad(januar)
            assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            håndterArbeidsgiveropplysninger(
                1.vedtaksperiode,
                OppgittInntekt(INNTEKT),
                OppgittRefusjon(INNTEKT, emptyList()),
                OppgittArbeidgiverperiode(listOf(1.januar til 6.januar, 10.januar til 15.januar)),
                UtbetaltDelerAvArbeidsgiverperioden(ManglerOpptjening, 15.januar)
            )
            assertEquals("SSSSSHR AASSSHH SNNNNHH SSSSSHH SSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertVarsler(listOf(RV_IM_8), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `oppgir at det er opphør av naturalytelser`() = Toggle.OpphørAvNaturalytelser.enable {
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(
                1.vedtaksperiode,
                OppgittInntekt(INNTEKT),
                OppgittRefusjon(INNTEKT, emptyList()),
                OppgittArbeidgiverperiode(listOf(1.januar til 16.januar)),
                OpphørAvNaturalytelser,
            )
            if (Toggle.OpphørAvNaturalytelser.enabled) {
                assertVarsel(Varselkode.RV_IM_7, 1.vedtaksperiode.filter())
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            } else {
                assertFunksjonellFeil(Varselkode.RV_IM_7, 1.vedtaksperiode.filter())
                assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            }
        }
    }

    @Test
    fun `oppgir at de har utbetalt redusert beløp med arbeidsgiverperiode flyttet litt frem`() {
        a1 {
            håndterSøknad(januar)
            assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            håndterArbeidsgiveropplysninger(
                1.vedtaksperiode,
                OppgittInntekt(INNTEKT),
                OppgittRefusjon(INNTEKT, emptyList()),
                OppgittArbeidgiverperiode(listOf(5.januar til 20.januar)),
                RedusertUtbetaltBeløpIArbeidsgiverperioden(ManglerOpptjening)
            )
            assertEquals("AAAANHH NNNNNHH NNNNNHH SSSSSHH SSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertVarsler(listOf(RV_IM_8), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `oppgir en begrunnelse vi ikke støtter`() {
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(1.vedtaksperiode, IkkeUtbetaltArbeidsgiverperiode(StreikEllerLockout))
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            assertFunksjonellFeil(RV_IM_8, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `oppgir at egenmeldingsdager fra sykmelding stemmer kort periode`() {
        a1 {
            håndterSøknad(Sykdom(2.januar, 17.januar, 100.prosent), egenmeldinger = listOf(1.januar.somPeriode()))
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            håndterArbeidsgiveropplysninger(1.vedtaksperiode, OppgittArbeidgiverperiode(listOf(1.januar til 16.januar)), OppgittInntekt(INNTEKT), OppgittRefusjon(INNTEKT, emptyList()))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `oppgir at egenmeldingsdager fra sykmelding stemmer for kort periode - kommet søknad i forkant`() {
        a1 {
            håndterSøknad(Sykdom(2.april, 17.april, 100.prosent), egenmeldinger = listOf(1.april.somPeriode()))
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

            tilGodkjenning(januar)
            assertVarsler(2.vedtaksperiode, RV_OO_1)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)

            val id = håndterArbeidsgiveropplysninger(1.vedtaksperiode, OppgittArbeidgiverperiode(listOf(1.april til 16.april)), OppgittInntekt(INNTEKT), OppgittRefusjon(INNTEKT, emptyList()))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertTrue(id to 1.vedtaksperiode in observatør.inntektsmeldingHåndtert)

            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `uenige om arbeidsgiverperiode med NAV_NO som avsendersystem gir varsel`()  {
        setupLiteGapA2SammeSkjæringstidspunkt()
        a2 {
            håndterArbeidsgiveropplysninger(listOf(2.januar til 17.januar), vedtaksperiodeId = 2.vedtaksperiode)

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
            assertInfo("Håndterer ikke arbeidsgiverperiode i AVSLUTTET", 1.vedtaksperiode.filter())
            assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
            val forespørselFebruar = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last { it.vedtaksperiodeId == 2.vedtaksperiode }
            assertEquals(0, forespørselFebruar.forespurteOpplysninger.filterIsInstance<PersonObserver.Arbeidsgiverperiode>().size)
            assertEquals(0, forespørselFebruar.forespurteOpplysninger.filterIsInstance<Inntekt>().size)
            assertEquals(1, forespørselFebruar.forespurteOpplysninger.filterIsInstance<Refusjon>().size)
        }
        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `tom arbeidsgiverperiode med NAV_NO som avsendersystem gir ikke varsel`()  {
        setupLiteGapA2SammeSkjæringstidspunkt()
        a2 {
            håndterArbeidsgiveropplysninger(emptyList(), vedtaksperiodeId = 2.vedtaksperiode)
            assertVarsler(emptyList(), 2.vedtaksperiode.filter())
        }
    }

    @Test
    fun `dokumentsporing fra inntektsmelding`() {
        a1 {
            håndterSøknad(januar)
            val id = håndterArbeidsgiveropplysninger(
                1.vedtaksperiode,
                OppgittInntekt(INNTEKT),
                OppgittRefusjon(INNTEKT, emptyList()),
                OppgittArbeidgiverperiode(listOf(1.januar til 16.januar))
            )
            assertForventetFeil(
                forklaring = "Må legge til for inntektsmeldingInntekt",
                ønsket = {
                    assertDokumentsporingPåSisteBehandling(
                        1.vedtaksperiode,
                        Dokumentsporing.inntektsmeldingDager(id),
                        Dokumentsporing.inntektsmeldingInntekt(id),
                        Dokumentsporing.inntektsmeldingRefusjon(id)
                    )
                },
                nå = {
                    assertDokumentsporingPåSisteBehandling(
                        1.vedtaksperiode,
                        Dokumentsporing.inntektsmeldingDager(id),
                        Dokumentsporing.inntektsmeldingRefusjon(id)
                    )
                }
            )

            val idKorrigert = håndterKorrigerteArbeidsgiveropplysninger(
                1.vedtaksperiode,
                OppgittInntekt(INNTEKT * 1.10)
            )
            assertForventetFeil(
                forklaring = "Må legge til for inntektsmeldingInntekt",
                ønsket = {
                    assertDokumentsporingPåSisteBehandling(
                        1.vedtaksperiode,
                        Dokumentsporing.inntektsmeldingDager(id),
                        Dokumentsporing.inntektsmeldingInntekt(id),
                        Dokumentsporing.inntektsmeldingRefusjon(id),
                        Dokumentsporing.inntektsmeldingInntekt(idKorrigert)
                    )
                },
                nå = {
                    assertDokumentsporingPåSisteBehandling(
                        1.vedtaksperiode,
                        Dokumentsporing.inntektsmeldingDager(id),
                        Dokumentsporing.inntektsmeldingRefusjon(id),
                    )
                }
            )
        }
    }

    private fun setupLiteGapA2SammeSkjæringstidspunkt() {
        listOf(a1, a2).nyeVedtak(januar)
        a1 { forlengVedtak(februar) }
        nullstillTilstandsendringer()
        a2 {
            håndterSøknad(10.februar til 28.februar)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
    }

    private fun assertDokumentsporingPåSisteBehandling(vedtaksperiode: UUID, vararg forventet: Dokumentsporing) {
        val faktisk = inspektør.vedtaksperioder(vedtaksperiode).behandlinger.behandlinger
            .last().endringer
            .map { it.dokumentsporing }.filter { when (it.dokumentType) {
                DokumentType.InntektsmeldingInntekt,
                DokumentType.InntektsmeldingRefusjon,
                DokumentType.InntektsmeldingDager -> true
                DokumentType.Søknad,
                DokumentType.Sykmelding,
                DokumentType.InntektFraAOrdningen,
                DokumentType.OverstyrTidslinje,
                DokumentType.OverstyrInntekt,
                DokumentType.OverstyrRefusjon,
                DokumentType.OverstyrArbeidsgiveropplysninger,
                DokumentType.OverstyrArbeidsforhold,
                DokumentType.SkjønnsmessigFastsettelse,
                DokumentType.AndreYtelser -> false
            } }
        assertEquals(forventet.toSet(), faktisk.toSet())
    }
}
