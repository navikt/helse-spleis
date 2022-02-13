package no.nav.helse.spleis.e2e

import no.nav.helse.Fødselsnummer
import no.nav.helse.hendelser.Periode
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.inspectors.personLogg
import no.nav.helse.person.*
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate
import java.util.*
import kotlin.reflect.KClass


internal fun assertInntektForDato(forventetInntekt: Inntekt?, dato: LocalDate, førsteFraværsdag: LocalDate = dato, inspektør: TestArbeidsgiverInspektør) {
    assertEquals(forventetInntekt, inspektør.inntektInspektør.grunnlagForSykepengegrunnlag(dato, førsteFraværsdag)?.grunnlagForSykepengegrunnlag())
}

internal fun AbstractEndToEndTest.erEtterspurt(type: Aktivitetslogg.Aktivitet.Behov.Behovtype, vedtaksperiodeIdInnhenter: IdInnhenter, orgnummer: String, tilstand: TilstandType): Boolean {
    return EtterspurtBehov.finnEtterspurtBehov(ikkeBesvarteBehov, type, vedtaksperiodeIdInnhenter, orgnummer, tilstand) != null
}

internal fun <T : ArbeidstakerHendelse> AbstractEndToEndTest.assertEtterspurt(løsning: KClass<T>, type: Aktivitetslogg.Aktivitet.Behov.Behovtype, vedtaksperiodeIdInnhenter: IdInnhenter, orgnummer: String) {
    val etterspurtBehov = EtterspurtBehov.finnEtterspurtBehov(ikkeBesvarteBehov, type, vedtaksperiodeIdInnhenter, orgnummer)
    assertTrue(ikkeBesvarteBehov.remove(etterspurtBehov)) {
        "Forventer at $type skal være etterspurt før ${løsning.simpleName} håndteres. Perioden er i ${
            observatør.tilstandsendringer[vedtaksperiodeIdInnhenter.id(orgnummer)]?.last()
        }.\nAktivitetsloggen:\n${person.personLogg}"
    }
}

internal fun <T : ArbeidstakerHendelse> AbstractEndToEndTest.assertIkkeEtterspurt(løsning: KClass<T>, type: Aktivitetslogg.Aktivitet.Behov.Behovtype, vedtaksperiodeIdInnhenter: IdInnhenter, orgnummer: String) {
    val etterspurtBehov = EtterspurtBehov.finnEtterspurtBehov(ikkeBesvarteBehov, type, vedtaksperiodeIdInnhenter, orgnummer)
    assertFalse(etterspurtBehov in ikkeBesvarteBehov) {
        "Forventer ikke at $type skal være etterspurt før ${løsning.simpleName} håndteres. Perioden er i ${
            observatør.tilstandsendringer[vedtaksperiodeIdInnhenter.id(orgnummer)]?.last()
        }"
    }
}

internal fun AbstractEndToEndTest.assertAlleBehovBesvart() {
    assertTrue(ikkeBesvarteBehov.isEmpty()) {
        "Ikke alle behov er besvart. Mangler fortsatt svar på behovene $ikkeBesvarteBehov"
    }
}

internal inline fun <reified R : Utbetalingstidslinje.Utbetalingsdag> assertUtbetalingsdag(dag: Utbetalingstidslinje.Utbetalingsdag, expectedDagtype: KClass<R>, expectedTotalgrad: Double = 100.0) {
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

    utbetalingstidslinje.filterNot { it.dato.erHelg() }.forEach {
        it.økonomi.medAvrundetData { _, arbeidsgiverRefusjonsbeløp, _, _, _, _, arbeidsgiverbeløp, personbeløp, _ ->
            assertEquals(forventetArbeidsgiverbeløp, arbeidsgiverbeløp)
            assertEquals(forventetArbeidsgiverRefusjonsbeløp, arbeidsgiverRefusjonsbeløp)
            assertEquals(0, personbeløp)
        }
    }
}

