package no.nav.helse.hendelser

import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.person.Aktivitetslogger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class YtelserTest {

    @Test
    fun `siste fraværsdato fra tom sykepengehistorikk`() {
        val ytelser = ytelser()
        assertNull(ytelser.sykepengehistorikk().sisteFraværsdag())
    }

    @Test
    fun `startdato fra sykepengehistorikk med én periode`() {
        val ytelser = ytelser(
            utbetalinger = listOf(
                Triple(1.juni, 2.juni, 1000)
            )
        )

        assertEquals(2.juni, ytelser.sykepengehistorikk().sisteFraværsdag())
    }

    @Test
    fun `siste fraværsdag fra sykepengehistorikk med flere periode`() {
        val ytelser = ytelser(
            utbetalinger = listOf(
                Triple(1.juni, 2.juni, 1000),
                Triple(1.juni, 2.juli, 1200)
            )
        )

        assertEquals(2.juli, ytelser.sykepengehistorikk().sisteFraværsdag())
    }

    @Test
    fun `tidslinje fra sykepengehistorikk med overlappende perioder`() {
        val sykepengehistorikkHendelse = ytelser(
            utbetalinger = listOf(
                Triple(1.juni, 2.juni, 1000),
                Triple(1.juni, 3.juni, 1200)
            )
        )

        assertEquals(3.juni, sykepengehistorikkHendelse.sykepengehistorikk().sisteFraværsdag())
    }

    private fun ytelser(
        utbetalinger: List<Triple<LocalDate, LocalDate, Int>> = listOf(),
        foreldrepenger: Periode? = null,
        svangerskapspenger: Periode? = null
    ) = ModelYtelser(
        hendelseId = UUID.randomUUID(),
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        sykepengehistorikk = ModelSykepengehistorikk(
            utbetalinger = utbetalinger.map {
                ModelSykepengehistorikk.Periode.RefusjonTilArbeidsgiver(
                    it.first,
                    it.second,
                    it.third
                )
            },
            inntektshistorikk = emptyList(),
            aktivitetslogger = Aktivitetslogger()
        ),
        foreldrepenger = ModelForeldrepenger(foreldrepenger, svangerskapspenger, Aktivitetslogger()),
        rapportertdato = LocalDateTime.now(),
        originalJson = "{}",
        aktivitetslogger = Aktivitetslogger()
    )

    private companion object {
        private val organisasjonsnummer = "123456789"
        private val aktørId = "987654321"
        private val fødselsnummer = "98765432111"
        private val vedtaksperiodeId = UUID.randomUUID()
    }
}
