package no.nav.helse.person.inntekt

import no.nav.helse.hendelser.Avsender
import no.nav.helse.økonomi.Inntekt
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.LocalDate
import java.util.UUID

internal fun Refusjonsopplysning(
    meldingsreferanseId: UUID,
    fom: LocalDate,
    tom: LocalDate?,
    beløp: Inntekt
) = Refusjonsopplysning(meldingsreferanseId, fom, tom, beløp, Avsender.SYSTEM, LocalDate.EPOCH.atStartOfDay())

internal fun Refusjonsopplysning(
    meldingsreferanseId: UUID,
    fom: LocalDate,
    tom: LocalDate?,
    beløp: Inntekt,
    avsender: Avsender
) = Refusjonsopplysning(meldingsreferanseId, fom, tom, beløp, avsender, LocalDate.EPOCH.atStartOfDay())

internal fun assertLikeRefusjonsopplysninger(
    expected: List<Refusjonsopplysning>,
    actual: List<Refusjonsopplysning>
) {
    assertEquals(expected.map { it.copy(tidsstempel = LocalDate.EPOCH.atStartOfDay()) }, actual.map { it.copy(tidsstempel = LocalDate.EPOCH.atStartOfDay()) })
}

internal fun assertLikRefusjonsopplysning(
    expected: Refusjonsopplysning,
    actual: Refusjonsopplysning
) {
    assertEquals(expected.copy(tidsstempel = LocalDate.EPOCH.atStartOfDay()), actual.copy(tidsstempel = LocalDate.EPOCH.atStartOfDay()))
}
