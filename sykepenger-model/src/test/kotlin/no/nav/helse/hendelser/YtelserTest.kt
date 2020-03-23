package no.nav.helse.hendelser

import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.person.Aktivitetslogg
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class YtelserTest {

    @Test
    fun `siste utbetalte dag fra tom sykepengehistorikk`() {
        val ytelser = ytelser()
        assertNull(ytelser.utbetalingshistorikk().sisteUtbetalteDag())
    }

    @Test
    fun `startdato fra sykepengehistorikk med én periode`() {
        val ytelser = ytelser(
            utbetalinger = listOf(
                Triple(1.juni, 2.juni, 1000)
            )
        )

        assertEquals(2.juni, ytelser.utbetalingshistorikk().sisteUtbetalteDag())
    }

    @Test
    fun `siste utbetalte dag fra sykepengehistorikk med flere periode`() {
        val ytelser = ytelser(
            utbetalinger = listOf(
                Triple(1.juni, 2.juni, 1000),
                Triple(1.juni, 2.juli, 1200)
            )
        )

        assertEquals(2.juli, ytelser.utbetalingshistorikk().sisteUtbetalteDag())
    }

    @Test
    fun `tidslinje fra sykepengehistorikk med overlappende perioder`() {
        val sykepengehistorikkHendelse = ytelser(
            utbetalinger = listOf(
                Triple(1.juni, 2.juni, 1000),
                Triple(1.juni, 3.juni, 1200)
            )
        )

        assertEquals(3.juni, sykepengehistorikkHendelse.utbetalingshistorikk().sisteUtbetalteDag())
    }

    private fun ytelser(
        utbetalinger: List<Triple<LocalDate, LocalDate, Int>> = listOf(),
        foreldrepenger: Periode? = null,
        svangerskapspenger: Periode? = null
    ) = Aktivitetslogg().let {
        Ytelser(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            utbetalingshistorikk = Utbetalingshistorikk(
                harUkjentePerioder = false,
                utbetalinger = utbetalinger.map {
                    Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(
                        it.first,
                        it.second,
                        it.third
                    )
                },
                inntektshistorikk = emptyList(),
                graderingsliste = emptyList(),
                aktivitetslogg = it
            ),
            foreldrepermisjon = Foreldrepermisjon(foreldrepenger, svangerskapspenger, it),
            aktivitetslogg = it
        )
    }
    private companion object {
        private val organisasjonsnummer = "123456789"
        private val aktørId = "987654321"
        private val fødselsnummer = "98765432111"
        private val vedtaksperiodeId = UUID.randomUUID()
    }
}
