package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.util.UUID
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
import kotlin.reflect.KClass


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
    orgnummer: String = AbstractPersonTest.ORGNUMMER
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
    orgnummer: String = AbstractPersonTest.ORGNUMMER
) {
    assertTrue(
        inspektør(orgnummer).hendelseIder(vedtaksperiodeIdInnhenter).containsAll(hendelseIder.toList())
    ) { "Perioden inneholder ikke alle hendelseidene" }
}

internal fun AbstractEndToEndTest.assertHarIkkeHendelseIder(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    vararg hendelseIder: UUID,
    orgnummer: String = AbstractPersonTest.ORGNUMMER
) {
    assertTrue(
        inspektør(orgnummer).hendelseIder(vedtaksperiodeIdInnhenter).none { it in hendelseIder.toList() }) { "Perioden inneholder ikke alle hendelseidene" }
}

internal fun AbstractEndToEndTest.assertTilstand(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    tilstand: TilstandType,
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
) {
    val sisteTilstand = inspektør(orgnummer).sisteTilstand(vedtaksperiodeIdInnhenter)
    assertEquals(tilstand, sisteTilstand) {
        "Forventet at perioden skal stå i tilstand $tilstand, mens den står faktisk i $sisteTilstand\n${person.personLogg}"
    }
}


internal fun AbstractEndToEndTest.assertSisteTilstand(vedtaksperiodeIdInnhenter: IdInnhenter, tilstand: TilstandType, orgnummer: String = AbstractPersonTest.ORGNUMMER, errortekst: (() -> String)? = null) {
    assertEquals(tilstand, observatør.tilstandsendringer[vedtaksperiodeIdInnhenter.id(orgnummer)]?.last(), errortekst)
}

internal fun AbstractEndToEndTest.assertTilstander(indeks: Int, vararg tilstander: TilstandType, orgnummer: String = AbstractPersonTest.ORGNUMMER) {
    assertTilstander(vedtaksperiodeIdInnhenter = (indeks + 1).vedtaksperiode, tilstander = tilstander, orgnummer = orgnummer)
}

