package no.nav.helse.tournament

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.dag.*

internal enum class Turneringsnøkkel {
    I,
    WD_A,
    WD_IM,
    S_SM,
    S_A,
    V_A,
    V_IM,
    Le_Areg,
    Le_A,
    SW,
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

            override fun visitArbeidsdag(arbeidsdag: Arbeidsdag.Inntektsmelding) {
                turneringsnøkkel = WD_IM
            }

            override fun visitArbeidsdag(arbeidsdag: Arbeidsdag.Søknad) {
                turneringsnøkkel = WD_A
            }

            override fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag.Inntektsmelding) {
                turneringsnøkkel = SRD_IM
            }

            override fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag.Søknad) {
                turneringsnøkkel = SRD_A
            }

            override fun visitFeriedag(feriedag: Feriedag.Inntektsmelding) {
                turneringsnøkkel = V_IM
            }

            override fun visitFeriedag(feriedag: Feriedag.Søknad) {
                turneringsnøkkel = V_A
            }

            override fun visitImplisittDag(implisittDag: ImplisittDag) {
                turneringsnøkkel = I
            }

            override fun visitPermisjonsdag(permisjonsdag: Permisjonsdag.Søknad) {
                turneringsnøkkel = Le_A
            }

            override fun visitPermisjonsdag(permisjonsdag: Permisjonsdag.Aareg) {
                turneringsnøkkel = Le_Areg
            }

            override fun visitStudiedag(studiedag: Studiedag) {
                turneringsnøkkel = EDU
            }

            override fun visitSykHelgedag(sykHelgedag: SykHelgedag) {
                turneringsnøkkel = SW
            }

            override fun visitSykedag(sykedag: Sykedag.Sykmelding) {
                turneringsnøkkel = S_SM
            }

            override fun visitSykedag(sykedag: Sykedag.Søknad) {
                turneringsnøkkel = S_A
            }

            override fun visitUbestemt(ubestemtdag: Ubestemtdag) {
                turneringsnøkkel = Undecided
            }
            override fun visitUtenlandsdag(utenlandsdag: Utenlandsdag) {
                turneringsnøkkel = DA
            }

        }
    }

}
