package no.nav.helse.inspectors

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.inntekt.IkkeRapportert
import no.nav.helse.person.inntekt.Infotrygd
import no.nav.helse.person.inntekt.Inntektsmeldinginntekt
import no.nav.helse.person.inntekt.Inntektsopplysning
import no.nav.helse.person.inntekt.Saksbehandler
import no.nav.helse.person.inntekt.SkattSykepengegrunnlag
import no.nav.helse.person.inntekt.SkjønnsmessigFastsatt
import no.nav.helse.økonomi.Inntekt

internal val Inntektsopplysning.inspektør get() = InntektsopplysningInspektør(this)

internal class InntektsopplysningInspektør(inntektsopplysning: Inntektsopplysning) {

    val beløp: Inntekt = inntektsopplysning.inntektsdata.beløp
    val hendelseId: UUID = inntektsopplysning.inntektsdata.hendelseId
    val tidsstempel: LocalDateTime = inntektsopplysning.inntektsdata.tidsstempel
    val forrigeInntekt = when (inntektsopplysning) {
        is Infotrygd -> null
        is Saksbehandler -> inntektsopplysning.overstyrtInntekt
        is SkjønnsmessigFastsatt -> inntektsopplysning.overstyrtInntekt
        is IkkeRapportert -> null
        is Inntektsmeldinginntekt -> null
        is SkattSykepengegrunnlag -> null
    }
}
