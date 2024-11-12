package no.nav.helse.hendelser

import no.nav.helse.oktober
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.september
import no.nav.helse.spleis.e2e.assertIngenVarsel
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

class ForeldrepengerTest {

    @Test
    fun `100 prosent foreldrepenger 14 dager i forkant`() {
        val foreldrepenger = Foreldrepenger(listOf(
            GradertPeriode(17.september til 30.september, 100)
        ))
        val aktivitetslogg = Aktivitetslogg()
        foreldrepenger.valider(aktivitetslogg, 1.oktober til 30.oktober, false)
        aktivitetslogg.assertIngenVarsel(Varselkode.RV_AY_12)
    }

    @Test
    fun `100 prosent foreldrepenger mer enn 14 dager i forkant`() {
        val foreldrepenger = Foreldrepenger(listOf(
            GradertPeriode(16.september til 30.september, 100)
        ))
        val aktivitetslogg = Aktivitetslogg()
        foreldrepenger.valider(aktivitetslogg, 1.oktober til 30.oktober, false)
        aktivitetslogg.assertVarsel(Varselkode.RV_AY_12)
    }

    @Test
    fun `80 prosent foreldrepenger mer enn 14 dager i forkant`() {
        val foreldrepenger = Foreldrepenger(listOf(
            GradertPeriode(16.september til 30.september, 80)
        ))
        val aktivitetslogg = Aktivitetslogg()
        foreldrepenger.valider(aktivitetslogg, 1.oktober til 30.oktober, false)
        aktivitetslogg.assertIngenVarsel(Varselkode.RV_AY_12)
    }
}