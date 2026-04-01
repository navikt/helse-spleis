package no.nav.helse.spleis.e2e.infotrygd

import java.time.LocalDate
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_3
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.spleis.e2e.arbeidsgiveropplysninger.TrengerArbeidsgiveropplysningerTest.Companion.assertEtterspurt
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class DobbelbehandlingIInfotrygdTest : AbstractDslTest() {

    @Test
    fun `avdekker overlapp dobbelbehandlinger i Infotrygd`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
            håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            val historie1 = listOf(
                ArbeidsgiverUtbetalingsperiode(a1, 3.januar, 26.januar)
            )
            håndterUtbetalingshistorikkEtterInfotrygdendring(
                historie1,
                besvart = LocalDate.EPOCH.atStartOfDay()
            )
            observatør.assertEtterspurt(1.vedtaksperiode, EventSubscription.Inntekt::class, EventSubscription.Refusjon::class)
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `utbetaling i infotrygd etterpå`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)

            val historie1 = listOf(
                ArbeidsgiverUtbetalingsperiode(a1, 17.januar, 26.januar)
            )
            håndterUtbetalingshistorikkEtterInfotrygdendring(
                historie1,
                besvart = LocalDate.EPOCH.atStartOfDay()
            )

            håndterPåminnelse(1.vedtaksperiode, AVSLUTTET, flagg = setOf("ønskerReberegning"))
            håndterYtelser(1.vedtaksperiode)

            assertVarsel(RV_IT_3, 1.vedtaksperiode.filter())
            inspektør.utbetalinger(1.vedtaksperiode).last().inspektør.also { utbetalingInspektør ->
                assertEquals(Endringskode.UEND, utbetalingInspektør.arbeidsgiverOppdrag.inspektør.endringskode)
                assertEquals(0, utbetalingInspektør.personOppdrag.inspektør.antallLinjer())
            }
        }
    }
}
