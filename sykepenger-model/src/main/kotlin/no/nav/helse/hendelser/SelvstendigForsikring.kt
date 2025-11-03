package no.nav.helse.hendelser

import java.time.LocalDate

data class SelvstendigForsikring (
    val virkningsdato: LocalDate,
    val opphørsdato: LocalDate?,
    val type: Forsikringstype
) {
    enum class Forsikringstype {
        ÅttiProsentFraDagEn,
        HundreProsentFraDagEn,
        HundreProsentFraDagSytten,
    }
}
