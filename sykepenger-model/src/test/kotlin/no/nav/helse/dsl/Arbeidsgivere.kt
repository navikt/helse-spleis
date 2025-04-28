package no.nav.helse.dsl

import no.nav.helse.person.Arbeidsledigtype
import no.nav.helse.person.Frilanstype
import no.nav.helse.person.Selvstendigtype

val a1 = "a1"
val a2 = "a2"
val a3 = "a3"
val a4 = "a4"

val frilans = Frilanstype
val selvstendig = Selvstendigtype
val arbeidsledig = Arbeidsledigtype

@Deprecated("erstatte med a1", replaceWith = ReplaceWith("a1"))
val ORGNUMMER = "bruk_heller_a1"
