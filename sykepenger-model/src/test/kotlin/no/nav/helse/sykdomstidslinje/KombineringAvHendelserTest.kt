package no.nav.helse.sykdomstidslinje

import no.nav.helse.sykdomstidslinje.dag.Egenmeldingsdag
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class KombineringAvHendelserTest {

    @Test
    internal fun `egenmelding oppgitt i søknad overskrives av arbeidsdager fra inntektsmelding`() {
        val søknadHendelse =
            søknad(NySøknadHendelseWrapper) {
                fom = Uke(1).torsdag
                tom = Uke(4).mandag
                søknadsperiode {
                    fom = Uke(1).torsdag
                    tom = Uke(4).mandag
                }
                egenmelding {
                    fom = Uke(1).mandag
                    tom = Uke(1).onsdag
                }
            }

        val inntektsmeldingHendelse = inntektsmelding {
            arbeidsgiverperiode {
                fom = Uke(1).torsdag
                tom = Uke(3).fredag
            }
        }.asHendelse()

        val resultat = søknadHendelse.sykdomstidslinje() + inntektsmeldingHendelse.sykdomstidslinje()
        assertTrue(resultat.flatten().take(16).all { dag -> dag !is Egenmeldingsdag })
        assertEquals(Uke(1).torsdag, resultat.utgangspunktForBeregningAvYtelse())
    }
}
