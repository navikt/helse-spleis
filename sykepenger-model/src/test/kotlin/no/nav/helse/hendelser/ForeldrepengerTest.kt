package no.nav.helse.hendelser

import no.nav.helse.*
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.assertVarsler
import org.junit.jupiter.api.Test

class ForeldrepengerTest {
    @Test
    fun `100 prosent foreldrepenger 14 dager i forkant`() {
        val foreldrepenger =
            Foreldrepenger(
                listOf(
                    GradertPeriode(17.september til 30.september, 100),
                ),
            )
        val aktivitetslogg = Aktivitetslogg()
        foreldrepenger.valider(aktivitetslogg, 1.oktober til 30.oktober, false)
        aktivitetslogg.assertVarsler(listOf(Varselkode.RV_AY_5))
    }

    @Test
    fun `100 prosent foreldrepenger mer enn 14 dager i forkant`() {
        val foreldrepenger =
            Foreldrepenger(
                listOf(
                    GradertPeriode(16.september til 30.september, 100),
                ),
            )
        val aktivitetslogg = Aktivitetslogg()
        foreldrepenger.valider(aktivitetslogg, 1.oktober til 30.oktober, false)
        aktivitetslogg.assertVarsel(Varselkode.RV_AY_12)
    }

    @Test
    fun `100 prosent foreldrepenger mer enn 14 dager i forkant - foreldrepengene slutter på en fredag og sykepengene begynner på mandag`() {
        val foreldrepenger =
            Foreldrepenger(
                listOf(
                    GradertPeriode(1.januar til fredag den 19.januar, 100),
                ),
            )
        val aktivitetslogg = Aktivitetslogg()
        foreldrepenger.valider(aktivitetslogg, mandag den 22.januar til 31.januar, false)
        aktivitetslogg.assertVarsel(Varselkode.RV_AY_12)
    }

    @Test
    fun `100 prosent foreldrepenger mer enn 14 dager i forkant - foreldrepengene slutter på en torsdag og sykepengene begynner på mandag`() {
        val foreldrepenger =
            Foreldrepenger(
                listOf(
                    GradertPeriode(1.januar til torsdag den 18.januar, 100),
                ),
            )
        val aktivitetslogg = Aktivitetslogg()
        foreldrepenger.valider(aktivitetslogg, mandag den 22.januar til 31.januar, false)
        aktivitetslogg.assertVarsler(listOf(Varselkode.RV_AY_5))
    }

    @Test
    fun `80 prosent foreldrepenger mer enn 14 dager i forkant`() {
        val foreldrepenger =
            Foreldrepenger(
                listOf(
                    GradertPeriode(16.september til 30.september, 80),
                ),
            )
        val aktivitetslogg = Aktivitetslogg()
        foreldrepenger.valider(aktivitetslogg, 1.oktober til 30.oktober, false)
        aktivitetslogg.assertVarsler(listOf(Varselkode.RV_AY_5))
    }
}
