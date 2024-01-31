package no.nav.helse.spleis.e2e.overstyring

import java.time.LocalDate
import java.time.LocalDate.MAX
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.april
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson
import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.inntekt.Refusjonsopplysning
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger.Companion.refusjonsopplysninger
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class GjenopplivVilkårsgrunnlagTest : AbstractDslTest() {

    @Test
    fun `Kopierer vilkårsgrunnlag inn på et senere skjæringstidspunkt`() {
        a1 {
            val inntekt = 31200.månedlig
            val im = vedtakFor(1.januar til 31.januar, inntekt)

            håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))

            kopierVilkårsgrunnlag(fra = 1.januar, til = 1.mars)
            håndterPåminnelse(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, reberegning = true)
            fraAvventerHistorikkTilAvsluttet(2.vedtaksperiode)

            assertEquals(Refusjonsopplysning(im, 1.mars, null, inntekt).refusjonsopplysninger, refusjonsopplysninger(1.mars))
            assertEquals(1.mars til MAX, inntektGjelder(1.mars))
        }
    }

    @Test
    fun `Kopierer vilkårsgrunnlag inn på et tidligere skjæringstidspunkt`() {
        a1 {
            val inntekt = 31500.månedlig
            val im = vedtakFor(1.mars til 31.mars, inntekt)

            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

            kopierVilkårsgrunnlag(fra = 1.mars, til = 1.januar)
            håndterPåminnelse(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, reberegning = true)
            fraAvventerHistorikkTilAvsluttet(2.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)

            assertEquals(Refusjonsopplysninger(
                Refusjonsopplysning(im, 1.januar, 28.februar, inntekt),
                Refusjonsopplysning(im, 1.mars, null, inntekt)
            ), refusjonsopplysninger(1.januar))
            assertEquals(1.januar til MAX, inntektGjelder(1.januar))
        }
    }

    @Test
    fun `Gjenoppliver bort deler av refusjonopplysningene`() {
        val id1 = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val id2 = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val id3 = UUID.fromString("00000000-0000-0000-0000-000000000003")
        val refusjonsopplysningerFør = Refusjonsopplysninger(
            Refusjonsopplysning(id1, 1.februar, 28.februar, 1000.daglig),
            Refusjonsopplysning(id2, 1.mars, 31.mars, 1500.daglig),
            Refusjonsopplysning(id3, 1.april, null, 2000.daglig)
        )

        val forventetRefusjonsopplysningerEtter = Refusjonsopplysninger(
            Refusjonsopplysning(id2, 15.mars, 31.mars, 1500.daglig),
            Refusjonsopplysning(id3, 1.april, null, 2000.daglig)
        )

        assertEquals(forventetRefusjonsopplysningerEtter, refusjonsopplysningerFør.gjenoppliv(15.mars))
    }

    private fun TestPerson.TestArbeidsgiver.vedtakFor(periode: Periode, inntekt: Inntekt): UUID {
        val vedtaksperiodeId = håndterSøknad(Sykdom(periode.start, periode.endInclusive, 100.prosent)) ?: throw IllegalStateException("What")
        val im = håndterInntektsmelding(listOf(periode.start til periode.start.plusDays(15)), beregnetInntekt = inntekt)
        håndterVilkårsgrunnlag(vedtaksperiodeId)
        håndterYtelser(vedtaksperiodeId)
        håndterSimulering(vedtaksperiodeId)
        håndterUtbetalingsgodkjenning(vedtaksperiodeId)
        håndterUtbetalt()
        assertSisteTilstand(vedtaksperiodeId, AVSLUTTET)
        return im
    }

    private fun TestPerson.TestArbeidsgiver.kopierVilkårsgrunnlag(fra: LocalDate, til: LocalDate) {
        val vilkårsgrunnlagId = inspektør.vilkårsgrunnlag(fra)!!.inspektør.vilkårsgrunnlagId
        håndterGjenopplivVilkårsgrunnlag(til, vilkårsgrunnlagId)
    }

    private fun TestPerson.TestArbeidsgiver.fraAvventerHistorikkTilAvsluttet(vedtaksperiodeId: UUID) {
        assertSisteTilstand(vedtaksperiodeId, AVVENTER_HISTORIKK)
        håndterYtelser(vedtaksperiodeId)
        håndterSimulering(vedtaksperiodeId)
        håndterUtbetalingsgodkjenning(vedtaksperiodeId)
        håndterUtbetalt()
        assertSisteTilstand(vedtaksperiodeId, AVSLUTTET)
    }

    private fun TestPerson.TestArbeidsgiver.refusjonsopplysninger(skjæringstidspunkt: LocalDate) =
        inspektør.vilkårsgrunnlag(skjæringstidspunkt)!!.inspektør.sykepengegrunnlag.inspektør.arbeidsgiverInntektsopplysninger.single { it.inspektør.orgnummer == this.orgnummer }.inspektør.refusjonsopplysninger

    private fun TestPerson.TestArbeidsgiver.inntektGjelder(skjæringstidspunkt: LocalDate) =
        inspektør.vilkårsgrunnlag(skjæringstidspunkt)!!.inspektør.sykepengegrunnlag.inspektør.arbeidsgiverInntektsopplysninger.single { it.inspektør.orgnummer == this.orgnummer }.inspektør.gjelder

    private fun Refusjonsopplysninger(vararg refusjonsopplysninger: Refusjonsopplysning): Refusjonsopplysning.Refusjonsopplysninger {
        val refusjonsopplysningerBuilder = Refusjonsopplysning.Refusjonsopplysninger.RefusjonsopplysningerBuilder()
        refusjonsopplysninger.forEach { refusjonsopplysningerBuilder.leggTil(it, LocalDateTime.now()) }
        return refusjonsopplysningerBuilder.build()
    }
}