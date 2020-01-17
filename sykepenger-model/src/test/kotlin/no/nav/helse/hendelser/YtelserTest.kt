package no.nav.helse.hendelser

import no.nav.helse.SpolePeriode
import no.nav.helse.TestConstants.sykepengehistorikk
import no.nav.helse.TestConstants.ytelser
import no.nav.helse.juli
import no.nav.helse.juni
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.*

internal class YtelserTest {

    @Test
    fun `siste fraværsdato fra tom sykepengehistorikk`() {
        val ytelser = ytelser(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            sykepengehistorikk = sykepengehistorikk()
        )

        assertNull(ytelser.sykepengehistorikk().sisteFraværsdag())
    }

    @Test
    fun `startdato fra sykepengehistorikk med én periode`() {
        val ytelser = ytelser(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            sykepengehistorikk = sykepengehistorikk(
                perioder = listOf(
                    SpolePeriode(1.juni, 2.juni, "100")
                )
            )
        )

        assertEquals(2.juni, ytelser.sykepengehistorikk().sisteFraværsdag())
    }

    @Test
    fun `siste fraværsdag fra sykepengehistorikk med flere periode`() {
        val sykepengehistorikkHendelse = ytelser(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            sykepengehistorikk = sykepengehistorikk(
                perioder = listOf(
                    SpolePeriode(1.juni, 2.juni, "100"),
                    SpolePeriode(1.juli, 2.juli, "100")
                )
            )
        )

        assertEquals(2.juli, sykepengehistorikkHendelse.sykepengehistorikk().sisteFraværsdag())
    }

    @Test
    fun `tidslinje fra sykepengehistorikk med overlappende perioder`() {
        val sykepengehistorikkHendelse = ytelser(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            sykepengehistorikk = sykepengehistorikk(
                perioder = listOf(
                    SpolePeriode(1.juni, 2.juni, "100"),
                    SpolePeriode(1.juni, 3.juni, "100")
                )
            )
        )

        assertEquals(3.juni, sykepengehistorikkHendelse.sykepengehistorikk().sisteFraværsdag())
    }

    private companion object {
        private val organisasjonsnummer = "123456789"
        private val aktørId = "987654321"
        private val fødselsnummer = "98765432111"
        private val vedtaksperiodeId = UUID.randomUUID()
    }
}
