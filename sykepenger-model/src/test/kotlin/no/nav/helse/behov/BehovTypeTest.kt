package no.nav.helse.behov

import no.nav.helse.person.Vedtaksperiodekontekst
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.januar
import no.nav.helse.utbetalingstidslinje.Utbetalingslinje
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

internal class BehovTypeTest {

    @Test
    internal fun `samme kontekst er lik`() {
        val id = UUID.randomUUID()
        assertEquals(vedtaksperiodekontekst(id), vedtaksperiodekontekst(id))
    }

    @Test
    internal fun `grupperer behov etter vedtaksperiode`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()

        val behov = listOf(
            BehovType.EgenAnsatt(vedtaksperiodekontekst(vedtaksperiodeId1)),
            BehovType.Opptjening(vedtaksperiodekontekst(vedtaksperiodeId1)),
            BehovType.Inntektsberegning(vedtaksperiodekontekst(vedtaksperiodeId1), YearMonth.now(), YearMonth.now()),
            BehovType.Sykepengehistorikk(vedtaksperiodekontekst(vedtaksperiodeId2), LocalDate.now()),
            BehovType.Foreldrepenger(vedtaksperiodekontekst(vedtaksperiodeId2))
        ).partisjoner()

        assertEquals(2, behov.size)
        assertEquals(3, (behov.first()["@behov"] as List<*>).size)
        assertEquals(vedtaksperiodeId1, behov.first()["vedtaksperiodeId"])
        assertEquals(2, (behov.last()["@behov"] as List<*>).size)
        assertEquals(vedtaksperiodeId2, behov.last()["vedtaksperiodeId"])
    }

    @Test
    internal fun `kombinerer behov til map`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val beregningStart = YearMonth.of(2020, 1)
        val beregningSlutt = YearMonth.of(2020, 2)

        val behov = listOf(
            BehovType.EgenAnsatt(vedtaksperiodekontekst(vedtaksperiodeId)),
            BehovType.Opptjening(vedtaksperiodekontekst(vedtaksperiodeId)),
            BehovType.Inntektsberegning(vedtaksperiodekontekst(vedtaksperiodeId), beregningStart, beregningSlutt)
        ).partisjoner().first()

        assertEquals("aktørId", behov["aktørId"])
        assertEquals("fnr", behov["fødselsnummer"])
        assertEquals("orgnr", behov["organisasjonsnummer"])
        assertEquals(vedtaksperiodeId, behov["vedtaksperiodeId"])
        assertEquals(beregningStart, behov["beregningStart"]) { "$behov" }
        assertEquals(beregningSlutt, behov["beregningSlutt"])
    }

    @Test
    internal fun `kan ikke kombinere behov med samme samme nøkler, med ulik verdi`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val beregningStart = YearMonth.of(2020, 1)
        val beregningSlutt = YearMonth.of(2020, 2)

        val context = vedtaksperiodekontekst(vedtaksperiodeId)
        val beregning1 = BehovType.Inntektsberegning(context, beregningStart, beregningSlutt)
        val beregning2 = BehovType.Inntektsberegning(context, beregningStart.plusYears(1), beregningSlutt.plusYears(1))

        assertDoesNotThrow { listOf(beregning1, beregning1).partisjoner() }
        assertThrows<IllegalStateException> { listOf(beregning1, beregning2).partisjoner() }
    }

    @Test
    internal fun `konverterer et utbetalingsbehov til et map`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val aktørId = "aktørId"
        val fødselsnummer = "fnr"
        val organisasjonsnummer = "orgnummer"
        val vedtaksperiodekontekst = Vedtaksperiodekontekst(aktørId, fødselsnummer,
            organisasjonsnummer, vedtaksperiodeId)
        val utbetalingsreferanse = "yes"
        val maksdato = 31.desember
        val saksbehandler = "no"

        val fom = 1.januar
        val tom = 2.januar
        val dagsats = 100

        val values = listOf(
            BehovType.Utbetaling(
                context = vedtaksperiodekontekst,
                utbetalingsreferanse = utbetalingsreferanse,
                utbetalingslinjer = listOf(Utbetalingslinje(fom, tom, dagsats)),
                maksdato = maksdato,
                saksbehandler = saksbehandler
            )
        ).partisjoner().first()

        assertEquals(
            mapOf(
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer,
                "organisasjonsnummer" to organisasjonsnummer,
                "vedtaksperiodeId" to vedtaksperiodeId,
                "@behov" to listOf("Utbetaling"),
                "utbetalingsreferanse" to utbetalingsreferanse,
                "utbetalingslinjer" to listOf(
                    mapOf(
                        "fom" to fom,
                        "tom" to tom,
                        "dagsats" to dagsats
                    )
                ),
                "maksdato" to maksdato,
                "saksbehandler" to saksbehandler
            ), values
        )
    }

    private fun vedtaksperiodekontekst(vedtaksperiodeId: UUID) =
        Vedtaksperiodekontekst("aktørId", "fnr", "orgnr", vedtaksperiodeId)
}
