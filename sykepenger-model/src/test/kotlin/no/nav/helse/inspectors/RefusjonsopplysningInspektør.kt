package no.nav.helse.inspectors

import no.nav.helse.person.inntekt.Refusjonsopplysning

internal val Refusjonsopplysning.Refusjonsopplysninger.inspektør get() = RefusjonsopplysningerInspektør(this)

internal class RefusjonsopplysningerInspektør(refusjonsopplysninger: Refusjonsopplysning.Refusjonsopplysninger) {
    val refusjonsopplysninger = refusjonsopplysninger.validerteRefusjonsopplysninger
}

internal val Refusjonsopplysning.inspektør get() = RefusjonsopplysningInspektør(this)

internal class RefusjonsopplysningInspektør(refusjonsopplysning: Refusjonsopplysning) {
    val meldingsreferanseId = refusjonsopplysning.meldingsreferanseId
    val beløp = refusjonsopplysning.beløp
    val periode = refusjonsopplysning.periode
}