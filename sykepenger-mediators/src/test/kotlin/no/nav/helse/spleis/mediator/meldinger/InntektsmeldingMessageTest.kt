package no.nav.helse.spleis.mediator.meldinger

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Inntektsmelding.Avsendersystem.Altinn
import no.nav.helse.hendelser.Inntektsmelding.Avsendersystem.LPS
import no.nav.helse.hendelser.Inntektsmelding.Avsendersystem.NavPortal
import no.nav.helse.spleis.meldinger.model.InntektsmeldingMessage.Companion.tilAvsendersystem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class InntektsmeldingMessageTest {

    @Test
    fun `mapper avsendersystem fra json`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val inntektsdato = LocalDate.EPOCH
        val førsteFraværsdag = LocalDate.EPOCH.plusDays(1)
        assertEquals(LPS(førsteFraværsdag),  objectMapper.nullNode().tilAvsendersystem(null, null, førsteFraværsdag))
        assertEquals(LPS(førsteFraværsdag), objectMapper.missingNode().tilAvsendersystem(null, null, førsteFraværsdag))
        assertEquals(LPS(førsteFraværsdag), objectMapper.readTree(mangler).tilAvsendersystem(null, null, førsteFraværsdag))
        assertEquals(LPS(førsteFraværsdag), objectMapper.readTree(sattTilNull).tilAvsendersystem(null, null, førsteFraværsdag))
        assertEquals(NavPortal(vedtaksperiodeId, inntektsdato, true), objectMapper.readTree(navNo).tilAvsendersystem(vedtaksperiodeId, inntektsdato, null))
        assertEquals(NavPortal(vedtaksperiodeId, inntektsdato, false), objectMapper.readTree(navNoSelvbestemt).tilAvsendersystem(vedtaksperiodeId, inntektsdato, null))
        assertEquals(Altinn(førsteFraværsdag), objectMapper.readTree(altinn).tilAvsendersystem(null, null, førsteFraværsdag))
        assertEquals(LPS(førsteFraværsdag), objectMapper.readTree(hvaSomHelst).tilAvsendersystem(null, null, førsteFraværsdag))
    }

    private companion object {
        val objectMapper = jacksonObjectMapper()
        val mangler = """{}"""
        val sattTilNull = """{ "navn": null }"""
        val navNo = """{ "navn": "NAV_NO" }"""
        val navNoSelvbestemt = """{ "navn": "NAV_NO_SELVBESTEMT" }"""
        val altinn = """{ "navn": "AltinnPortal" }"""
        val hvaSomHelst = """{ "navn": "HvaSomHelst" }"""
    }
}