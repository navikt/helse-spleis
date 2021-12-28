package no.nav.helse.person.builders

import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.til
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.NAVv2
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class UtbetaltEventBuilderTest {

    @Test
    fun `mapper felter`() {
        val hendelser = setOf(UUID.randomUUID(), UUID.randomUUID())
        val periode = 1.januar til 18.januar
        val sykepengegrunnlag = 25000.månedlig
        val grunnlagForSykepengegrunnlag = 26000.månedlig
        val vilkårsgrunnlag = VilkårsgrunnlagHistorikk.Grunnlagsdata(
            skjæringstidspunkt = 1.januar,
            sykepengegrunnlag = Sykepengegrunnlag(sykepengegrunnlag, emptyList(), grunnlagForSykepengegrunnlag, Sykepengegrunnlag.Begrensning.ER_IKKE_6G_BEGRENSET),
            sammenligningsgrunnlag = 26000.månedlig,
            avviksprosent = null,
            antallOpptjeningsdagerErMinst = 30,
            harOpptjening = true,
            medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
            harMinimumInntekt = true,
            vurdertOk = true,
            meldingsreferanseId = UUID.randomUUID(),
            vilkårsgrunnlagId = UUID.randomUUID()
        )
        val utbetalingId = UUID.randomUUID()
        val utbetalingOpprettet = LocalDateTime.now()
        val maksdato = LocalDate.now()
        val gjenståendeSykedager = 250
        val forbrukteSykedager = 10
        val mottaker1 = "orgnr"
        val mottaker2 = "fnr"

        val builder = UtbetaltEventBuilder(hendelser, periode, vilkårsgrunnlag)
        val ident = "Z123456"
        val automatiskBehandling = false
        val result = builder
            .utbetalingId(utbetalingId)
            .utbetalingOpprettet(utbetalingOpprettet)
            .godkjentAv(ident)
            .automatiskBehandling(automatiskBehandling)
            .utbetalingstidslinje(tidslinjeOf(16.AP, 2.NAVv2))
            .gjenståendeSykedager(gjenståendeSykedager)
            .forbrukteSykedager(forbrukteSykedager)
            .maksdato(maksdato)
            .oppdrag(Oppdrag(mottaker1, Fagområde.SykepengerRefusjon), Oppdrag(mottaker2, Fagområde.Sykepenger))
            .result()

        assertEquals(periode.start, result.fom)
        assertEquals(periode.endInclusive, result.tom)
        assertEquals(hendelser, result.hendelser)
        assertEquals(utbetalingId, result.utbetalingId)
        assertEquals(utbetalingOpprettet, result.opprettet)
        assertEquals(2, result.oppdrag.size)
        assertTrue(result.oppdrag.any { it.mottaker == mottaker1 })
        assertTrue(result.oppdrag.any { it.mottaker == mottaker2 })
        assertEquals(forbrukteSykedager, result.forbrukteSykedager)
        assertEquals(gjenståendeSykedager, result.gjenståendeSykedager)
        assertEquals(maksdato, result.maksdato)
        assertEquals(ident, result.godkjentAv)
        assertEquals(automatiskBehandling, result.automatiskBehandling)
        assertEquals(sykepengegrunnlag.reflection { årlig, _, _, _ -> årlig }, result.sykepengegrunnlag)
        assertEquals(grunnlagForSykepengegrunnlag.reflection { _, månedlig, _, _ -> månedlig }, result.månedsinntekt)
    }
}
