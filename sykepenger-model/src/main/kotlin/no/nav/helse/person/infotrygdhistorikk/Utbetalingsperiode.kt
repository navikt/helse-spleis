package no.nav.helse.person.infotrygdhistorikk

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.InfotrygdhistorikkVisitor
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingstidslinje.Historie
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import java.util.*

class Utbetalingsperiode(
    private val orgnr: String,
    periode: Periode,
    private val grad: Prosentdel,
    private val inntekt: Inntekt
) : Infotrygdperiode(periode) {
    override fun sykdomstidslinje(kilde: SykdomstidslinjeHendelse.Hendelseskilde): Sykdomstidslinje {
        return Sykdomstidslinje.sykedager(start, endInclusive, grad, kilde)
    }

    override fun utbetalingstidslinje() =
        Utbetalingstidslinje().also { utbetalingstidslinje ->
            this.forEach { dag -> nyDag(utbetalingstidslinje, dag) }
        }

    private fun nyDag(utbetalingstidslinje: Utbetalingstidslinje, dato: LocalDate) {
        if (dato.erHelg()) utbetalingstidslinje.addHelg(dato, Økonomi.sykdomsgrad(grad).inntekt(Inntekt.INGEN, skjæringstidspunkt = dato))
        else utbetalingstidslinje.addNAVdag(dato, Økonomi.sykdomsgrad(grad).inntekt(inntekt, skjæringstidspunkt = dato))
    }

    override fun append(bøtte: Historie.Historikkbøtte, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        bøtte.add(orgnr, sykdomstidslinje(kilde))
        bøtte.add(orgnr, utbetalingstidslinje())
    }

    override fun accept(visitor: InfotrygdhistorikkVisitor) {
        visitor.visitInfotrygdhistorikkUtbetalingsperiode(orgnr, this, grad, inntekt)
    }

    override fun valider(aktivitetslogg: IAktivitetslogg, periode: Periode) {
        validerOverlapp(aktivitetslogg, periode)
    }

    override fun validerOverlapp(aktivitetslogg: IAktivitetslogg, periode: Periode) {
        if (!overlapperMed(periode)) return
        aktivitetslogg.info("Utbetaling i Infotrygd %s til %s overlapper med vedtaksperioden", start, endInclusive)
        aktivitetslogg.error("Utbetaling i Infotrygd overlapper med vedtaksperioden")
    }

    override fun gjelder(orgnummer: String) = orgnummer == this.orgnr

    override fun hashCode() =
        Objects.hash(orgnr, start, endInclusive, grad, inntekt)

    override fun equals(other: Any?): Boolean {
        if (other !is Utbetalingsperiode) return false
        return this.orgnr == other.orgnr && this.start == other.start
            && this.grad == other.grad && this.inntekt == other.inntekt
    }
}
