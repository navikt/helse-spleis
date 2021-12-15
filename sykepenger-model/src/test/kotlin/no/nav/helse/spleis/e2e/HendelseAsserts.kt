package no.nav.helse.spleis.e2e

import no.nav.helse.Organisasjonsnummer
import no.nav.helse.hendelser.Periode
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.person.*
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.LocalDate
import java.util.*
import kotlin.reflect.KClass


internal fun assertInntektForDato(forventetInntekt: Inntekt?, dato: LocalDate, førsteFraværsdag: LocalDate = dato, inspektør: TestArbeidsgiverInspektør) {
    Assertions.assertEquals(forventetInntekt, inspektør.inntektInspektør.grunnlagForSykepengegrunnlag(dato, førsteFraværsdag)?.grunnlagForSykepengegrunnlag())
}

internal fun AbstractEndToEndTest.erEtterspurt(type: Aktivitetslogg.Aktivitet.Behov.Behovtype, vedtaksperiodeIdInnhenter: IdInnhenter, orgnummer: Organisasjonsnummer, tilstand: TilstandType): Boolean {
    return EtterspurtBehov.finnEtterspurtBehov(ikkeBesvarteBehov, type, vedtaksperiodeIdInnhenter, orgnummer, tilstand) != null
}

internal fun <T : ArbeidstakerHendelse> AbstractEndToEndTest.assertEtterspurt(løsning: KClass<T>, type: Aktivitetslogg.Aktivitet.Behov.Behovtype, vedtaksperiodeIdInnhenter: IdInnhenter, orgnummer: Organisasjonsnummer) {
    val etterspurtBehov = EtterspurtBehov.finnEtterspurtBehov(ikkeBesvarteBehov, type, vedtaksperiodeIdInnhenter, orgnummer)
    assertTrue(ikkeBesvarteBehov.remove(etterspurtBehov)) {
        "Forventer at $type skal være etterspurt før ${løsning.simpleName} håndteres. Perioden er i ${
            observatør.tilstandsendringer[vedtaksperiodeIdInnhenter(orgnummer)]?.last()
        }.\nAktivitetsloggen:\n${inspektør.personLogg}"
    }
}

internal fun <T : ArbeidstakerHendelse> AbstractEndToEndTest.assertIkkeEtterspurt(løsning: KClass<T>, type: Aktivitetslogg.Aktivitet.Behov.Behovtype, vedtaksperiodeIdInnhenter: IdInnhenter, orgnummer: Organisasjonsnummer) {
    val etterspurtBehov = EtterspurtBehov.finnEtterspurtBehov(ikkeBesvarteBehov, type, vedtaksperiodeIdInnhenter, orgnummer)
    Assertions.assertFalse(etterspurtBehov in ikkeBesvarteBehov) {
        "Forventer ikke at $type skal være etterspurt før ${løsning.simpleName} håndteres. Perioden er i ${
            observatør.tilstandsendringer[vedtaksperiodeIdInnhenter(orgnummer)]?.last()
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
            Assertions.assertEquals(expectedTotalgrad, totalGrad)
        }
        Assertions.assertEquals(it::class, expectedDagtype)
    }
}

internal fun AbstractEndToEndTest.assertUtbetalingsbeløp(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    forventetArbeidsgiverbeløp: Int,
    forventetArbeidsgiverRefusjonsbeløp: Int,
    subset: Periode? = null,
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER
) {
    val utbetalingstidslinje = inspektør(orgnummer).utbetalingstidslinjer(vedtaksperiodeIdInnhenter).let { subset?.let(it::subset) ?: it }

    utbetalingstidslinje.filterNot { it.dato.erHelg() }.forEach {
        it.økonomi.medAvrundetData { _, arbeidsgiverRefusjonsbeløp, _, _, arbeidsgiverbeløp, personbeløp, _ ->
            Assertions.assertEquals(forventetArbeidsgiverbeløp, arbeidsgiverbeløp)
            Assertions.assertEquals(forventetArbeidsgiverRefusjonsbeløp, arbeidsgiverRefusjonsbeløp)
            Assertions.assertEquals(0, personbeløp)
        }
    }
}

