package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import no.nav.helse.person.InfotrygdhistorikkVisitor

class UkjentInfotrygdperiode(fom: LocalDate, tom: LocalDate) : Infotrygdperiode(fom, tom) {

    override fun accept(visitor: InfotrygdhistorikkVisitor) {
        visitor.visitInfotrygdhistorikkUkjentPeriode(this)
    }
}
