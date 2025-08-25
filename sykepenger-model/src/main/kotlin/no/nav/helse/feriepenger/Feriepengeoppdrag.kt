package no.nav.helse.feriepenger

import java.time.LocalDateTime
import no.nav.helse.dto.FeriepengerendringskodeDto
import no.nav.helse.dto.FeriepengerfagområdeDto
import no.nav.helse.dto.deserialisering.FeriepengeoppdragInnDto
import no.nav.helse.dto.serialisering.FeriepengeoppdragUtDto
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst

data class Feriepengeoppdrag(
    val mottaker: String,
    val fagområde: Feriepengerfagområde,
    val linje: Feriepengeutbetalingslinje?,
    val fagsystemId: String,
    val endringskode: Feriepengerendringskode,
    val tidsstempel: LocalDateTime,
) : Aktivitetskontekst {

    companion object {
        fun gjenopprett(dto: FeriepengeoppdragInnDto): Feriepengeoppdrag {
            return Feriepengeoppdrag(
                mottaker = dto.mottaker,
                fagområde = when (dto.fagområde) {
                    FeriepengerfagområdeDto.SP -> Feriepengerfagområde.Sykepenger
                    FeriepengerfagområdeDto.SPREF -> Feriepengerfagområde.SykepengerRefusjon
                },
                linje = dto.linjer.map { Feriepengeutbetalingslinje.gjenopprett(it) }.singleOrNull(),
                fagsystemId = dto.fagsystemId,
                endringskode = Feriepengerendringskode.gjenopprett(dto.endringskode),
                tidsstempel = dto.tidsstempel
            )
        }
    }

    val totalbeløp: Int = if (linje == null || linje.datoStatusFom != null) 0 else linje.beløp
    val skalSendeOppdrag = linje?.endringskode?.let { it != Feriepengerendringskode.UEND } ?: false

    fun overfør(aktivitetslogg: IAktivitetslogg, saksbehandler: String) {
        val aktivitetsloggMedOppdragkontekst = aktivitetslogg.kontekst(this)
        if (!skalSendeOppdrag)
            return aktivitetsloggMedOppdragkontekst.info("Overfører ikke oppdrag uten endring for fagområde=$fagområde med fagsystemId=$fagsystemId")
        check(endringskode != Feriepengerendringskode.UEND)
        aktivitetsloggMedOppdragkontekst.behov(Behovtype.Feriepengeutbetaling, "Trenger å sende utbetaling til Oppdrag", behovdetaljer(saksbehandler))
    }

    // obs: vi setter ikke nye tidsstempel
    fun endreBeløp(beløp: Int): Feriepengeoppdrag {
        if (beløp == 0) return this.annuller()
        if (beløp == this.linje?.beløp) return this.utenEndring()
        return this.medEndring(beløp)
    }

    private fun annuller(): Feriepengeoppdrag {
        checkNotNull(linje) { "forventer at oppdraget har en linje" }
        if (linje.datoStatusFom != null) return this.utenEndring()
        return copy(
            endringskode = Feriepengerendringskode.ENDR,
            linje = linje.copy(
                endringskode = Feriepengerendringskode.ENDR,
                refFagsystemId = null,
                refDelytelseId = null,
                datoStatusFom = linje.fom
            ),
            tidsstempel = LocalDateTime.now()
        )
    }

    private fun utenEndring(): Feriepengeoppdrag {
        checkNotNull(linje) { "forventer at oppdraget har en linje" }
        return this.copy(
            endringskode = Feriepengerendringskode.UEND,
            linje = this.linje.copy(endringskode = Feriepengerendringskode.UEND),
            tidsstempel = LocalDateTime.now()
        )
    }

    private fun medEndring(beløp: Int): Feriepengeoppdrag {
        checkNotNull(linje) { "forventer at oppdraget har en linje" }
        return this.copy(
            endringskode = Feriepengerendringskode.ENDR,
            linje = this.linje.copy(
                endringskode = Feriepengerendringskode.NY,
                beløp = beløp,
                delytelseId = this.linje.delytelseId + 1,
                refDelytelseId = this.linje.delytelseId,
                refFagsystemId = this.fagsystemId,
                datoStatusFom = null
            ),
            tidsstempel = LocalDateTime.now()
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
            Feriepengerfagområde.SykepengerRefusjon -> FeriepengerfagområdeDto.SPREF
            Feriepengerfagområde.Sykepenger -> FeriepengerfagområdeDto.SP
        },
        linjer = listOfNotNull(linje?.dto()),
        fagsystemId = fagsystemId,
        endringskode = when (endringskode) {
            Feriepengerendringskode.NY -> FeriepengerendringskodeDto.NY
            Feriepengerendringskode.UEND -> FeriepengerendringskodeDto.UEND
            Feriepengerendringskode.ENDR -> FeriepengerendringskodeDto.ENDR
        },
        tidsstempel = tidsstempel
    )
}

enum class Feriepengerfagområde(val verdi: String) {
    SykepengerRefusjon("SPREF"),
    Sykepenger("SP");

    override fun toString() = verdi
}
