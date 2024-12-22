package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.util.UUID
import kotlin.reflect.KClass
import no.nav.helse.Personidentifikator
import no.nav.helse.erHelg
import no.nav.helse.etterspurtBehov
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Periode
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.inspectors.personLogg
import no.nav.helse.person.AbstractPersonTest
import no.nav.helse.person.AbstractPersonTest.Companion.ORGNUMMER
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.Person
import no.nav.helse.person.TilstandType
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

internal fun assertInntektshistorikkForDato(forventetInntekt: Inntekt?, dato: LocalDate, inspektør: TestArbeidsgiverInspektør) {
    assertEquals(forventetInntekt, inspektør.inntektInspektør.omregnetÅrsinntekt(dato)?.sykepengegrunnlag)
}

internal fun assertInntektForDato(forventetInntekt: Inntekt?, dato: LocalDate, inspektør: TestArbeidsgiverInspektør) {
    val grunnlagsdataInspektør = inspektør.vilkårsgrunnlaghistorikk().grunnlagsdata(dato).inspektør
    val sykepengegrunnlagInspektør = grunnlagsdataInspektør.inntektsgrunnlag.inspektør
    sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver[inspektør.orgnummer]?.inspektør.also {
        assertEquals(forventetInntekt, it?.inntektsopplysning?.inspektør?.beløp)
    }
}

internal fun AbstractEndToEndTest.erEtterspurt(type: Aktivitet.Behov.Behovtype, vedtaksperiodeIdInnhenter: IdInnhenter, orgnummer: String, tilstand: TilstandType): Boolean {
    return EtterspurtBehov.finnEtterspurtBehov(ikkeBesvarteBehov, type, vedtaksperiodeIdInnhenter, orgnummer, tilstand) != null
}

internal fun AbstractEndToEndTest.assertEtterspurt(løsning: KClass<out Hendelse>, type: Aktivitet.Behov.Behovtype, vedtaksperiodeIdInnhenter: IdInnhenter, orgnummer: String) {
    val etterspurtBehov = EtterspurtBehov.finnEtterspurtBehov(ikkeBesvarteBehov, type, vedtaksperiodeIdInnhenter, orgnummer)
    assertTrue(ikkeBesvarteBehov.remove(etterspurtBehov)) {
        "Forventer at $type skal være etterspurt før ${løsning.simpleName} håndteres. Perioden er i ${
            observatør.tilstandsendringer[vedtaksperiodeIdInnhenter.id(orgnummer)]?.last()
        }."
    }
}
/*

internal fun <T : PersonHendelse> AbstractEndToEndTest.assertEtterspurt(løsning: KClass<T>, type: Aktivitet.Behov.Behovtype) {
    val etterspurtBehov = EtterspurtBehov.finnEtterspurtBehov(ikkeBesvarteBehov, type, vedtaksperiodeIdInnhenter, orgnummer)
    assertTrue(ikkeBesvarteBehov.remove(etterspurtBehov)) {
        "Forventer at $type skal være etterspurt før ${løsning.simpleName} håndteres. Perioden er i ${
            observatør.tilstandsendringer[vedtaksperiodeIdInnhenter.id(orgnummer)]?.last()
        }.\nAktivitetsloggen:\n${person.personLogg}"
    }
}
*/

internal fun AbstractEndToEndTest.assertIkkeEtterspurt(løsning: KClass<out Hendelse>, type: Aktivitet.Behov.Behovtype, vedtaksperiodeIdInnhenter: IdInnhenter, orgnummer: String) {
    val etterspurtBehov = EtterspurtBehov.finnEtterspurtBehov(ikkeBesvarteBehov, type, vedtaksperiodeIdInnhenter, orgnummer)
    assertFalse(etterspurtBehov in ikkeBesvarteBehov) {
        "Forventer ikke at $type skal være etterspurt før ${løsning.simpleName} håndteres. Perioden er i ${
            observatør.tilstandsendringer[vedtaksperiodeIdInnhenter.id(orgnummer)]?.last()
        }"
    }
}

