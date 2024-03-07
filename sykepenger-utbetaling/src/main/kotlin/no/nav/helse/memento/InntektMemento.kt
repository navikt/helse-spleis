package no.nav.helse.memento

data class InntektMemento(
    val årlig: Double,
    val månedligDouble: Double,
    val dagligDouble: Double,
    val dagligInt: Int
)

