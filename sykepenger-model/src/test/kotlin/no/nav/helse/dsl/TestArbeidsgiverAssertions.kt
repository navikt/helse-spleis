package no.nav.helse.dsl

import java.util.UUID
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Periode
import no.nav.helse.inspectors.PersonInspektør
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.TilstandType
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.AktivitetsloggVisitor
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.AktivitetsloggFilter
import no.nav.helse.spleis.e2e.TestObservatør
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

internal class TestArbeidsgiverAssertions(private val observatør: TestObservatør, private val inspektør: TestArbeidsgiverInspektør, private val personInspektør: PersonInspektør) {
    internal fun assertSisteTilstand(vedtaksperiodeId: UUID, tilstand: TilstandType, errortekst: (() -> String)? = null) {
        assertEquals(tilstand, observatør.tilstandsendringer[vedtaksperiodeId]?.last(), errortekst)
    }

    internal fun assertTilstander(id: UUID, vararg tilstander: TilstandType) {
        assertFalse(inspektør.periodeErForkastet(id)) { "Perioden er forkastet med tilstander: ${observatør.tilstandsendringer[id]}:\n${personInspektør.aktivitetslogg}" }
        assertTrue(inspektør.periodeErIkkeForkastet(id)) { "Perioden er forkastet med tilstander: ${observatør.tilstandsendringer[id]}\n${personInspektør.aktivitetslogg}" }
        assertEquals(tilstander.asList(), observatør.tilstandsendringer[id])
    }

    internal fun assertForkastetPeriodeTilstander(id: UUID, vararg tilstander: TilstandType) {
        assertTrue(inspektør.periodeErForkastet(id)) { "Perioden er ikke forkastet" }
        assertFalse(inspektør.periodeErIkkeForkastet(id)) { "Perioden er ikke forkastet" }
        assertEquals(tilstander.asList(), observatør.tilstandsendringer[id])
    }

    internal fun assertHarHendelseIder(vedtaksperiodeId: UUID, vararg hendelseIder: UUID) {
        assertTrue(inspektør.hendelseIder(vedtaksperiodeId).containsAll(hendelseIder.toSet()))
    }
    internal fun assertHarIkkeHendelseIder(vedtaksperiodeId: UUID, vararg hendelseIder: UUID) {
        assertEquals(emptySet<UUID>(), inspektør.hendelseIder(vedtaksperiodeId).intersect(hendelseIder.toSet()))
    }

    internal fun assertUtbetalingsbeløp(
        vedtaksperiodeId: UUID,
        forventetArbeidsgiverbeløp: Int,
        forventetArbeidsgiverRefusjonsbeløp: Int,
        subset: Periode? = null
    ) {
        val utbetalingstidslinje = inspektør.utbetalingstidslinjer(vedtaksperiodeId).let { subset?.let(it::subset) ?: it }

        utbetalingstidslinje.filterNot { it.dato.erHelg() }.forEach { utbetalingsdag ->
            assertEquals(forventetArbeidsgiverbeløp.daglig, utbetalingsdag.økonomi.inspektør.arbeidsgiverbeløp) { "feil arbeidsgiverbeløp for dag ${utbetalingsdag.dato} " }
            assertEquals(forventetArbeidsgiverRefusjonsbeløp.daglig, utbetalingsdag.økonomi.inspektør.arbeidsgiverRefusjonsbeløp.rundTilDaglig())
            assertEquals(INGEN, utbetalingsdag.økonomi.inspektør.personbeløp)
        }
    }

    internal fun assertInfo(forventet: String, vararg filtre: AktivitetsloggFilter) {
        val info = collectInfo(*filtre)
        assertTrue(info.any { it == forventet }, "fant ikke ett tilfelle av info. Info:\n${info.joinToString("\n")}")
    }

    internal fun assertIngenInfo(forventet: String, vararg filtre: AktivitetsloggFilter) {
        val info = collectInfo(*filtre)
        assertEquals(0, info.count { it == forventet }, "fant uventet info. Info:\n${info.joinToString("\n")}")
    }

    internal fun assertIngenInfoSomInneholder(forventet: String, vararg filtre: AktivitetsloggFilter) {
        val info = collectInfo(*filtre)
        assertEquals(0, info.count { it.contains(forventet) }, "fant uventet info. Info:\n${info.joinToString("\n")}")
    }

    internal fun assertIngenVarsler(vararg filtre: AktivitetsloggFilter) {
        val warnings = collectVarsler(*filtre)
        assertTrue(warnings.isEmpty(), "Forventet ingen warnings. Warnings:\n${warnings.joinToString("\n")}")
    }

    internal fun assertVarsler(vararg filtre: AktivitetsloggFilter) {
        assertTrue(collectVarsler(*filtre).isNotEmpty(), "Forventet warnings, fant ingen")
    }

    internal fun assertVarsel(warning: String, vararg filtre: AktivitetsloggFilter) {
        val warnings = collectVarsler(*filtre)
        assertTrue(warnings.contains(warning), "\nFant ikke forventet warning:\n\t$warning\nWarnings funnet:\n\t${warnings.joinToString("\n\t")}\n")
    }

    internal fun assertVarsel(kode: Varselkode, vararg filtre: AktivitetsloggFilter) {
        val varselkoder = collectVarselkoder(*filtre)
        assertTrue(varselkoder.contains(kode), "\nFant ikke forventet varselkode:\n\t$kode\nVarselkoder funnet:\n\t${varselkoder.joinToString("\n\t")}\n")
    }

