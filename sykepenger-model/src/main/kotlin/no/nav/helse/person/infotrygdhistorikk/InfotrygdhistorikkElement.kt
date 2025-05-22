package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.dto.deserialisering.InfotrygdhistorikkelementInnDto
import no.nav.helse.dto.serialisering.InfotrygdhistorikkelementUtDto
import no.nav.helse.hendelser.Hendelseskilde
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode.Companion.utbetalingsperioder
import no.nav.helse.sykdomstidslinje.Dag.Companion.sammenhengendeSykdom
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

class InfotrygdhistorikkElement private constructor(
    val id: UUID,
    val tidsstempel: LocalDateTime,
    val hendelseId: MeldingsreferanseId,
    perioder: List<Infotrygdperiode>,
    oppdatert: LocalDateTime
) {
    var oppdatert = oppdatert
        private set
    val perioder = Infotrygdperiode.sorter(perioder)
    private val kilde = Hendelseskilde("Infotrygdhistorikk", hendelseId, tidsstempel)

    companion object {
        fun opprett(
            oppdatert: LocalDateTime,
            hendelseId: MeldingsreferanseId,
            perioder: List<Infotrygdperiode>
        ) =
            InfotrygdhistorikkElement(
                id = UUID.randomUUID(),
                tidsstempel = LocalDateTime.now(),
                hendelseId = hendelseId,
                perioder = perioder,
                oppdatert = oppdatert
            )

        internal fun gjenopprett(dto: InfotrygdhistorikkelementInnDto): InfotrygdhistorikkElement {
            return InfotrygdhistorikkElement(
                id = dto.id,
                tidsstempel = dto.tidsstempel,
                hendelseId = MeldingsreferanseId.gjenopprett(dto.hendelseId),
                perioder = dto.arbeidsgiverutbetalingsperioder.map { ArbeidsgiverUtbetalingsperiode.gjenopprett(it) } +
                    dto.personutbetalingsperioder.map { PersonUtbetalingsperiode.gjenopprett(it) } +
                    dto.ferieperioder.map { Friperiode.gjenopprett(it) },
                oppdatert = dto.oppdatert
            )
        }
    }

    internal fun betaltePerioder(orgnummer: String? = null): List<Periode> = perioder.utbetalingsperioder(orgnummer)
    internal fun friperioder(): List<Periode> = perioder.filterIsInstance<Friperiode>().map { it.periode }

    internal fun sykdomstidslinje(): Sykdomstidslinje {
        return perioder.fold(Sykdomstidslinje()) { result, periode ->
            result.merge(periode.sykdomstidslinje(kilde), sammenhengendeSykdom)
        }
    }

    internal fun validerMedFunksjonellFeil(aktivitetslogg: IAktivitetslogg, periode: Periode): Boolean {
        aktivitetslogg.info("Sjekker utbetalte perioder")
        perioder.forEach { it.valider(aktivitetslogg, periode, IAktivitetslogg::funksjonellFeil) }
        return !aktivitetslogg.harFunksjonelleFeilEllerVerre()
    }

    internal fun validerMedVarsel(aktivitetslogg: IAktivitetslogg, periode: Periode) {
        aktivitetslogg.info("Sjekker utbetalte perioder")
        perioder.forEach { it.valider(aktivitetslogg, periode, IAktivitetslogg::varsel) }
    }

    internal fun validerNyereOpplysninger(aktivitetslogg: IAktivitetslogg, periode: Periode) {
        perioder.forEach {
            it.validerNyereOpplysninger(aktivitetslogg, periode)
        }
    }

    internal fun utbetalingstidslinje() =
        perioder
            .map { it.utbetalingstidslinje() }
            .fold(Utbetalingstidslinje(), Utbetalingstidslinje::plus)

    internal fun funksjoneltLik(other: InfotrygdhistorikkElement): Boolean {
        return harLikePerioder(other)
    }

    private fun harLikePerioder(other: InfotrygdhistorikkElement): Boolean {
        if (other.perioder.size != this.perioder.size) return false
        return other.perioder.zip(this.perioder, Infotrygdperiode::funksjoneltLik).all { it }
    }

    internal fun erstatter(other: InfotrygdhistorikkElement): Boolean {
        if (!this.funksjoneltLik(other)) return false
        oppdater(other)
        return true
    }

    private fun oppdater(other: InfotrygdhistorikkElement) {
        other.oppdatert = this.oppdatert
    }

    internal fun tidligsteEndringMellom(other: InfotrygdhistorikkElement?): LocalDate? {
        if (other == null || other.perioder.isEmpty()) return this.perioder.firstOrNull()?.periode?.start
        if (this.perioder.isEmpty()) return other.perioder.first().periode.start
        // tidligste dato som ikke er i begge lister
        val førsteUlikePeriode = this.førsteUlikePeriode(other) ?: other.førsteUlikePeriode(this)
        return førsteUlikePeriode?.periode?.start
    }

    private fun førsteUlikePeriode(other: InfotrygdhistorikkElement): Infotrygdperiode? {
        return this.perioder.firstOrNull { other.perioder.none { otherIt -> it.funksjoneltLik(otherIt) } }
    }

    internal fun harUtbetaltI(periode: Periode) = betaltePerioder().any { it.overlapperMed(periode) }

    internal fun harFerieI(periode: Periode) = perioder.filterIsInstance<Friperiode>().any { it.overlapperMed(periode) }

    internal fun dto() = InfotrygdhistorikkelementUtDto(
        id = this.id,
        tidsstempel = this.tidsstempel,
        hendelseId = this.hendelseId.dto(),
        ferieperioder = this.perioder.filterIsInstance<Friperiode>().map { it.dto() },
        arbeidsgiverutbetalingsperioder = this.perioder.filterIsInstance<ArbeidsgiverUtbetalingsperiode>().map { it.dto() },
        personutbetalingsperioder = this.perioder.filterIsInstance<PersonUtbetalingsperiode>().map { it.dto() },
        oppdatert = this.oppdatert
    )
}

