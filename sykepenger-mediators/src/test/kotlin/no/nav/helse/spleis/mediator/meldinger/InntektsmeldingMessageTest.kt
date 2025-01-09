package no.nav.helse.spleis.mediator.meldinger

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.januar
import no.nav.helse.spleis.meldinger.model.tilOpphørAvNaturalytelser
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class InntektsmeldingMessageTest {

    @Test
    fun `mapper naturalytelser fra json`() {
        assertEquals(emptyList<Inntektsmelding.OpphørAvNaturalytelse>(), objectMapper.readTree(naturalytelseTom).tilOpphørAvNaturalytelser())
        assertEquals(listOf(Inntektsmelding.OpphørAvNaturalytelse(naturalytelse = "ANNET", beløp = 1200.månedlig, fom = 1.januar)), objectMapper.readTree(naturalytelse).tilOpphørAvNaturalytelser())
        assertThrows<NullPointerException> { objectMapper.readTree(naturalytelseUgyldig).tilOpphørAvNaturalytelser() }
    }

    private companion object {
        val objectMapper = jacksonObjectMapper()
        val naturalytelseTom = """[]"""
        val naturalytelse = """[{"naturalytelse":"ANNET", "beloepPrMnd":"1200.0", "fom":"2018-01-01"}]"""
        val naturalytelseUgyldig = """[{"beloepPrMnd":"1200.0"}]"""
    }
}
