package no.nav.helse.hendelser

import java.time.LocalDate
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent

sealed interface Forsikring {
    fun dekningsgrad(): Prosentdel
    fun navOvertarAnsvarForVentetid(): Boolean
}

/*
    forsikring du har som effekt av å være jordbruker
 */
object KollektivJordbruksforsikring : Forsikring {
    override fun dekningsgrad() = 100.prosent
    override fun navOvertarAnsvarForVentetid() = false
}

/*
    forsikring du velger å kjøpe av Nav
 */
data class SelvstendigForsikring (
    val virkningsdato: LocalDate,
    val opphørsdato: LocalDate?,
    val type: Forsikringstype,
    val premiegrunnlag: Inntekt
) : Forsikring {
    enum class Forsikringstype {
        ÅttiProsentFraDagEn,
        HundreProsentFraDagEn,
        HundreProsentFraDagSytten,
    }

    override fun dekningsgrad() = when (type) {
        Forsikringstype.ÅttiProsentFraDagEn -> 80.prosent
        Forsikringstype.HundreProsentFraDagEn,
        Forsikringstype.HundreProsentFraDagSytten -> 100.prosent
    }

    override fun navOvertarAnsvarForVentetid() = when (type) {
        Forsikringstype.ÅttiProsentFraDagEn,
        Forsikringstype.HundreProsentFraDagEn -> true
        Forsikringstype.HundreProsentFraDagSytten -> false
    }
}

fun Forsikring.arbeidssituasjonForsikringstype() = when (this) {
    is KollektivJordbruksforsikring -> "KollektivJordbruksforsikring"
    is SelvstendigForsikring -> "SelvstendigForsikring"
}
