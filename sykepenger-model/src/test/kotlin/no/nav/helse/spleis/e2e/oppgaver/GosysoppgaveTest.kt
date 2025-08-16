package no.nav.helse.spleis.e2e.oppgaver

import java.util.*
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype.FRILANSER
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype.ORDINÆRT
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.oktober
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_3
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_1
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class GosysoppgaveTest: AbstractDslTest() {

    @Test
    fun `Forlengelse av AUU blir oppgave i Infotrygdkø`() {
        a1 {
            håndterSøknad(1.januar til 16.januar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent), utenlandskSykmelding = true)
            assertOppgaveIInfotrygdkø(2.vedtaksperiode)
        }
    }

    @Test
    fun `Kort gap til AUU blir oppgave i Infotrygdkø`() {
        a1 {
            håndterSøknad(1.januar til 16.januar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            håndterSøknad(Sykdom(3.februar, 28.februar, 100.prosent), utenlandskSykmelding = true)
            assertOppgaveIInfotrygdkø(2.vedtaksperiode)
        }
    }

    @Test
    fun `Kort søknad med kort gap til annen kort søknad blir oppgave i Infotrygdkø`() {
        a1 {
            håndterSøknad(1.januar til 5.januar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            håndterSøknad(Sykdom(10.januar, 15.januar, 100.prosent), utenlandskSykmelding = true)
            assertOppgaveIInfotrygdkø(2.vedtaksperiode)
        }
    }

    @Test
    fun `Forlengelse på allerede utbetalt periode blir oppgave i Speilkø`() {
        a1 {
            nyttVedtak(1.januar til 17.januar)
            håndterSøknad(Sykdom(18.januar, 31.januar, 100.prosent), utenlandskSykmelding = true)
            assertOppgaveISpeilkø(2.vedtaksperiode)
        }
    }

    @Test
    fun `Kort gap til allerede utbetalt periode blir oppgave i Speilkø`() {
        a1 {
            nyttVedtak(1.januar til 17.januar)
            håndterSøknad(Sykdom(26.januar, 31.januar, 100.prosent), utenlandskSykmelding = true)
            assertOppgaveISpeilkø(2.vedtaksperiode)
        }
    }

    @Test
    fun `Kant-i-kant på AUU hos annen arbeidsgiver blir oppgave i Infotrygdkø`() {
        a1 {
            håndterSøknad(1.januar til 16.januar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }
        a2 {
            håndterSøknad(17.januar til 17.februar)
            håndterInntektsmelding(listOf(17.januar til 1.februar))
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to INNTEKT, a2 to INNTEKT),
                arbeidsforhold = listOf(
                    Vilkårsgrunnlag.Arbeidsforhold(a1, ansattFom = 1.oktober(2017), type = ORDINÆRT),
                    Vilkårsgrunnlag.Arbeidsforhold(a2, ansattFom = 1.oktober(2017), type = FRILANSER),
                )
            )
            assertVarsler(1.vedtaksperiode, RV_VV_1)
            assertForkastetPeriodeTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, TIL_INFOTRYGD, varselkode = RV_IV_3)
            assertOppgaveIInfotrygdkø(1.vedtaksperiode)
        }
        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }
    }

    private fun opprettesOppgaveISpeilkø(vedtaksperiodeId: UUID) = observatør.forkastet(vedtaksperiodeId).speilrelatert
    private fun assertOppgaveISpeilkø(vedtaksperiodeId: UUID) = assertTrue(opprettesOppgaveISpeilkø(vedtaksperiodeId))
    private fun assertOppgaveIInfotrygdkø(vedtaksperiodeId: UUID) = assertFalse(opprettesOppgaveISpeilkø(vedtaksperiodeId))
}
