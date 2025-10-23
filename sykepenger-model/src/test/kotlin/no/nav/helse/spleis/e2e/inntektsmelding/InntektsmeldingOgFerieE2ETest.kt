package no.nav.helse.spleis.e2e.inntektsmelding

import no.nav.helse.dsl.a1
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_ANNULLERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_ANNULLERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertForkastetPeriodeTilstander
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.håndterAnnullerUtbetaling
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class InntektsmeldingOgFerieE2ETest : AbstractEndToEndTest() {

    @Test
    fun ferieforlengelse() {
        nyttVedtak(januar)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 20.februar))
        nullstillTilstandsendringer()
        håndterSøknad(Sykdom(1.februar, 20.februar, 100.prosent), Ferie(1.februar, 20.februar))

        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `forkaster kort periode etter annullering`() {
        nyPeriode(1.januar til 5.januar, a1)
        nyPeriode(10.januar til 16.januar, a1)
        nyPeriode(17.januar til 20.januar, a1)
        nyttVedtak(21.januar til 31.januar, arbeidsgiverperiode = listOf(1.januar til 5.januar, 10.januar til 20.januar), orgnummer = a1, vedtaksperiodeIdInnhenter = 4.vedtaksperiode)
        nullstillTilstandsendringer()
        håndterAnnullerUtbetaling()
        håndterUtbetalt(orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
        assertTilstander(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
        assertForkastetPeriodeTilstander(4.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD, orgnummer = a1)
    }
}
