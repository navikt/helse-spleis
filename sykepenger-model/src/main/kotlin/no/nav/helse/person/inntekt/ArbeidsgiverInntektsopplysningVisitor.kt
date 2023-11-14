package no.nav.helse.person.inntekt

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.RefusjonsopplysningerVisitor

internal interface ArbeidsgiverInntektsopplysningVisitor : InntektsopplysningVisitor, RefusjonsopplysningerVisitor {
    fun preVisitArbeidsgiverInntektsopplysning(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning, orgnummer: String, gjelder: Periode) {}
    fun postVisitArbeidsgiverInntektsopplysning(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning, orgnummer: String, gjelder: Periode) {}
}