internal fun AbstractEndToEndTest.assertTilstander(vedtaksperiodeIdInnhenter: IdInnhenter, vararg tilstander: TilstandType, orgnummer: String = AbstractPersonTest.ORGNUMMER, inspektør: TestArbeidsgiverInspektør = inspektør(orgnummer), message: String? = null) {
    val id = vedtaksperiodeIdInnhenter.id(orgnummer)
    assertFalse(inspektør.periodeErForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er forkastet med tilstander: ${observatør.tilstandsendringer[id]}:\n${person.personLogg}" }
    assertTrue(inspektør.periodeErIkkeForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er forkastet med tilstander: ${observatør.tilstandsendringer[id]}\n${person.personLogg}" }
    assertEquals(tilstander.asList(), observatør.tilstandsendringer[id], message)
}

internal fun AbstractEndToEndTest.assertForkastetPeriodeTilstander(indeks: Int, vararg tilstander: TilstandType, orgnummer: String = AbstractPersonTest.ORGNUMMER) {
    assertForkastetPeriodeTilstander(vedtaksperiodeIdInnhenter = indeks.vedtaksperiode, tilstander = tilstander, orgnummer = orgnummer)
}

internal fun AbstractEndToEndTest.assertForkastetPeriodeTilstander(vedtaksperiodeIdInnhenter: IdInnhenter, vararg tilstander: TilstandType, orgnummer: String = AbstractPersonTest.ORGNUMMER, inspektør: TestArbeidsgiverInspektør = inspektør(orgnummer), varselkode: Varselkode? = null) {
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

internal fun AbstractPersonTest.assertInfo(forventet: String, vararg filtre: AktivitetsloggFilter) = person.personLogg.assertInfo(forventet, *filtre)
internal fun AbstractPersonTest.assertIngenInfo(forventet: String, vararg filtre: AktivitetsloggFilter) = person.personLogg.assertIngenInfo(forventet, *filtre)
internal fun AbstractPersonTest.assertIngenVarsler(vararg filtre: AktivitetsloggFilter) = person.personLogg.assertIngenVarsler(*filtre)
internal fun AbstractPersonTest.assertVarsler(vararg filtre: AktivitetsloggFilter) = person.personLogg.assertVarsler(*filtre)
internal fun AbstractPersonTest.assertVarsler(warnings: List<String>, vararg filtre: AktivitetsloggFilter) = person.personLogg.assertVarsler(warnings, *filtre)
internal fun AbstractPersonTest.assertVarsel(warning: String, vararg filtre: AktivitetsloggFilter) = person.personLogg.assertVarsel(warning, *filtre)
internal fun AbstractPersonTest.assertVarsel(varsel: Varselkode, vararg filtre: AktivitetsloggFilter) = person.personLogg.assertVarsel(varsel, *filtre)
internal fun AbstractPersonTest.assertIngenVarsel(warning: String, vararg filtre: AktivitetsloggFilter) = person.personLogg.assertIngenVarsel(warning, *filtre)
internal fun AbstractPersonTest.assertIngenVarsel(varselkode: Varselkode, vararg filtre: AktivitetsloggFilter) = person.personLogg.assertIngenVarsel(varselkode, *filtre)
internal fun AbstractPersonTest.assertFunksjonellFeil(error: String, vararg filtre: AktivitetsloggFilter) = person.personLogg.assertFunksjonellFeil(error, *filtre)
internal fun AbstractPersonTest.assertFunksjonellFeil(varselkode: Varselkode, vararg filtre: AktivitetsloggFilter) = person.personLogg.assertFunksjonellFeil(varselkode.funksjonellFeilTekst, *filtre)
internal fun AbstractPersonTest.assertFunksjonelleFeil(vararg filtre: AktivitetsloggFilter) = person.personLogg.assertFunksjonelleFeil(*filtre)
internal fun AbstractPersonTest.assertIngenFunksjonellFeil(varselkode: Varselkode, vararg filtre: AktivitetsloggFilter) = person.personLogg.assertIngenFunksjonellFeil(varselkode, *filtre)

internal fun AbstractPersonTest.assertIngenFunksjonelleFeil(vararg filtre: AktivitetsloggFilter) = person.personLogg.assertIngenFunksjonelleFeil(*filtre)
internal fun AbstractPersonTest.assertLogiskFeil(severe: String, vararg filtre: AktivitetsloggFilter) = person.personLogg.assertLogiskFeil(severe, *filtre)

internal fun Aktivitetslogg.assertInfo(forventet: String, vararg filtre: AktivitetsloggFilter) {
    val info = collectInfo(*filtre)
    assertTrue(info.contains(forventet), "fant ikke ett tilfelle av infomelding <$forventet>. Info:\n${info.joinToString("\n")}")
}

internal fun Aktivitetslogg.assertIngenInfo(forventet: String, vararg filtre: AktivitetsloggFilter) {
    val info = collectInfo(*filtre)
    assertEquals(0, info.count { it == forventet }, "fant uventet info. Info:\n${info.joinToString("\n")}")
}

internal fun Aktivitetslogg.assertIngenVarsler(vararg filtre: AktivitetsloggFilter) {
    val warnings = collectVarsler(*filtre)
    assertTrue(warnings.isEmpty(), "Forventet ingen warnings. Warnings:\n${warnings.joinToString("\n")}")
}

internal fun Aktivitetslogg.assertVarsler(vararg filtre: AktivitetsloggFilter) {
    assertTrue(collectVarsler(*filtre).isNotEmpty(), "Forventet warnings, fant ingen")
}

internal fun Aktivitetslogg.assertVarsler(warnings: List<String>, vararg filtre: AktivitetsloggFilter) {
    assertVarsler(*filtre)
    val ekteWarnings = collectVarsler(*filtre)
    assertTrue(warnings.containsAll(ekteWarnings)) { "Forventet ikke warnings: ${ekteWarnings.filterNot { it in warnings }}" }
}

internal fun Aktivitetslogg.assertVarsel(warning: String, vararg filtre: AktivitetsloggFilter) {
    val warnings = collectVarsler(*filtre)
    assertTrue(warnings.contains(warning), "\nFant ikke forventet warning:\n\t$warning\nWarnings funnet:\n\t${warnings.joinToString("\n\t")}\n")
}

internal fun Aktivitetslogg.assertVarsel(varsel: Varselkode, vararg filtre: AktivitetsloggFilter) {
    val varselkoder = collectVarselkoder(*filtre)
    assertTrue(varselkoder.contains(varsel), "\nFant ikke forventet varselkode:\n\t$varsel\nVarselkoder funnet:\n\t${varselkoder.joinToString("\n\t")}\n")
}

internal fun Aktivitetslogg.assertIngenVarsel(warning: String, vararg filtre: AktivitetsloggFilter) {
    val warnings = collectVarsler(*filtre)
    assertFalse(warnings.contains(warning), "\nFant et varsel vi ikke forventet:\n\t$warning\nWarnings funnet:\n\t${warnings.joinToString("\n\t")}\n")
}

internal fun Aktivitetslogg.assertIngenVarsel(varselkode: Varselkode, vararg filtre: AktivitetsloggFilter) {
    val varselkoder = collectVarselkoder(*filtre)
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

internal fun Aktivitetslogg.assertFunksjonellFeil(error: String, vararg filtre: AktivitetsloggFilter) {
    val errors = collectFunksjonelleFeil(*filtre)
    assertTrue(errors.contains(error), "fant ikke forventet error. Errors:\n${errors.joinToString("\n")}")
}

internal fun Aktivitetslogg.assertFunksjonelleFeil(vararg filtre: AktivitetsloggFilter) {
    val errors = collectFunksjonelleFeil(*filtre)
    assertTrue(errors.isNotEmpty(), "forventet errors, fant ingen.")
}

internal fun Aktivitetslogg.assertFunksjonellFeil(varselkode: Varselkode, vararg filtre: AktivitetsloggFilter) {
    val errors = collectFunksjonelleFeil(*filtre)
    assertTrue(errors.contains(varselkode.funksjonellFeilTekst), "fant ikke forventet error. Errors:\n${errors.joinToString("\n")}")
}

internal fun Aktivitetslogg.assertIngenFunksjonelleFeil(vararg filtre: AktivitetsloggFilter) {
    val errors = collectFunksjonelleFeil(*filtre)
    assertTrue(errors.isEmpty(), "forventet ingen errors. Errors: \n${errors.joinToString("\n")}")
}

internal fun Aktivitetslogg.assertIngenFunksjonellFeil(varselkode: Varselkode, vararg filtre: AktivitetsloggFilter) {
    val errors = collectFunksjonelleFeil(*filtre)
    assertFalse(errors.contains(varselkode.varseltekst), "\nFant en varselkode vi ikke forventet:\n\t$varselkode\nVarselkoder funnet:\n\t${errors.joinToString("\n\t")}\n")
}

internal fun Aktivitetslogg.assertIngenFunksjonellFeil(vararg filtre: AktivitetsloggFilter) {
    val errors = collectFunksjonelleFeil(*filtre)
    assertTrue(errors.isEmpty(), "\nVarselkoder funnet:\n\t${errors.joinToString("\n\t")}\n")
}

internal fun Aktivitetslogg.assertLogiskFeil(severe: String, vararg filtre: AktivitetsloggFilter) {
    val severes = collectLogiskeFeil(*filtre)
    assertTrue(severes.contains(severe), "fant ikke forventet severe. Severes:\n${severes.joinToString("\n")}")
}

internal fun Aktivitetslogg.collectInfo(vararg filtre: AktivitetsloggFilter): List<String> {
    return info
        .filter { filtre.all { filter -> it.kontekster.any { filter.filtrer(it) } } }
        .map { it.melding }
}

internal fun Aktivitetslogg.collectVarsler(vararg filtre: AktivitetsloggFilter): List<String> {
    return varsel
        .filter { filtre.all { filter -> it.kontekster.any { filter.filtrer(it) } } }
        .map { it.kode.toString() }
}

internal fun Aktivitetslogg.collectVarselkoder(vararg filtre: AktivitetsloggFilter): List<Varselkode> {
    return varsel
        .filter { filtre.all { filter -> it.kontekster.any { filter.filtrer(it) } } }
        .map { it.kode }
}

internal fun Aktivitetslogg.collectFunksjonelleFeil(vararg filtre: AktivitetsloggFilter): List<String> {
    return funksjonellFeil
        .filter { filtre.all { filter -> it.kontekster.any { filter.filtrer(it) } } }
        .map { it.melding }
}

internal fun Aktivitetslogg.collectLogiskeFeil(vararg filtre: AktivitetsloggFilter): List<String> {
    return logiskFeil
        .filter { filtre.all { filter -> it.kontekster.any { filter.filtrer(it) } } }
        .map { it.melding }
}

internal fun interface AktivitetsloggFilter {
    companion object {
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