internal fun assertWarn(message: String, aktivitetslogg: Aktivitetslogg) {
    var fant = false
    aktivitetslogg.accept(object : AktivitetsloggVisitor {
        override fun visitWarn(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Warn, melding: String, tidsstempel: String) {
            if (message == melding) fant = true
        }
    })
    assertTrue(fant)
}
internal fun AbstractEndToEndTest.assertHendelseIder(
    vararg hendelseIder: UUID,
    orgnummer: Organisasjonsnummer,
    vedtaksperiodeIndeks: Int = 2,
) {
    Assertions.assertEquals(
        hendelseIder.toSet(),
        inspektør(orgnummer).hendelseIder(vedtaksperiodeIndeks.vedtaksperiode)
    )
}

internal fun AbstractEndToEndTest.assertHarHendelseIder(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    vararg hendelseIder: UUID,
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER
) {
    assertTrue(
        inspektør(orgnummer).hendelseIder(vedtaksperiodeIdInnhenter).containsAll(hendelseIder.toList())
    ) { "Perioden inneholder ikke alle hendelseidene" }
}

internal fun AbstractEndToEndTest.assertHarIkkeHendelseIder(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    vararg hendelseIder: UUID,
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER
) {
    assertTrue(
        inspektør(orgnummer).hendelseIder(vedtaksperiodeIdInnhenter).none { it in hendelseIder.toList() }) { "Perioden inneholder ikke alle hendelseidene" }
}

internal fun AbstractEndToEndTest.assertTilstand(
    orgnummer: Organisasjonsnummer,
    tilstand: TilstandType,
    vedtaksperiodeIndeks: Int = 1
) {
    assertTilstand(vedtaksperiodeIndeks.vedtaksperiode, tilstand, orgnummer)
}

internal fun AbstractEndToEndTest.assertTilstand(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    tilstand: TilstandType,
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER,
) {
    Assertions.assertEquals(tilstand, inspektør(orgnummer).sisteTilstand(vedtaksperiodeIdInnhenter)) {
        inspektør.personLogg.toString()
    }
}

internal fun AbstractEndToEndTest.assertInntektskilde(
    orgnummer: Organisasjonsnummer,
    inntektskilde: Inntektskilde,
    vedtaksperiodeIndeks: Int = 1
) {
    Assertions.assertEquals(inntektskilde, inspektør(orgnummer).inntektskilde(vedtaksperiodeIndeks.vedtaksperiode))
}


internal fun AbstractEndToEndTest.assertSisteTilstand(vedtaksperiodeIdInnhenter: IdInnhenter, tilstand: TilstandType, orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER) {
    Assertions.assertEquals(tilstand, observatør.tilstandsendringer[vedtaksperiodeIdInnhenter(orgnummer)]?.last())
}

internal fun AbstractEndToEndTest.assertTilstander(indeks: Int, vararg tilstander: TilstandType, orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER) {
    assertTilstander(vedtaksperiodeIdInnhenter = (indeks + 1).vedtaksperiode, tilstander = tilstander, orgnummer = orgnummer)
}

