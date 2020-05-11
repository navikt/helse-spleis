package no.nav.helse.tournament

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.sykdomstidslinje.NyDag.*
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde.Companion.INGEN
import no.nav.helse.tournament.Turneringsnøkkel.*
import no.nav.helse.tournament.Turneringsnøkkel.Companion.fraDag
import no.nav.helse.tournament.TurneringsnøkkelTest.TestHendelse.Companion.aareg
import no.nav.helse.tournament.TurneringsnøkkelTest.TestHendelse.Companion.inntektsmelding
import no.nav.helse.tournament.TurneringsnøkkelTest.TestHendelse.Companion.sykmelding
import no.nav.helse.tournament.TurneringsnøkkelTest.TestHendelse.Companion.søknad
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class TurneringsnøkkelTest {
    private val enDag = LocalDate.now()
    private val grad = 100.0

    @Test
    internal fun turneringsnøkler() {
        assertEquals(Arbeidsdag_IM, fraDag(NyArbeidsdag(enDag, inntektsmelding)))
        assertEquals(Arbeidsdag_SØ, fraDag(NyArbeidsdag(enDag, søknad)))
        assertEquals(Arbeidsgiverdag_IM, fraDag(NyArbeidsgiverdag(enDag, 100, inntektsmelding)))
        assertEquals(Arbeidsgiverdag_SØ, fraDag(NyArbeidsgiverdag(enDag, 100, søknad)))
        assertEquals(Arbeidsgiverdag_IM, fraDag(NyArbeidsgiverHelgedag(enDag, 100, inntektsmelding)))
        assertEquals(Arbeidsgiverdag_SØ, fraDag(NyArbeidsgiverHelgedag(enDag, 100, søknad)))
        assertEquals(ForeldetSykedag, fraDag(NyForeldetSykedag(enDag, 100, inntektsmelding)))
        assertEquals(ForeldetSykedag, fraDag(NyForeldetSykedag(enDag, 100, søknad)))
        assertEquals(Feriedag_IM, fraDag(NyFeriedag(enDag, inntektsmelding)))
        assertEquals(Feriedag_SØ, fraDag(NyFeriedag(enDag, søknad)))
        assertEquals(Feriedag_IM, fraDag(NyFriskHelgedag(enDag, inntektsmelding)))
        assertEquals(Feriedag_SØ, fraDag(NyFriskHelgedag(enDag, søknad)))
        assertEquals(UkjentDag, fraDag(NyUkjentDag(enDag, INGEN)))
        assertEquals(UbestemtDag, fraDag(ProblemDag(enDag, INGEN, "")))
        assertEquals(Permisjonsdag_AAREG, fraDag(NyPermisjonsdag(enDag, aareg)))
        assertEquals(Permisjonsdag_SØ, fraDag(NyPermisjonsdag(enDag, søknad)))
        assertEquals(Turneringsnøkkel.Studiedag, fraDag(NyStudiedag(enDag, søknad)))
        assertEquals(Sykedag_SM, fraDag(NySykedag(enDag, grad, sykmelding)))
        assertEquals(Sykedag_SØ, fraDag(NySykedag(enDag, grad, søknad)))
        assertEquals(SykHelgedag_SM, fraDag(NySykHelgedag(enDag, grad, sykmelding)))
        assertEquals(SykHelgedag_SØ, fraDag(NySykHelgedag(enDag, grad, søknad)))
        assertEquals(Turneringsnøkkel.Utenlandsdag, fraDag(NyUtenlandsdag(enDag, søknad)))
    }

    private sealed class TestHendelse() : SykdomstidslinjeHendelse(UUID.randomUUID()) {
        companion object {
            val søknad = Søknad.kilde
            val inntektsmelding = Inntektsmelding.kilde
            val sykmelding = Sykmelding.kilde
            val aareg = Aareg.kilde
        }

        // Objects impersonating real-life sources of sickness timeline days
        object Inntektsmelding : TestHendelse()
        object Sykmelding : TestHendelse()
        object Søknad : TestHendelse()
        object Aareg : TestHendelse() // Dette er ren spekulasjon omkring AAreg som kilde

        override fun sykdomstidslinje(tom: LocalDate): Sykdomstidslinje = TODO()
        override fun sykdomstidslinje(): Sykdomstidslinje = TODO()
        override fun valider(periode: Periode): Aktivitetslogg = TODO()
        override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) = TODO()
        override fun aktørId(): String = TODO()
        override fun fødselsnummer(): String = TODO()
        override fun organisasjonsnummer(): String = TODO()
    }
}
