package no.nav.helse.spleis.e2e.tilkommen_arbeidsgiver

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Søknad.TilkommenInntekt
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class TilkommenInntektTest : AbstractDslTest() {

    @Test
    fun `blanke ark - men skal nok ha varsel`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), tilkomneInntekter = listOf(TilkommenInntekt(fom = 10.januar, tom = null, orgnummer = "a2", beløp = 10000.månedlig)))
            assertVarsel(Varselkode.RV_SV_5)
        }
    }
}