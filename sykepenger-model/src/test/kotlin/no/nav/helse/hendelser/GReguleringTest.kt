package no.nav.helse.hendelser

import no.nav.helse.Grunnbeløp
import no.nav.helse.testhelpers.april
import no.nav.helse.testhelpers.mai
import no.nav.helse.testhelpers.september
import org.junit.jupiter.api.BeforeEach

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

internal class GReguleringTest {

    private companion object {

        private const val FNR = "12312313"
        private const val AKTØRID = "123123123"
        private const val ORGNR = "123123"
        private val id = UUID.randomUUID()
        private const val FAGSYSTEMID = "123123"
    }

    @Test
    fun `uregulert periode er relevant for regulering`() {
        val virkningFra = 21.september(2020)
        val beregningsdato = 1.mai(2020)
        val gRegulering = GRegulering(id, AKTØRID, FNR, ORGNR, virkningFra, FAGSYSTEMID)
        assertTrue(gRegulering.erRelevant(FAGSYSTEMID, beregningsdato, Grunnbeløp.`1G`.beløp(beregningsdato)))
    }

    @Test
    fun `gammel periode er irrelevant for regulering`() {
        val virkningFra = 21.september(2020)
        val beregningsdato = 30.april(2020)
        val gRegulering = GRegulering(id, AKTØRID, FNR, ORGNR, virkningFra, FAGSYSTEMID)
        assertFalse(gRegulering.erRelevant(FAGSYSTEMID, beregningsdato, Grunnbeløp.`1G`.beløp(beregningsdato)))
    }

    @Test
    fun `uregulert periode er irrelevant for regulering ved ulik fagsystemid`() {
        val virkningFra = 21.september(2020)
        val beregningsdato = 1.mai(2020)
        val gRegulering = GRegulering(id, AKTØRID, FNR, ORGNR, virkningFra, FAGSYSTEMID)
        assertFalse(gRegulering.erRelevant("noetull", beregningsdato, Grunnbeløp.`1G`.beløp(beregningsdato)))
    }

    @Test
    fun `ny periode er irrelevant for regulering`() {
        val virkningFra = 21.september(2020)
        val beregningsdato = 21.september(2020)
        val gRegulering = GRegulering(id, AKTØRID, FNR, ORGNR, virkningFra, FAGSYSTEMID)
        assertFalse(gRegulering.erRelevant(FAGSYSTEMID, beregningsdato, Grunnbeløp.`1G`.beløp(beregningsdato)))
    }

    @Test
    fun `regulert periode er irrelevant for regulering`() {
        val virkningFra = 21.september(2020)
        val beregningsdato = 1.mai(2020)
        val gRegulering = GRegulering(id, AKTØRID, FNR, ORGNR, virkningFra, FAGSYSTEMID)
        assertFalse(gRegulering.erRelevant(FAGSYSTEMID, beregningsdato, Grunnbeløp.`1G`.beløp(beregningsdato, virkningFra)))
    }

}
