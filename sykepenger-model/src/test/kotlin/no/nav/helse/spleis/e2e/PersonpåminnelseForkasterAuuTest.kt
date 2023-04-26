package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class PersonpåminnelseForkasterAuuTest: AbstractDslTest() {

    @Test
    fun `En ensom auu som ikke overlapper med infotrygd`() {
        a1 {
            nyPeriode(1.januar til 16.januar)
            nullstillTilstandsendringer()
            håndterPersonPåminnelse()
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }
    }

    @Test
    fun `En ensom auu som overlapper med infotrygd`() {
        a1 {
            nyPeriode(1.januar til 16.januar)
            infotrygdUtbetalingUtenFunksjonelleFeil(1.januar til 16.januar)
            nullstillTilstandsendringer()
            håndterPersonPåminnelse()
            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `En ensom auu som overlapper med ferie i infotrygd`() {
        a1 {
            nyPeriode(1.januar til 16.januar)
            håndterUtbetalingshistorikkEtterInfotrygdendring(utbetalinger = listOf(Friperiode(1.januar, 16.januar)))
            nullstillTilstandsendringer()
            håndterPersonPåminnelse()
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }
    }

    @Test
    fun `Auu som har perioder etter i avventer inntektsmelding innenfor samme arbeidsgiverperiode`() {
        a1 {
            nyPeriode(1.januar til 16.januar)
            nyPeriode(3.februar til 28.februar)
            infotrygdUtbetalingUtenFunksjonelleFeil(1.januar til 16.januar)
            nullstillTilstandsendringer()
            håndterPersonPåminnelse()
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `Auu som har perioder etter i avventer inntektsmelding med annen arbeidsgiverperiode`() {
        a1 {
            nyPeriode(1.januar til 16.januar)
            nyPeriode(4.februar til 28.februar)
            infotrygdUtbetalingUtenFunksjonelleFeil(1.januar til 16.januar)
            nullstillTilstandsendringer()
            håndterPersonPåminnelse()
            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, TIL_INFOTRYGD)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `Auu som har perioder etter på annen ag i avventer inntektsmelding innenfor samme arbeidsgiverperiode`() {
        a1 {
            nyPeriode(1.januar til 16.januar)
        }
        a2 {
            nyPeriode(3.februar til 28.februar)
            infotrygdUtbetalingUtenFunksjonelleFeil(1.januar til 16.januar)
            nullstillTilstandsendringer()
            håndterPersonPåminnelse()
            assertTilstander(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }
    }

    @Test
    fun `Auu som har perioder på annen ag etter i avventer inntektsmelding med annen arbeidsgiverperiode`() {
        a1 {
            nyPeriode(1.januar til 16.januar)
        }
        a2 {
            nyPeriode(4.februar til 28.februar)
            infotrygdUtbetalingUtenFunksjonelleFeil(1.januar til 16.januar)
            nullstillTilstandsendringer()
            håndterPersonPåminnelse()
            assertTilstander(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
        a1 {
            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `Auu som inneholder skjæringstidspunktet på personen skal ikke forkastes`() {
        a1 {
            nyPeriode(1.januar til 16.januar)
            nullstillTilstandsendringer()
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }
        a2 {
            nyPeriode(17.januar til 28.februar)
            nullstillTilstandsendringer()
            assertTilstander(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
        a1 {
            infotrygdUtbetalingUtenFunksjonelleFeil(1.januar til 10.januar)
            håndterPersonPåminnelse()
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }

    }

    @Test
    fun `Auu bridger gapet mellom to perioder på annen arbeidsgiver`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
        }
        a2 {
            nyPeriode(1.februar til 16.februar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }
        a1 {
            håndterSøknad(Sykdom(17.februar, 15.mars, 100.prosent))
            håndterInntektsmelding(listOf(17.februar til 4.mars))
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
        }

        a2 {
            infotrygdUtbetalingUtenFunksjonelleFeil(1.februar til 16.februar)
            nullstillTilstandsendringer()
            håndterPersonPåminnelse()
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }

        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `Auu har både overlappende periode og periode som starter etter`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
        }
        a2 {
            nyPeriode(31.januar til 15.februar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }
        a1 {
            håndterSøknad(Sykdom(16.februar, 15.mars, 100.prosent))
            håndterInntektsmelding(listOf(16.februar til 4.mars))
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
        }

        a2 {
            infotrygdUtbetalingUtenFunksjonelleFeil(1.februar til 16.februar)
            nullstillTilstandsendringer()
            håndterPersonPåminnelse()
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }

        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `Auu med snute på annen arbeidsgiver `() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
        }

        a2 {
            nyPeriode(31.januar til 15.februar)
            infotrygdUtbetalingUtenFunksjonelleFeil(31.januar til 15.februar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            nullstillTilstandsendringer()
            håndterPersonPåminnelse()
            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, TIL_INFOTRYGD)
        }
        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
        }

    }



    @Test
    fun `Auu med hale på annen arbeidsgiver `() {
        a1 {
            nyPeriode(1.januar til 16.januar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }
        a2 {
            nyttVedtak(16.januar, 15.februar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            infotrygdUtbetalingUtenFunksjonelleFeil(1.januar til 16.januar)
            nullstillTilstandsendringer()
            håndterPersonPåminnelse()
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
        }
        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }

    }

    @Test
    fun `Auu slukt av periode på annen arbeidsgiver`() {
        a1 {
            nyPeriode(2.januar til 17.januar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }
        a2 {
            nyttVedtak(1.januar, 18.januar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            infotrygdUtbetalingUtenFunksjonelleFeil(2.januar til 17.januar)
            nullstillTilstandsendringer()
            håndterPersonPåminnelse()
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
        }

        a1 {
            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `Vedtaksperiode på annen ag sluker Auu, men vi har en vedtaksperiode på samme ag innenfor samme agp`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        }
        a2 {
            nyPeriode(20.januar til 25.januar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            nyPeriode(5.februar til 28.februar)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            infotrygdUtbetalingUtenFunksjonelleFeil(20.januar til 25.januar)
            nullstillTilstandsendringer()
            håndterPersonPåminnelse()
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }

        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
        }
    }


    private fun TestPerson.TestArbeidsgiver.infotrygdUtbetalingUtenFunksjonelleFeil(periode: Periode) {
        håndterUtbetalingshistorikkEtterInfotrygdendring(utbetalinger = listOf(ArbeidsgiverUtbetalingsperiode(a3, periode.start, periode.endInclusive, 100.prosent, INNTEKT)))
        assertIngenFunksjonelleFeil()
    }
}