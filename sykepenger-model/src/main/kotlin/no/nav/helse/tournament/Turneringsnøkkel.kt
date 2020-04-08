package no.nav.helse.tournament

import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.dag.*

internal enum class Turneringsnøkkel {
    I,
    WD_A,
    WD_IM,
    S_SM,
    S_A,
    K_A,
    V_A,
    V_IM,
    Le_Areg,
    Le_A,
    SW_SM,
    SW_A,
    SRD_IM,
    SRD_A,
    EDU,
    OI_Int,
    OI_A,
    DA,
    Undecided;

    companion object {
        fun fraDag(dag: Dag) = TurneringsnøkkelVisitor(dag).turneringsnøkkel()

        private class TurneringsnøkkelVisitor(private val dag: Dag) : SykdomstidslinjeVisitor {

            private var turneringsnøkkel: Turneringsnøkkel? = null

            init {
                dag.accept(this)
            }

            internal fun turneringsnøkkel() = requireNotNull(turneringsnøkkel) { "Finner ikke turneringsnøkkel for ${dag::class.simpleName}"}

            override fun visitArbeidsdag(dag: Arbeidsdag.Inntektsmelding) {
                turneringsnøkkel = WD_IM
            }

            override fun visitArbeidsdag(dag: Arbeidsdag.Søknad) {
                turneringsnøkkel = WD_A
            }

            override fun visitEgenmeldingsdag(dag: Egenmeldingsdag.Inntektsmelding) {
                turneringsnøkkel = SRD_IM
            }

            override fun visitEgenmeldingsdag(dag: Egenmeldingsdag.Søknad) {
                turneringsnøkkel = SRD_A
            }

            override fun visitFeriedag(dag: Feriedag.Inntektsmelding) {
                turneringsnøkkel = V_IM
            }

            override fun visitFeriedag(dag: Feriedag.Søknad) {
                turneringsnøkkel = V_A
            }

            override fun visitFriskHelgedag(dag: FriskHelgedag.Inntektsmelding) {
                turneringsnøkkel = V_IM
            }
            override fun visitFriskHelgedag(dag: FriskHelgedag.Søknad) {
                turneringsnøkkel = V_A
            }

            override fun visitImplisittDag(dag: ImplisittDag) {
                turneringsnøkkel = I
            }

            override fun visitKunArbeidsgiverSykedag(dag: KunArbeidsgiverSykedag) {
                turneringsnøkkel = K_A
            }

            override fun visitPermisjonsdag(dag: Permisjonsdag.Søknad) {
                turneringsnøkkel = Le_A
            }

            override fun visitPermisjonsdag(dag: Permisjonsdag.Aareg) {
                turneringsnøkkel = Le_Areg
            }

            override fun visitStudiedag(dag: Studiedag) {
                turneringsnøkkel = EDU
            }

            override fun visitSykHelgedag(dag: SykHelgedag.Sykmelding) {
                turneringsnøkkel = SW_SM
            }

            override fun visitSykHelgedag(dag: SykHelgedag.Søknad) {
                turneringsnøkkel = SW_A
            }

            override fun visitSykedag(dag: Sykedag.Sykmelding) {
                turneringsnøkkel = S_SM
            }

            override fun visitSykedag(dag: Sykedag.Søknad) {
                turneringsnøkkel = S_A
            }

            override fun visitUbestemt(dag: Ubestemtdag) {
                turneringsnøkkel = Undecided
            }
            override fun visitUtenlandsdag(dag: Utenlandsdag) {
                turneringsnøkkel = DA
            }

        }
    }

}
