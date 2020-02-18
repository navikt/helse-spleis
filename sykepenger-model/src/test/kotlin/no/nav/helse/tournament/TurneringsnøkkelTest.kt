package no.nav.helse.tournament

import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.tournament.Turneringsnøkkel.*
import no.nav.helse.tournament.Turneringsnøkkel.Companion.fraDag
import no.nav.helse.tournament.Turneringsnøkkel.Undecided
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class TurneringsnøkkelTest {
    private val enDag = LocalDate.now()

    @Test
    internal fun turneringsnøkkel() {
        assertEquals(WD_A, fraDag(Arbeidsdag.Søknad(enDag)))
        assertEquals(WD_IM, fraDag(Arbeidsdag.Inntektsmelding(enDag)))
        assertEquals(SRD_A, fraDag(Egenmeldingsdag.Søknad(enDag)))
        assertEquals(SRD_IM, fraDag(Egenmeldingsdag.Inntektsmelding(enDag)))
        assertEquals(V_A, fraDag(Feriedag.Søknad(enDag)))
        assertEquals(V_IM, fraDag(Feriedag.Inntektsmelding(enDag)))
        assertEquals(I, fraDag(ImplisittDag(enDag)))
        assertEquals(Le_A, fraDag(Permisjonsdag.Søknad(enDag)))
        assertEquals(Le_Areg, fraDag(Permisjonsdag.Aareg(enDag)))
        assertEquals(EDU, fraDag(Studiedag(enDag)))
        assertEquals(S_A, fraDag(Sykedag.Søknad(enDag)))
        assertEquals(S_SM, fraDag(Sykedag.Sykmelding(enDag)))
        assertEquals(SW, fraDag(SykHelgedag(enDag)))
        assertEquals(Undecided, fraDag(Ubestemtdag(enDag)))
        assertEquals(DA, fraDag(Utenlandsdag(enDag)))
    }
}
