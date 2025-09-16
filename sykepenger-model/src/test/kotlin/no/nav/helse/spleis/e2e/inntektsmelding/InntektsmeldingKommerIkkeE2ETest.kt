package no.nav.helse.spleis.e2e.inntektsmelding

import java.time.LocalDateTime
import java.time.YearMonth
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.februar
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonObserver.SkatteinntekterLagtTilGrunnEvent.Skatteinntekt
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_A_ORDNINGEN
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class InntektsmeldingKommerIkkeE2ETest : AbstractDslTest() {

    @Test
    fun `varsel legges på perioden som håndterer skatteopplysningene -- helgegap og out of order`() {
        val inntektFraSkatt = 10000.månedlig
        a1 {
            håndterSøknad(29.januar til 10.februar)
            håndterSøknad(1.januar til 26.januar)
            assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

            håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, tilstandsendringstidspunkt = 1.januar.atStartOfDay(), nåtidspunkt = 1.januar.plusDays(90).atStartOfDay())
            håndterSykepengegrunnlagForArbeidsgiver(
                1.vedtaksperiode,
                1.januar,
                listOf(
                    ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 12), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", ""),
                    ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 11), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", ""),
                    ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 10), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", "")
                )
            )
            assertTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertVarsel(Varselkode.RV_IV_10, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `legger varsel på riktig periode når det er en senere periode som blir påminnet -- helgegap`() {
        val inntektFraSkatt = 10000.månedlig
        a1 {
            håndterSøknad(1.januar til 26.januar)
            håndterSøknad(29.januar til 10.februar)
            assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            nullstillTilstandsendringer()
            håndterPåminnelse(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, tilstandsendringstidspunkt = 1.januar.atStartOfDay(), nåtidspunkt = 1.januar.plusDays(90).atStartOfDay())
            håndterSykepengegrunnlagForArbeidsgiver(
                2.vedtaksperiode,
                1.januar,
                listOf(
                    ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 12), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", ""),
                    ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 11), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", ""),
                    ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 10), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", "")
                )
            )
            assertTilstander(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_A_ORDNINGEN, AVVENTER_BLOKKERENDE_PERIODE)
            assertVarsel(Varselkode.RV_IV_10, 2.vedtaksperiode.filter())
        }
    }

    @Test
    fun `event om at vi bruker skatteopplysninger`() {
        val inntektFraSkatt = 10000.månedlig
        a1 {
            håndterSøknad(januar)
            håndterPåminnelse(
                1.vedtaksperiode,
                AVVENTER_INNTEKTSMELDING,
                tilstandsendringstidspunkt = 10.november(2024).atStartOfDay(),
                nåtidspunkt = 10.februar(2025).atStartOfDay()
            )
            håndterSykepengegrunnlagForArbeidsgiver(
                1.vedtaksperiode,
                1.januar,
                listOf(
                    ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 12), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", ""),
                    ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 11), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", ""),
                    ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 10), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", "")
                )
            )
            assertVarsel(Varselkode.RV_IV_10, 1.vedtaksperiode.filter())
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            val event = observatør.skatteinntekterLagtTilGrunnEventer.single()
            val forventet = PersonObserver.SkatteinntekterLagtTilGrunnEvent(
                yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                vedtaksperiodeId = 1.vedtaksperiode,
                behandlingId = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.single().id,
                skjæringstidspunkt = 1.januar,
                skatteinntekter = listOf(
                    Skatteinntekt(desember(2017), 10000.0),
                    Skatteinntekt(november(2017), 10000.0),
                    Skatteinntekt(oktober(2017), 10000.0)
                ),
                omregnetÅrsinntekt = 120000.0
            )
            assertEquals(forventet, event)
        }
    }

    @Test
    fun `event om at vi bruker skatteopplysninger med sprø minus`() {
        val inntektFraSkatt = 10000.månedlig
        val sprøInntektFraSkatt = 30000.månedlig * -1
        a1 {
            håndterSøknad(januar)
            håndterPåminnelse(
                1.vedtaksperiode,
                AVVENTER_INNTEKTSMELDING,
                tilstandsendringstidspunkt = 10.november(2024).atStartOfDay(),
                nåtidspunkt = 10.februar(2025).atStartOfDay()
            )
            håndterSykepengegrunnlagForArbeidsgiver(
                1.vedtaksperiode, 1.januar, listOf(
                ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 12), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", ""),
                ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 11), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", ""),
                ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 10), sprøInntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", "")
            )
            )
            assertVarsel(Varselkode.RV_IV_10, 1.vedtaksperiode.filter())
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertVarsler(listOf(Varselkode.RV_SV_1, Varselkode.RV_IV_10, Varselkode.RV_VV_4), 1.vedtaksperiode.filter())

            val event = observatør.skatteinntekterLagtTilGrunnEventer.single()
            val forventet = PersonObserver.SkatteinntekterLagtTilGrunnEvent(
                yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                vedtaksperiodeId = 1.vedtaksperiode,
                behandlingId = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.single().id,
                skjæringstidspunkt = 1.januar,
                skatteinntekter = listOf(
                    Skatteinntekt(desember(2017), 10000.0),
                    Skatteinntekt(november(2017), 10000.0),
                    Skatteinntekt(oktober(2017), -30000.0),
                ),
                omregnetÅrsinntekt = 0.0
            )
            assertEquals(forventet, event)
        }
    }

    @Test
    fun `lager ikke påminnelse om vedtaksperioden har ventet mindre enn tre måneder`() {
        val nå = LocalDateTime.now()
        val tilstandsendringstidspunkt = nå.minusDays(89)
        a1 {
            håndterSøknad(januar)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, tilstandsendringstidspunkt, nå)
            assertIngenBehov(1.vedtaksperiode, Aktivitet.Behov.Behovtype.InntekterForSykepengegrunnlagForArbeidsgiver)
        }
    }

    @Test
    fun `lager påminnelse om vedtaksperioden har ventet mer enn tre måneder`() {
        val nå = 10.februar(2025).atStartOfDay()
        val tilstandsendringstidspunkt = 10.november(2024).atStartOfDay()
        a1 {
            håndterSøknad(januar)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, tilstandsendringstidspunkt, nå)
            assertBehov(1.vedtaksperiode, Aktivitet.Behov.Behovtype.InntekterForSykepengegrunnlagForArbeidsgiver)
        }
    }

    @Test
    fun `lagrer skatteinntektene som inntektsmelding og går videre`() {
        val inntektFraSkatt = 10000.månedlig
        a1 {
            håndterSøknad(januar)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, tilstandsendringstidspunkt = 1.januar.atStartOfDay(), nåtidspunkt = 1.januar.plusDays(90).atStartOfDay())
            val meldingsreferanseId = håndterSykepengegrunnlagForArbeidsgiver(
                1.vedtaksperiode, 1.januar, listOf(
                ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 12), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", ""),
                ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 11), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", ""),
                ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 10), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", "")
            )
            )
            val hendelser = inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.hendelser
            assertTrue(hendelser.contains(Dokumentsporing.inntektFraAOrdingen(MeldingsreferanseId(meldingsreferanseId))))

            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_IV_10, 1.vedtaksperiode.filter())
            assertUtbetalingsbeløp(
                1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 0,
                forventetPersonbeløp = 462,
                subset = 17.januar til 31.januar
            )
            assertTilstander(
                1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING,
                AVVENTER_A_ORDNINGEN, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING
            )
        }
    }

    @Test
    fun `Tar med inntekter når det er rapportert 0 kr i AOrdningen`() {
        val inntektFraSkatt = INGEN
        a1 {
            håndterSøknad(januar)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, tilstandsendringstidspunkt = 1.januar.atStartOfDay(), nåtidspunkt = 1.januar.plusDays(90).atStartOfDay())
            håndterSykepengegrunnlagForArbeidsgiver(
                1.vedtaksperiode, 1.januar, listOf(
                ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 12), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", ""),
                ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 11), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", ""),
                ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 10), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", "")
            )
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertVarsler(listOf(Varselkode.RV_SV_1, Varselkode.RV_IV_10, Varselkode.RV_VV_4), 1.vedtaksperiode.filter())
            assertUtbetalingsbeløp(
                1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 0,
                forventetPersonbeløp = 0,
                subset = 17.januar til 31.januar
            )
            assertTilstander(
                1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING,
                AVVENTER_A_ORDNINGEN, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING
            )
            val forelagteOpplysninger = observatør.skatteinntekterLagtTilGrunnEventer.last()
            assertEquals(
                listOf(
                    Skatteinntekt(YearMonth.of(2017, 12), 0.0),
                    Skatteinntekt(YearMonth.of(2017, 11), 0.0),
                    Skatteinntekt(YearMonth.of(2017, 10), 0.0)
                ),
                forelagteOpplysninger.skatteinntekter
            )
            assertEquals(0.0, forelagteOpplysninger.omregnetÅrsinntekt)
        }
    }

    @Test
    fun `skal ikke slippe gjennom ting som har ventet veldig lenge`() {
        val cutoff = 10.februar(2025)
        a1 {
            håndterSøknad(januar)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, tilstandsendringstidspunkt = cutoff.minusDays(1).minusMonths(3).atStartOfDay())
            assertIngenBehov(1.vedtaksperiode, Aktivitet.Behov.Behovtype.InntekterForSykepengegrunnlagForArbeidsgiver)
        }
    }

    @Test
    fun `Tar ikke med inntekter når det ikke er rapportert inntekt i AOrdningen`() {
        a1 {
            håndterSøknad(januar)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, tilstandsendringstidspunkt = 1.januar.atStartOfDay(), nåtidspunkt = 1.januar.plusDays(90).atStartOfDay())
            håndterSykepengegrunnlagForArbeidsgiver(1.vedtaksperiode, 1.januar, emptyList())
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertVarsler(listOf(Varselkode.RV_SV_1, Varselkode.RV_IV_10, Varselkode.RV_VV_4), 1.vedtaksperiode.filter())
            assertUtbetalingsbeløp(
                1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 0,
                forventetPersonbeløp = 0,
                subset = 17.januar til 31.januar
            )
            assertTilstander(
                1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING,
                AVVENTER_A_ORDNINGEN, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING
            )

            val forelagteOpplysninger = observatør.skatteinntekterLagtTilGrunnEventer.last()
            assertEquals(emptyList<Skatteinntekt>(), forelagteOpplysninger.skatteinntekter)
            assertEquals(0.0, forelagteOpplysninger.omregnetÅrsinntekt)
        }
    }
}
