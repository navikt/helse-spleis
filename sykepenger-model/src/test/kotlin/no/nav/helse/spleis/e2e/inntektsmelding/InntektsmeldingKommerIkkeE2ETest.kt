package no.nav.helse.spleis.e2e.inntektsmelding

import java.time.LocalDateTime
import java.time.YearMonth
import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonObserver.SkatteinntekterLagtTilGrunnEvent.Skatteinntekt
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class InntektsmeldingKommerIkkeE2ETest : AbstractDslTest() {

    @Test
    fun `lager ikke påminnelse om vedtaksperioden har ventet mindre enn tre måneder`() = Toggle.InntektsmeldingSomIkkeKommer.enable {
        val nå = LocalDateTime.now()
        val tilstandsendringstidspunkt = nå.minusMonths(3).plusDays(1)
        a1 {
            håndterSøknad(januar)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, tilstandsendringstidspunkt, nå)
            assertIngenBehov(1.vedtaksperiode, Aktivitet.Behov.Behovtype.InntekterForSykepengegrunnlagForArbeidsgiver)
        }
    }

    @Test
    fun `lager påminnelse om vedtaksperioden har ventet mer enn tre måneder`() = Toggle.InntektsmeldingSomIkkeKommer.enable {
        val nå = LocalDateTime.now()
        val tilstandsendringstidspunkt = nå.minusMonths(3)
        a1 {
            håndterSøknad(januar)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, tilstandsendringstidspunkt, nå)
            assertBehov(1.vedtaksperiode, Aktivitet.Behov.Behovtype.InntekterForSykepengegrunnlagForArbeidsgiver)
        }
    }

    @Test
    fun `lagrer skatteinntektene som inntektsmelding og går videre`() = Toggle.InntektsmeldingSomIkkeKommer.enable {
        val inntektFraSkatt = 10000.månedlig
        a1 {
            håndterSøknad(januar)
            val meldingsreferanseId = håndterSykepengegrunnlagForArbeidsgiver(
                1.vedtaksperiode, 1.januar, listOf(
                ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 12), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", ""),
                ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 11), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", ""),
                ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 10), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", "")
            )
            )
            val hendelser = inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.hendelser
            assertTrue(hendelser.contains(Dokumentsporing.inntektFraAOrdingen(meldingsreferanseId)))

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
                AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING
            )
        }
    }

    @Test
    fun `Tar med inntekter når det er rapportert 0 kr i AOrdningen`() = Toggle.InntektsmeldingSomIkkeKommer.enable {
        val inntektFraSkatt = INGEN
        a1 {
            håndterSøknad(januar)
            håndterSykepengegrunnlagForArbeidsgiver(
                1.vedtaksperiode, 1.januar, listOf(
                ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 12), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", ""),
                ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 11), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", ""),
                ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 10), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", "")
            )
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_IV_10, 1.vedtaksperiode.filter())
            assertUtbetalingsbeløp(
                1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 0,
                forventetPersonbeløp = 0,
                subset = 17.januar til 31.januar
            )
            assertTilstander(
                1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING
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
    fun `Tar ikke med inntekter når det ikke er rapportert inntekt i AOrdningen`() = Toggle.InntektsmeldingSomIkkeKommer.enable {
        a1 {
            håndterSøknad(januar)
            håndterSykepengegrunnlagForArbeidsgiver(1.vedtaksperiode, 1.januar, emptyList())
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_IV_10, 1.vedtaksperiode.filter())
            assertUtbetalingsbeløp(
                1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 0,
                forventetPersonbeløp = 0,
                subset = 17.januar til 31.januar
            )
            assertTilstander(
                1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING
            )

            val forelagteOpplysninger = observatør.skatteinntekterLagtTilGrunnEventer.last()
            assertEquals(emptyList<Skatteinntekt>(), forelagteOpplysninger.skatteinntekter)
            assertEquals(0.0, forelagteOpplysninger.omregnetÅrsinntekt)
        }
    }
}
