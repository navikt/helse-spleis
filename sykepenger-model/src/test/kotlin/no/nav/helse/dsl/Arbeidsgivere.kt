package no.nav.helse.dsl

val a1 = "a1"
val a2 = "a2"
val a3 = "a3"
val a4 = "a4"

val frilans = "FRILANS"
val selvstendig = "SELVSTENDIG"
val arbeidsledig = "ARBEIDSLEDIG"

@Deprecated("erstatte med a1", replaceWith = ReplaceWith("a1"))
val ORGNUMMER = "bruk_heller_a1"
