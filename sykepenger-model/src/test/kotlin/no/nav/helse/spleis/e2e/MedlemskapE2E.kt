package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Medlemskapsvurdering.Medlemskapstatus.Nei
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_MV_2
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingstidslinje.Begrunnelse.ManglerMedlemskap
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.AvvistDag
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class MedlemskapE2E : AbstractDslTest() {
    @Test
    fun `søknad med arbeidUtenforNorge gir varsel om medlemskap`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), arbeidUtenforNorge = true)
            assertVarsel(Varselkode.RV_MV_3, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `søknad uten arbeidUtenforNorge gir ikke varsel om medlemskap`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), arbeidUtenforNorge = false)
            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `ikke medlem av folketrygden`() {
        a1 {
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode, medlemskapstatus = Nei)
            assertVarsel(RV_MV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            val forventetAvvisteDager = setOf(17.januar, 18.januar, 19.januar, 22.januar, 23.januar, 24.januar, 25.januar, 26.januar, 29.januar, 30.januar, 31.januar)
            val forventetAvvisteUtbetalingsdager = inspektør.utbetalingstidslinjer(1.vedtaksperiode).drop(16).filterNot { it.dato.erHelg() }
            assertEquals(forventetAvvisteDager, forventetAvvisteUtbetalingsdager.map { it.dato }.toSet())

            forventetAvvisteUtbetalingsdager.forEach { utbetalingsdag ->
                assertTrue(utbetalingsdag is AvvistDag)
                assertEquals(ManglerMedlemskap, (utbetalingsdag as AvvistDag).begrunnelser.single())
            }
        }
    }
}
