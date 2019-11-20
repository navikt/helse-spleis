package no.nav.helse.unit.person.hendelser.sykepengehistorikk

import no.nav.helse.SpolePeriode
import no.nav.helse.TestConstants.sykepengehistorikk
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.hendelser.sykepengehistorikk.SykepengehistorikkHendelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.*

internal class SykepengehistorikkHendelseTest {

    @Test
    fun `siste fraværsdato fra tom sykepengehistorikk`() {
        val sykepengehistorikkHendelse = SykepengehistorikkHendelse(sykepengehistorikk(
                perioder = emptyList(),
                organisasjonsnummer = organisasjonsnummer,
                aktørId = aktørId,
                vedtaksperiodeId = vedtaksperiodeId
        ))

        assertNull(sykepengehistorikkHendelse.sisteFraværsdag())
    }

    @Test
    fun `startdato fra sykepengehistorikk med én periode`() {
        val sykepengehistorikkHendelse = SykepengehistorikkHendelse(sykepengehistorikk(
                perioder = listOf(
                        SpolePeriode(1.juni, 2.juni, "100")
                ),
                organisasjonsnummer = organisasjonsnummer,
                aktørId = aktørId,
                vedtaksperiodeId = vedtaksperiodeId
        ))

        assertEquals(2.juni, sykepengehistorikkHendelse.sisteFraværsdag())
    }

    @Test
    fun `siste fraværsdag fra sykepengehistorikk med flere periode`() {
        val sykepengehistorikkHendelse = SykepengehistorikkHendelse(sykepengehistorikk(
                perioder = listOf(
                        SpolePeriode(1.juni, 2.juni, "100"),
                        SpolePeriode(1.juli, 2.juli, "100")
                ),
                organisasjonsnummer = organisasjonsnummer,
                aktørId = aktørId,
                vedtaksperiodeId = vedtaksperiodeId
        ))

        assertEquals(2.juli, sykepengehistorikkHendelse.sisteFraværsdag())
    }

    @Test
    fun `tidslinje fra sykepengehistorikk med overlappende perioder`() {
        val sykepengehistorikkHendelse = SykepengehistorikkHendelse(sykepengehistorikk(
                perioder = listOf(
                        SpolePeriode(1.juni, 2.juni, "100"),
                        SpolePeriode(1.juni, 3.juni, "100")
                ),
                organisasjonsnummer = organisasjonsnummer,
                aktørId = aktørId,
                vedtaksperiodeId = vedtaksperiodeId
        ))

        assertEquals(3.juni, sykepengehistorikkHendelse.sisteFraværsdag())
    }

    private companion object {
        private val organisasjonsnummer = "123456789"
        private val aktørId = "987654321"
        private val vedtaksperiodeId = UUID.randomUUID()
    }
}
