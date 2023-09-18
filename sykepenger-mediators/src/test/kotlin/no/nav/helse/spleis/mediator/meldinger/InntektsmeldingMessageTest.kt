package no.nav.helse.spleis.mediator.meldinger

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.spleis.meldinger.model.InntektsmeldingMessage.Companion.tilAvsendersystem
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class InntektsmeldingMessageTest {

    @Test
    fun `mapper avsendersystem fra json`() {
        assertNull(objectMapper.nullNode().tilAvsendersystem())
        assertNull(objectMapper.missingNode().tilAvsendersystem())
        assertNull(objectMapper.readTree(mangler).tilAvsendersystem())
        assertNull(objectMapper.readTree(sattTilNull).tilAvsendersystem())
        assertNull(objectMapper.readTree(navNo).tilAvsendersystem())
        assertNull(objectMapper.readTree(altinn).tilAvsendersystem())
        assertNull(objectMapper.readTree(hvaSomHelst).tilAvsendersystem())

    }

    private companion object {
        val objectMapper = jacksonObjectMapper()
        val mangler = """{}"""
        val sattTilNull = """{ "navn": null }"""
        val navNo = """{ "navn": "NAV_NO" }"""
        val altinn = """{ "navn": "AltinnPortal" }"""
        val hvaSomHelst = """{ "navn": "HvaSomHelst" }"""
    }
}