package no.nav.helse.person.inntekt

import no.nav.helse.person.InntektsopplysningVisitor
import no.nav.helse.person.RefusjonsopplysningerVisitor

internal interface ArbeidsgiverInntektsopplysningVisitor : InntektsopplysningVisitor, RefusjonsopplysningerVisitor {
    fun preVisitArbeidsgiverInntektsopplysning(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning, orgnummer: String) {}
    fun postVisitArbeidsgiverInntektsopplysning(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning, orgnummer: String) {}
}