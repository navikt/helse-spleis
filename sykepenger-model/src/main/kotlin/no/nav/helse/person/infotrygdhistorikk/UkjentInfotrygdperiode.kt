package no.nav.helse.person.infotrygdhistorikk

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.InfotrygdhistorikkVisitor
import java.time.LocalDate

class UkjentInfotrygdperiode(fom: LocalDate, tom: LocalDate) : Infotrygdperiode(fom, tom) {

    override fun accept(visitor: InfotrygdhistorikkVisitor) {
        visitor.visitInfotrygdhistorikkUkjentPeriode(this)
    }
}