    internal fun assertIngenVarsel(warning: String, vararg filtre: AktivitetsloggFilter) {
        val warnings = collectVarsler(*filtre)
        assertFalse(warnings.contains(warning), "\nFant et varsel vi ikke forventet:\n\t$warning\nWarnings funnet:\n\t${warnings.joinToString("\n\t")}\n")
    }
    internal fun assertIngenVarsel(warning: Varselkode, vararg filtre: AktivitetsloggFilter) {
        val varselkoder = collectVarselkoder(*filtre)
        assertTrue(warning !in varselkoder, "\nFant et varsel vi ikke forventet:\n\t$warning\nWarnings funnet:\n\t${varselkoder.joinToString("\n\t")}\n")
    }

    internal fun assertFunksjonellFeil(error: String, vararg filtre: AktivitetsloggFilter) {
        val errors = collectFunksjonelleFeil(*filtre)
        assertTrue(errors.contains(error), "fant ikke forventet error. Errors:\n${errors.joinToString("\n")}")
    }

    private fun funksjonelleFeilFørOgEtter(block: () -> Unit): Pair<Map<String,Int>, Map<String, Int>>{
        val funksjonelleFeilFør = collectFunksjonelleFeil().groupBy { it }.mapValues { it.value.size }
        block()
        val funksjonelleFeilEtter = collectFunksjonelleFeil().groupBy { it }.mapValues { it.value.size }
        return funksjonelleFeilFør to funksjonelleFeilEtter
    }
    internal fun ingenNyeFunksjonelleFeil(block: () -> Unit) {
        val (før, etter) = funksjonelleFeilFørOgEtter(block)
        assertEquals(før, etter) { "Det er tilkommet nye funksjonelle feil, eller fler tilfeller av funksjonelle feil!" }
    }
    internal fun nyeFunksjonelleFeil(block: () -> Unit): Boolean {
        val (før, etter) = funksjonelleFeilFørOgEtter(block)
        return før != etter
    }

    internal fun assertFunksjonelleFeil(vararg filtre: AktivitetsloggFilter) {
        val errors = collectFunksjonelleFeil(*filtre)
        assertTrue(errors.isNotEmpty(), "forventet errors, fant ingen.")
    }


    internal fun assertIngenFunksjonelleFeil(vararg filtre: AktivitetsloggFilter) {
        val errors = collectFunksjonelleFeil(*filtre)
        assertTrue(errors.isEmpty(), "forventet ingen errors. Errors: \n${errors.joinToString("\n")}")
    }

    internal fun assertLogiskFeil(severe: String, vararg filtre: AktivitetsloggFilter) {
        val severes = collectLogiskeFeil(*filtre)
        assertTrue(severes.contains(severe), "fant ikke forventet severe. Severes:\n${severes.joinToString("\n")}")
    }

    private fun collectInfo(vararg filtre: AktivitetsloggFilter): MutableList<String> {
        val info = mutableListOf<String>()
        personInspektør.aktivitetslogg.accept(object : AktivitetsloggVisitor {
            override fun visitInfo(id: UUID, kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitet.Info, melding: String, tidsstempel: String) {
                if (filtre.all { filter -> kontekster.any { filter.filtrer(it) } }) {
                    info.add(melding)
                }
            }
        })
        return info
    }

    private fun collectVarsler(vararg filtre: AktivitetsloggFilter): MutableList<String> {
        val warnings = mutableListOf<String>()
        personInspektør.aktivitetslogg.accept(object : AktivitetsloggVisitor {
            override fun visitVarsel(id: UUID, kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitet.Varsel, kode: Varselkode?, melding: String, tidsstempel: String) {
                if (filtre.all { filter -> kontekster.any { filter.filtrer(it) } }) {
                    warnings.add(melding)
                }
            }
        })
        return warnings
    }

    private fun collectVarselkoder(vararg filtre: AktivitetsloggFilter): MutableList<Varselkode> {
        val warnings = mutableListOf<Varselkode>()
        personInspektør.aktivitetslogg.accept(object : AktivitetsloggVisitor {
            override fun visitVarsel(id: UUID, kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitet.Varsel, kode: Varselkode?, melding: String, tidsstempel: String) {
                if (filtre.all { filter -> kontekster.any { filter.filtrer(it) } } && kode != null) {
                    warnings.add(kode)
                }
            }
        })
        return warnings
    }

    private fun collectFunksjonelleFeil(vararg filtre: AktivitetsloggFilter): MutableList<String> {
        val errors = mutableListOf<String>()
        personInspektør.aktivitetslogg.accept(object : AktivitetsloggVisitor {
            override fun visitFunksjonellFeil(id: UUID, kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitet.FunksjonellFeil, melding: String, tidsstempel: String) {
                if (filtre.all { filter -> kontekster.any { filter.filtrer(it) } }) {
                    errors.add(melding)
                }
            }
        })
        return errors
    }

    private fun collectLogiskeFeil(vararg filtre: AktivitetsloggFilter): MutableList<String> {
        val severes = mutableListOf<String>()
        personInspektør.aktivitetslogg.accept(object : AktivitetsloggVisitor {
            override fun visitLogiskFeil(id: UUID, kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitet.LogiskFeil, melding: String, tidsstempel: String) {
                if (filtre.all { filter -> kontekster.any { filter.filtrer(it) } }) {
                    severes.add(melding)
                }
            }
        })
        return severes
    }
}