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
    val linje: Feriepengeutbetalingslinje?,
    val fagsystemId: String,
    val endringskode: Endringskode,
    val tidsstempel: LocalDateTime,
) : Aktivitetskontekst {

    companion object {
        fun gjenopprett(dto: FeriepengeoppdragInnDto): Feriepengeoppdrag {
            return Feriepengeoppdrag(
                mottaker = dto.mottaker,
                fagområde = when (dto.fagområde) {
                    FagområdeDto.SP -> Fagområde.Sykepenger
                    FagområdeDto.SPREF -> Fagområde.SykepengerRefusjon
                },
                linje = dto.linjer.map { Feriepengeutbetalingslinje.gjenopprett(it) }.singleOrNull(),
                fagsystemId = dto.fagsystemId,
                endringskode = Endringskode.gjenopprett(dto.endringskode),
                tidsstempel = dto.tidsstempel
            )
        }
    }

    val totalbeløp: Int = if (linje == null || linje.datoStatusFom != null) 0 else linje.beløp
    val skalSendeOppdrag = linje?.endringskode?.let { it != Endringskode.UEND } ?: false

    fun detaljer(): OppdragDetaljer {
        return OppdragDetaljer(
            fagsystemId = fagsystemId,
            fagområde = fagområde.verdi,
            mottaker = mottaker,
            stønadsdager = 1,
            nettoBeløp = linje?.beløp ?: 0,
            fom = linje?.fom ?: LocalDate.MIN,
            tom = linje?.tom ?: LocalDate.MIN,
            linjer = listOfNotNull(linje?.detaljer())
        )
    }

    fun overfør(aktivitetslogg: IAktivitetslogg, saksbehandler: String) {
        val aktivitetsloggMedOppdragkontekst = aktivitetslogg.kontekst(this)
        if (!skalSendeOppdrag)
            return aktivitetsloggMedOppdragkontekst.info("Overfører ikke oppdrag uten endring for fagområde=$fagområde med fagsystemId=$fagsystemId")
        check(endringskode != Endringskode.UEND)
        aktivitetsloggMedOppdragkontekst.behov(Behovtype.Feriepengeutbetaling, "Trenger å sende utbetaling til Oppdrag", behovdetaljer(saksbehandler))
    }

    fun endreBeløp(beløp: Int): Feriepengeoppdrag {
        if (beløp == 0) return this.annuller()
        if (beløp == this.linje?.beløp) return this.utenEndring()
        return this.medEndring(beløp)
    }

    private fun annuller(): Feriepengeoppdrag {
        checkNotNull(linje) { "forventer at oppdraget har en linje" }
        if (linje.datoStatusFom != null) return this.utenEndring()
        return copy(
            endringskode = Endringskode.ENDR,
            linje = linje.copy(
                endringskode = Endringskode.ENDR,
                refFagsystemId = null,
                refDelytelseId = null,
                datoStatusFom = linje.fom
            )
        )
    }

    private fun utenEndring(): Feriepengeoppdrag {
        checkNotNull(linje) { "forventer at oppdraget har en linje" }
        return this.copy(
            endringskode = Endringskode.UEND,
            linje = this.linje.copy(endringskode = Endringskode.UEND)
        )
    }

    private fun medEndring(beløp: Int): Feriepengeoppdrag {
        checkNotNull(linje) { "forventer at oppdraget har en linje" }
        return this.copy(
            endringskode = Endringskode.ENDR,
            linje = this.linje.copy(
                endringskode = Endringskode.NY,
                beløp = beløp,
                delytelseId = this.linje.delytelseId + 1,
                refDelytelseId = this.linje.delytelseId,
                refFagsystemId = this.fagsystemId,
                datoStatusFom = null
            )
        )
    }

    override fun toSpesifikkKontekst() = SpesifikkKontekst("Feriepengeoppdrag", mapOf("fagsystemId" to fagsystemId))

    private fun behovdetaljer(saksbehandler: String): Map<String, Any> {
        checkNotNull(linje) { "forventer at oppdraget har en linje" }
        return mapOf(
            "mottaker" to mottaker,
            "fagområde" to "$fagområde",
            "linjer" to listOf(linje.behovdetaljer()),
            "fagsystemId" to fagsystemId,
            "endringskode" to "$endringskode",
            "saksbehandler" to saksbehandler
        )
    }

    fun dto() = FeriepengeoppdragUtDto(
        mottaker = mottaker,
        fagområde = when (fagområde) {
            Fagområde.SykepengerRefusjon -> FagområdeDto.SPREF
            Fagområde.Sykepenger -> FagområdeDto.SP
        },
        linjer = listOfNotNull(linje?.dto()),
        fagsystemId = fagsystemId,
        endringskode = when (endringskode) {
            Endringskode.NY -> EndringskodeDto.NY
            Endringskode.UEND -> EndringskodeDto.UEND
            Endringskode.ENDR -> EndringskodeDto.ENDR
        },
        tidsstempel = tidsstempel
    )
}

