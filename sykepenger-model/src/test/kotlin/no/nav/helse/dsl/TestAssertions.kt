package no.nav.helse.dsl

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.inspectors.PersonInspektør
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.AktivitetsloggVisitor
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.inntektsopplysningPerArbeidsgiver
import no.nav.helse.person.SpesifikkKontekst
import no.nav.helse.person.TilstandType
import no.nav.helse.spleis.e2e.AktivitetsloggFilter
import no.nav.helse.spleis.e2e.TestObservatør
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

internal class TestAssertions(private val observatør: TestObservatør, private val inspektør: TestArbeidsgiverInspektør, private val personInspektør: PersonInspektør) {
    internal fun assertSisteTilstand(vedtaksperiodeId: UUID, tilstand: TilstandType, errortekst: (() -> String)? = null) {
        assertEquals(tilstand, observatør.tilstandsendringer[vedtaksperiodeId]?.last(), errortekst)
    }

    internal fun assertTilstander(id: UUID, vararg tilstander: TilstandType) {
        assertFalse(inspektør.periodeErForkastet(id)) { "Perioden er forkastet med tilstander: ${observatør.tilstandsendringer[id]}:\n${personInspektør.aktivitetslogg}" }
        assertTrue(inspektør.periodeErIkkeForkastet(id)) { "Perioden er forkastet med tilstander: ${observatør.tilstandsendringer[id]}\n${personInspektør.aktivitetslogg}" }
        assertEquals(tilstander.asList(), observatør.tilstandsendringer[id]) { personInspektør.aktivitetslogg.toString() }
    }

    internal fun assertForkastetPeriodeTilstander(id: UUID, vararg tilstander: TilstandType) {
        assertTrue(inspektør.periodeErForkastet(id)) { "Perioden er ikke forkastet" }
        assertFalse(inspektør.periodeErIkkeForkastet(id)) { "Perioden er ikke forkastet" }
        assertEquals(tilstander.asList(), observatør.tilstandsendringer[id]) { personInspektør.aktivitetslogg.toString() }
    }

    internal fun assertArbeidsgivereISykepengegrunnlag(skjæringstidspunkt: LocalDate, vararg arbeidsgivere: String) {
        assertEquals(arbeidsgivere.toSet(), personInspektør.grunnlagsdata(skjæringstidspunkt).inspektør.sykepengegrunnlag.inspektør.arbeidsgiverInntektsopplysninger.inntektsopplysningPerArbeidsgiver().keys)
    }

    internal fun assertInfo(forventet: String, vararg filtre: AktivitetsloggFilter) {
        val info = collectInfo(*filtre)
        assertEquals(1, info.count { it == forventet }, "fant ikke ett tilfelle av info. Info:\n${info.joinToString("\n")}")
    }

    internal fun assertNoInfo(forventet: String, vararg filtre: AktivitetsloggFilter) {
        val info = collectInfo(*filtre)
        assertEquals(0, info.count { it == forventet }, "fant uventet info. Info:\n${info.joinToString("\n")}")
    }

    internal fun assertNoWarnings(vararg filtre: AktivitetsloggFilter) {
        val warnings = collectWarnings(*filtre)
        assertTrue(warnings.isEmpty(), "Forventet ingen warnings. Warnings:\n${warnings.joinToString("\n")}")
    }

    internal fun assertWarnings(vararg filtre: AktivitetsloggFilter) {
        assertTrue(collectWarnings(*filtre).isNotEmpty(), "Forventet warnings, fant ingen")
    }

    internal fun assertWarning(warning: String, vararg filtre: AktivitetsloggFilter) {
        val warnings = collectWarnings(*filtre)
        assertTrue(warnings.contains(warning), "\nFant ikke forventet warning:\n\t$warning\nWarnings funnet:\n\t${warnings.joinToString("\n\t")}\n")
    }

    internal fun assertNoWarning(warning: String, vararg filtre: AktivitetsloggFilter) {
        val warnings = collectWarnings(*filtre)
        assertFalse(warnings.contains(warning), "\nFant ikke-forventet warning:\n\t$warning\nWarnings funnet:\n\t${warnings.joinToString("\n\t")}\n")
    }

    internal fun assertError(error: String, vararg filtre: AktivitetsloggFilter) {
        val errors = collectErrors(*filtre)
        assertTrue(errors.contains(error), "fant ikke forventet error. Errors:\n${errors.joinToString("\n")}")
    }

    internal fun assertErrors(vararg filtre: AktivitetsloggFilter) {
        val errors = collectErrors(*filtre)
        assertTrue(errors.isNotEmpty(), "forventet errors, fant ingen.")
    }


    internal fun assertNoErrors(vararg filtre: AktivitetsloggFilter) {
        val errors = collectErrors(*filtre)
        assertTrue(errors.isEmpty(), "forventet ingen errors. Errors: \n${errors.joinToString("\n")}")
    }

    internal fun assertSevere(severe: String, vararg filtre: AktivitetsloggFilter) {
        val severes = collectSeveres(*filtre)
        assertTrue(severes.contains(severe), "fant ikke forventet severe. Severes:\n${severes.joinToString("\n")}")
    }

    private fun collectInfo(vararg filtre: AktivitetsloggFilter): MutableList<String> {
        val info = mutableListOf<String>()
        personInspektør.aktivitetslogg.accept(object : AktivitetsloggVisitor {
            override fun visitInfo(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Info, melding: String, tidsstempel: String) {
                if (filtre.all { filter -> kontekster.any { filter.filtrer(it) } }) {
                    info.add(melding)
                }
            }
        })
        return info
    }

    private fun collectWarnings(vararg filtre: AktivitetsloggFilter): MutableList<String> {
        val warnings = mutableListOf<String>()
        personInspektør.aktivitetslogg.accept(object : AktivitetsloggVisitor {
            override fun visitWarn(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Warn, melding: String, tidsstempel: String) {
                if (filtre.all { filter -> kontekster.any { filter.filtrer(it) } }) {
                    warnings.add(melding)
                }
            }
        })
        return warnings
    }

    internal fun collectErrors(vararg filtre: AktivitetsloggFilter): MutableList<String> {
        val errors = mutableListOf<String>()
        personInspektør.aktivitetslogg.accept(object : AktivitetsloggVisitor {
            override fun visitError(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Error, melding: String, tidsstempel: String) {
                if (filtre.all { filter -> kontekster.any { filter.filtrer(it) } }) {
                    errors.add(melding)
                }
            }
        })
        return errors
    }

    internal fun collectSeveres(vararg filtre: AktivitetsloggFilter): MutableList<String> {
        val severes = mutableListOf<String>()
        personInspektør.aktivitetslogg.accept(object : AktivitetsloggVisitor {
            override fun visitSevere(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Severe, melding: String, tidsstempel: String) {
                if (filtre.all { filter -> kontekster.any { filter.filtrer(it) } }) {
                    severes.add(melding)
                }
            }
        })
        return severes
    }
}