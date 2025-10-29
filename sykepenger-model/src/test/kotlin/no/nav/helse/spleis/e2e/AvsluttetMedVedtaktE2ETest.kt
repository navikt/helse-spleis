package no.nav.helse.spleis.e2e

import no.nav.helse.februar
import no.nav.helse.person.EventSubscription.UtkastTilVedtakEvent.FastsattIInfotrygd
import no.nav.helse.person.aktivitetslogg.Varselkode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AvsluttetMedVedtaktE2ETest : AbstractEndToEndTest() {
    @Test
    fun `sender vedtak fattet med sykepengegrunnlag fastsatt i Infotrygd`() {
        createOvergangFraInfotrygdPerson()
        forlengVedtak(februar)

        assertVarsel(Varselkode.RV_IT_14, 1.vedtaksperiode.filter())
        assertEquals(1, observatør.avsluttetMedVedtakEvent.size)
        val event = observatør.avsluttetMedVedtakEvent.values.single()
        val forventetSykepengegrunnlagsfakta = FastsattIInfotrygd(372000.0, "a1")
        assertEquals(forventetSykepengegrunnlagsfakta, event.sykepengegrunnlagsfakta)
    }
}
