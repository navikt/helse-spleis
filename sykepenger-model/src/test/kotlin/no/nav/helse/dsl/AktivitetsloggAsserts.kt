package no.nav.helse.dsl

import java.util.UUID
import no.nav.helse.etterspurteBehov
import no.nav.helse.hentFeltFraBehov
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.AktivitetsloggFilter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

internal class AktivitetsloggAsserts(
    private val aktivitetslogg: Aktivitetslogg,
    private val assertetVarsler: Varslersamler.AssertetVarsler
) {
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

    internal fun assertVarsler(varsler: Collection<Varselkode>, filter: AktivitetsloggFilter) {
        val actualVarsler = collectVarselkoder(filter)
        val result = varsler.filterNot { it in actualVarsler }
        val ikkeAssertet = actualVarsler.filterNot { it in varsler }

        assertTrue(result.isEmpty()) {
            "\nFant ikke forventet warning:\n\t${result.joinToString(separator = "\n\t")}\nWarnings funnet:\n\t${actualVarsler.joinToString("\n\t")}\n"
        }
        assertTrue(ikkeAssertet.isEmpty()) {
            "\nIkke alle varsler er assertet:\n\t${ikkeAssertet.joinToString(separator = "\n\t")}\n"
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

    internal fun assertIngenFunksjonellFeil(kode: Varselkode, filter: AktivitetsloggFilter) {
        val varselkoder = collectFunksjonellFeilkoder(filter)
        assertTrue(kode !in varselkoder, "\nFant en funksjonell feil vi ikke forventet:\n\t$kode\nFunksjonelle feil funnet:\n\t${varselkoder.joinToString("\n\t")}\n")
    }

    internal fun assertFunksjonellFeil(error: String, filter: AktivitetsloggFilter) {
        val errors = collectFunksjonelleFeil(filter)
        assertTrue(errors.contains(error), "fant ikke forventet error. Errors:\n${errors.joinToString("\n")}")
    }

    internal fun assertFunksjonellFeil(varselkode: Varselkode, filter: AktivitetsloggFilter) {
        val errors = collectFunksjonelleFeil(filter)
        assertTrue(errors.contains(varselkode.varseltekst), "fant ikke forventet error. Errors:\n${errors.joinToString("\n")}")
    }

    internal fun assertIngenBehov(vedtaksperiode: UUID, behovtype: Aktivitet.Behov.Behovtype) {
        assertTrue(aktivitetslogg.etterspurteBehov(vedtaksperiode).none { it.type == behovtype })
    }

    internal fun assertBehov(vedtaksperiode: UUID, behovtype: Aktivitet.Behov.Behovtype) {
        assertTrue(aktivitetslogg.etterspurteBehov(vedtaksperiode).any { it.type == behovtype })
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

    internal fun assertHarTag(vedtaksperiode: UUID, forventetTag: String) {
        val tags = aktivitetslogg.hentFeltFraBehov<Set<String>>(
            vedtaksperiodeId = vedtaksperiode,
            behov = Aktivitet.Behov.Behovtype.Godkjenning,
            felt = "tags"
        )
        assertTrue(tags?.contains(forventetTag) ?: false, "Fant ikke forventet tag: $forventetTag. Faktiske tags: $tags ")
    }

    internal fun assertHarIkkeTag(vedtaksperiode: UUID, ikkeForventetTag: String) {
        val tags = aktivitetslogg.hentFeltFraBehov<Set<String>>(
            vedtaksperiodeId = vedtaksperiode,
            behov = Aktivitet.Behov.Behovtype.Godkjenning,
            felt = "tags"
        )
        assertFalse(tags?.contains(ikkeForventetTag) ?: true, "Fant tag vi ikke forventet: $ikkeForventetTag. Faktiske tags: $tags ")
    }

    private fun <A : Aktivitet> List<A>.collect(filter: AktivitetsloggFilter) =
        this.filter { it.kontekster.isEmpty() || it.kontekster.any { filter.filtrer(it) } }

    private fun List<Aktivitet>.collectStrings(filter: AktivitetsloggFilter) =
        collect(filter).map { it.melding }

    private fun collectInfo(filter: AktivitetsloggFilter): List<String> {
        return aktivitetslogg.info.collectStrings(filter)
    }

    private fun collectVarsler(filter: AktivitetsloggFilter): List<String> {
        return aktivitetslogg.varsel.collectStrings(filter)
    }

    private fun collectVarselkoder(filter: AktivitetsloggFilter): List<Varselkode> {
        return aktivitetslogg.varsel.collect(filter).map { it.kode }
    }

    private fun collectFunksjonellFeilkoder(filter: AktivitetsloggFilter): List<Varselkode> {
        return aktivitetslogg.varsel.collect(filter).map { it.kode }
    }

    private fun collectFunksjonelleFeil(filter: AktivitetsloggFilter): List<String> {
        return aktivitetslogg.funksjonellFeil.collectStrings(filter)
    }

    private fun collectLogiskeFeil(filter: AktivitetsloggFilter): List<String> {
        return aktivitetslogg.logiskFeil.collectStrings(filter)
    }
}
