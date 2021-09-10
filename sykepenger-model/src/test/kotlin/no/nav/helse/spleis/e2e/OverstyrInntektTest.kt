package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.person.TilstandType
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class OverstyrInntektTest : AbstractEndToEndTest() {

    @BeforeEach
    fun setup() {
        Toggles.RevurderInntekt.enable()
    }

    @AfterEach
    fun tearDown() {
        Toggles.RevurderInntekt.pop()
    }

    @Test
    fun `skal kunne overstyre en inntekt i et enkelt case`() {
        val fom = 1.januar(2021)
        val overstyrtInntekt = 32000.månedlig
        tilGodkjenning(fom, 31.januar(2021), 100.prosent, fom)

        assertInntektForDato(INNTEKT, fom, inspektør)

        håndterOverstyring(inntekt = overstyrtInntekt, orgnummer = ORGNUMMER, skjæringstidspunkt = fom, ident = "a123456")

        assertTilstander(1.vedtaksperiode,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
            TilstandType.AVVENTER_SØKNAD_FERDIG_GAP,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING, // <-- Her står vi når vi overstyrer inntekt.
            TilstandType.AVVENTER_VILKÅRSPRØVING)

        // dra saken til AVVENTER_GODKJENNING
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        // assert at vi går gjennom restene av tilstandene som vanlig
        assertTilstander(1.vedtaksperiode,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
            TilstandType.AVVENTER_SØKNAD_FERDIG_GAP,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING, // <-- Her sto vi da vi overstyrte inntekt.
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING) // <-- og her skal den nye, overstyrte inntekten vært benyttet

        // assert at vi bruker den nye inntekten i beregning av penger til sjuk.
        assertInntektForDato(overstyrtInntekt, fom, inspektør)
    }

    @Test
    fun `overstyrt inntekt til mer enn 25 prosent avvik skal sendes til infotrygd`() {
        val fom = 1.januar(2021)
        val overstyrtInntekt = INNTEKT*1.40
        tilGodkjenning(fom, 31.januar(2021), 100.prosent, fom)

        håndterOverstyring(inntekt = overstyrtInntekt, orgnummer = ORGNUMMER, skjæringstidspunkt = fom, ident = "a123456")

        håndterVilkårsgrunnlag(1.vedtaksperiode)

        assertForkastetPeriodeTilstander(1.vedtaksperiode,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
            TilstandType.AVVENTER_SØKNAD_FERDIG_GAP,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.TIL_INFOTRYGD)
    }
}
