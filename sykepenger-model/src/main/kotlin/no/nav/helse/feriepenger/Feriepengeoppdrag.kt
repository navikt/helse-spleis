package no.nav.helse.feriepenger

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.dto.EndringskodeDto
import no.nav.helse.dto.FagområdeDto
import no.nav.helse.dto.deserialisering.FeriepengeoppdragInnDto
import no.nav.helse.dto.serialisering.FeriepengeoppdragUtDto
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.OppdragDetaljer

data class Feriepengeoppdrag(
    val mottaker: String,
    val fagområde: Fagområde,
    val linjer: List<Feriepengeutbetalingslinje>,
    val fagsystemId: String,
    val endringskode: Endringskode,
    val tidsstempel: LocalDateTime,
) : Aktivitetskontekst {

    init {
        check(linjer.size <= 1) {
            "Et feriepengeoppdrag kan maksimalt ha én linje"
        }
    }

    companion object {
        fun gjenopprett(dto: FeriepengeoppdragInnDto): Feriepengeoppdrag {
            return Feriepengeoppdrag(
                mottaker = dto.mottaker,
                fagområde = when (dto.fagområde) {
                    FagområdeDto.SP -> Fagområde.Sykepenger
                    FagområdeDto.SPREF -> Fagområde.SykepengerRefusjon
                },
                linjer = dto.linjer.map { Feriepengeutbetalingslinje.gjenopprett(it) },
                fagsystemId = dto.fagsystemId,
                endringskode = Endringskode.gjenopprett(dto.endringskode),
                tidsstempel = dto.tidsstempel
            )
        }
    }

    fun detaljer(): OppdragDetaljer {
        val linjene = linjer.map { it.detaljer() }
        return OppdragDetaljer(
            fagsystemId = fagsystemId,
            fagområde = fagområde.verdi,
            mottaker = mottaker,
            stønadsdager = 1,
            nettoBeløp = linjer.singleOrNull()?.beløp ?: 0,
            fom = linjene.firstOrNull()?.fom ?: LocalDate.MIN,
            tom = linjene.lastOrNull()?.tom ?: LocalDate.MIN,
            linjer = linjene
        )
    }

    fun overfør(aktivitetslogg: IAktivitetslogg, saksbehandler: String) {
        val aktivitetsloggMedOppdragkontekst = aktivitetslogg.kontekst(this)
        if (!linjer.any(Feriepengeutbetalingslinje::erForskjell))
            return aktivitetsloggMedOppdragkontekst.info("Overfører ikke oppdrag uten endring for fagområde=$fagområde med fagsystemId=$fagsystemId")
        check(endringskode != Endringskode.UEND)
        aktivitetsloggMedOppdragkontekst.behov(Behovtype.Feriepengeutbetaling, "Trenger å sende utbetaling til Oppdrag", behovdetaljer(saksbehandler))
    }

    override fun toSpesifikkKontekst() = SpesifikkKontekst("Feriepengeoppdrag", mapOf("fagsystemId" to fagsystemId))

    private fun behovdetaljer(saksbehandler: String): Map<String, Any> {
        return mapOf(
            "mottaker" to mottaker,
            "fagområde" to "$fagområde",
            "linjer" to linjer.map(Feriepengeutbetalingslinje::behovdetaljer),
            "fagsystemId" to fagsystemId,
            "endringskode" to "$endringskode",
            "saksbehandler" to saksbehandler
        )
    }

    fun totalbeløp(): Int {
        if (linjer.single().datoStatusFom != null) return 0
        return linjer.single().beløp
    }

    fun linjerUtenOpphør() = linjer.filter { !it.erOpphør() }

    fun annuller(): Feriepengeoppdrag {
        return copy(
            linjer = listOf(this.linjer.single().opphørslinje(this.linjer.single().fom)),
            endringskode = Endringskode.ENDR
        )
    }

    fun dto() = FeriepengeoppdragUtDto(
        mottaker = mottaker,
        fagområde = when (fagområde) {
            Fagområde.SykepengerRefusjon -> FagområdeDto.SPREF
            Fagområde.Sykepenger -> FagområdeDto.SP
        },
        linjer = linjer.map { it.dto() },
        fagsystemId = fagsystemId,
        endringskode = when (endringskode) {
            Endringskode.NY -> EndringskodeDto.NY
            Endringskode.UEND -> EndringskodeDto.UEND
            Endringskode.ENDR -> EndringskodeDto.ENDR
        },
        tidsstempel = tidsstempel
    )
}

