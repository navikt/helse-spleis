package no.nav.helse.inspectors

import java.time.LocalDateTime
import java.util.*
import no.nav.helse.person.inntekt.Saksbehandler
import no.nav.helse.økonomi.Inntekt

internal val Saksbehandler.inspektør get() = SaksbehandlerInspektør(this)

internal class SaksbehandlerInspektør(inntektsopplysning: Saksbehandler) {

    val beløp: Inntekt = inntektsopplysning.inntektsdata.beløp
    val hendelseId: UUID = inntektsopplysning.inntektsdata.hendelseId.id
    val tidsstempel: LocalDateTime = inntektsopplysning.inntektsdata.tidsstempel
}
