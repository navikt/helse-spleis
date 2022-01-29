package no.nav.helse.spleis.e2e

import no.nav.helse.ForventetFeil
import no.nav.helse.Toggle
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * Samtlige tester her setter opp et enkelt tilfelle med flere arbeidsgivere hvor vi har flere perioder i spill;
 * noen er i Ferdig-tilstand og andre venter (Uferdig).
 *
 * Dersom noe forkastes hos en arbeidsgiver trigges det 'søppelbøtte' på personnivå,
 * som potensielt forkaster perioder på tvers av arbeidsgivere.
 *
 * Testene er på ingen måte uttømmende, men forsøker å trigge forkasting
 * basert på noen trivielle errors.
 */
internal class ForkastingFlereArbeidsgivere : AbstractEndToEndTest() {
    @ForventetFeil("Behandling på gjenopptas på tvers av flere arbeidsgivere")
    @Test
    fun `feil i utbetalingshistorikk skal ikke medføre at perioder hos annen AG blir stuck`() = Toggle.FlereArbeidsgivereFraInfotrygd.disable {
        setupTest()
        håndterUtbetalingshistorikk(
            1.vedtaksperiode, inntektshistorikk = listOf(
                Inntektsopplysning(a1, 1.januar, INNTEKT, true),
                Inntektsopplysning(a2, 1.januar, INNTEKT, true),
            ), orgnummer = a2
        )

        assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD, orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD, orgnummer = a2)
        assertSisteTilstand(2.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP, orgnummer = a2)
    }

    @ForventetFeil("Behandling på gjenopptas på tvers av flere arbeidsgivere")
    @Test
    fun `feil i overlappende søknad hos AG1 skal ikke medføre at perioder hos annen AG blir stuck`() {
        setupTest()
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Arbeid(30.januar, 31.januar), orgnummer = a1)

        assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD, orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD, orgnummer = a2)
        assertSisteTilstand(2.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP, orgnummer = a2)
    }

    @ForventetFeil("Behandling på gjenopptas på tvers av flere arbeidsgivere")
    @Test
    fun `feil i overlappende søknad hos AG2 skal ikke medføre at perioder hos annen AG blir stuck`() {
        setupTest()
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Arbeid(30.januar, 31.januar), orgnummer = a2)

        assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD, orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD, orgnummer = a2)
        assertSisteTilstand(2.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP, orgnummer = a2)
    }

    @ForventetFeil("Behandling på gjenopptas på tvers av flere arbeidsgivere")
    @Test
    fun `makstid på periode hos AG2 skal ikke medføre at perioder hos annen AG blir stuck`() {
        setupTest()
        håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, LocalDateTime.now().minusDays(200), orgnummer = a2)

        assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD, orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD, orgnummer = a2)
        assertSisteTilstand(2.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP, orgnummer = a2)
    }

    @ForventetFeil("Behandling på gjenopptas på tvers av flere arbeidsgivere")
    @Test
    fun `makstid på periode hos AG1 skal ikke medføre at perioder hos annen AG blir stuck`() {
        setupTest()
        håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, LocalDateTime.now().minusDays(200), orgnummer = a1)

        assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD, orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD, orgnummer = a2)
        assertSisteTilstand(2.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP, orgnummer = a2)
    }

    private fun setupTest() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(1.mars til 16.mars), orgnummer = a1)
    }
}