internal inline fun <reified R : Utbetalingsdag> assertUtbetalingsdag(dag: Utbetalingsdag, expectedDagtype: KClass<R>, expectedTotalgrad: Int = 100) {
    dag.let {
        assertEquals(it::class, expectedDagtype)
        it.økonomi.brukTotalGrad { totalGrad -> assertEquals(expectedTotalgrad, totalGrad) }
    }
}

internal fun AbstractEndToEndTest.assertUtbetalingsbeløp(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    forventetArbeidsgiverbeløp: Int,
    forventetArbeidsgiverRefusjonsbeløp: Int,
    subset: Periode? = null,
    orgnummer: String = ORGNUMMER
) {
    val utbetalingstidslinje = inspektør(orgnummer).utbetalingstidslinjer(vedtaksperiodeIdInnhenter).let { subset?.let(it::subset) ?: it }

    utbetalingstidslinje.filterNot { it.dato.erHelg() }.forEach { utbetalingsdag ->
        assertEquals(forventetArbeidsgiverbeløp.daglig, utbetalingsdag.økonomi.inspektør.arbeidsgiverbeløp) { "feil arbeidsgiverbeløp for dag ${utbetalingsdag.dato} " }
        assertEquals(forventetArbeidsgiverRefusjonsbeløp.daglig, utbetalingsdag.økonomi.inspektør.arbeidsgiverRefusjonsbeløp.rundTilDaglig())
        assertEquals(INGEN, utbetalingsdag.økonomi.inspektør.personbeløp)
    }
}

internal fun AbstractEndToEndTest.assertHarHendelseIder(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    vararg hendelseIder: UUID,
    orgnummer: String = ORGNUMMER
) {
    assertTrue(
        inspektør(orgnummer).hendelseIder(vedtaksperiodeIdInnhenter).containsAll(hendelseIder.toList())
    ) { "Perioden inneholder ikke alle hendelseidene" }
}

internal fun AbstractEndToEndTest.assertHarIkkeHendelseIder(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    vararg hendelseIder: UUID,
    orgnummer: String = ORGNUMMER
) {
    assertTrue(
        inspektør(orgnummer).hendelseIder(vedtaksperiodeIdInnhenter).none { it in hendelseIder.toList() }) { "Perioden inneholder ikke alle hendelseidene" }
}

internal fun AbstractEndToEndTest.assertTilstand(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    tilstand: TilstandType,
    orgnummer: String = ORGNUMMER,
) {
    val sisteTilstand = inspektør(orgnummer).sisteTilstand(vedtaksperiodeIdInnhenter)
    assertEquals(tilstand, sisteTilstand) {
        "Forventet at perioden skal stå i tilstand $tilstand, mens den står faktisk i $sisteTilstand\n${person.personLogg}"
    }
}

internal fun AbstractEndToEndTest.assertSisteTilstand(vedtaksperiodeIdInnhenter: IdInnhenter, tilstand: TilstandType, orgnummer: String = ORGNUMMER, errortekst: (() -> String)? = null) {
    assertEquals(tilstand, observatør.tilstandsendringer[vedtaksperiodeIdInnhenter.id(orgnummer)]?.last(), errortekst)
}

internal fun AbstractEndToEndTest.assertTilstander(indeks: Int, vararg tilstander: TilstandType, orgnummer: String = ORGNUMMER) {
    assertTilstander(vedtaksperiodeIdInnhenter = (indeks + 1).vedtaksperiode, tilstander = tilstander, orgnummer = orgnummer)
}

