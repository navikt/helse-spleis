package no.nav.helse.spleis.e2e.refusjon

import no.nav.helse.Toggle
import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.refusjon.RefusjonsservitørView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MigrereUbrukteRefusjonsopplysningerTest : AbstractDslTest() {

    private lateinit var forrigeUbrukteRefusjonsopplysninger: RefusjonsservitørView

    private fun setup1og2() {
        nyttVedtak(januar, refusjon = Inntektsmelding.Refusjon(INNTEKT, 1.mars))
    }

    @Test
    @Order(1)
    fun `Endring i refusjon frem i tid fra inntektsmelding - med toggle`() = Toggle.LagreUbrukteRefusjonsopplysninger.enable {
        setup1og2()
        forrigeUbrukteRefusjonsopplysninger = inspektør.ubrukteRefusjonsopplysninger
    }

    @Test
    @Order(2)
    fun `Endring i refusjon frem i tid fra inntektsmelding - uten toggle`() = Toggle.LagreUbrukteRefusjonsopplysninger.disable {
        setup1og2()
        migrerUbrukteRefusjonsopplysninger()
        assertForventetFeil(
            forklaring = "Vi har ikke skrevet migreringen ennå",
            nå = { assertEquals(RefusjonsservitørView(emptyMap()), inspektør.ubrukteRefusjonsopplysninger) },
            ønsket = { assertEquals(forrigeUbrukteRefusjonsopplysninger, inspektør.ubrukteRefusjonsopplysninger) }
        )
    }

    private fun migrerUbrukteRefusjonsopplysninger() {}

}