package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.EndringIRefusjonDto
import no.nav.helse.dto.deserialisering.RefusjonInnDto
import no.nav.helse.dto.deserialisering.RefusjonshistorikkInnDto
import no.nav.helse.dto.serialisering.RefusjonUtDto
import no.nav.helse.dto.serialisering.RefusjonshistorikkUtDto
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.inntekt.Refusjonshistorikk.Refusjon.Companion.leggTilRefusjon
import no.nav.helse.økonomi.Inntekt

internal class Refusjonshistorikk {
    private val refusjoner = mutableListOf<Refusjon>()

    internal fun leggTilRefusjon(refusjon: Refusjon) {
        refusjoner.leggTilRefusjon(refusjon)
    }

    fun view() = RefusjonshistorikkView(refusjoner.map {
        RefusjonView(
            meldingsreferanseId = it.meldingsreferanseId,
            førsteFraværsdag = it.førsteFraværsdag,
            arbeidsgiverperioder = it.arbeidsgiverperioder,
            beløp = it.beløp,
            sisteRefusjonsdag = it.sisteRefusjonsdag,
            endringerIRefusjon = it.endringerIRefusjon.map {
                EndringIRefusjonView(it.beløp, it.endringsdato)
            },
            tidsstempel = it.tidsstempel
        )
    })

    internal class Refusjon(
        val meldingsreferanseId: UUID,
        val førsteFraværsdag: LocalDate?,
        val arbeidsgiverperioder: List<Periode>,
        val beløp: Inntekt?,
        val sisteRefusjonsdag: LocalDate?,
        val endringerIRefusjon: List<EndringIRefusjon>,
        val tidsstempel: LocalDateTime = LocalDateTime.now()
    ) {
        private fun muligDuplikat(other: Refusjon) =
            this.meldingsreferanseId == other.meldingsreferanseId && this.utledetFørsteFraværsdag() == other.utledetFørsteFraværsdag()

        internal companion object {
            internal fun MutableList<Refusjon>.leggTilRefusjon(refusjon: Refusjon) {
                if (any { eksisterende -> eksisterende.muligDuplikat(refusjon) }) return
                add(refusjon)
            }

            private fun Refusjon.utledetFørsteFraværsdag() = førsteFraværsdag ?: arbeidsgiverperioder.maxOf { it.start }

            internal fun gjenopprett(dto: RefusjonInnDto): Refusjon {
                return Refusjon(
                    meldingsreferanseId = dto.meldingsreferanseId,
                    førsteFraværsdag = dto.førsteFraværsdag,
                    arbeidsgiverperioder = dto.arbeidsgiverperioder.map { Periode.gjenopprett(it) },
                    beløp = dto.beløp?.let { Inntekt.gjenopprett(it) },
                    sisteRefusjonsdag = dto.sisteRefusjonsdag,
                    endringerIRefusjon = dto.endringerIRefusjon.map { EndringIRefusjon.gjenopprett(it) },
                    tidsstempel = dto.tidsstempel
                )
            }
        }

        internal data class EndringIRefusjon(
            internal val beløp: Inntekt,
            internal val endringsdato: LocalDate
        ) {
            internal companion object {
                internal fun gjenopprett(dto: EndringIRefusjonDto): EndringIRefusjon {
                    return EndringIRefusjon(
                        beløp = Inntekt.gjenopprett(dto.beløp),
                        endringsdato = dto.endringsdato
                    )
                }
            }

            internal fun dto() = EndringIRefusjonDto(beløp.dtoMånedligDouble(), endringsdato)
        }

        internal fun dto() = RefusjonUtDto(
            meldingsreferanseId = meldingsreferanseId,
            førsteFraværsdag = førsteFraværsdag,
            arbeidsgiverperioder = arbeidsgiverperioder.map { it.dto() },
            beløp = beløp?.dto(),
            sisteRefusjonsdag = sisteRefusjonsdag,
            endringerIRefusjon = endringerIRefusjon.map { it.dto() },
            tidsstempel = tidsstempel
        )
    }

    internal fun dto() = RefusjonshistorikkUtDto(
        refusjoner = refusjoner.map { it.dto() }
    )

    internal companion object {
        fun gjenopprett(dto: RefusjonshistorikkInnDto): Refusjonshistorikk {
            return Refusjonshistorikk().apply {
                dto.refusjoner.forEach {
                    leggTilRefusjon(Refusjon.gjenopprett(it))
                }
            }
        }
    }
}

data class RefusjonshistorikkView(val refusjoner: List<RefusjonView>)
data class RefusjonView(
    val meldingsreferanseId: UUID,
    val førsteFraværsdag: LocalDate?,
    val arbeidsgiverperioder: List<Periode>,
    val beløp: Inntekt?,
    val sisteRefusjonsdag: LocalDate?,
    val endringerIRefusjon: List<EndringIRefusjonView>,
    val tidsstempel: LocalDateTime
)

data class EndringIRefusjonView(
    val beløp: Inntekt,
    val endringsdato: LocalDate
)
