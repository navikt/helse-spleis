package no.nav.helse.spleis.mediator.meldinger

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Inntektsmelding.Avsendersystem.Altinn
import no.nav.helse.hendelser.Inntektsmelding.Avsendersystem.LPS
import no.nav.helse.hendelser.Inntektsmelding.Avsendersystem.Nav
import no.nav.helse.hendelser.Inntektsmelding.Avsendersystem.NavSelvbestemt
import no.nav.helse.spleis.meldinger.model.InntektsmeldingMessage.Companion.tilAvsendersystem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class InntektsmeldingMessageTest {

    @Test
    fun `mapper avsendersystem fra json`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val inntektsdato = LocalDate.EPOCH
        assertEquals(LPS,  objectMapper.nullNode().tilAvsendersystem(null, null))
        assertEquals(LPS, objectMapper.missingNode().tilAvsendersystem(null, null))
        assertEquals(LPS, objectMapper.readTree(mangler).tilAvsendersystem(null, null))
        assertEquals(LPS, objectMapper.readTree(sattTilNull).tilAvsendersystem(null, null))
        assertEquals(Nav(vedtaksperiodeId, inntektsdato), objectMapper.readTree(navNo).tilAvsendersystem(vedtaksperiodeId, inntektsdato))
        assertEquals(NavSelvbestemt(vedtaksperiodeId, inntektsdato), objectMapper.readTree(navNoSelvbestemt).tilAvsendersystem(vedtaksperiodeId, inntektsdato))
        assertEquals(Altinn, objectMapper.readTree(altinn).tilAvsendersystem(null, null))
        assertEquals(LPS, objectMapper.readTree(hvaSomHelst).tilAvsendersystem(null, null))
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