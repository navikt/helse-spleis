package no.nav.helse.hendelser

import java.time.LocalDate

data class SelvstendigForsikring (
    val startdato: LocalDate,
    val sluttdato: LocalDate?,
    val type: Forsikringstype
) {
    enum class Forsikringstype {
        ÅttiProsentFraDagEn,
        HundreProsentFraDagEn,
        HundreProsentFraDagSytten,
    }
}
