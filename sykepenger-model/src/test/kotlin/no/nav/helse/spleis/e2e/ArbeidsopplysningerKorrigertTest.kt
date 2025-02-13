package no.nav.helse.spleis.e2e

import no.nav.helse.desember
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.PersonObserver.ArbeidsgiveropplysningerKorrigertEvent
import no.nav.helse.person.PersonObserver.Inntektsopplysningstype.INNTEKTSMELDING
import no.nav.helse.person.PersonObserver.Inntektsopplysningstype.SAKSBEHANDLER
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_24
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ArbeidsopplysningerKorrigertTest : AbstractEndToEndTest() {

    @Test
    fun `Sender ut spisset event ved korrigerende inntektsmelding som endrer inntekt og refusjon`() {
        nyPeriode(januar)
        val korrigertInntektsmeldingId = håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = 31000.månedlig,
            refusjon = Inntektsmelding.Refusjon(31000.månedlig, null),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        val korrigerendeInntektsmeldingId = håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = 30000.månedlig,
            refusjon = Inntektsmelding.Refusjon(29000.månedlig, null)
        )

        assertVarsel(RV_IM_4, 1.vedtaksperiode.filter())
        val expected = ArbeidsgiveropplysningerKorrigertEvent(
            korrigertInntektsmeldingId = korrigertInntektsmeldingId,
            korrigerendeInntektsopplysningId = korrigerendeInntektsmeldingId,
            korrigerendeInntektektsopplysningstype = INNTEKTSMELDING
        )
        val actual = observatør.arbeidsgiveropplysningerKorrigert.single()
        assertEquals(expected, actual)
    }

    @Test
    fun `Sender ut spisset event ved korrigerende inntektsmelding som endrer agp`() {
        nyPeriode(januar)
        val korrigertInntektsmeldingId = håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        val korrigerendeInntektsmeldingId = håndterInntektsmelding(
            listOf(2.januar til 17.januar)
        )
        assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())

        val expected = ArbeidsgiveropplysningerKorrigertEvent(
            korrigertInntektsmeldingId = korrigertInntektsmeldingId,
            korrigerendeInntektsopplysningId = korrigerendeInntektsmeldingId,
            korrigerendeInntektektsopplysningstype = INNTEKTSMELDING
        )
        val actual = observatør.arbeidsgiveropplysningerKorrigert.single()
        assertEquals(expected, actual)
    }

    @Test
    fun `Sender ut spisset event ved saksbehandleroverstyring som endrer inntekt og refusjon`() {
        nyPeriode(januar)
        val korrigertInntektsmeldingId = håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        val korrigerendeInntektsopplysningId = håndterOverstyrArbeidsgiveropplysninger(
            1.januar,
            arbeidsgiveropplysninger = listOf(
                OverstyrtArbeidsgiveropplysning(
                    a1,
                    25000.månedlig,
                    refusjonsopplysninger = listOf(Triple(1.januar, null, INNTEKT))
                )
            )
        )

        val expected = ArbeidsgiveropplysningerKorrigertEvent(
            korrigertInntektsmeldingId = korrigertInntektsmeldingId,
            korrigerendeInntektsopplysningId = korrigerendeInntektsopplysningId,
            korrigerendeInntektektsopplysningstype = SAKSBEHANDLER
        )
        val actual = observatør.arbeidsgiveropplysningerKorrigert.single()
        assertEquals(expected, actual)
    }

    @Test
    fun `Sender ut to spissede eventer ved saksbehandleroverstyring som endrer inntekt hos to arbeidsgivere`() {
        val fom = 1.januar
        val tom = 31.januar
        nyPeriode(fom til tom, orgnummer = a1)
        nyPeriode(fom til tom, orgnummer = a2)
        val korrigertInntektsmeldingIdA1 = håndterArbeidsgiveropplysninger(
            listOf(fom til fom.plusDays(15)),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        val korrigertInntektsmeldingIdA2 = håndterArbeidsgiveropplysninger(
            listOf(fom til fom.plusDays(15)),
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt()
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt()

        val korrigerendeInntektsopplysningId = håndterOverstyrArbeidsgiveropplysninger(
            1.januar,
            arbeidsgiveropplysninger = listOf(
                OverstyrtArbeidsgiveropplysning(
                    a1,
                    25000.månedlig,
                    refusjonsopplysninger = listOf(Triple(1.januar, null, INNTEKT))
                ),
                OverstyrtArbeidsgiveropplysning(
                    a2,
                    25000.månedlig,
                    refusjonsopplysninger = listOf(Triple(1.januar, null, INNTEKT))
                )
            )
        )
        val expectedA1 = ArbeidsgiveropplysningerKorrigertEvent(
            korrigertInntektsmeldingId = korrigertInntektsmeldingIdA1,
            korrigerendeInntektsopplysningId = korrigerendeInntektsopplysningId,
            korrigerendeInntektektsopplysningstype = SAKSBEHANDLER
        )
        val expectedA2 = ArbeidsgiveropplysningerKorrigertEvent(
            korrigertInntektsmeldingId = korrigertInntektsmeldingIdA2,
            korrigerendeInntektsopplysningId = korrigerendeInntektsopplysningId,
            korrigerendeInntektektsopplysningstype = SAKSBEHANDLER
        )
        assertEquals(expectedA1, observatør.arbeidsgiveropplysningerKorrigert[0])
        assertEquals(expectedA2, observatør.arbeidsgiveropplysningerKorrigert[1])
        assertEquals(2, observatør.arbeidsgiveropplysningerKorrigert.size)
    }

    @Test
    fun `Sender ut ett spisset event ved saksbehandleroverstyring som endrer inntekt hos en av to arbeidsgivere`() {
        val fom = 1.januar
        val tom = 31.januar
        nyPeriode(fom til tom, orgnummer = a1)
        nyPeriode(fom til tom, orgnummer = a2)
        val korrigertInntektsmeldingIdA1 = håndterArbeidsgiveropplysninger(
            listOf(fom til fom.plusDays(15)),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterArbeidsgiveropplysninger(
            listOf(fom til fom.plusDays(15)),
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt()
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt()

        val korrigerendeInntektsopplysningId = håndterOverstyrArbeidsgiveropplysninger(
            1.januar,
            arbeidsgiveropplysninger = listOf(
                OverstyrtArbeidsgiveropplysning(
                    a1,
                    25000.månedlig,
                    refusjonsopplysninger = listOf(Triple(1.januar, null, INNTEKT))
                )
            )
        )
        val expected = ArbeidsgiveropplysningerKorrigertEvent(
            korrigertInntektsmeldingId = korrigertInntektsmeldingIdA1,
            korrigerendeInntektsopplysningId = korrigerendeInntektsopplysningId,
            korrigerendeInntektektsopplysningstype = SAKSBEHANDLER
        )
        assertEquals(expected, observatør.arbeidsgiveropplysningerKorrigert.first())
        assertEquals(1, observatør.arbeidsgiveropplysningerKorrigert.size)
    }

    @Test
    fun `Sender ut spisset event ved saksbehandleroverstyring som endrer agp`() {
        nyPeriode(januar)
        val korrigertInntektsmeldingId = håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        val korrigerendeInntektsopplysningId = håndterOverstyrTidslinje(
            listOf(
                ManuellOverskrivingDag(
                    31.desember(2017),
                    Dagtype.Egenmeldingsdag
                )
            )
        ).metadata.meldingsreferanseId
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)

        assertVarsler(listOf(Varselkode.RV_OS_2, Varselkode.RV_IV_7), 1.vedtaksperiode.filter())
        val expected = ArbeidsgiveropplysningerKorrigertEvent(
            korrigertInntektsmeldingId = korrigertInntektsmeldingId,
            korrigerendeInntektsopplysningId = korrigerendeInntektsopplysningId.id,
            korrigerendeInntektektsopplysningstype = SAKSBEHANDLER
        )
        val actual = observatør.arbeidsgiveropplysningerKorrigert.single()
        assertEquals(expected, actual)
    }

    @Test
    fun `Sender ikke ut spisset event ved tidslinjeoverstyring som ikke fører til en endring av agp`() {
        nyttVedtak(januar)
        håndterOverstyrTidslinje(
            listOf(
                ManuellOverskrivingDag(
                    10.januar,
                    Dagtype.Feriedag
                )
            )
        )
        håndterYtelser(1.vedtaksperiode)
        val actual = observatør.arbeidsgiveropplysningerKorrigert
        assertEquals(0, actual.size)
    }

    @Test
    fun `Sender ikke ut spisset event ved korrigerende inntektsmelding som ikke fører til endring`() {
        nyPeriode(januar)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT,
            refusjon = Inntektsmelding.Refusjon(INNTEKT, null),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        assertVarsler(1.vedtaksperiode.filter(), etter = listOf(RV_IM_4)) {
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT,
                refusjon = Inntektsmelding.Refusjon(INNTEKT, null)
            )
        }
        håndterOverstyrArbeidsgiveropplysninger(
            1.januar,
            arbeidsgiveropplysninger = listOf(
                OverstyrtArbeidsgiveropplysning(
                    a1,
                    INNTEKT,
                    refusjonsopplysninger = listOf(Triple(1.januar, null, INNTEKT))
                )
            )
        )
        assertTrue(observatør.arbeidsgiveropplysningerKorrigert.isEmpty())
    }

    @Test
    fun `Sender ikke ut spisset event ved korrigerende inntektsmelding som korrigerer noe annet enn en inntektsmelding`() {
        nyttVedtak(januar)
        håndterOverstyrArbeidsgiveropplysninger(
            1.januar,
            arbeidsgiveropplysninger = listOf(
                OverstyrtArbeidsgiveropplysning(
                    a1,
                    40000.månedlig,
                    refusjonsopplysninger = listOf(Triple(1.januar, null, INNTEKT))
                )
            )
        )
        assertEquals(1, observatør.arbeidsgiveropplysningerKorrigert.size)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = 41000.månedlig
        )
        assertEquals(1, observatør.arbeidsgiveropplysningerKorrigert.size)
        assertVarsel(RV_IM_4, 1.vedtaksperiode.filter())
    }
}
