package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.erHelg
import no.nav.helse.hendelser.ArbeidstakerHendelse
import no.nav.helse.hendelser.Periode
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.inspectors.personLogg
import no.nav.helse.person.AbstractPersonTest
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.Person
import no.nav.helse.person.TilstandType
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.AktivitetsloggVisitor
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.økonomi.Inntekt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import kotlin.reflect.KClass


internal fun assertInntektshistorikkForDato(forventetInntekt: Inntekt?, dato: LocalDate, førsteFraværsdag: LocalDate = dato, inspektør: TestArbeidsgiverInspektør) {
    assertEquals(forventetInntekt, inspektør.inntektInspektør.omregnetÅrsinntekt(dato, førsteFraværsdag)?.inspektør?.beløp)
}
internal fun assertInntektForDato(forventetInntekt: Inntekt?, dato: LocalDate, inspektør: TestArbeidsgiverInspektør) {
    val grunnlagsdataInspektør = inspektør.vilkårsgrunnlagHistorikkInnslag().firstOrNull()?.vilkårsgrunnlagFor(dato)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag for $dato" }
    val sykepengegrunnlagInspektør = grunnlagsdataInspektør.sykepengegrunnlag.inspektør
    sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver[inspektør.orgnummer]?.inspektør.also {
        assertEquals(forventetInntekt, it?.inntektsopplysning?.inspektør?.beløp)
    }
}

internal fun AbstractEndToEndTest.erEtterspurt(type: Aktivitet.Behov.Behovtype, vedtaksperiodeIdInnhenter: IdInnhenter, orgnummer: String, tilstand: TilstandType): Boolean {
    return EtterspurtBehov.finnEtterspurtBehov(ikkeBesvarteBehov, type, vedtaksperiodeIdInnhenter, orgnummer, tilstand) != null
}

internal fun <T : ArbeidstakerHendelse> AbstractEndToEndTest.assertEtterspurt(løsning: KClass<T>, type: Aktivitet.Behov.Behovtype, vedtaksperiodeIdInnhenter: IdInnhenter, orgnummer: String) {
    val etterspurtBehov = EtterspurtBehov.finnEtterspurtBehov(ikkeBesvarteBehov, type, vedtaksperiodeIdInnhenter, orgnummer)
    assertTrue(ikkeBesvarteBehov.remove(etterspurtBehov)) {
        "Forventer at $type skal være etterspurt før ${løsning.simpleName} håndteres. Perioden er i ${
            observatør.tilstandsendringer[vedtaksperiodeIdInnhenter.id(orgnummer)]?.last()
        }.\nAktivitetsloggen:\n${person.personLogg}"
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

internal fun <T : ArbeidstakerHendelse> AbstractEndToEndTest.assertIkkeEtterspurt(løsning: KClass<T>, type: Aktivitet.Behov.Behovtype, vedtaksperiodeIdInnhenter: IdInnhenter, orgnummer: String) {
    val etterspurtBehov = EtterspurtBehov.finnEtterspurtBehov(ikkeBesvarteBehov, type, vedtaksperiodeIdInnhenter, orgnummer)
    assertFalse(etterspurtBehov in ikkeBesvarteBehov) {
        "Forventer ikke at $type skal være etterspurt før ${løsning.simpleName} håndteres. Perioden er i ${
            observatør.tilstandsendringer[vedtaksperiodeIdInnhenter.id(orgnummer)]?.last()
        }"
    }
}

