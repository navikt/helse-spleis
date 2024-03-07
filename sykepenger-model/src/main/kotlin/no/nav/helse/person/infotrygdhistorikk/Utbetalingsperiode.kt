package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Periode
import no.nav.helse.memento.InfotrygdArbeidsgiverutbetalingsperiodeMemento
import no.nav.helse.memento.InfotrygdPersonutbetalingsperiodeMemento
import no.nav.helse.person.InfotrygdperiodeVisitor
import no.nav.helse.person.Vedtaksperiode.Companion.MINIMALT_TILLATT_AVSTAND_TIL_INFOTRYGD
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_3
import no.nav.helse.sykdomstidslinje.SykdomshistorikkHendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Økonomi

abstract class Utbetalingsperiode(
    protected val orgnr: String,
    fom: LocalDate,
    tom: LocalDate,
    protected val grad: Prosentdel,
    protected val inntekt: Inntekt
) : Infotrygdperiode(fom, tom) {
    companion object {
        // inntektbeløpet i Infotrygd-utbetalingene er gradert; justerer derfor "opp igjen"
        fun inntekt(inntekt: Inntekt, grad: Prosentdel) = Inntekt.fraGradert(inntekt, grad)
    }
    override fun sykdomstidslinje(kilde: SykdomshistorikkHendelse.Hendelseskilde): Sykdomstidslinje {
        return Sykdomstidslinje.sykedager(periode.start, periode.endInclusive, grad, kilde)
    }

    override fun utbetalingstidslinje() =
        Utbetalingstidslinje.Builder().apply {
            periode.forEach { dag -> nyDag(this, dag) }
        }.build()

    private fun nyDag(builder: Utbetalingstidslinje.Builder, dato: LocalDate) {
        val økonomi = Økonomi.sykdomsgrad(grad)
        if (dato.erHelg()) return builder.addHelg(dato, økonomi.inntekt(INGEN, `6G` = INGEN, refusjonsbeløp = INGEN))
        builder.addNAVdag(dato, økonomi.inntekt(inntekt, `6G` = INGEN, refusjonsbeløp = INGEN))
    }

    internal fun valider(aktivitetslogg: IAktivitetslogg, organisasjonsnummer: String, periode: Periode) {
        validerHarBetaltTidligere(periode, aktivitetslogg)
        validerOverlapp(aktivitetslogg, periode)
        validerNyereOpplysninger(aktivitetslogg, organisasjonsnummer, periode)
    }


    private fun validerHarBetaltTidligere(periode: Periode, aktivitetslogg: IAktivitetslogg) {
        if (!harBetaltTidligere(periode)) return
        aktivitetslogg.funksjonellFeil(Varselkode.RV_IT_37)
    }

    private fun harBetaltTidligere(other: Periode): Boolean {
        val periodeMellom = periode.periodeMellom(other.start) ?: return false
        return periodeMellom.count() < MINIMALT_TILLATT_AVSTAND_TIL_INFOTRYGD
    }

    private fun validerOverlapp(aktivitetslogg: IAktivitetslogg, periode: Periode) {
        if (!this.periode.overlapperMed(periode)) return
        aktivitetslogg.info("Utbetaling i Infotrygd %s til %s overlapper med vedtaksperioden", this.periode.start, this.periode.endInclusive)
        aktivitetslogg.varsel(RV_IT_3)
    }

    private fun validerNyereOpplysninger(aktivitetslogg: IAktivitetslogg, organisasjonsnummer: String, periode: Periode) {
        if (!gjelder(organisasjonsnummer)) return
        if (this.periode.start <= periode.endInclusive) return
        aktivitetslogg.varsel(RV_IT_1)
    }

    override fun gjelder(orgnummer: String) = orgnummer == this.orgnr

    override fun funksjoneltLik(other: Infotrygdperiode): Boolean {
        if (!super.funksjoneltLik(other)) return false
        other as Utbetalingsperiode
        return this.orgnr == other.orgnr && this.periode.start == other.periode.start && this.grad == other.grad && this.inntekt == other.inntekt
    }
}

class ArbeidsgiverUtbetalingsperiode(orgnr: String, fom: LocalDate, tom: LocalDate, grad: Prosentdel, inntekt: Inntekt) :
    Utbetalingsperiode(orgnr, fom, tom, grad, inntekt) {

    override fun accept(visitor: InfotrygdperiodeVisitor) {
        visitor.visitInfotrygdhistorikkArbeidsgiverUtbetalingsperiode(this, orgnr, periode.start, periode.endInclusive, grad, inntekt)
    }

    internal fun memento() = InfotrygdArbeidsgiverutbetalingsperiodeMemento(
        orgnr = orgnr,
        periode = periode.memento(),
        grad = grad.memento(),
        inntekt = inntekt.memento()
    )
}

class PersonUtbetalingsperiode(orgnr: String, fom: LocalDate, tom: LocalDate, grad: Prosentdel, inntekt: Inntekt) :
    Utbetalingsperiode(orgnr, fom, tom, grad, inntekt) {

    override fun accept(visitor: InfotrygdperiodeVisitor) {
        visitor.visitInfotrygdhistorikkPersonUtbetalingsperiode(this, orgnr, periode.start, periode.endInclusive, grad, inntekt)
    }

    internal fun memento() = InfotrygdPersonutbetalingsperiodeMemento(
        orgnr = orgnr,
        periode = periode.memento(),
        grad = grad.memento(),
        inntekt = inntekt.memento()
    )
}
