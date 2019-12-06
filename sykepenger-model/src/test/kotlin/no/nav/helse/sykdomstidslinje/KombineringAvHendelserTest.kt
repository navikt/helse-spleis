package no.nav.helse.sykdomstidslinje

import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.sykdomstidslinje.dag.Egenmeldingsdag
import no.nav.helse.testhelpers.Uke
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.sykepengesoknad.dto.PeriodeDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class KombineringAvHendelserTest {

    @Test
    internal fun `egenmelding oppgitt i søknad overskrives av arbeidsdager fra inntektsmelding`() {
        val søknadHendelse = sendtSøknadHendelse(
            egenmeldinger = listOf(PeriodeDTO(fom = Uke(1).mandag, tom = Uke(1).onsdag)),
            fravær = emptyList(),
            søknadsperioder = listOf(SoknadsperiodeDTO(fom = Uke(1).torsdag, tom = Uke(4).fredag))
        )
        val inntektsmeldingHendelse = inntektsmeldingHendelse(
            arbeidsgiverperioder = listOf(
                Periode(fom = Uke(1).torsdag, tom = Uke(3).fredag)
            )
        )

        val resultat = søknadHendelse.sykdomstidslinje() + inntektsmeldingHendelse.sykdomstidslinje()
        assertTrue(resultat.flatten().take(16).all { dag -> dag !is Egenmeldingsdag })
        assertEquals(Uke(1).torsdag, resultat.utgangspunktForBeregningAvYtelse())
    }
}
