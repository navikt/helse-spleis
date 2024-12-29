package no.nav.helse.dsl

import no.nav.helse.person.Arbeidsledig
import no.nav.helse.person.Frilans
import no.nav.helse.person.Selvstendig

val a1 = "a1"
val a2 = "a2"
val a3 = "a3"
val a4 = "a4"

val frilans = Frilans
val selvstendig = Selvstendig
val arbeidsledig = Arbeidsledig

@Deprecated("erstatte med a1", replaceWith = ReplaceWith("a1"))
val ORGNUMMER = "bruk_heller_a1"
