package no.nav.helse.spleis.e2e.søknad

import java.time.YearMonth
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.førsteArbeidsdag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FremtidigSøknadE2ETest : AbstractDslTest() {
    private companion object {
        private val inneværendeMåned = YearMonth.now()
        private val nesteMåned = inneværendeMåned.plusMonths(1)
        private val fom = inneværendeMåned.atDay(14)
        private val tom = nesteMåned.atDay(14).førsteArbeidsdag()
        private val sisteArbeidsgiverdag = fom.plusDays(15)
    }

    @Test
    fun `kan sende inn søknad før periode er gått ut`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(fom, tom))
            håndterSøknad(Sykdom(fom, tom, 100.prosent))
            håndterArbeidsgiveropplysninger(
                    listOf(Periode(fom, sisteArbeidsgiverdag)),
                    vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt(Oppdragstatus.AKSEPTERT)
            assertTilstander(
                    1.vedtaksperiode,
                    START,
                    AVVENTER_INFOTRYGDHISTORIKK,
                    AVVENTER_INNTEKTSMELDING,
                    AVVENTER_BLOKKERENDE_PERIODE,
                    AVVENTER_VILKÅRSPRØVING,
                    AVVENTER_HISTORIKK,
                    AVVENTER_SIMULERING,
                    AVVENTER_GODKJENNING,
                    TIL_UTBETALING,
                    AVSLUTTET
            )
            val arbeidsgiverOppdrag = inspektør.utbetaling(0).arbeidsgiverOppdrag
            val oppdragInspektør = arbeidsgiverOppdrag.inspektør
            assertEquals(sisteArbeidsgiverdag.plusDays(1), arbeidsgiverOppdrag.first().inspektør.fom)
            assertEquals(tom, arbeidsgiverOppdrag.last().inspektør.tom)
            assertEquals(tom, oppdragInspektør.periode?.endInclusive)
        }
    }
}
