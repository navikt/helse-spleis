package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.januar
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
            håndterUtbetalingshistorikkEtterInfotrygdendring(utbetalinger = listOf(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 16.januar, 100.prosent, INNTEKT)))
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
            håndterUtbetalingshistorikkEtterInfotrygdendring(utbetalinger = listOf(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 16.januar, 100.prosent, INNTEKT)))
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
            håndterUtbetalingshistorikkEtterInfotrygdendring(utbetalinger = listOf(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 16.januar, 100.prosent, INNTEKT)))
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
            håndterUtbetalingshistorikkEtterInfotrygdendring(utbetalinger = listOf(ArbeidsgiverUtbetalingsperiode(a3, 1.januar, 16.januar, 100.prosent, INNTEKT)))
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
            håndterUtbetalingshistorikkEtterInfotrygdendring(utbetalinger = listOf(ArbeidsgiverUtbetalingsperiode(a3, 1.januar, 16.januar, 100.prosent, INNTEKT)))
            nullstillTilstandsendringer()
            håndterPersonPåminnelse()
            assertTilstander(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
        a1 {
            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, TIL_INFOTRYGD)
        }
    }
}