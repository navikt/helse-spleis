package no.nav.helse.hendelser

import java.time.LocalDate
import no.nav.helse.dto.SelvstendigForsikringDto

data class SelvstendigForsikring (
    val virkningsdato: LocalDate,
    val opphørsdato: LocalDate?,
    val type: Forsikringstype
) {
    enum class Forsikringstype {
        ÅttiProsentFraDagEn,
        HundreProsentFraDagEn,
        HundreProsentFraDagSytten
    }

    internal fun dto() = SelvstendigForsikringDto(
            virkningsdato = virkningsdato,
            opphørsdato = opphørsdato,
            type = when (type) {
                Forsikringstype.ÅttiProsentFraDagEn -> SelvstendigForsikringDto.ForsikringstypeDto.ÅttiProsentFraDagEn
                Forsikringstype.HundreProsentFraDagEn -> SelvstendigForsikringDto.ForsikringstypeDto.HundreProsentFraDagEn
                Forsikringstype.HundreProsentFraDagSytten -> SelvstendigForsikringDto.ForsikringstypeDto.HundreProsentFraDagSytten
            }
        )

    internal companion object {
        fun gjenopprett(selvstendigForsikringDto: SelvstendigForsikringDto): SelvstendigForsikring {
            return SelvstendigForsikring(
                virkningsdato = selvstendigForsikringDto.virkningsdato,
                opphørsdato = selvstendigForsikringDto.opphørsdato,
                type = when (selvstendigForsikringDto.type) {
                    SelvstendigForsikringDto.ForsikringstypeDto.ÅttiProsentFraDagEn -> Forsikringstype.ÅttiProsentFraDagEn
                    SelvstendigForsikringDto.ForsikringstypeDto.HundreProsentFraDagEn -> Forsikringstype.HundreProsentFraDagEn
                    SelvstendigForsikringDto.ForsikringstypeDto.HundreProsentFraDagSytten -> Forsikringstype.HundreProsentFraDagSytten
                }
            )
        }
    }
}


