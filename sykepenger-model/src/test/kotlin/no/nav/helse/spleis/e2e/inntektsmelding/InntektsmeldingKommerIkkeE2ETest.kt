package no.nav.helse.spleis.e2e.inntektsmelding

import java.time.LocalDateTime
import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.januar
import no.nav.helse.person.TilstandType
import no.nav.helse.person.aktivitetslogg.Aktivitet
import org.junit.jupiter.api.Test

internal class InntektsmeldingKommerIkkeE2ETest : AbstractDslTest() {

    @Test
    fun `lager ikke påminnelse om vedtaksperioden har ventet mindre enn tre måneder`() = Toggle.InntektsmeldingSomIkkeKommer.enable {
        val nå = LocalDateTime.now()
        val tilstandsendringstidspunkt = nå.minusMonths(3).plusDays(1)
        a1 {
            håndterSøknad(januar)
            håndterPåminnelse(1.vedtaksperiode, TilstandType.AVVENTER_INNTEKTSMELDING, tilstandsendringstidspunkt, nå)
            assertIngenBehov(1.vedtaksperiode, Aktivitet.Behov.Behovtype.InntekterForSykepengegrunnlagForArbeidsgiver)
        }
    }

    @Test
    fun `lager påminnelse om vedtaksperioden har ventet mer enn tre måneder`() = Toggle.InntektsmeldingSomIkkeKommer.enable {
        val nå = LocalDateTime.now()
        val tilstandsendringstidspunkt = nå.minusMonths(3)
        a1 {
            håndterSøknad(januar)
            håndterPåminnelse(1.vedtaksperiode, TilstandType.AVVENTER_INNTEKTSMELDING, tilstandsendringstidspunkt, nå)
            assertBehov(1.vedtaksperiode, Aktivitet.Behov.Behovtype.InntekterForSykepengegrunnlagForArbeidsgiver)
        }
    }
}