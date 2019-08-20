package no.nav.helse.sykmelding.domain

import no.nav.helse.sakskompleks.domain.periode
import no.nav.helse.sakskompleks.domain.sykmelding
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SykmeldingKtTest {

    @Test
    fun `sykmeldingen gjelder fra syketilfelleStartDato om denne er før periodene i sykmeldingen`() {
        val sykmelding = sykmelding(
            syketilfelleStartDato = LocalDate.of(2019, 5, 1),
            perioder = listOf(
                periode(fom = LocalDate.of(2019, 5, 14), tom = LocalDate.of(2019, 5, 24)),
                periode(fom = LocalDate.of(2019, 5, 7), tom = LocalDate.of(2019, 5, 13))
            )
        )

        assertEquals(LocalDate.of(2019, 5, 1), sykmelding.gjelderFra())
    }

    @Test
    fun `sykmeldingen gjelder fra tidligste FOM i periodene om ikke syketilfelleStartDato er før`() {
        val sykmelding = sykmelding(
            syketilfelleStartDato = LocalDate.of(2019, 5, 7),
            perioder = listOf(
                periode(fom = LocalDate.of(2019, 5, 18), tom = LocalDate.of(2019, 5, 25)),
                periode(fom = LocalDate.of(2019, 5, 1), tom = LocalDate.of(2019, 5, 13)),
                periode(fom = LocalDate.of(2019, 5, 14), tom = LocalDate.of(2019, 5, 17))
            )
        )

        assertEquals(LocalDate.of(2019, 5, 1), sykmelding.gjelderFra())
    }
}
