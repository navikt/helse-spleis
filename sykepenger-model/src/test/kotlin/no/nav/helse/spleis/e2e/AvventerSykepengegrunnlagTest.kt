package no.nav.helse.spleis.e2e

import no.nav.helse.EnableAvventerSykepengegrunnlag
import no.nav.helse.Toggle
import no.nav.helse.dsl.*
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.EventSubscription.TrengerSykepengegrunnlagEvent.Arbeidsgiver
import no.nav.helse.person.EventSubscription.TrengerSykepengegrunnlagEvent.Inntektskilde.A_ORDNINGEN
import no.nav.helse.person.EventSubscription.TrengerSykepengegrunnlagEvent.Inntektskilde.INNTEKTSMELDING
import no.nav.helse.person.EventSubscription.TrengerSykepengegrunnlagEvent.Selvstendig
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_10
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_7
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_2
import no.nav.helse.person.tilstandsmaskin.TilstandType.*
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

@EnableAvventerSykepengegrunnlag
internal class AvventerSykepengegrunnlagTest : AbstractDslTest() {
    @Test
    fun `arbeidstaker med inntektsmelding går gjennom AvventerSykepengegrunnlag`() {
        a1 {
            håndterSøknad(januar)
            val inntektsmeldingId = håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SYKEPENGEGRUNNLAG)
            assertEquals(
                "SYKEPENGEGRUNNLAG",
                observatør.vedtaksperiodeVenter
                    .last()
                    .venterPå.venteårsak.hva,
            )

            val behov = sykepengegrunnlagbehov(1.vedtaksperiode).single()
            assertEquals(1.januar, behov.skjæringstidspunkt)
            assertEquals(inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.rettighetskategoriId, behov.rettighetskategoriId)
            assertEquals(listOf(Arbeidsgiver(a1, INNTEKTSMELDING, inntektsmeldingId)), behov.arbeidsgivere)
            assertNull(behov.selvstendig)

            håndterSykepengegrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_RETTIGHETSKATEGORI,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_SYKEPENGEGRUNNLAG,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET,
            )
        }
    }

    @Test
    fun `behovet sendes på nytt ved påminnelse`() {
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            assertEquals(1, sykepengegrunnlagbehov(1.vedtaksperiode).size)

            håndterPåminnelse(1.vedtaksperiode, AVVENTER_SYKEPENGEGRUNNLAG)
            assertEquals(2, sykepengegrunnlagbehov(1.vedtaksperiode).size)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SYKEPENGEGRUNNLAG)
        }
    }

    @Test
    fun `arbeidstaker med inntekt fra a-ordningen`() {
        a1 {
            håndterSøknad(januar)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, flagg = setOf("ønskerInntektFraAOrdningen"))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            assertVarsel(RV_IV_10, 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SYKEPENGEGRUNNLAG)

            val behov = sykepengegrunnlagbehov(1.vedtaksperiode).single()
            assertEquals(listOf(Arbeidsgiver(a1, A_ORDNINGEN, null)), behov.arbeidsgivere)

            håndterSykepengegrunnlag(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        }
    }

    @Test
    fun `ghost-arbeidsforhold får inntektskilden a-ordningen`() {
        a1 {
            håndterSøknad(januar)
            val inntektsmeldingId = håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode, skatteinntekter = listOf(a1 to INNTEKT, a2 to INNTEKT))
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SYKEPENGEGRUNNLAG)

            val behov = sykepengegrunnlagbehov(1.vedtaksperiode).single()
            assertEquals(
                setOf(
                    Arbeidsgiver(a1, INNTEKTSMELDING, inntektsmeldingId),
                    Arbeidsgiver(a2, A_ORDNINGEN, null),
                ),
                behov.arbeidsgivere.toSet(),
            )

            håndterSykepengegrunnlag(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        }
    }

    @Test
    fun `gjenbrukt inntektsmelding fra et tidligere skjæringstidspunkt`() {
        a1 {
            håndterSøknad(januar)
            val inntektsmeldingId = håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)

            håndterOverstyrTidslinje(
                listOf(
                    ManuellOverskrivingDag(1.januar, Dagtype.Arbeidsdag),
                    ManuellOverskrivingDag(2.januar, Dagtype.Arbeidsdag),
                ),
            )
            assertEquals(3.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)

            håndterVilkårsgrunnlag(1.vedtaksperiode)
            assertVarsel(RV_IV_7, 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SYKEPENGEGRUNNLAG)

            val behov = sykepengegrunnlagbehov(1.vedtaksperiode).single()
            assertEquals(3.januar, behov.skjæringstidspunkt)
            assertEquals(listOf(Arbeidsgiver(a1, INNTEKTSMELDING, inntektsmeldingId)), behov.arbeidsgivere)
        }
    }

    @Test
    fun `selvstendig næringsdrivende sender søknaden og ingen arbeidsgivere`() {
        selvstendig {
            val søknadId = UUID.randomUUID()
            håndterFørstegangssøknadSelvstendig(januar, søknadId = søknadId)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SYKEPENGEGRUNNLAG)

            val behov = sykepengegrunnlagbehov(1.vedtaksperiode).single()
            assertEquals(1.januar, behov.skjæringstidspunkt)
            assertEquals(emptyList<Arbeidsgiver>(), behov.arbeidsgivere)
            assertEquals(Selvstendig(søknadId), behov.selvstendig)

            håndterSykepengegrunnlag(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        }
    }

    @Test
    fun `forlengelse går ikke gjennom AvventerSykepengegrunnlag`() {
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterSykepengegrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterSøknad(februar)
            assertTilstander(
                2.vedtaksperiode,
                START,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_HISTORIKK,
            )
            assertTrue(sykepengegrunnlagbehov(2.vedtaksperiode).isEmpty())
        }
    }

    @Test
    fun `uten togglen går perioden rett til AvventerHistorikk`() =
        Toggle.AvventerSykepengegrunnlag.disable {
            a1 {
                håndterSøknad(januar)
                håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
                håndterVilkårsgrunnlag(1.vedtaksperiode)
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
                assertTrue(sykepengegrunnlagbehov(1.vedtaksperiode).isEmpty())
            }
        }
}
