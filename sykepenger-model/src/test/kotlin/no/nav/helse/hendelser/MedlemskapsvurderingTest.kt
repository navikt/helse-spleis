package no.nav.helse.hendelser

import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_MV_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_MV_2
import no.nav.helse.spleis.e2e.assertInfo
import no.nav.helse.spleis.e2e.assertIngenVarsler
import no.nav.helse.spleis.e2e.assertVarsel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MedlemskapsvurderingTest {
    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    fun setup() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `bruker er medlem`() {
        Medlemskapsvurdering.varsle(Medlemskapsvurdering.Medlemskapstatus.Ja, aktivitetslogg)
        aktivitetslogg.assertIngenVarsler()
        aktivitetslogg.assertInfo("Bruker er medlem av Folketrygden")
    }

    @Test
    fun `bruker er kanskje medlem`() {
        Medlemskapsvurdering.varsle(Medlemskapsvurdering.Medlemskapstatus.VetIkke, aktivitetslogg)
        aktivitetslogg.assertIngenVarsler()
        aktivitetslogg.assertInfo("Bruker er VetIkke-medlem av Folketrygden")
    }

    @Test
    fun `får varsel ved status UavklartMedBrukerspørsmål`() {
        Medlemskapsvurdering.varsle(Medlemskapsvurdering.Medlemskapstatus.UavklartMedBrukerspørsmål, aktivitetslogg)
        aktivitetslogg.assertVarsel(RV_MV_1)
    }

    @Test
    fun `bruker er ikke medlem`() {
        Medlemskapsvurdering.varsle(Medlemskapsvurdering.Medlemskapstatus.Nei, aktivitetslogg)
        aktivitetslogg.assertVarsel(RV_MV_2)
    }
}