internal fun AbstractEndToEndTest.assertHendelseIder(
    vararg hendelseIder: UUID,
    orgnummer: String,
    vedtaksperiodeIndeks: Int = 2,
) {
    assertEquals(
        hendelseIder.toSet(),
        inspektør(orgnummer).hendelseIder(vedtaksperiodeIndeks.vedtaksperiode)
    )
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

internal fun AbstractEndToEndTest.assertInntektskilde(
    orgnummer: String,
    inntektskilde: Inntektskilde,
    vedtaksperiodeIndeks: Int = 1
) {
    assertEquals(inntektskilde, inspektør(orgnummer).inntektskilde(vedtaksperiodeIndeks.vedtaksperiode))
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
    assertTrue(inspektør.periodeErForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
    assertFalse(inspektør.periodeErIkkeForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
    assertEquals(tilstander.asList(), observatør.tilstandsendringer[vedtaksperiodeIdInnhenter.id(orgnummer)])
}

internal fun AbstractEndToEndTest.assertForkastetPeriodeTilstander(orgnummer: String, vedtaksperiodeIdInnhenter: IdInnhenter, vararg tilstander: TilstandType) {
    assertTrue(inspektør(orgnummer).periodeErForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
    assertFalse(inspektør(orgnummer).periodeErIkkeForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
    assertEquals(tilstander.asList(), observatør.tilstandsendringer[vedtaksperiodeIdInnhenter.id(orgnummer)])
}

internal fun AbstractEndToEndTest.assertSisteForkastetPeriodeTilstand(orgnummer: String, vedtaksperiodeIdInnhenter: IdInnhenter, tilstand: TilstandType) {
    assertTrue(inspektør(orgnummer).periodeErForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
    assertFalse(inspektør(orgnummer).periodeErIkkeForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
    assertEquals(tilstand, observatør.tilstandsendringer[vedtaksperiodeIdInnhenter.id(orgnummer)]?.last())
}

internal fun assertNoErrors(person: Person) {
    assertFalse(person.personLogg.hasErrorsOrWorse(), person.personLogg.toString())
}

internal fun AbstractPersonTest.assertNoWarnings(vararg filtre: AktivitetsloggFilter) {
    val warnings = collectWarnings(*filtre)
    assertTrue(warnings.isEmpty(), "Forventet ingen warnings. Warnings:\n${warnings.joinToString("\n")}")
}

internal fun AbstractPersonTest.assertWarnings(vararg filtre: AktivitetsloggFilter) {
    assertTrue(collectWarnings(*filtre).isNotEmpty(), "Forventet warnings, fant ingen")
}

internal fun AbstractPersonTest.assertWarning(warning: String, vararg filtre: AktivitetsloggFilter) {
    val warnings = collectWarnings(*filtre)
    assertTrue(warnings.contains(warning), "fant ikke forventet warning. Warnings:\n${warnings.joinToString("\n")}")
}

internal fun AbstractPersonTest.assertNoWarning(warning: String, vararg filtre: AktivitetsloggFilter) {
    val warnings = collectWarnings(*filtre)
    assertFalse(warnings.contains(warning), "fant ikke forventet warning. Warnings:\n${warnings.joinToString("\n")}")
}

private fun AbstractPersonTest.collectWarnings(vararg filtre: AktivitetsloggFilter): MutableList<String> {
    val warnings = mutableListOf<String>()
    person.personLogg.accept(object : AktivitetsloggVisitor {
        override fun visitWarn(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Warn, melding: String, tidsstempel: String) {
            if (filtre.all { filter -> kontekster.any { filter.filtrer(it) } }) {
                warnings.add(melding)
            }
        }
    })
    return warnings
}

internal fun AbstractEndToEndTest.assertInfo(forventet: String, vararg filtre: AktivitetsloggFilter) {
    val info = collectInfo(*filtre)
    assertEquals(1, info.count { it == forventet }, "fant ikke ett tilfelle av info. Info:\n${info.joinToString("\n")}")
}

internal fun AbstractEndToEndTest.assertNoInfo(forventet: String, vararg filtre: AktivitetsloggFilter) {
    val info = collectInfo(*filtre)
    assertEquals(0, info.count { it == forventet }, "fant uventet info. Info:\n${info.joinToString("\n")}")
}

private fun AbstractEndToEndTest.collectInfo(vararg filtre: AktivitetsloggFilter): MutableList<String> {
    val info = mutableListOf<String>()
    person.personLogg.accept(object : AktivitetsloggVisitor {
        override fun visitInfo(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Info, melding: String, tidsstempel: String) {
            if (filtre.all { filter -> kontekster.any { filter.filtrer(it) } }) {
                info.add(melding)
            }
        }
    })
    return info
}

internal fun AbstractEndToEndTest.assertError(idInnhenter: IdInnhenter, error: String, orgnummer: String = AbstractPersonTest.ORGNUMMER) {
    val errors = collectErrors(idInnhenter, orgnummer)
    assertTrue(errors.contains(error), "fant ikke forventet error for $orgnummer. Errors:\n${errors.joinToString("\n")}")
}

internal fun AbstractEndToEndTest.assertSevere(severe: String, vararg filtre: AktivitetsloggFilter) {
    val severes = collectSeveres(*filtre)
    assertTrue(severes.contains(severe), "fant ikke forventet severe. Severes:\n${severes.joinToString("\n")}")
}

internal fun AbstractEndToEndTest.collectErrors(idInnhenter: IdInnhenter, orgnummer: String): MutableList<String> {
    val errors = mutableListOf<String>()
    person.personLogg.accept(object : AktivitetsloggVisitor {
        override fun visitError(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Error, melding: String, tidsstempel: String) {
            if (kontekster.any { it.kontekstMap["vedtaksperiodeId"] == idInnhenter.id(orgnummer).toString() }) {
                errors.add(melding)
            }
        }
    })
    return errors
}

internal fun AbstractEndToEndTest.collectSeveres(vararg filtre: AktivitetsloggFilter): MutableList<String> {
    val severes = mutableListOf<String>()
    person.personLogg.accept(object : AktivitetsloggVisitor {
        override fun visitSevere(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Severe, melding: String, tidsstempel: String) {
            if (filtre.all { filter -> kontekster.any { filter.filtrer(it) } }) {
                severes.add(melding)
            }
        }
    })
    return severes
}

internal fun interface AktivitetsloggFilter {
    companion object {
        internal fun vedtaksperiode(idInnhenter: IdInnhenter, orgnummer: String): AktivitetsloggFilter = AktivitetsloggFilter { kontekst ->
            kontekst.kontekstMap["vedtaksperiodeId"] == idInnhenter.id(orgnummer).toString()
        }

        internal fun person(fødselsnummer: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018): AktivitetsloggFilter = AktivitetsloggFilter { kontekst ->
            kontekst.kontekstMap["fødselsnummer"] == fødselsnummer.toString()
        }
    }

    fun filtrer(kontekst: SpesifikkKontekst): Boolean
}

internal fun AbstractEndToEndTest.assertNoErrors(idInnhenter: IdInnhenter, orgnummer: String = AbstractPersonTest.ORGNUMMER) {
    val errors = mutableListOf<String>()
    person.personLogg.accept(object : AktivitetsloggVisitor {
        override fun visitError(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Error, melding: String, tidsstempel: String) {
            if (kontekster.any { it.kontekstMap["vedtaksperiodeId"] == idInnhenter.id(orgnummer).toString() }) {
                errors.add(melding)
            }
        }
    })
    assertTrue(errors.isEmpty(), "forventet ingen errors for orgnummer $orgnummer. Errors:\n${errors.joinToString("\n")}")
}

internal fun assertErrorTekst(person: Person, vararg errors: String) {
    val errorList = errors.toMutableList()
    val actualErrors: MutableList<String> = mutableListOf()
    person.personLogg.accept(object : AktivitetsloggVisitor {
        override fun visitError(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Error, melding: String, tidsstempel: String) {
            errorList.remove(melding)
            actualErrors.add(melding)
        }
    })
    assertTrue(errorList.isEmpty(), "har ikke fått errors $errorList, faktiske errors: $actualErrors")
}

internal fun assertErrors(person: Person) {
    assertTrue(person.personLogg.hasErrorsOrWorse(), person.personLogg.toString())
}

internal fun assertActivities(person: Person) {
    assertTrue(person.personLogg.hasActivities(), person.personLogg.toString())
}
