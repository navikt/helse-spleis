package no.nav.helse.person.infotrygdhistorikk

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.InfotrygdhistorikkVisitor
import java.time.LocalDate

class UkjentInfotrygdperiode(fom: LocalDate, tom: LocalDate) : Infotrygdperiode(fom, tom) {
    override fun valider(aktivitetslogg: IAktivitetslogg, periode: Periode, nødnummer: Nødnummer) {
        if (endInclusive < periode.start.minusDays(18)) return
        aktivitetslogg.warn("Perioden er lagt inn i Infotrygd, men ikke utbetalt. Fjern fra Infotrygd hvis det utbetales via speil.")
    }

    override fun accept(visitor: InfotrygdhistorikkVisitor) {
        visitor.visitInfotrygdhistorikkUkjentPeriode(this)
    }
}
