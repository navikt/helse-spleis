package no.nav.helse.spleis.mediator.meldinger

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.Inntektsmelding.Avsendersystem.ALTINN
import no.nav.helse.hendelser.Inntektsmelding.Avsendersystem.LPS
import no.nav.helse.hendelser.Inntektsmelding.Avsendersystem.NAV_NO
import no.nav.helse.hendelser.Inntektsmelding.Avsendersystem.NAV_NO_SELVBESTEMT
import no.nav.helse.spleis.meldinger.model.InntektsmeldingMessage.Companion.tilAvsendersystem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class InntektsmeldingMessageTest {

    @Test
    fun `mapper avsendersystem fra json`() {
        assertNull(objectMapper.nullNode().tilAvsendersystem())
        assertNull(objectMapper.missingNode().tilAvsendersystem())
        assertNull(objectMapper.readTree(mangler).tilAvsendersystem())
        assertNull(objectMapper.readTree(sattTilNull).tilAvsendersystem())
        assertEquals(NAV_NO, objectMapper.readTree(navNo).tilAvsendersystem())
        assertEquals(NAV_NO_SELVBESTEMT, objectMapper.readTree(navNoSelvbestemt).tilAvsendersystem())
        assertEquals(ALTINN, objectMapper.readTree(altinn).tilAvsendersystem())
        assertEquals(LPS, objectMapper.readTree(hvaSomHelst).tilAvsendersystem())
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