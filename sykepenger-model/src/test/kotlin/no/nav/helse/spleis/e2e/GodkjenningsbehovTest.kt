package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Grunnbeløp
import no.nav.helse.Personidentifikator
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.UgyldigeSituasjonerObservatør.Companion.assertUgyldigSituasjon
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.til
import no.nav.helse.hentFeltFraBehov
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.person.PersonObserver.UtkastTilVedtakEvent.Inntektskilde
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_10
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_24
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.sisteBehov
import no.nav.helse.utbetalingslinjer.Utbetalingstatus.IKKE_GODKJENT
import no.nav.helse.utbetalingslinjer.Utbetalingstatus.IKKE_UTBETALT
import no.nav.helse.økonomi.Inntekt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class GodkjenningsbehovTest : AbstractEndToEndTest() {
    private val IDENTIFIKATOR_SOM_KAN_BEHANDLES_UTEN_IM = Personidentifikator("30019212345")
    private val FØDSELSDATO_SOM_KAN_BEHANDLES_UTEN_IM = 30.januar(1992)

    @Test
    fun `sender med inntektskilder i sykepengegrunnlaget i godkjenningsbehovet`() {
        createTestPerson(IDENTIFIKATOR_SOM_KAN_BEHANDLES_UTEN_IM, FØDSELSDATO_SOM_KAN_BEHANDLES_UTEN_IM)
        nyPeriode(januar, orgnummer = a1)
        nyPeriode(januar, orgnummer = a2)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        this@GodkjenningsbehovTest.håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, tilstandsendringstidspunkt = 10.november(2024).atStartOfDay(), nå = 10.februar(2025).atStartOfDay(), orgnummer = a2)
        this@GodkjenningsbehovTest.håndterSykepengegrunnlagForArbeidsgiver(orgnummer = a2)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1)
        this@GodkjenningsbehovTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        assertVarsel(RV_IV_10, 1.vedtaksperiode.filter(orgnummer = a2))
        val inntektskilder = inntektskilder(1.vedtaksperiode, orgnummer = a1)
        assertEquals(listOf(Inntektskilde.Arbeidsgiver, Inntektskilde.AOrdningen), inntektskilder)
    }

    @Test
    fun `sender med inntektskilde saksbehandler i sykepengegrunnlaget i godkjenningsbehovet ved skjønnsmessig fastsettelse -- AOrdning på orginal inntekt`() {
        createTestPerson(IDENTIFIKATOR_SOM_KAN_BEHANDLES_UTEN_IM, FØDSELSDATO_SOM_KAN_BEHANDLES_UTEN_IM)
        nyPeriode(januar, a1)
        this@GodkjenningsbehovTest.håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, tilstandsendringstidspunkt = 10.november(2024).atStartOfDay(), nå = 10.februar(2025).atStartOfDay(), orgnummer = a1)
        this@GodkjenningsbehovTest.håndterSykepengegrunnlagForArbeidsgiver(orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@GodkjenningsbehovTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        this@GodkjenningsbehovTest.håndterSkjønnsmessigFastsettelse(
            1.januar, listOf(
            OverstyrtArbeidsgiveropplysning(
                orgnummer = a1,
                inntekt = INNTEKT * 2
            )
        )
        )
        this@GodkjenningsbehovTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertVarsel(RV_IV_10, 1.vedtaksperiode.filter())
        val inntektskilder = inntektskilder(1.vedtaksperiode, orgnummer = a1)
        assertEquals(listOf(Inntektskilde.Saksbehandler), inntektskilder)
    }

    @Test
    fun `sender med inntektskilde saksbehandler i sykepengegrunnlaget i godkjenningsbehovet ved skjønnsmessig fastsettelse -- inntektsmelding på orginal inntekt`() {
        tilGodkjenning(januar, a1)

        this@GodkjenningsbehovTest.håndterSkjønnsmessigFastsettelse(
            1.januar, listOf(
            OverstyrtArbeidsgiveropplysning(
                orgnummer = a1,
                inntekt = INNTEKT * 2
            )
        )
        )
        this@GodkjenningsbehovTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        val inntektskilder = inntektskilder(1.vedtaksperiode, orgnummer = a1)
        assertEquals(listOf(Inntektskilde.Saksbehandler), inntektskilder)
    }

    @Test
    fun `sender med sykepengegrunnlag i godkjenningsbehovet`() {
        tilGodkjenning(januar, beregnetInntekt = INNTEKT * 6, organisasjonsnummere = arrayOf(a1))
        assertEquals(Grunnbeløp.`6G`.beløp(1.januar).årlig, sykepengegrunnlag(1.vedtaksperiode))
    }

    @Test
    fun `sender med feil vilkårsgrunnlagId i påminnet godkjenningsbehov om det har kommet nytt vilkårsgrunnlag med endring _senere_ enn perioden`() {
        tilGodkjenning(januar, a1)
        val vilkårsgrunnlagId1 = vilkårsgrunnlagIdFraVilkårsgrunnlaghistorikken(1.januar)
        assertEquals(vilkårsgrunnlagId1, vilkårsgrunnlagIdFraSisteGodkjenningsbehov(1.vedtaksperiode))
        håndterSøknad(februar)
        håndterSøknad(mars)
        nullstillTilstandsendringer()

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.mars,
            refusjon = Inntektsmelding.Refusjon(Inntekt.INGEN, null)
        )
        assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING) // Reberegnes ikke ettersom endringen gjelder fra og med 1.mars
        val vilkårsgrunnlagId2 = vilkårsgrunnlagIdFraVilkårsgrunnlaghistorikken(1.januar)
        assertEquals(vilkårsgrunnlagId1, vilkårsgrunnlagId2)

        this@GodkjenningsbehovTest.håndterPåminnelse(1.vedtaksperiode, AVVENTER_GODKJENNING)

        assertEquals(vilkårsgrunnlagId1, vilkårsgrunnlagIdFraSisteGodkjenningsbehov(1.vedtaksperiode))
    }

    @Test
    fun `sender med feil vilkårsgrunnlagId i første godkjenningsbehov om det har kommet nytt vilkårsgrunnlag med endring _senere_ enn perioden mens den står i avventer simulering`()  {
        håndterSøknad(januar)
        håndterSøknad(februar)
        håndterSøknad(mars)
        håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        val vilkårsgrunnlagId1 = vilkårsgrunnlagIdFraVilkårsgrunnlaghistorikken(1.januar)
        this@GodkjenningsbehovTest.håndterYtelser(1.vedtaksperiode)
        nullstillTilstandsendringer()

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.mars,
            refusjon = Inntektsmelding.Refusjon(Inntekt.INGEN, null)
        )

        assertTilstander(1.vedtaksperiode, AVVENTER_SIMULERING) // Reberegnes ikke ettersom endringen gjelder fra og med 1.mars
        val vilkårsgrunnlagId2 = vilkårsgrunnlagIdFraVilkårsgrunnlaghistorikken(1.januar)
        assertEquals(vilkårsgrunnlagId1, vilkårsgrunnlagId2)

        håndterSimulering(1.vedtaksperiode)
        assertTilstander(1.vedtaksperiode, AVVENTER_SIMULERING, AVVENTER_GODKJENNING)

        assertEquals(vilkårsgrunnlagId1, vilkårsgrunnlagIdFraSisteGodkjenningsbehov(1.vedtaksperiode))
    }

    @Test
    fun `godkjenningsbehov som ikke kan avvises blir forsøkt avvist`() {
        håndterSøknad(1.januar til 16.januar)
        håndterSøknad(17.januar til 31.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@GodkjenningsbehovTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        this@GodkjenningsbehovTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        håndterSøknad(mars)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            begrunnelseForReduksjonEllerIkkeUtbetalt = "Agp skal utbetales av NAV!!"
        )
        assertVarsler(listOf(Varselkode.RV_IM_8), 1.vedtaksperiode.filter())
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        this@GodkjenningsbehovTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        assertFalse(kanAvvises(1.vedtaksperiode))

        val utbetalingId = UUID.fromString(personlogg.sisteBehov(Aktivitet.Behov.Behovtype.Godkjenning).alleKontekster["utbetalingId"])
        val utbetaling = inspektør.utbetalinger(1.vedtaksperiode).last()
        assertEquals(utbetalingId, utbetaling.inspektør.utbetalingId)

        assertEquals(IKKE_UTBETALT, utbetaling.inspektør.tilstand)
        assertUgyldigSituasjon("En vedtaksperiode i AVVENTER_GODKJENNING trenger hjelp!") {
            this@GodkjenningsbehovTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, utbetalingId = utbetalingId, utbetalingGodkjent = false)
        }
        assertEquals(IKKE_GODKJENT, inspektør.utbetalinger(1.vedtaksperiode).last().inspektør.tilstand)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

        assertVarsel(RV_UT_24, 1.vedtaksperiode.filter())
    }

    @Test
    fun `førstegangsbehandling som kan avvises`() {
        tilGodkjenning(januar, a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        assertTrue(kanAvvises(1.vedtaksperiode))
    }

    @Test
    fun `omgjøring som kan avvises`() {
        håndterSøknad(2.januar til 17.januar)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@GodkjenningsbehovTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        assertTrue(kanAvvises(1.vedtaksperiode))
    }

    @Test
    fun `omgjøring som _ikke_ kan avvises`() {
        håndterSøknad(2.januar til 17.januar)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        håndterSøknad(18.januar til 31.januar)
        håndterInntektsmelding(listOf(2.januar til 17.januar))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@GodkjenningsbehovTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
        assertTrue(kanAvvises(2.vedtaksperiode))

        this@GodkjenningsbehovTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)

        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@GodkjenningsbehovTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        assertFalse(kanAvvises(1.vedtaksperiode))

        this@GodkjenningsbehovTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        this@GodkjenningsbehovTest.håndterYtelser(2.vedtaksperiode)

        assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
        assertFalse(kanAvvises(2.vedtaksperiode))
    }

    @Test
    fun `revurdering kan ikke avvises`() {
        nyttVedtak(januar)
        this@GodkjenningsbehovTest.håndterOverstyrArbeidsgiveropplysninger(
            1.januar, listOf(
            OverstyrtArbeidsgiveropplysning(
                a1, INNTEKT * 1.05,
            emptyList())))
        this@GodkjenningsbehovTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
        assertFalse(kanAvvises(1.vedtaksperiode))
    }

    @Test
    fun `kan avvise en out of order rett i forkant av en utbetalt periode`() {
        nyttVedtak(februar)
        håndterSøknad(januar)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)

        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@GodkjenningsbehovTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
        assertTrue(kanAvvises(2.vedtaksperiode))

        this@GodkjenningsbehovTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode, utbetalingGodkjent = false)

        assertSisteForkastetPeriodeTilstand(a1, 2.vedtaksperiode, TIL_INFOTRYGD)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_REVURDERING)

        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@GodkjenningsbehovTest.håndterYtelser(1.vedtaksperiode)

        assertVarsel(Varselkode.RV_IV_7, 1.vedtaksperiode.filter())
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
        assertFalse(kanAvvises(1.vedtaksperiode))
    }

    @Test
    fun `kan avvise en out of order selv om noe er utbetalt senere på annen agp`() {
        nyttVedtak(mars)
        håndterSøknad(januar)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            vedtaksperiodeIdInnhenter = 2.vedtaksperiode
        )
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@GodkjenningsbehovTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
        assertTrue(kanAvvises(2.vedtaksperiode))

        this@GodkjenningsbehovTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode, utbetalingGodkjent = false)

        assertSisteForkastetPeriodeTilstand(a1, 2.vedtaksperiode, TIL_INFOTRYGD)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)

        this@GodkjenningsbehovTest.håndterYtelser(1.vedtaksperiode)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
        assertFalse(kanAvvises(1.vedtaksperiode))

        this@GodkjenningsbehovTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
    }

    @Test
    fun `markerer godkjenningsbehov som har brukt skatteinntekter istedenfor inntektsmelding med riktig tag`() {
        createTestPerson(IDENTIFIKATOR_SOM_KAN_BEHANDLES_UTEN_IM, FØDSELSDATO_SOM_KAN_BEHANDLES_UTEN_IM)
        nyPeriode(januar)
        this@GodkjenningsbehovTest.håndterPåminnelse(
            1.vedtaksperiode,
            AVVENTER_INNTEKTSMELDING,
            tilstandsendringstidspunkt = 10.november(2024).atStartOfDay(),
            nå = 10.februar(2025).atStartOfDay()
        )
        this@GodkjenningsbehovTest.håndterSykepengegrunnlagForArbeidsgiver()
        assertVarsel(RV_IV_10, 1.vedtaksperiode.filter())
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@GodkjenningsbehovTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        hendelselogg.assertHarTag(
            vedtaksperiode = 1.vedtaksperiode,
            forventetTag = "InntektFraAOrdningenLagtTilGrunn"
        )
    }

    @Test
    fun `markerer godkjenningsbehov som har brukt skatteinntekter istedenfor inntektsmelding med riktig tag for flere arbeidsgivere med ulik start`() {
        nyPeriode(januar, a1)
        nyPeriode(februar, a2)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterInntektsmelding(
            listOf(1.februar til 16.februar),
            førsteFraværsdag = 1.februar,
            orgnummer = a2
        )

        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        this@GodkjenningsbehovTest.håndterYtelser(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, orgnummer = a1)
        this@GodkjenningsbehovTest.håndterUtbetalingsgodkjenning(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        this@GodkjenningsbehovTest.håndterYtelser(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, orgnummer = a2)

        hendelselogg.assertHarTag(
            vedtaksperiode = 1.vedtaksperiode,
            forventetTag = "InntektFraAOrdningenLagtTilGrunn",
            orgnummer = a2
        )
    }

    private fun kanAvvises(vedtaksperiode: IdInnhenter, orgnummer: String = a1) = hendelselogg.hentFeltFraBehov<Boolean>(
        vedtaksperiodeId = vedtaksperiode.id(orgnummer),
        behov = Aktivitet.Behov.Behovtype.Godkjenning,
        felt = "kanAvvises"
    )!!

    private fun vilkårsgrunnlagIdFraSisteGodkjenningsbehov(vedtaksperiode: IdInnhenter, orgnummer: String = a1) = hendelselogg.hentFeltFraBehov<String>(
        vedtaksperiodeId = vedtaksperiode.id(orgnummer),
        behov = Aktivitet.Behov.Behovtype.Godkjenning,
        felt = "vilkårsgrunnlagId"
    )!!.let { UUID.fromString(it) }

    private fun vilkårsgrunnlagIdFraVilkårsgrunnlaghistorikken(skjæringstidspunkt: LocalDate, orgnummer: String = a1) = inspektør(orgnummer).vilkårsgrunnlag(skjæringstidspunkt)!!.view().inspektør.vilkårsgrunnlagId
    private fun sykepengegrunnlag(vedtaksperiode: IdInnhenter, orgnummer: String = a1) = hendelselogg.hentFeltFraBehov<Map<String, Any>>(
        vedtaksperiodeId = vedtaksperiode.id(orgnummer),
        behov = Aktivitet.Behov.Behovtype.Godkjenning,
        felt = "sykepengegrunnlagsfakta"
    )!!["sykepengegrunnlag"]

    private fun inntektskilder(vedtaksperiode: IdInnhenter, orgnummer: String = a1) = hendelselogg.hentFeltFraBehov<Map<String, List<Map<String, Any>>>>(
        vedtaksperiodeId = vedtaksperiode.id(orgnummer),
        behov = Aktivitet.Behov.Behovtype.Godkjenning,
        felt = "sykepengegrunnlagsfakta"
    )!!["arbeidsgivere"]!!.map { it["inntektskilde"] }
}
