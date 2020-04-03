package no.nav.helse.tournament

import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.sykdomstidslinje.dag.ImplisittDag
import no.nav.helse.sykdomstidslinje.dag.Studiedag
import no.nav.helse.sykdomstidslinje.dag.Utenlandsdag
import no.nav.helse.tournament.Turneringsnøkkel.*
import no.nav.helse.tournament.Turneringsnøkkel.Companion.fraDag
import no.nav.helse.tournament.Turneringsnøkkel.UbestemtDag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class TurneringsnøkkelTest {
    private val enDag = LocalDate.now()
    private val grad = 100.0

    @Test
    internal fun turneringsnøkkel() {
        assertEquals(Arbeidsdag_SØ, fraDag(Arbeidsdag.Søknad(enDag)))
        assertEquals(Arbeidsdag_IM, fraDag(Arbeidsdag.Inntektsmelding(enDag)))
        assertEquals(Egenmeldingsdag_SØ, fraDag(Egenmeldingsdag.Søknad(enDag)))
        assertEquals(Egenmeldingsdag_IM, fraDag(Egenmeldingsdag.Inntektsmelding(enDag)))
        assertEquals(Feriedag_SØ, fraDag(Feriedag.Søknad(enDag)))
        assertEquals(Feriedag_IM, fraDag(Feriedag.Inntektsmelding(enDag)))
        assertEquals(Turneringsnøkkel.ImplisittDag, fraDag(ImplisittDag(enDag)))
        assertEquals(Permisjonsdag_SØ, fraDag(Permisjonsdag.Søknad(enDag)))
        assertEquals(Permisjonsdag_AAREG, fraDag(Permisjonsdag.Aareg(enDag)))
        assertEquals(Turneringsnøkkel.Studiedag, fraDag(Studiedag(enDag)))
        assertEquals(Sykedag_SØ, fraDag(Sykedag.Søknad(enDag, grad)))
        assertEquals(Sykedag_SM, fraDag(Sykedag.Sykmelding(enDag, grad)))
        assertEquals(SykHelgedag_SM, fraDag(SykHelgedag.Sykmelding(enDag, grad)))
        assertEquals(SykHelgedag_SØ, fraDag(SykHelgedag.Søknad(enDag, grad)))
        assertEquals(UbestemtDag, fraDag(Ubestemtdag(enDag)))
        assertEquals(Turneringsnøkkel.Utenlandsdag, fraDag(Utenlandsdag(enDag)))
    }
}
