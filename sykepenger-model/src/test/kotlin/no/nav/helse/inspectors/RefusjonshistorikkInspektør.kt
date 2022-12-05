package no.nav.helse.inspectors

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.RefusjonshistorikkVisitor
import no.nav.helse.person.inntekt.Refusjonshistorikk
import no.nav.helse.økonomi.Inntekt

internal val Refusjonshistorikk.inspektør get() = RefusjonshistorikkInspektør(this)

internal class RefusjonshistorikkInspektør(refusjonshistorikk: Refusjonshistorikk): RefusjonshistorikkVisitor {

    private var antallRefusjonsopplysninger = 0
    internal val antall get() = antallRefusjonsopplysninger

    init {
        refusjonshistorikk.accept(this)
    }

    override fun preVisitRefusjon(
        meldingsreferanseId: UUID,
        førsteFraværsdag: LocalDate?,
        arbeidsgiverperioder: List<Periode>,
        beløp: Inntekt?,
        sisteRefusjonsdag: LocalDate?,
        endringerIRefusjon: List<Refusjonshistorikk.Refusjon.EndringIRefusjon>,
        tidsstempel: LocalDateTime
    ) {
       antallRefusjonsopplysninger++
    }
}