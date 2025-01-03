package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Avsender
import no.nav.helse.økonomi.Inntekt

internal fun Refusjonsopplysning(
    meldingsreferanseId: UUID,
    fom: LocalDate,
    tom: LocalDate?,
    beløp: Inntekt,
) = Refusjonsopplysning(meldingsreferanseId, fom, tom, beløp, Avsender.SYSTEM, LocalDate.EPOCH.atStartOfDay())

internal fun Refusjonsopplysning(
    meldingsreferanseId: UUID,
    fom: LocalDate,
    tom: LocalDate?,
    beløp: Inntekt,
    avsender: Avsender
) = Refusjonsopplysning(meldingsreferanseId, fom, tom, beløp, avsender, LocalDate.EPOCH.atStartOfDay())
