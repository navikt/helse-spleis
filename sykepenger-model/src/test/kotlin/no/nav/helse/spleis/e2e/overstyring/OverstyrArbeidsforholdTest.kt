package no.nav.helse.spleis.e2e.overstyring

import java.time.LocalDate
import no.nav.helse.desember
import no.nav.helse.dsl.Arbeidstakerkilde
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.a3
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.februar
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.hentFeltFraBehov
import no.nav.helse.januar
import no.nav.helse.november
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_OV_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SV_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_2
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.assertVarsler
import no.nav.helse.spleis.e2e.håndterArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterOverstyrArbeidsforhold
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlagFlereArbeidsgivere
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.inspectors.inspektør
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail

internal class OverstyrArbeidsforholdTest : AbstractEndToEndTest() {
    @Test
    fun `fjerner arbeidsforhold fra arbeidsforholdhistorikken ved overstyring`() {
        håndterSykmelding(januar)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            arbeidsforhold = listOf(
                Triple(a1, LocalDate.EPOCH, null),
                Triple(a2, 1.desember(2017), null)
            )
        )
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        this@OverstyrArbeidsforholdTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        val skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode)
        val relevanteOrgnumre1: Iterable<String> = hendelselogg.hentFeltFraBehov(1.vedtaksperiode.id(a1), Behovtype.Godkjenning, "orgnummereMedRelevanteArbeidsforhold") ?: fail { "forventet orgnummereMedRelevanteArbeidsforhold" }
        assertEquals(listOf(a1, a2).toList(), relevanteOrgnumre1.toList())
        this@OverstyrArbeidsforholdTest.håndterOverstyrArbeidsforhold(skjæringstidspunkt, listOf(OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(a2, true, "forklaring")))
        this@OverstyrArbeidsforholdTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        val relevanteOrgnumre2: Iterable<String> = hendelselogg.hentFeltFraBehov(1.vedtaksperiode.id(a1), Behovtype.Godkjenning, "orgnummereMedRelevanteArbeidsforhold") ?: fail { "forventet orgnummereMedRelevanteArbeidsforhold" }
        assertEquals(listOf(a1), relevanteOrgnumre2.toList())
    }

    @Test
    fun `Overstyring av arbeidsforhold fører til et nytt vilkårsgrunnlag med nye inntektsopplysninger`() {
        håndterSykmelding(januar)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            skatteinntekter = listOf(a1 to INNTEKT),
            arbeidsforhold = listOf(
                Triple(a1, LocalDate.EPOCH, null),
                Triple(a2, 1.desember(2017), null)
            )
        )
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        this@OverstyrArbeidsforholdTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        val skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode)
        inspektør(a1).utbetalingstidslinjer(1.vedtaksperiode)[17.januar].also {
            assertEquals(1431.daglig, it.økonomi.inspektør.arbeidsgiverbeløp)
            assertEquals(0.daglig, it.økonomi.inspektør.personbeløp)
            assertEquals(31000.månedlig, it.økonomi.inspektør.aktuellDagsinntekt)
        }
        this@OverstyrArbeidsforholdTest.håndterOverstyrArbeidsforhold(skjæringstidspunkt, listOf(OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(a2, true, "forklaring")))
        this@OverstyrArbeidsforholdTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        inspektør(a1).utbetalingstidslinjer(1.vedtaksperiode)[17.januar].also {
            assertEquals(1431.daglig, it.økonomi.inspektør.arbeidsgiverbeløp)
            assertEquals(0.daglig, it.økonomi.inspektør.personbeløp)
            assertEquals(31000.månedlig, it.økonomi.inspektør.aktuellDagsinntekt)
        }

        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, INNTEKT)
            assertInntektsgrunnlag(a2, INGEN, forventetkilde = Arbeidstakerkilde.AOrdningen, deaktivert = true)
        }
    }

    @Test
    fun `godtar overstyring uavhengig av rekkefølgen på arbeidsgivere`() {
        nyttVedtak(1.januar(2017) til 31.januar(2017), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar), orgnummer = a1)

        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            skatteinntekter = listOf(a1 to INNTEKT, a2 to INNTEKT),
            arbeidsforhold = listOf(
                Triple(a1, LocalDate.EPOCH, null),
                Triple(a2, LocalDate.EPOCH, null),
                Triple(a3, 1.desember(2017), null)
            ),
            orgnummer = a2
        )
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a2))

        this@OverstyrArbeidsforholdTest.håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        inspektør(a2).utbetalingstidslinjer(1.vedtaksperiode)[17.januar].also {
            assertEquals(1080.daglig, it.økonomi.inspektør.arbeidsgiverbeløp)
            assertEquals(0.daglig, it.økonomi.inspektør.personbeløp)
            assertEquals(31000.månedlig, it.økonomi.inspektør.aktuellDagsinntekt)
        }
        this@OverstyrArbeidsforholdTest.håndterOverstyrArbeidsforhold(1.januar, listOf(OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(a3, true, "forklaring")))
        this@OverstyrArbeidsforholdTest.håndterYtelser(1.vedtaksperiode, orgnummer = a2)

        inspektør(a2).utbetalingstidslinjer(1.vedtaksperiode)[17.januar].also {
            assertEquals(1080.daglig, it.økonomi.inspektør.arbeidsgiverbeløp)
            assertEquals(0.daglig, it.økonomi.inspektør.personbeløp)
            assertEquals(31000.månedlig, it.økonomi.inspektør.aktuellDagsinntekt)
        }

        assertInntektsgrunnlag(1.januar(2017), forventetAntallArbeidsgivere = 1) {
            assertInntektsgrunnlag(a1, INNTEKT)
        }

        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 3) {
            assertInntektsgrunnlag(a1, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
            assertInntektsgrunnlag(a2, INNTEKT)
            assertInntektsgrunnlag(a3, INGEN, forventetkilde = Arbeidstakerkilde.AOrdningen, deaktivert = true)
        }
    }

    @Test
    fun `kan ikke overstyre arbeidsforhold for arbeidsgiver vi ikke kjenner til`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )

        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        this@OverstyrArbeidsforholdTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        assertThrows<IllegalStateException>("Kan ikke overstyre arbeidsforhold for en arbeidsgiver vi ikke kjenner til") {
            this@OverstyrArbeidsforholdTest.håndterOverstyrArbeidsforhold(1.januar, listOf(OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(a3, true, "forklaring")))
        }
    }

    @Test
    fun `deaktivering av arbeidsforhold uten sykdom fører til nytt sykepengegrunnlag uten arbeidsforholdet, selv med inntekt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )

        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            skatteinntekter = listOf(
                a1 to INNTEKT,
                a2 to INNTEKT,
                a3 to 4000.årlig // Liten inntekt som saksbehandler ikke ser på som relevant
            ),
            orgnummer = a1
        )
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        this@OverstyrArbeidsforholdTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        inspektør(a1).utbetalingstidslinjer(1.vedtaksperiode)[17.januar].also {
            assertEquals(1075.daglig, it.økonomi.inspektør.arbeidsgiverbeløp)
            assertEquals(0.daglig, it.økonomi.inspektør.personbeløp)
            assertEquals(31000.månedlig, it.økonomi.inspektør.aktuellDagsinntekt)
        }
        this@OverstyrArbeidsforholdTest.håndterOverstyrArbeidsforhold(1.januar, listOf(OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(a3, true, "forklaring")))
        this@OverstyrArbeidsforholdTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        inspektør(a1).utbetalingstidslinjer(1.vedtaksperiode)[17.januar].also {
            assertEquals(1080.daglig, it.økonomi.inspektør.arbeidsgiverbeløp)
            assertEquals(0.daglig, it.økonomi.inspektør.personbeløp)
            assertEquals(31000.månedlig, it.økonomi.inspektør.aktuellDagsinntekt)
        }

        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 3) {
            assertInntektsgrunnlag(a1, INNTEKT)
            assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
            assertInntektsgrunnlag(a3, 4000.årlig, forventetkilde = Arbeidstakerkilde.AOrdningen, deaktivert = true)
        }
    }

    @Test
    fun `tar med inntekt fra inntektsmelding selv om vi ikke finner et aktivt arbeidsforhold i arbeidsforholdhistorikken`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            skatteinntekter = listOf(
                a1 to INNTEKT,
                a3 to INNTEKT
            ),
            arbeidsforhold = listOf(
                Triple(a1, LocalDate.EPOCH, 30.november(2017)),
                Triple(a3, LocalDate.EPOCH, null)
            ),
            orgnummer = a2
        )
        assertVarsler(listOf(Varselkode.RV_VV_1, RV_VV_2), 1.vedtaksperiode.filter(orgnummer = a2))
        this@OverstyrArbeidsforholdTest.håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        assertVarsel(Varselkode.RV_VV_8, 1.vedtaksperiode.filter(orgnummer = a2))
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a2, INNTEKT)
            assertInntektsgrunnlag(a3, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
        }
    }

    @Test
    fun `kan ikke overstyre arbeidsforhold dersom ingen vedtaksperioder kan håndtere hendelsen`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )

        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            skatteinntekter = listOf(a1 to INNTEKT, a2 to 1000.månedlig),
            orgnummer = a1
        )
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        this@OverstyrArbeidsforholdTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        assertThrows<IllegalStateException> {
            this@OverstyrArbeidsforholdTest.håndterOverstyrArbeidsforhold(2.januar, listOf(OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(a2, true, "forklaring")))
        }
    }

    @Test
    fun `vi vilkårsprøver krav om minimum inntekt ved overstyring av arbeidsforhold`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = 3800.månedlig,
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )

        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            skatteinntekter = listOf(
                a1 to 3800.månedlig,
                a2 to 300.månedlig
            ),
            orgnummer = a1
        )
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        this@OverstyrArbeidsforholdTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@OverstyrArbeidsforholdTest.håndterOverstyrArbeidsforhold(1.januar, listOf(OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(a2, true, "forklaring")))
        this@OverstyrArbeidsforholdTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, 3800.månedlig)
            assertInntektsgrunnlag(a2, 300.månedlig, forventetkilde = Arbeidstakerkilde.AOrdningen, deaktivert = true)
        }
        assertVarsel(RV_SV_1, 1.vedtaksperiode.filter(a1))
    }

    @Test
    fun `vi vilkårsprøver krav om opptjening ved overstyring av arbeidsforhold`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT,
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )

        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            skatteinntekter = listOf(a2 to 1000.månedlig),
            arbeidsforhold = listOf(
                Triple(a1, 31.desember(2017), null),
                Triple(a2, LocalDate.EPOCH, 5.januar),
            ),
            orgnummer = a1
        )
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        this@OverstyrArbeidsforholdTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        inspektør(a1).utbetalingstidslinjer(1.vedtaksperiode)[17.januar].also {
            assertEquals(1431.daglig, it.økonomi.inspektør.arbeidsgiverbeløp)
            assertEquals(0.daglig, it.økonomi.inspektør.personbeløp)
            assertEquals(31000.månedlig, it.økonomi.inspektør.aktuellDagsinntekt)
        }
        this@OverstyrArbeidsforholdTest.håndterOverstyrArbeidsforhold(1.januar, listOf(OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(a2, true, "forklaring")))
        this@OverstyrArbeidsforholdTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        inspektør(a1).utbetalingstidslinjer(1.vedtaksperiode)[17.januar].also {
            assertEquals(0.daglig, it.økonomi.inspektør.arbeidsgiverbeløp)
            assertEquals(0.daglig, it.økonomi.inspektør.personbeløp)
            assertEquals(31000.månedlig, it.økonomi.inspektør.aktuellDagsinntekt)
        }

        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, INNTEKT)
            assertInntektsgrunnlag(a2, 1000.månedlig, forventetkilde = Arbeidstakerkilde.AOrdningen, deaktivert = true)
        }
        assertVarsel(RV_OV_1, 1.vedtaksperiode.filter(a1))
        assertInstanceOf(Utbetalingsdag.AvvistDag::class.java, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[31.januar])
    }
}
