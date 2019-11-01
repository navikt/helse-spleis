package no.nav.helse.sykdomstidslinje

import java.math.BigDecimal

private val ARBEIDSGIVERPERIODE = 16

internal class SyketilfelleSplitter(private val dagsats: BigDecimal) : SykdomstidslinjeVisitor {

    private var state: BetalingsState = Arbeidsgiverperiode
    private val betalingslinje = mutableListOf<Betalingslinje>()

    fun results(): List<Betalingslinje> {
        return betalingslinje.toList()
    }

    abstract class BetalingsState

    private object Arbeidsgiverperiode : BetalingsState()
    private object IkkeSyk : BetalingsState()
    private object Syk : BetalingsState()
    private object Ugyldig : BetalingsState()


}

