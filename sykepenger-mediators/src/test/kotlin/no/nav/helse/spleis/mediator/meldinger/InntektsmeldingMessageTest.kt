package no.nav.helse.spleis.mediator.meldinger

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsmelding.Avsendersystem.Altinn
import no.nav.helse.hendelser.Inntektsmelding.Avsendersystem.LPS
import no.nav.helse.hendelser.Inntektsmelding.Avsendersystem.NavPortal
import no.nav.helse.januar
import no.nav.helse.spleis.meldinger.model.InntektsmeldingMessage.Companion.tilAvsendersystem
import no.nav.helse.spleis.meldinger.model.InntektsmeldingMessage.Companion.tilOpphørAvNaturalytelser
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class InntektsmeldingMessageTest {

    @Test
    fun `mapper avsendersystem fra json`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val førsteFraværsdag = LocalDate.EPOCH.plusDays(1)
        assertEquals(LPS(førsteFraværsdag), objectMapper.nullNode().tilAvsendersystem(null, null, førsteFraværsdag))
        assertEquals(LPS(førsteFraværsdag), objectMapper.missingNode().tilAvsendersystem(null, null, førsteFraværsdag))
        assertEquals(LPS(førsteFraværsdag), objectMapper.readTree(mangler).tilAvsendersystem(null, null, førsteFraværsdag))
        assertEquals(LPS(førsteFraværsdag), objectMapper.readTree(sattTilNull).tilAvsendersystem(null, null, førsteFraværsdag))
        assertEquals(NavPortal(vedtaksperiodeId, null, true), objectMapper.readTree(navNo).tilAvsendersystem(vedtaksperiodeId, null, null))
        assertEquals(NavPortal(vedtaksperiodeId, null, false), objectMapper.readTree(navNoSelvbestemt).tilAvsendersystem(vedtaksperiodeId, null, null))
        assertEquals(Altinn(førsteFraværsdag), objectMapper.readTree(altinn).tilAvsendersystem(null, null, førsteFraværsdag))
        assertEquals(LPS(førsteFraværsdag), objectMapper.readTree(hvaSomHelst).tilAvsendersystem(null, null, førsteFraværsdag))
    }

    @Test
    fun `mapper naturalytelser fra json`() {
        assertEquals(emptyList<Inntektsmelding.OpphørAvNaturalytelse>(), objectMapper.readTree(naturalytelseTom).tilOpphørAvNaturalytelser())
        assertEquals(listOf(Inntektsmelding.OpphørAvNaturalytelse(naturalytelse = "ANNET", beløp = null, fom = null)), objectMapper.readTree(naturalytelse1).tilOpphørAvNaturalytelser())
        assertEquals(listOf(Inntektsmelding.OpphørAvNaturalytelse(naturalytelse = null, beløp = 1200.månedlig, fom = null)), objectMapper.readTree(naturalytelse2).tilOpphørAvNaturalytelser())
        assertEquals(listOf(Inntektsmelding.OpphørAvNaturalytelse(naturalytelse = null, beløp = null, fom = 1.januar)), objectMapper.readTree(naturalytelse3).tilOpphørAvNaturalytelser())
    }

    private companion object {
        val objectMapper = jacksonObjectMapper()
        val mangler = """{}"""
        val sattTilNull = """{ "navn": null }"""
        val navNo = """{ "navn": "NAV_NO" }"""
        val navNoSelvbestemt = """{ "navn": "NAV_NO_SELVBESTEMT" }"""
        val altinn = """{ "navn": "AltinnPortal" }"""
        val hvaSomHelst = """{ "navn": "HvaSomHelst" }"""
        val naturalytelseTom = """[]"""
        val naturalytelse1 = """[{"naturalytelse":"ANNET"}]"""
        val naturalytelse2 = """[{"beloepPrMnd":"1200.0"}]"""
        val naturalytelse3 = """[{"fom":"2018-01-01"}]"""
    }
}
