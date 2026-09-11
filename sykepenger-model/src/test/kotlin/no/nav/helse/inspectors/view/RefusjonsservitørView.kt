package no.nav.helse.inspectors.view

import java.time.LocalDate
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.refusjon.Refusjonsservitør

internal data class RefusjonsservitørView(val refusjonstidslinjer: Map<LocalDate, Beløpstidslinje>)

internal fun Refusjonsservitør.view() = RefusjonsservitørView(refusjonsrester.toMap())
