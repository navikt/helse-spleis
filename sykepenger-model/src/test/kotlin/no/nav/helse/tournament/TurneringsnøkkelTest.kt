package no.nav.helse.tournament

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde.Companion.INGEN
import no.nav.helse.tournament.Turneringsnøkkel.*
import no.nav.helse.tournament.Turneringsnøkkel.Companion.fraDag
import no.nav.helse.tournament.TurneringsnøkkelTest.TestHendelse.Companion.aareg
import no.nav.helse.tournament.TurneringsnøkkelTest.TestHendelse.Companion.inntektsmelding
import no.nav.helse.tournament.TurneringsnøkkelTest.TestHendelse.Companion.sykmelding
import no.nav.helse.tournament.TurneringsnøkkelTest.TestHendelse.Companion.søknad
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class TurneringsnøkkelTest {
    private val enDag = LocalDate.now()
    private val økonomi = Økonomi.sykdomsgrad(100.prosent)

    @Test
    fun turneringsnøkler() {
        assertEquals(Arbeidsdag_IM, fraDag(Arbeidsdag(enDag, inntektsmelding)))
        assertEquals(Arbeidsdag_SØ, fraDag(Arbeidsdag(enDag, søknad)))
        assertEquals(Arbeidsgiverdag_IM, fraDag(Arbeidsgiverdag(enDag, økonomi, inntektsmelding)))
        assertEquals(Arbeidsgiverdag_SØ, fraDag(Arbeidsgiverdag(enDag, økonomi, søknad)))
        assertEquals(ArbeidsgiverHelgedag_IM, fraDag(ArbeidsgiverHelgedag(enDag, økonomi, inntektsmelding)))
        assertEquals(ArbeidsgiverHelgedag_SØ, fraDag(ArbeidsgiverHelgedag(enDag, økonomi, søknad)))
        assertEquals(Turneringsnøkkel.ForeldetSykedag, fraDag(Dag.ForeldetSykedag(enDag, økonomi, inntektsmelding)))
        assertEquals(Turneringsnøkkel.ForeldetSykedag, fraDag(Dag.ForeldetSykedag(enDag, økonomi, søknad)))
        assertEquals(Feriedag_IM, fraDag(Feriedag(enDag, inntektsmelding)))
        assertEquals(Feriedag_SØ, fraDag(Feriedag(enDag, søknad)))
        assertEquals(Feriedag_IM, fraDag(FriskHelgedag(enDag, inntektsmelding)))
        assertEquals(Feriedag_SØ, fraDag(FriskHelgedag(enDag, søknad)))
        assertEquals(Turneringsnøkkel.UkjentDag, fraDag(Dag.UkjentDag(enDag, INGEN)))
        assertEquals(UbestemtDag, fraDag(ProblemDag(enDag, INGEN, "")))
        assertEquals(Permisjonsdag_AAREG, fraDag(Permisjonsdag(enDag, aareg)))
        assertEquals(Permisjonsdag_SØ, fraDag(Permisjonsdag(enDag, søknad)))
        assertEquals(Sykedag_SM, fraDag(Sykedag(enDag, økonomi, sykmelding)))
        assertEquals(Sykedag_SØ, fraDag(Sykedag(enDag, økonomi, søknad)))
        assertEquals(SykHelgedag_SM, fraDag(SykHelgedag(enDag, økonomi, sykmelding)))
        assertEquals(SykHelgedag_SØ, fraDag(SykHelgedag(enDag, økonomi, søknad)))
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
