package no.nav.helse.inspectors

import java.time.LocalDateTime
import java.util.*
import no.nav.helse.person.inntekt.SkjønnsmessigFastsatt
import no.nav.helse.økonomi.Inntekt

internal val SkjønnsmessigFastsatt.inspektør get() = SkjønnsmessigFastsattInspektør(this)

internal class SkjønnsmessigFastsattInspektør(inntektsopplysning: SkjønnsmessigFastsatt) {

    val beløp: Inntekt = inntektsopplysning.inntektsdata.beløp
    val hendelseId: UUID = inntektsopplysning.inntektsdata.hendelseId.id
    val tidsstempel: LocalDateTime = inntektsopplysning.inntektsdata.tidsstempel
}
