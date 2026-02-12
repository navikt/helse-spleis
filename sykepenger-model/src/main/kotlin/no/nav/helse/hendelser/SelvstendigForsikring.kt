package no.nav.helse.hendelser

import java.time.LocalDate
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel.Companion.prosent

data class SelvstendigForsikring (
    val virkningsdato: LocalDate,
    val opphørsdato: LocalDate?,
    val type: Forsikringstype,
    val premiegrunnlag: Inntekt
) {
    enum class Forsikringstype {
        ÅttiProsentFraDagEn,
        HundreProsentFraDagEn,
        HundreProsentFraDagSytten,
    }

    fun dekningsgrad() = when (type) {
        Forsikringstype.ÅttiProsentFraDagEn -> 80.prosent
        Forsikringstype.HundreProsentFraDagEn,
        Forsikringstype.HundreProsentFraDagSytten -> 100.prosent
    }

    fun navOvertarAnsvarForVentetid() = when (type) {
        Forsikringstype.ÅttiProsentFraDagEn,
        Forsikringstype.HundreProsentFraDagEn -> true

        Forsikringstype.HundreProsentFraDagSytten -> false
    }
}
