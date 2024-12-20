package no.nav.helse.dsl

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.erHelg
import no.nav.helse.etterspurteBehov
import no.nav.helse.hendelser.Periode
import no.nav.helse.inspectors.PersonInspektør
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.TilstandType
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.AktivitetsloggFilter
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.spleis.e2e.TestObservatør
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

internal class TestArbeidsgiverAssertions(
    private val observatør: TestObservatør,
    private val inspektør: TestArbeidsgiverInspektør,
    private val personInspektør: PersonInspektør,
    private val assertetVarsler: Varslersamler.AssertetVarsler
) {
    internal fun assertSisteTilstand(vedtaksperiodeId: UUID, tilstand: TilstandType, errortekst: (() -> String)? = null) {
        assertEquals(tilstand, observatør.tilstandsendringer[vedtaksperiodeId]?.last(), errortekst)
    }

    internal fun assertSisteForkastetTilstand(vedtaksperiodeId: UUID, tilstand: TilstandType, errortekst: (() -> String)? = null) {
        assertTrue(inspektør.periodeErForkastet(vedtaksperiodeId)) { "Perioden er ikke forkastet" }
        assertFalse(inspektør.periodeErIkkeForkastet(vedtaksperiodeId)) { "Perioden er ikke forkastet" }
        assertEquals(tilstand, observatør.tilstandsendringer[vedtaksperiodeId]?.last(), errortekst)
    }

    internal fun assertTilstander(id: UUID, vararg tilstander: TilstandType) {
        assertFalse(inspektør.periodeErForkastet(id)) { "Perioden er forkastet med tilstander: ${observatør.tilstandsendringer[id]}:\n${personInspektør.aktivitetslogg}" }
        assertTrue(inspektør.periodeErIkkeForkastet(id)) { "Perioden er forkastet med tilstander: ${observatør.tilstandsendringer[id]}\n${personInspektør.aktivitetslogg}" }
        assertEquals(tilstander.asList(), observatør.tilstandsendringer[id])
    }

    internal fun assertForkastetPeriodeTilstander(id: UUID, vararg tilstander: TilstandType, varselkode: Varselkode?) {
        assertTrue(inspektør.periodeErForkastet(id)) { "Perioden er ikke forkastet" }
        assertFalse(inspektør.periodeErIkkeForkastet(id)) { "Perioden er ikke forkastet" }
        assertEquals(tilstander.asList(), observatør.tilstandsendringer[id])
        varselkode?.let {
            assertFunksjonellFeil(varselkode, id.filter())
        }
    }

    internal fun assertSkjæringstidspunkt(vararg skjæringstidspunkt: LocalDate) {
        assertEquals(skjæringstidspunkt.toSet(), personInspektør.vilkårsgrunnlagHistorikk.aktiveSpleisSkjæringstidspunkt)
    }

    internal fun assertAntallOpptjeningsdager(forventet: Int, skjæringstidspunkt: LocalDate) {
        val opptjening = personInspektør.vilkårsgrunnlagHistorikk.grunnlagsdata(skjæringstidspunkt).opptjening!!
        assertEquals(forventet, opptjening.opptjeningsdager)
    }

    internal fun assertErOppfylt(skjæringstidspunkt: LocalDate) {
        val opptjening = personInspektør.vilkårsgrunnlagHistorikk.grunnlagsdata(skjæringstidspunkt).opptjening!!
        assertTrue(opptjening.erOppfylt)
    }

    internal fun assertErIkkeOppfylt(skjæringstidspunkt: LocalDate) {
        val opptjening = personInspektør.vilkårsgrunnlagHistorikk.grunnlagsdata(skjæringstidspunkt).opptjening!!
        assertFalse(opptjening.erOppfylt)
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
        forventetPersonbeløp: Int = 0,
        subset: Periode? = null
    ) {
        val utbetalingstidslinje = inspektør.utbetalingstidslinjer(vedtaksperiodeId).let { subset?.let(it::subset) ?: it }

        utbetalingstidslinje.filterNot { it.dato.erHelg() }.forEach { utbetalingsdag ->
            assertEquals(forventetArbeidsgiverbeløp.daglig, utbetalingsdag.økonomi.inspektør.arbeidsgiverbeløp) { "feil arbeidsgiverbeløp for dag ${utbetalingsdag.dato} " }
            assertEquals(forventetArbeidsgiverRefusjonsbeløp.daglig, utbetalingsdag.økonomi.inspektør.arbeidsgiverRefusjonsbeløp.rundTilDaglig())
            assertEquals(forventetPersonbeløp.daglig, utbetalingsdag.økonomi.inspektør.personbeløp)
        }
    }

    internal fun assertInfo(forventet: String, filter: AktivitetsloggFilter) {
        val info = collectInfo(filter)
        assertTrue(info.any { it == forventet }, "fant ikke ett tilfelle av info. Info:\n${info.joinToString("\n")}")
    }

    internal fun assertIngenInfo(forventet: String, filter: AktivitetsloggFilter) {
        val info = collectInfo(filter)
        assertEquals(0, info.count { it == forventet }, "fant uventet info. Info:\n${info.joinToString("\n")}")
    }

    internal fun assertIngenInfoSomInneholder(forventet: String, filter: AktivitetsloggFilter) {
        val info = collectInfo(filter)
        assertEquals(0, info.count { it.contains(forventet) }, "fant uventet info. Info:\n${info.joinToString("\n")}")
    }

    internal fun assertIngenVarsler(filter: AktivitetsloggFilter) {
        val warnings = collectVarselkoder(filter)
        assertTrue(warnings.isEmpty(), "Forventet ingen warnings. Warnings:\n${warnings.joinToString("\n")}")
    }

    internal fun assertVarsler(varsler: Collection<Varselkode>, filter: AktivitetsloggFilter) {
        val actualVarsler = collectVarselkoder(filter)
        val result = varsler.filterNot { it in actualVarsler }
        assertTrue(result.isEmpty()) {
            "\nFant ikke forventet warning:\n\t${result.joinToString(separator = "\n\t")}\nWarnings funnet:\n\t${actualVarsler.joinToString("\n\t")}\n"
        }
        assertetVarsler.kvitterVarsel(filter, varsler)
    }

    internal fun assertVarsel(warning: String, filter: AktivitetsloggFilter) {
        val warnings = collectVarsler(filter)
        assertTrue(warnings.contains(warning), "\nFant ikke forventet warning:\n\t$warning\nWarnings funnet:\n\t${warnings.joinToString("\n\t")}\n")
    }

    internal fun assertVarsel(kode: Varselkode, filter: AktivitetsloggFilter) {
        val varselkoder = collectVarselkoder(filter)
        assertTrue(varselkoder.contains(kode), "\nFant ikke forventet varselkode:\n\t$kode\nVarselkoder funnet:\n\t${varselkoder.joinToString("\n\t")}\n")

        assertetVarsler.kvitterVarsel(filter, kode)
    }

    internal fun assertIngenVarsel(warning: String, filter: AktivitetsloggFilter) {
        val warnings = collectVarsler(filter)
        assertFalse(warnings.contains(warning), "\nFant et varsel vi ikke forventet:\n\t$warning\nWarnings funnet:\n\t${warnings.joinToString("\n\t")}\n")
    }

    internal fun assertIngenVarsel(warning: Varselkode, filter: AktivitetsloggFilter) {
        val varselkoder = collectVarselkoder(filter)
        assertTrue(warning !in varselkoder, "\nFant et varsel vi ikke forventet:\n\t$warning\nWarnings funnet:\n\t${varselkoder.joinToString("\n\t")}\n")
    }

    internal fun assertFunksjonellFeil(error: String, filter: AktivitetsloggFilter) {
        val errors = collectFunksjonelleFeil(filter)
        assertTrue(errors.contains(error), "fant ikke forventet error. Errors:\n${errors.joinToString("\n")}")
    }

    internal fun assertFunksjonellFeil(varselkode: Varselkode, filter: AktivitetsloggFilter) {
        val errors = collectFunksjonelleFeil(filter)
        assertTrue(errors.contains(varselkode.funksjonellFeilTekst), "fant ikke forventet error. Errors:\n${errors.joinToString("\n")}")
    }

    internal fun assertIngenBehov(vedtaksperiode: UUID, behovtype: Aktivitet.Behov.Behovtype) {
        assertTrue(personInspektør.aktivitetslogg.etterspurteBehov(vedtaksperiode).none { it.type == behovtype })
    }

    internal fun assertBehov(vedtaksperiode: UUID, behovtype: Aktivitet.Behov.Behovtype) {
        assertTrue(personInspektør.aktivitetslogg.etterspurteBehov(vedtaksperiode).any { it.type == behovtype })
    }

    private fun funksjonelleFeilFørOgEtter(block: () -> Unit): Pair<Map<String, Int>, Map<String, Int>> {
        val funksjonelleFeilFør = collectFunksjonelleFeil(AktivitetsloggFilter.Alle).groupBy { it }.mapValues { it.value.size }
        block()
        val funksjonelleFeilEtter = collectFunksjonelleFeil(AktivitetsloggFilter.Alle).groupBy { it }.mapValues { it.value.size }
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

    internal fun assertFunksjonelleFeil(filter: AktivitetsloggFilter) {
        val errors = collectFunksjonelleFeil(filter)
        assertTrue(errors.isNotEmpty(), "forventet errors, fant ingen.")
    }

    internal fun assertIngenFunksjonelleFeil(filter: AktivitetsloggFilter) {
        val errors = collectFunksjonelleFeil(filter)
        assertTrue(errors.isEmpty(), "forventet ingen errors. Errors: \n${errors.joinToString("\n")}")
    }

    internal fun assertLogiskFeil(severe: String, filter: AktivitetsloggFilter) {
        val severes = collectLogiskeFeil(filter)
        assertTrue(severes.contains(severe), "fant ikke forventet severe. Severes:\n${severes.joinToString("\n")}")
    }

    private fun collectInfo(filter: AktivitetsloggFilter): List<String> {
        return personInspektør.aktivitetslogg.info
            .filter { it.kontekster.any { filter.filtrer(it) } }
            .map { it.melding }
    }

    private fun collectVarsler(filter: AktivitetsloggFilter): List<String> {
        return personInspektør.aktivitetslogg.varsel
            .filter { it.kontekster.any { filter.filtrer(it) } }
            .map { it.melding }
    }

    private fun collectVarselkoder(filter: AktivitetsloggFilter): List<Varselkode> {
        return personInspektør.aktivitetslogg.varsel
            .filter { it.kontekster.any { filter.filtrer(it) } }
            .map { it.kode }
    }

    private fun collectFunksjonelleFeil(filter: AktivitetsloggFilter): List<String> {
        return personInspektør.aktivitetslogg.funksjonellFeil
            .filter { it.kontekster.any { filter.filtrer(it) } }
            .map { it.melding }
    }

    private fun collectLogiskeFeil(filter: AktivitetsloggFilter): List<String> {
        return personInspektør.aktivitetslogg.logiskFeil
            .filter { it.kontekster.any { filter.filtrer(it) } }
            .map { it.melding }
    }
}
