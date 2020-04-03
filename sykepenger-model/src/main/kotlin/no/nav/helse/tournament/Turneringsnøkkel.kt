package no.nav.helse.tournament

import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.dag.*

internal enum class Turneringsnøkkel {
    ImplisittDag,
    Arbeidsdag_SØ,
    Arbeidsdag_IM,
    Sykedag_SM,
    Sykedag_SØ,
    Kun_arbeidsgiverdag,
    Feriedag_SØ,
    Feriedag_IM,
    Permisjonsdag_AAREG,
    Permisjonsdag_SØ,
    SykHelgedag_SM,
    SykHelgedag_SØ,
    Egenmeldingsdag_IM,
    Egenmeldingsdag_SØ,
    Studiedag,
    AnnenInntekt_INNTK,
    AnnenInntekt_SØ,
    Utenlandsdag,
    UbestemtDag;

    companion object {
        fun fraDag(dag: Dag) = TurneringsnøkkelVisitor(dag).turneringsnøkkel()

        private class TurneringsnøkkelVisitor(private val dag: Dag) : SykdomstidslinjeVisitor {

            private var turneringsnøkkel: Turneringsnøkkel? = null

            init {
                dag.accept(this)
            }

            internal fun turneringsnøkkel() = requireNotNull(turneringsnøkkel) { "Finner ikke turneringsnøkkel for ${dag::class.simpleName}"}

            override fun visitArbeidsdag(dag: Arbeidsdag.Inntektsmelding) {
                turneringsnøkkel = Arbeidsdag_IM
            }

            override fun visitArbeidsdag(dag: Arbeidsdag.Søknad) {
                turneringsnøkkel = Arbeidsdag_SØ
            }

            override fun visitEgenmeldingsdag(dag: Egenmeldingsdag.Inntektsmelding) {
                turneringsnøkkel = Egenmeldingsdag_IM
            }

            override fun visitEgenmeldingsdag(dag: Egenmeldingsdag.Søknad) {
                turneringsnøkkel = Egenmeldingsdag_SØ
            }

            override fun visitFeriedag(dag: Feriedag.Inntektsmelding) {
                turneringsnøkkel = Feriedag_IM
            }

            override fun visitFeriedag(dag: Feriedag.Søknad) {
                turneringsnøkkel = Feriedag_SØ
            }

            override fun visitFriskHelgedag(dag: FriskHelgedag.Inntektsmelding) {
                turneringsnøkkel = Feriedag_IM
            }
            override fun visitFriskHelgedag(dag: FriskHelgedag.Søknad) {
                turneringsnøkkel = Feriedag_SØ
            }

            override fun visitImplisittDag(dag: no.nav.helse.sykdomstidslinje.dag.ImplisittDag) {
                turneringsnøkkel = ImplisittDag
            }

            override fun visitKunArbeidsgiverSykedag(dag: KunArbeidsgiverSykedag) {
                turneringsnøkkel = Kun_arbeidsgiverdag
            }

            override fun visitPermisjonsdag(dag: Permisjonsdag.Søknad) {
                turneringsnøkkel = Permisjonsdag_SØ
            }

            override fun visitPermisjonsdag(dag: Permisjonsdag.Aareg) {
                turneringsnøkkel = Permisjonsdag_AAREG
            }

            override fun visitStudiedag(dag: no.nav.helse.sykdomstidslinje.dag.Studiedag) {
                turneringsnøkkel = Studiedag
            }

            override fun visitSykHelgedag(dag: SykHelgedag.Sykmelding) {
                turneringsnøkkel = SykHelgedag_SM
            }

            override fun visitSykHelgedag(dag: SykHelgedag.Søknad) {
                turneringsnøkkel = SykHelgedag_SØ
            }

            override fun visitSykedag(dag: Sykedag.Sykmelding) {
                turneringsnøkkel = Sykedag_SM
            }

            override fun visitSykedag(dag: Sykedag.Søknad) {
                turneringsnøkkel = Sykedag_SØ
            }

            override fun visitUbestemt(dag: Ubestemtdag) {
                turneringsnøkkel = UbestemtDag
            }
            override fun visitUtenlandsdag(dag: no.nav.helse.sykdomstidslinje.dag.Utenlandsdag) {
                turneringsnøkkel = Utenlandsdag
            }

        }
    }

}