internal inline fun <reified R : Utbetalingsdag> assertUtbetalingsdag(dag: Utbetalingsdag, expectedDagtype: KClass<R>, expectedTotalgrad: Double = 100.0) {
    dag.let {
        it.økonomi.medData { _, _, _, _, totalGrad, _, _, _, _ ->
            assertEquals(expectedTotalgrad, totalGrad)
        }
        assertEquals(it::class, expectedDagtype)
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
        utbetalingsdag.økonomi.medAvrundetData { _, arbeidsgiverRefusjonsbeløp, _, _, _, _, arbeidsgiverbeløp, personbeløp, _ ->
            assertEquals(forventetArbeidsgiverbeløp, arbeidsgiverbeløp) { "feil arbeidsgiverbeløp for dag ${utbetalingsdag.dato} "}
            assertEquals(forventetArbeidsgiverRefusjonsbeløp, arbeidsgiverRefusjonsbeløp)
            assertEquals(0, personbeløp)
        }
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

internal fun AbstractEndToEndTest.assertForkastetPeriodeTilstander(vedtaksperiodeIdInnhenter: IdInnhenter, vararg tilstander: TilstandType, orgnummer: String = AbstractPersonTest.ORGNUMMER, inspektør: TestArbeidsgiverInspektør = inspektør(orgnummer)) {
    assertEquals(tilstander.asList(), observatør.tilstandsendringer[vedtaksperiodeIdInnhenter.id(orgnummer)])
    assertTrue(inspektør.periodeErForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
    assertFalse(inspektør.periodeErIkkeForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
}

internal fun AbstractEndToEndTest.assertForkastetPeriodeTilstander(orgnummer: String, vedtaksperiodeIdInnhenter: IdInnhenter, vararg tilstander: TilstandType) {
    assertEquals(tilstander.asList(), observatør.tilstandsendringer[vedtaksperiodeIdInnhenter.id(orgnummer)])
    assertTrue(inspektør(orgnummer).periodeErForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
    assertFalse(inspektør(orgnummer).periodeErIkkeForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
}

internal fun AbstractEndToEndTest.assertSisteForkastetPeriodeTilstand(orgnummer: String, vedtaksperiodeIdInnhenter: IdInnhenter, tilstand: TilstandType) {
    assertEquals(tilstand, observatør.tilstandsendringer[vedtaksperiodeIdInnhenter.id(orgnummer)]?.last())
    assertTrue(inspektør(orgnummer).periodeErForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
    assertFalse(inspektør(orgnummer).periodeErIkkeForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
}

internal fun AbstractPersonTest.assertInfo(forventet: String, vararg filtre: AktivitetsloggFilter) = person.aktivitetslogg.assertInfo(forventet, *filtre)
internal fun AbstractPersonTest.assertIngenInfo(forventet: String, vararg filtre: AktivitetsloggFilter) = person.aktivitetslogg.assertIngenInfo(forventet, *filtre)
internal fun AbstractPersonTest.assertIngenVarsler(vararg filtre: AktivitetsloggFilter) = person.aktivitetslogg.assertIngenVarsler(*filtre)
internal fun AbstractPersonTest.assertVarsler(vararg filtre: AktivitetsloggFilter) = person.aktivitetslogg.assertVarsler(*filtre)
internal fun AbstractPersonTest.assertVarsler(warnings: List<String>, vararg filtre: AktivitetsloggFilter) = person.aktivitetslogg.assertVarsler(warnings, *filtre)
internal fun AbstractPersonTest.assertVarsel(warning: String, vararg filtre: AktivitetsloggFilter) = person.aktivitetslogg.assertVarsel(warning, *filtre)
internal fun AbstractPersonTest.assertVarsel(varsel: Varselkode, vararg filtre: AktivitetsloggFilter) = person.aktivitetslogg.assertVarsel(varsel, *filtre)
internal fun AbstractPersonTest.assertIngenVarsel(warning: String, vararg filtre: AktivitetsloggFilter) = person.aktivitetslogg.assertIngenVarsel(warning, *filtre)
internal fun AbstractPersonTest.assertIngenVarsel(varselkode: Varselkode, vararg filtre: AktivitetsloggFilter) = person.aktivitetslogg.assertIngenVarsel(varselkode, *filtre)
internal fun AbstractPersonTest.assertFunksjonellFeil(error: String, vararg filtre: AktivitetsloggFilter) = person.aktivitetslogg.assertFunksjonellFeil(error, *filtre)
internal fun AbstractPersonTest.assertFunksjonellFeil(varselkode: Varselkode, vararg filtre: AktivitetsloggFilter) = person.aktivitetslogg.assertFunksjonellFeil(varselkode.varseltekst, *filtre)
internal fun AbstractPersonTest.assertFunksjonelleFeil(vararg filtre: AktivitetsloggFilter) = person.aktivitetslogg.assertFunksjonelleFeil(*filtre)
internal fun AbstractPersonTest.assertIngenFunksjonellFeil(varselkode: Varselkode, vararg filtre: AktivitetsloggFilter) = person.aktivitetslogg.assertIngenFunksjonellFeil(varselkode, *filtre)

internal fun AbstractPersonTest.assertIngenFunksjonelleFeil(vararg filtre: AktivitetsloggFilter) = person.aktivitetslogg.assertIngenFunksjonelleFeil(*filtre)
internal fun AbstractPersonTest.assertLogiskFeil(severe: String, vararg filtre: AktivitetsloggFilter) = person.aktivitetslogg.assertLogiskFeil(severe, *filtre)

internal fun Aktivitetslogg.assertInfo(forventet: String, vararg filtre: AktivitetsloggFilter) {
    val info = collectInfo(*filtre)
    assertTrue(info.contains(forventet), "fant ikke ett tilfelle av info. Info:\n${info.joinToString("\n")}")
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
    assertTrue(warnings.containsAll(ekteWarnings)) { "Forventet ikke warnings: ${ekteWarnings.filterNot { it in warnings }}"}
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

internal fun Aktivitetslogg.assertFunksjonellFeil(error: String, vararg filtre: AktivitetsloggFilter) {
    val errors = collectFunksjonelleFeil(*filtre)
    assertTrue(errors.contains(error), "fant ikke forventet error. Errors:\n${errors.joinToString("\n")}")
}

internal fun Aktivitetslogg.assertFunksjonelleFeil(vararg filtre: AktivitetsloggFilter) {
    val errors = collectFunksjonelleFeil(*filtre)
    assertTrue(errors.isNotEmpty(), "forventet errors, fant ingen.")
}

internal fun Aktivitetslogg.assertIngenFunksjonelleFeil(vararg filtre: AktivitetsloggFilter) {
    val errors = collectFunksjonelleFeil(*filtre)
    assertTrue(errors.isEmpty(), "forventet ingen errors. Errors: \n${errors.joinToString("\n")}")
}

internal fun Aktivitetslogg.assertIngenFunksjonellFeil(varselkode: Varselkode, vararg filtre: AktivitetsloggFilter) {
    val errors = collectFunksjonelleFeil(*filtre)
    assertFalse(errors.contains(varselkode.varseltekst), "\nFant en varselkode vi ikke forventet:\n\t$varselkode\nVarselkoder funnet:\n\t${errors.joinToString("\n\t")}\n")
}

internal fun Aktivitetslogg.assertLogiskFeil(severe: String, vararg filtre: AktivitetsloggFilter) {
    val severes = collectLogiskeFeil(*filtre)
    assertTrue(severes.contains(severe), "fant ikke forventet severe. Severes:\n${severes.joinToString("\n")}")
}

internal fun Aktivitetslogg.collectInfo(vararg filtre: AktivitetsloggFilter): MutableList<String> {
    val info = mutableListOf<String>()
    this.accept(object : AktivitetsloggVisitor {
        override fun visitInfo(id: UUID, kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitet.Info, melding: String, tidsstempel: String) {
            if (filtre.all { filter -> kontekster.any { filter.filtrer(it) } }) {
                info.add(melding)
            }
        }
    })
    return info
}

internal fun Aktivitetslogg.collectVarsler(vararg filtre: AktivitetsloggFilter): MutableList<String> {
    val warnings = mutableListOf<String>()
    this.accept(object : AktivitetsloggVisitor {
        override fun visitVarsel(id: UUID, kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitet.Varsel, kode: Varselkode?, melding: String, tidsstempel: String) {
            if (filtre.all { filter -> kontekster.any { filter.filtrer(it) } }) {
                warnings.add(melding)
            }
        }
    })
    return warnings
}
internal fun Aktivitetslogg.collectVarselkoder(vararg filtre: AktivitetsloggFilter): List<Varselkode> {
    val varselkoder = mutableListOf<Varselkode>()
    accept(object : AktivitetsloggVisitor {
        override fun visitVarsel(id: UUID, kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitet.Varsel, kode: Varselkode?, melding: String, tidsstempel: String) {
            if (filtre.all { filter -> kontekster.any { filter.filtrer(it) } } && kode != null) {
                varselkoder.add(kode)
            }
        }
    })
    return varselkoder
}

internal fun Aktivitetslogg.collectFunksjonelleFeil(vararg filtre: AktivitetsloggFilter): MutableList<String> {
    val errors = mutableListOf<String>()
    accept(object : AktivitetsloggVisitor {
        override fun visitFunksjonellFeil(id: UUID, kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitet.FunksjonellFeil, melding: String, tidsstempel: String) {
            if (filtre.all { filter -> kontekster.any { filter.filtrer(it) } }) {
                errors.add(melding)
            }
        }
    })
    return errors
}

internal fun Aktivitetslogg.collectLogiskeFeil(vararg filtre: AktivitetsloggFilter): MutableList<String> {
    val severes = mutableListOf<String>()
    accept(object : AktivitetsloggVisitor {
        override fun visitLogiskFeil(id: UUID, kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitet.LogiskFeil, melding: String, tidsstempel: String) {
            if (filtre.all { filter -> kontekster.any { filter.filtrer(it) } }) {
                severes.add(melding)
            }
        }
    })
    return severes
}

internal fun interface AktivitetsloggFilter {
    companion object {
        internal fun UUID.filter() = vedtaksperiode(this)

        internal fun vedtaksperiode(vedtaksperiodeId: UUID): AktivitetsloggFilter = AktivitetsloggFilter { kontekst ->
            kontekst.kontekstMap["vedtaksperiodeId"] == vedtaksperiodeId.toString()
        }
        internal fun vedtaksperiode(idInnhenter: IdInnhenter, orgnummer: String): AktivitetsloggFilter = AktivitetsloggFilter { kontekst ->
            kontekst.kontekstMap["vedtaksperiodeId"] == idInnhenter.id(orgnummer).toString()
        }

        internal fun person(personidentifikator: Personidentifikator = AbstractPersonTest.UNG_PERSON_FNR_2018): AktivitetsloggFilter = AktivitetsloggFilter { kontekst ->
            kontekst.kontekstMap["fødselsnummer"] == personidentifikator.toString()
        }
    }

    fun filtrer(kontekst: SpesifikkKontekst): Boolean
}

internal fun assertActivities(person: Person) {
    assertTrue(person.personLogg.harAktiviteter(), person.personLogg.toString())
}
