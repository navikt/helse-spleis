package no.nav.helse.spleis.e2e.selvstendig

import java.time.LocalDate
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.selvstendig
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Søknad.Søknadsperiode.MeldingTilNavDager
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.Avslagstidslinje
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AvslåttMeldingTilNavdagTest : AbstractDslTest() {

    @Test
    fun `overstyring til avslått melding til Navdag`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(MeldingTilNavDager(1.januar, 1.januar), Sykdom(2.januar, 31.januar, 100.prosent))
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertEquals("MSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomstidslinje.toShortString())
            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar))
            with(inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør) {
                assertEquals(emptyList<LocalDate>(), avvistedatoer)
            }

            håndterOverstyrTidslinje((1.januar.somPeriode()).map { ManuellOverskrivingDag(it, Dagtype.AvslattMeldingTilNavdag) })
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertEquals("ASSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomstidslinje.toShortString())
            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 2.januar, listOf(2.januar til 17.januar))
            with(inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør) {
                assertEquals(listOf(Begrunnelse.AvslåttMeldingTilNavDag), begrunnelse(1.januar))
                assertEquals(listOf(1. januar), avvistedatoer)
            }

            assertGjenoppbygget(dto())
        }
    }

    @Test
    fun `overstyring til avslått melding Navdag men så ombestemmer saksbehandler seg`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(MeldingTilNavDager(1.januar, 1.januar), Sykdom(2.januar, 31.januar, 100.prosent))
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertEquals("MSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomstidslinje.toShortString())
            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar))
            assertEquals(Avslagstidslinje(), inspektør.vedtaksperioder(1.vedtaksperiode).avslagstidslinje)
            with(inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør) {
                assertEquals(emptyList<LocalDate>(), avvistedatoer)
            }

            håndterOverstyrTidslinje((1.januar.somPeriode()).map { ManuellOverskrivingDag(it, Dagtype.AvslattMeldingTilNavdag) })
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertEquals("ASSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomstidslinje.toShortString())
            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 2.januar, listOf(2.januar til 17.januar))
            assertEquals(Avslagstidslinje(1.januar.somPeriode() to Avslagstidslinje.Avslagsdag(listOf(Begrunnelse.AvslåttMeldingTilNavDag), "Saksbehandler")), inspektør.vedtaksperioder(1.vedtaksperiode).avslagstidslinje)
            with(inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør) {
                assertEquals(listOf(Begrunnelse.AvslåttMeldingTilNavDag), begrunnelse(1.januar))
                assertEquals(listOf(1. januar), avvistedatoer)
            }

            håndterOverstyrTidslinje((1.januar.somPeriode()).map { ManuellOverskrivingDag(it, Dagtype.MeldingTilNavdag) })
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertEquals("MSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomstidslinje.toShortString())
            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar))
            with(inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør) {
                assertEquals(emptyList<LocalDate>(), avvistedatoer)
            }
            assertEquals(Avslagstidslinje(), inspektør.vedtaksperioder(1.vedtaksperiode).avslagstidslinje)
        }
    }

    @Test
    fun `overstyring fra MeldingTilNavDag til avslått melding til Nav dag over helg`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(MeldingTilNavDager(29.desember(2017), 1.januar), Sykdom(2.januar, 31.januar, 100.prosent))
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertEquals("MOO MSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomstidslinje.toShortString())
            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 29.desember(2017), listOf(29.desember(2017) til 13.januar))
            with(inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør) {
                assertEquals(emptyList<LocalDate>(), avvistedatoer)
            }
            assertEquals(Avslagstidslinje(), inspektør.vedtaksperioder(1.vedtaksperiode).avslagstidslinje)

            val avslagsperiode = (29.desember(2017) til 1.januar)
            håndterOverstyrTidslinje(avslagsperiode.map { ManuellOverskrivingDag(it, Dagtype.AvslattMeldingTilNavdag) })
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertEquals("ARR ASSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomstidslinje.toString())
            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 2.januar, listOf(2.januar til 17.januar))
            assertEquals(Avslagstidslinje(avslagsperiode to Avslagstidslinje.Avslagsdag(listOf(Begrunnelse.AvslåttMeldingTilNavDag), "Saksbehandler")), inspektør.vedtaksperioder(1.vedtaksperiode).avslagstidslinje)

            with(inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør) {
                assertEquals(avslagsperiode.iterator().asSequence().toList(), avvistedatoer)
                avvistedatoer.forEach { dato ->
                    assertEquals(listOf(Begrunnelse.AvslåttMeldingTilNavDag), begrunnelse(dato))
                }
            }
        }
    }
}
