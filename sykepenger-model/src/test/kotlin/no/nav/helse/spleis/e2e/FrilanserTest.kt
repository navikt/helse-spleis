package no.nav.helse.spleis.e2e

import java.time.LocalDate.EPOCH
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.frilans
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.oktober
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class FrilanserTest : AbstractDslTest() {

    @Test
    fun `frilanssøknad gir error`() {
        frilans {
            håndterSøknad(januar)
            assertFunksjonelleFeil()
            assertForkastetPeriodeTilstander(1.vedtaksperiode, START, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `frilansarbeidsforhold med feriepenger siste tre måneder før skjæringstidspunktet`() {
        a1 {
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertIngenFunksjonelleFeil()
            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        }
    }

    @Test
    fun `Person med frilanserinntekt i løpet av de siste 3 månedene sendes til infotrygd`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to INNTEKT),
                arbeidsforhold = listOf(
                    Vilkårsgrunnlag.Arbeidsforhold(a1, 1.oktober(2017), 31.oktober(2017), Arbeidsforholdtype.FRILANSER),
                    Vilkårsgrunnlag.Arbeidsforhold(a2, EPOCH, type = Arbeidsforholdtype.FRILANSER)
                )
            )
            assertVarsler(listOf(Varselkode.RV_VV_1), 1.vedtaksperiode.filter())
            assertFunksjonellFeil("Fant frilanserinntekt på en arbeidsgiver de siste 3 månedene", 1.vedtaksperiode.filter())
            assertForkastetPeriodeTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_VILKÅRSPRØVING,
                TIL_INFOTRYGD
            )
        }
    }

    @Test
    fun `Person med frilanserarbeidsforhold uten inntekt i løpet av de siste 3 månedene skal `() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            assertIngenFunksjonelleFeil(1.vedtaksperiode.filter())
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_HISTORIKK
            )
        }
    }

    @Test
    fun `Frilansere oppdages ikke i Aa-reg, men case med frilanser-ghost skal gi warning`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, beregnetInntekt = INNTEKT)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                orgnummer = a1,
                skatteinntekter = listOf(a1 to INNTEKT, a2 to 1000.månedlig),
                arbeidsforhold = listOf(
                    Triple(a1, EPOCH, null)
                )
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt(orgnummer = a1)

            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET)
        }
    }

    @Test
    fun `Førstegangsbehandling med ekstra arbeidsforhold som ikke er aktivt - skal ikke få warning hvis det er flere arbeidsforhold`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(mars)
            håndterInntektsmelding(
                listOf(1.mars til 16.mars),
                førsteFraværsdag = 1.mars,
                beregnetInntekt = 10000.månedlig
            )
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                orgnummer = a1,
                skatteinntekter = listOf(a1 to 10000.månedlig, a2 to 100.månedlig),
                arbeidsforhold = listOf(
                    Triple(a1, EPOCH, null),
                    Triple(a2, EPOCH, 1.februar)
                )
            )
            håndterYtelser(1.vedtaksperiode)
            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `En arbeidsgiver med inntekter fra flere arbeidsgivere de siste tre månedene - får ikke warning om at det er flere arbeidsgivere, får warning om at det er flere inntektskilder enn arbeidsforhold`() {
        a1 {
            val periode = januar
            håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive))
            håndterSøknad(Sykdom(periode.start, periode.endInclusive, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt()

            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
        }
    }
}
