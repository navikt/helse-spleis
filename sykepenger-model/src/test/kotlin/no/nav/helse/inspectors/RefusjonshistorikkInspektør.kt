package no.nav.helse.inspectors

import no.nav.helse.person.inntekt.RefusjonshistorikkView

internal val RefusjonshistorikkView.inspektør get() = RefusjonshistorikkInspektør(this)

internal class RefusjonshistorikkInspektør(refusjonshistorikk: RefusjonshistorikkView) {

    internal val antall = refusjonshistorikk.refusjoner.size
}