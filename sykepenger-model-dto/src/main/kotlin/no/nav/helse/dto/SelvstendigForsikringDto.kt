package no.nav.helse.dto

import java.time.LocalDate

data class SelvstendigForsikringDto (
    val virkningsdato: LocalDate,
    val opphørsdato: LocalDate?,
    val type: ForsikringstypeDto
) {
    enum class ForsikringstypeDto {
        ÅttiProsentFraDagEn,
        HundreProsentFraDagEn,
        HundreProsentFraDagSytten
    }
}
