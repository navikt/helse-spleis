package no.nav.helse.dsl

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Periode
import no.nav.helse.inspectors.PersonInspektør
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.person.ArbeidstakerOpptjeningView
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.AktivitetsloggFilter
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.spleis.e2e.TestObservatør
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.inspectors.inspektør
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

internal class TestArbeidsgiverAssertions(
    private val observatør: TestObservatør,
    private val inspektør: TestArbeidsgiverInspektør,
    private val personInspektør: PersonInspektør,
    private val aktivitetsloggAsserts: AktivitetsloggAsserts
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
        assertFalse(inspektør.periodeErForkastet(id)) { "Perioden er forkastet med tilstander: ${observatør.tilstandsendringer[id]}" }
        assertTrue(inspektør.periodeErIkkeForkastet(id)) { "Perioden er forkastet med tilstander: ${observatør.tilstandsendringer[id]}" }
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
        val opptjening = personInspektør.vilkårsgrunnlagHistorikk.grunnlagsdata(skjæringstidspunkt).opptjening!! as ArbeidstakerOpptjeningView
        assertEquals(forventet, opptjening.opptjeningsdager)
    }

    internal fun assertErOppfylt(skjæringstidspunkt: LocalDate) {
        val opptjening = personInspektør.vilkårsgrunnlagHistorikk.grunnlagsdata(skjæringstidspunkt).opptjening!! as ArbeidstakerOpptjeningView
        assertTrue(opptjening.erOppfylt)
    }

    internal fun assertErIkkeOppfylt(skjæringstidspunkt: LocalDate) {
        val opptjening = personInspektør.vilkårsgrunnlagHistorikk.grunnlagsdata(skjæringstidspunkt).opptjening!! as ArbeidstakerOpptjeningView
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

    internal fun assertInfo(forventet: String, filter: AktivitetsloggFilter) =
        aktivitetsloggAsserts.assertInfo(forventet, filter)

    internal fun assertIngenInfo(forventet: String, filter: AktivitetsloggFilter) =
        aktivitetsloggAsserts.assertIngenInfo(forventet, filter)

    internal fun assertIngenInfoSomInneholder(forventet: String, filter: AktivitetsloggFilter) =
        aktivitetsloggAsserts.assertIngenInfoSomInneholder(forventet, filter)

    internal fun assertVarsler(varsler: Collection<Varselkode>, filter: AktivitetsloggFilter) =
        aktivitetsloggAsserts.assertVarsler(varsler, filter)

    internal fun assertVarsel(warning: String, filter: AktivitetsloggFilter) =
        aktivitetsloggAsserts.assertVarsel(warning, filter)

    internal fun assertVarsel(kode: Varselkode, filter: AktivitetsloggFilter) =
        aktivitetsloggAsserts.assertVarsel(kode, filter)

    internal fun assertFunksjonellFeil(error: String, filter: AktivitetsloggFilter) =
        aktivitetsloggAsserts.assertFunksjonellFeil(error, filter)

    internal fun assertFunksjonellFeil(varselkode: Varselkode, filter: AktivitetsloggFilter) =
        aktivitetsloggAsserts.assertFunksjonellFeil(varselkode, filter)

    internal fun assertIngenBehov(vedtaksperiode: UUID, behovtype: Aktivitet.Behov.Behovtype) =
        aktivitetsloggAsserts.assertIngenBehov(vedtaksperiode, behovtype)

    internal fun assertBehov(vedtaksperiode: UUID, behovtype: Aktivitet.Behov.Behovtype) =
        aktivitetsloggAsserts.assertBehov(vedtaksperiode, behovtype)

    internal fun ingenNyeFunksjonelleFeil(block: () -> Unit) {
        return aktivitetsloggAsserts.ingenNyeFunksjonelleFeil(block)
    }

    internal fun nyeFunksjonelleFeil(block: () -> Unit): Boolean {
        return aktivitetsloggAsserts.nyeFunksjonelleFeil(block)
    }

    internal fun assertFunksjonelleFeil(filter: AktivitetsloggFilter) =
        aktivitetsloggAsserts.assertFunksjonelleFeil(filter)

    internal fun assertIngenFunksjonelleFeil(filter: AktivitetsloggFilter) =
        aktivitetsloggAsserts.assertIngenFunksjonelleFeil(filter)

    internal fun assertLogiskFeil(severe: String, filter: AktivitetsloggFilter) =
        aktivitetsloggAsserts.assertLogiskFeil(severe, filter)
}
