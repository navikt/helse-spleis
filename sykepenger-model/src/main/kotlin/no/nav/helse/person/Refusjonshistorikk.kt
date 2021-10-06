package no.nav.helse.person

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Refusjonshistorikk.Refusjon.Companion.leggTilRefusjon
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class Refusjonshistorikk {
    private val refusjoner = mutableListOf<Refusjon>()

    internal fun leggTilRefusjon(refusjon: Refusjon) {
        refusjoner.leggTilRefusjon(refusjon)
    }

    internal fun accept(visitor: RefusjonshistorikkVisitor) {
        visitor.preVisitRefusjonshistorikk(this)
        refusjoner.forEach { it.accept(visitor) }
        visitor.postVisitRefusjonshistorikk(this)
    }

    internal class Refusjon(
        private val meldingsreferanseId: UUID,
        private val førsteFraværsdag: LocalDate?,
        private val arbeidsgiverperioder: List<Periode>,
        private val beløp: Inntekt?,
        private val opphørsdato: LocalDate?,
        private val endringerIRefusjon: List<EndringIRefusjon>,
        private val tidsstempel: LocalDateTime = LocalDateTime.now()
    ) {
        internal companion object {
            internal fun MutableList<Refusjon>.leggTilRefusjon(refusjon: Refusjon) {
                if (refusjon.meldingsreferanseId !in map { it.meldingsreferanseId }) add(refusjon)
            }
        }

        internal fun accept(visitor: RefusjonshistorikkVisitor) {
            visitor.preVisitRefusjon(
                meldingsreferanseId,
                førsteFraværsdag,
                arbeidsgiverperioder,
                beløp,
                opphørsdato,
                endringerIRefusjon,
                tidsstempel
            )
            endringerIRefusjon.forEach { it.accept(visitor) }
            visitor.postVisitRefusjon(
                meldingsreferanseId,
                førsteFraværsdag,
                arbeidsgiverperioder,
                beløp,
                opphørsdato,
                endringerIRefusjon,
                tidsstempel
            )
        }

        internal class EndringIRefusjon(
            private val beløp: Inntekt,
            private val endringsdato: LocalDate
        ){
            internal fun accept(visitor: RefusjonshistorikkVisitor){
                visitor.visitEndringIRefusjon(beløp, endringsdato)
            }
        }
    }
}