internal fun AbstractEndToEndTest.assertTilstander(vedtaksperiodeIdInnhenter: IdInnhenter, vararg tilstander: TilstandType, orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER, inspektør: TestArbeidsgiverInspektør = inspektør(orgnummer), message: String? = null) {
    val id = vedtaksperiodeIdInnhenter(orgnummer)
    Assertions.assertFalse(inspektør.periodeErForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er forkastet med tilstander: ${observatør.tilstandsendringer[id]}" }
    assertTrue(inspektør.periodeErIkkeForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er forkastet med tilstander: ${observatør.tilstandsendringer[id]}" }
    Assertions.assertEquals(tilstander.asList(), observatør.tilstandsendringer[id], message)
}

internal fun AbstractEndToEndTest.assertForkastetPeriodeTilstander(indeks: Int, vararg tilstander: TilstandType, orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER) {
    assertForkastetPeriodeTilstander(vedtaksperiodeIdInnhenter = indeks.vedtaksperiode, tilstander = tilstander, orgnummer = orgnummer)
}

internal fun AbstractEndToEndTest.assertForkastetPeriodeTilstander(vedtaksperiodeIdInnhenter: IdInnhenter, vararg tilstander: TilstandType, orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER, inspektør: TestArbeidsgiverInspektør = inspektør(orgnummer)) {
    assertTrue(inspektør.periodeErForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
    Assertions.assertFalse(inspektør.periodeErIkkeForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
    Assertions.assertEquals(tilstander.asList(), observatør.tilstandsendringer[vedtaksperiodeIdInnhenter(orgnummer)])
}

internal fun AbstractEndToEndTest.assertForkastetPeriodeTilstander(orgnummer: Organisasjonsnummer, vedtaksperiodeIdInnhenter: IdInnhenter, vararg tilstander: TilstandType) {
    assertTrue(inspektør(orgnummer).periodeErForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
    Assertions.assertFalse(inspektør(orgnummer).periodeErIkkeForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
    Assertions.assertEquals(tilstander.asList(), observatør.tilstandsendringer[vedtaksperiodeIdInnhenter(orgnummer)])
}

internal fun AbstractEndToEndTest.assertSisteForkastetPeriodeTilstand(orgnummer: Organisasjonsnummer, vedtaksperiodeIdInnhenter: IdInnhenter, tilstand: TilstandType) {
    assertTrue(inspektør(orgnummer).periodeErForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
    Assertions.assertFalse(inspektør(orgnummer).periodeErIkkeForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
    Assertions.assertEquals(tilstand, observatør.tilstandsendringer[vedtaksperiodeIdInnhenter(orgnummer)]?.last())
}

internal fun assertNoErrors(inspektør: TestArbeidsgiverInspektør) {
    Assertions.assertFalse(inspektør.personLogg.hasErrorsOrWorse(), inspektør.personLogg.toString())
}

internal fun assertNoWarnings(inspektør: TestArbeidsgiverInspektør) {
    Assertions.assertFalse(inspektør.personLogg.hasWarningsOrWorse(), inspektør.personLogg.toString())
}

internal fun assertWarnings(inspektør: TestArbeidsgiverInspektør) {
    assertTrue(inspektør.personLogg.hasWarningsOrWorse(), inspektør.personLogg.toString())
}

internal fun AbstractEndToEndTest.assertNoWarnings(idInnhenter: IdInnhenter, orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER) {
    val warnings = collectWarnings(idInnhenter, orgnummer)
    assertTrue(warnings.isEmpty(), "forventet ingen warnings for orgnummer $orgnummer. Warnings:\n${warnings.joinToString("\n")}")
}

internal fun AbstractEndToEndTest.assertWarning(idInnhenter: IdInnhenter, warning: String, orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER) {
    val warnings = collectWarnings(idInnhenter, orgnummer)
    assertTrue(warnings.contains(warning), "fant ikke forventet warning for $orgnummer. Warnings:\n${warnings.joinToString("\n")}")
}

private fun AbstractEndToEndTest.collectWarnings(idInnhenter: IdInnhenter, orgnummer: Organisasjonsnummer): MutableList<String> {
    val warnings = mutableListOf<String>()
    inspektør.personLogg.accept(object : AktivitetsloggVisitor {
        override fun visitWarn(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Warn, melding: String, tidsstempel: String) {
            if (kontekster.any { it.kontekstMap["vedtaksperiodeId"] == idInnhenter(orgnummer).toString() }) {
                warnings.add(melding)
            }
        }
    })
    return warnings
}

internal fun AbstractEndToEndTest.assertInfo(idInnhenter: IdInnhenter, forventet: String, orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER) {
    val info = collectInfo(idInnhenter, orgnummer)
    Assertions.assertEquals(1, info.count { it == forventet }, "fant ikke ett tilfelle av info for $orgnummer. Info:\n${info.joinToString("\n")}")
}

private fun AbstractEndToEndTest.collectInfo(idInnhenter: IdInnhenter, orgnummer: Organisasjonsnummer): MutableList<String> {
    val info = mutableListOf<String>()
    inspektør.personLogg.accept(object : AktivitetsloggVisitor {
        override fun visitInfo(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Info, melding: String, tidsstempel: String) {
            if (kontekster.any { it.kontekstMap["vedtaksperiodeId"] == idInnhenter(orgnummer).toString() }) {
                info.add(melding)
            }
        }
    })
    return info
}

internal fun AbstractEndToEndTest.assertError(idInnhenter: IdInnhenter, error: String, orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER) {
    val errors = collectErrors(idInnhenter, orgnummer)
    assertTrue(errors.contains(error), "fant ikke forventet error for $orgnummer. Errors:\n${errors.joinToString("\n")}")
}

internal fun AbstractEndToEndTest.collectErrors(idInnhenter: IdInnhenter, orgnummer: Organisasjonsnummer): MutableList<String> {
    val errors = mutableListOf<String>()
    inspektør.personLogg.accept(object : AktivitetsloggVisitor {
        override fun visitError(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Error, melding: String, tidsstempel: String) {
            if (kontekster.any { it.kontekstMap["vedtaksperiodeId"] == idInnhenter(orgnummer).toString() }) {
                errors.add(melding)
            }
        }
    })
    return errors
}

internal fun AbstractEndToEndTest.assertNoErrors(idInnhenter: IdInnhenter, orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER) {
    val errors = mutableListOf<String>()
    inspektør.personLogg.accept(object : AktivitetsloggVisitor {
        override fun visitError(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Error, melding: String, tidsstempel: String) {
            if (kontekster.any { it.kontekstMap["vedtaksperiodeId"] == idInnhenter(orgnummer).toString() }) {
                errors.add(melding)
            }
        }
    })
    assertTrue(errors.isEmpty(), "forventet ingen errors for orgnummer $orgnummer. Errors:\n${errors.joinToString("\n")}")
}

internal fun assertWarningTekst(inspektør: TestArbeidsgiverInspektør, vararg warnings: String) {
    val wantedWarnings = warnings.toMutableList()
    val actualWarnings:MutableList<String> = mutableListOf()
    inspektør.personLogg.accept(object : AktivitetsloggVisitor {
        override fun visitWarn(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Warn, melding: String, tidsstempel: String) {
            wantedWarnings.remove(melding)
            actualWarnings.add(melding)
        }
    })
    assertTrue(wantedWarnings.isEmpty(), "forventede warnings mangler: $wantedWarnings, faktiske warnings: $actualWarnings")
}

internal fun assertErrorTekst(inspektør: TestArbeidsgiverInspektør, vararg errors: String) {
    val errorList = errors.toMutableList()
    val actualErrors: MutableList<String> = mutableListOf()
    inspektør.personLogg.accept(object : AktivitetsloggVisitor {
        override fun visitError(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Error, melding: String, tidsstempel: String) {
            errorList.remove(melding)
            actualErrors.add(melding)
        }
    })
    assertTrue(errorList.isEmpty(), "har ikke fått errors $errorList, faktiske errors: $actualErrors")
}

internal fun assertErrors(inspektør: TestArbeidsgiverInspektør) {
    assertTrue(inspektør.personLogg.hasErrorsOrWorse(), inspektør.personLogg.toString())
}

internal fun assertActivities(inspektør: TestArbeidsgiverInspektør) {
    assertTrue(inspektør.personLogg.hasActivities(), inspektør.personLogg.toString())
}
