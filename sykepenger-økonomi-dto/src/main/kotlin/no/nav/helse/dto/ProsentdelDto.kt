package no.nav.helse.dto

data class ProsentdelDto(val prosentDesimal: Double) {
    init {
        check(prosentDesimal in 0.0..1.0) { "Prosentdel må være mellom 0 og 1" }
    }
}