internal fun AbstractEndToEndTest.assertTilstander(vedtaksperiodeIdInnhenter: IdInnhenter, vararg tilstander: TilstandType, orgnummer: String = ORGNUMMER, inspektør: TestArbeidsgiverInspektør = inspektør(orgnummer), message: String? = null) {
    val id = vedtaksperiodeIdInnhenter.id(orgnummer)
    assertFalse(inspektør.periodeErForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er forkastet med tilstander: ${observatør.tilstandsendringer[id]}:\n${person.personLogg}" }
    assertTrue(inspektør.periodeErIkkeForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er forkastet med tilstander: ${observatør.tilstandsendringer[id]}\n${person.personLogg}" }
    assertEquals(tilstander.asList(), observatør.tilstandsendringer[id], message)
}

internal fun AbstractEndToEndTest.assertForkastetPeriodeTilstander(indeks: Int, vararg tilstander: TilstandType, orgnummer: String = ORGNUMMER) {
    assertForkastetPeriodeTilstander(vedtaksperiodeIdInnhenter = indeks.vedtaksperiode, tilstander = tilstander, orgnummer = orgnummer)
}

internal fun AbstractEndToEndTest.assertForkastetPeriodeTilstander(vedtaksperiodeIdInnhenter: IdInnhenter, vararg tilstander: TilstandType, orgnummer: String = ORGNUMMER, inspektør: TestArbeidsgiverInspektør = inspektør(orgnummer), varselkode: Varselkode? = null) {
    assertTrue(inspektør.periodeErForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
    assertFalse(inspektør.periodeErIkkeForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
    assertEquals(tilstander.asList(), observatør.tilstandsendringer[vedtaksperiodeIdInnhenter.id(orgnummer)])
    varselkode?.let { assertFunksjonellFeil(it, vedtaksperiodeIdInnhenter.filter()) }
}

internal fun AbstractEndToEndTest.assertForkastetPeriodeTilstander(orgnummer: String, vedtaksperiodeIdInnhenter: IdInnhenter, vararg tilstander: TilstandType) {
    assertTrue(inspektør(orgnummer).periodeErForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
    assertFalse(inspektør(orgnummer).periodeErIkkeForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
    assertEquals(tilstander.asList(), observatør.tilstandsendringer[vedtaksperiodeIdInnhenter.id(orgnummer)])
}

internal fun AbstractEndToEndTest.assertSisteForkastetPeriodeTilstand(orgnummer: String, vedtaksperiodeIdInnhenter: IdInnhenter, tilstand: TilstandType) {
    assertEquals(tilstand, observatør.tilstandsendringer[vedtaksperiodeIdInnhenter.id(orgnummer)]?.last())
    assertTrue(inspektør(orgnummer).periodeErForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
    assertFalse(inspektør(orgnummer).periodeErIkkeForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
}

internal fun AbstractPersonTest.assertInfo(forventet: String, filter: AktivitetsloggFilter = AktivitetsloggFilter.Alle) = person.personLogg.assertInfo(forventet, filter)
internal fun AbstractPersonTest.assertIngenInfo(forventet: String, filter: AktivitetsloggFilter = AktivitetsloggFilter.Alle) = person.personLogg.assertIngenInfo(forventet, filter)
internal fun AbstractPersonTest.assertIngenVarsler(filter: AktivitetsloggFilter = AktivitetsloggFilter.Alle) = person.personLogg.assertIngenVarsler(filter)
internal fun AbstractPersonTest.assertVarsler(filter: AktivitetsloggFilter = AktivitetsloggFilter.Alle) = person.personLogg.assertVarsler(filter)
internal fun AbstractPersonTest.assertVarsel(varsel: Varselkode, filter: AktivitetsloggFilter) = person.personLogg.assertVarsel(varsel, filter)
internal fun AbstractPersonTest.assertIngenVarsel(varselkode: Varselkode, filter: AktivitetsloggFilter = AktivitetsloggFilter.Alle) = person.personLogg.assertIngenVarsel(varselkode, filter)
internal fun AbstractPersonTest.assertFunksjonellFeil(varselkode: Varselkode, filter: AktivitetsloggFilter = AktivitetsloggFilter.Alle) = person.personLogg.assertFunksjonellFeil(varselkode, filter)
internal fun AbstractPersonTest.assertFunksjonelleFeil(filter: AktivitetsloggFilter) = person.personLogg.assertFunksjonelleFeil(filter)
internal fun AbstractPersonTest.assertIngenFunksjonellFeil(varselkode: Varselkode, filter: AktivitetsloggFilter = AktivitetsloggFilter.Alle) = person.personLogg.assertIngenFunksjonellFeil(varselkode, filter)

internal fun AbstractPersonTest.assertIngenFunksjonelleFeil(filter: AktivitetsloggFilter = AktivitetsloggFilter.Alle) = person.personLogg.assertIngenFunksjonelleFeil(filter)
internal fun AbstractPersonTest.assertLogiskFeil(severe: String, filter: AktivitetsloggFilter) = person.personLogg.assertLogiskFeil(severe, filter)

internal fun Aktivitetslogg.assertInfo(forventet: String, filter: AktivitetsloggFilter = AktivitetsloggFilter.Alle) {
    val info = collectInfo(filter)
    assertTrue(info.contains(forventet), "fant ikke ett tilfelle av infomelding <$forventet>. Info:\n${info.joinToString("\n")}")
}

internal fun Aktivitetslogg.assertIngenInfo(forventet: String, filter: AktivitetsloggFilter) {
    val info = collectInfo(filter)
    assertEquals(0, info.count { it == forventet }, "fant uventet info. Info:\n${info.joinToString("\n")}")
}

internal fun Aktivitetslogg.assertIngenVarsler(filter: AktivitetsloggFilter = AktivitetsloggFilter.Alle) {
    val warnings = collectVarselkoder(filter)
    assertTrue(warnings.isEmpty(), "Forventet ingen warnings. Warnings:\n${warnings.joinToString("\n")}")
}

internal fun Aktivitetslogg.assertVarsler(filter: AktivitetsloggFilter) {
    assertTrue(collectVarselkoder(filter).isNotEmpty(), "Forventet warnings, fant ingen")
}

internal fun Aktivitetslogg.assertVarsel(varsel: Varselkode, filter: AktivitetsloggFilter = AktivitetsloggFilter.Alle) {
    val varselkoder = collectVarselkoder(filter)
    assertTrue(varselkoder.contains(varsel), "\nFant ikke forventet varselkode:\n\t$varsel\nVarselkoder funnet:\n\t${varselkoder.joinToString("\n\t")}\n")
}

internal fun Aktivitetslogg.assertIngenVarsel(varselkode: Varselkode, filter: AktivitetsloggFilter = AktivitetsloggFilter.Alle) {
    val varselkoder = collectVarselkoder(filter)
    assertFalse(varselkoder.contains(varselkode), "\nFant en varselkode vi ikke forventet:\n\t$varselkode\nVarselkoder funnet:\n\t${varselkoder.joinToString("\n\t")}\n")
}

internal fun Aktivitetslogg.assertHarTag(vedtaksperiode: IdInnhenter, orgnummer: String = ORGNUMMER, forventetTag: String) {
    val tags = this.etterspurtBehov<Set<String>>(
        vedtaksperiodeId = vedtaksperiode.id(orgnummer),
        behov = Aktivitet.Behov.Behovtype.Godkjenning,
        felt = "tags"
    )
    assertTrue(tags?.contains(forventetTag) ?: false, "Fant ikke forventet tag: $forventetTag. Faktiske tags: $tags ")
}

internal fun Aktivitetslogg.assertFunksjonelleFeil(filter: AktivitetsloggFilter) {
    val errors = collectFunksjonelleFeil(filter)
    assertTrue(errors.isNotEmpty(), "forventet errors, fant ingen.")
}

internal fun Aktivitetslogg.assertFunksjonellFeil(varselkode: Varselkode, filter: AktivitetsloggFilter = AktivitetsloggFilter.Alle) {
    val errors = collectFunksjonelleFeil(filter)
    assertTrue(errors.contains(varselkode.funksjonellFeilTekst), "fant ikke forventet error. Errors:\n${errors.joinToString("\n")}")
}

internal fun Aktivitetslogg.assertIngenFunksjonelleFeil(filter: AktivitetsloggFilter) {
    val errors = collectFunksjonelleFeil(filter)
    assertTrue(errors.isEmpty(), "forventet ingen errors. Errors: \n${errors.joinToString("\n")}")
}

internal fun Aktivitetslogg.assertIngenFunksjonellFeil(varselkode: Varselkode, filter: AktivitetsloggFilter) {
    val errors = collectFunksjonelleFeil(filter)
    assertFalse(errors.contains(varselkode.varseltekst), "\nFant en varselkode vi ikke forventet:\n\t$varselkode\nVarselkoder funnet:\n\t${errors.joinToString("\n\t")}\n")
}

internal fun Aktivitetslogg.assertIngenFunksjonellFeil(filter: AktivitetsloggFilter = AktivitetsloggFilter.Alle) {
    val errors = collectFunksjonelleFeil(filter)
    assertTrue(errors.isEmpty(), "\nVarselkoder funnet:\n\t${errors.joinToString("\n\t")}\n")
}

internal fun Aktivitetslogg.assertLogiskFeil(severe: String, filter: AktivitetsloggFilter) {
    val severes = collectLogiskeFeil(filter)
    assertTrue(severes.contains(severe), "fant ikke forventet severe. Severes:\n${severes.joinToString("\n")}")
}

internal fun Aktivitetslogg.collectInfo(filter: AktivitetsloggFilter): List<String> {
    return info.collectStrings(filter)
}

internal fun Aktivitetslogg.collectVarselkoder(filter: AktivitetsloggFilter): List<Varselkode> {
    return varsel.collectKode(filter)
}

internal fun Aktivitetslogg.collectFunksjonelleFeil(filter: AktivitetsloggFilter): List<String> {
    return funksjonellFeil.collectStrings(filter)
}

internal fun Aktivitetslogg.collectLogiskeFeil(filter: AktivitetsloggFilter): List<String> {
    return logiskFeil.collectStrings(filter)
}

private fun <A : Aktivitet> List<A>.collect(filter: AktivitetsloggFilter): List<A> {
    return this
        .filter { it.kontekster.isEmpty() || it.kontekster.any { filter.filtrer(it) } }
}

private fun List<Aktivitet>.collectStrings(filter: AktivitetsloggFilter): List<String> {
    return collect(filter).map { it.melding }
}

private fun List<Aktivitet.Varsel>.collectKode(filter: AktivitetsloggFilter): List<Varselkode> {
    return collect(filter).map { it.kode }
}

internal fun interface AktivitetsloggFilter {
    companion object {
        internal val Alle = AktivitetsloggFilter { true }
        internal fun UUID.filter() = vedtaksperiode(this)

        internal fun vedtaksperiode(vedtaksperiodeId: UUID): AktivitetsloggFilter = AktivitetsloggFilter { kontekst ->
            kontekst.kontekstType == "Vedtaksperiode" && kontekst.kontekstMap["vedtaksperiodeId"] == vedtaksperiodeId.toString()
        }

        internal fun vedtaksperiode(idInnhenter: IdInnhenter, orgnummer: String): AktivitetsloggFilter = AktivitetsloggFilter { kontekst ->
            kontekst.kontekstType == "Vedtaksperiode" && kontekst.kontekstMap["vedtaksperiodeId"] == idInnhenter.id(orgnummer).toString()
        }

        internal fun person(personidentifikator: Personidentifikator = AbstractPersonTest.UNG_PERSON_FNR_2018): AktivitetsloggFilter = AktivitetsloggFilter { kontekst ->
            kontekst.kontekstMap["fødselsnummer"] == personidentifikator.toString()
        }

        internal fun arbeidsgiver(orgnummer: String): AktivitetsloggFilter = AktivitetsloggFilter { kontekst ->
            kontekst.kontekstType == "Arbeidsgiver" && kontekst.kontekstMap["organisasjonsnummer"] == orgnummer
        }
    }

    fun filtrer(kontekst: SpesifikkKontekst): Boolean
}

internal fun assertActivities(person: Person) {
    assertTrue(person.personLogg.aktiviteter.isNotEmpty(), person.personLogg.toString())
}
