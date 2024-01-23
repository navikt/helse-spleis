package no.nav.helse.spleis.e2e.generasjoner

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class GenerasjonOpprettetEventTest : AbstractDslTest() {

    @Test
    fun `event om opprettet generasjon`() {
        a1 {
            val søknadId = UUID.randomUUID()
            val opprettet = LocalDateTime.now()
            val innsendt = opprettet.minusHours(2)
            håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), søknadId = søknadId, sendtTilNAVEllerArbeidsgiver = innsendt, opprettet = opprettet)
            val generasjonOpprettetEvent = observatør.generasjonOpprettetEventer.last()
            inspektør(1.vedtaksperiode).generasjoner.also { generasjoner ->
                val generasjonsId = generasjoner.single().id
                assertEquals(generasjonsId, generasjonOpprettetEvent.generasjonId)
            }
        }
    }
}