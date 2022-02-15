package no.nav.helse.serde.api.v2.buildere

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Refusjonshistorikk
import no.nav.helse.person.RefusjonshistorikkVisitor
import no.nav.helse.serde.api.v2.Refusjon
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class RefusjonerBuilder(historikk: Refusjonshistorikk) : RefusjonshistorikkVisitor {

    private val refusjoner = mutableMapOf<InntektsmeldingId, Refusjon>()
    private val endringer = mutableListOf<Refusjon.Endring>()

    init {
        historikk.accept(this)
    }

    fun build(): Map<InntektsmeldingId, Refusjon> = refusjoner

    override fun visitEndringIRefusjon(beløp: Inntekt, endringsdato: LocalDate) {
        endringer.add(
            Refusjon.Endring(
                beløp = beløp.reflection { _, månedlig, _, _ -> månedlig },
                dato = endringsdato
            )
        )
    }

    override fun postVisitRefusjon(
        meldingsreferanseId: UUID,
        førsteFraværsdag: LocalDate?,
        arbeidsgiverperioder: List<Periode>,
        beløp: Inntekt?,
        sisteRefusjonsdag: LocalDate?,
        endringerIRefusjon: List<Refusjonshistorikk.Refusjon.EndringIRefusjon>,
        tidsstempel: LocalDateTime
    ) {
        refusjoner.putIfAbsent(
            meldingsreferanseId,
            Refusjon(
                arbeidsgiverperioder = arbeidsgiverperioder.map { Refusjon.Periode(it.start, it.endInclusive) },
                endringer = endringer,
                førsteFraværsdag = førsteFraværsdag,
                sisteRefusjonsdag = sisteRefusjonsdag,
                beløp = beløp?.reflection { _, månedlig, _, _ -> månedlig }
            )
        )
    }

}
