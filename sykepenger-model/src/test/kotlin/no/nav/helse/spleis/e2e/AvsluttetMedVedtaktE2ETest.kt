package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.person.EventSubscription.UtkastTilVedtakEvent.FastsattIInfotrygd
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AvsluttetMedVedtaktE2ETest : AbstractDslTest() {

    @Test
    fun `sender vedtak fattet med sykepengegrunnlag fastsatt i Infotrygd`() {
        medJSONPerson("/personer/infotrygdforlengelse.json", 334)
        a1 {
            håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar))
            håndterSykmelding(februar)
            håndterSøknad(februar)
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_IT_14, 1.vedtaksperiode.filter())
            assertEquals(1, observatør.avsluttetMedVedtakEvent.size)
            val event = observatør.avsluttetMedVedtakEvent.values.single()
            val forventetSykepengegrunnlagsfakta = FastsattIInfotrygd(372000.0, "a1")
            assertEquals(forventetSykepengegrunnlagsfakta, event.sykepengegrunnlagsfakta)
        }
    }
}
