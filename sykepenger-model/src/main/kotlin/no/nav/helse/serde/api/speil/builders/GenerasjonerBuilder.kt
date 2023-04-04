package no.nav.helse.serde.api.speil.builders

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Alder
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidsgiverVisitor
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.Dokumentsporing.Companion.ider
import no.nav.helse.person.ForkastetVedtaksperiode
import no.nav.helse.person.ForlengelseFraInfotrygd
import no.nav.helse.person.InntektsmeldingInfo
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.serde.api.dto.GenerasjonDTO
import no.nav.helse.serde.api.dto.HendelseDTO
import no.nav.helse.serde.api.dto.Tidslinjeperiode.Companion.sorterEtterHendelse
import no.nav.helse.serde.api.speil.Generasjoner
import no.nav.helse.serde.api.speil.Tidslinjeberegninger
import no.nav.helse.serde.api.speil.builders.GenerasjonerBuilder.Byggetilstand.AktivePerioder
import no.nav.helse.serde.api.speil.builders.GenerasjonerBuilder.Byggetilstand.ForkastedePerioder
import no.nav.helse.serde.api.speil.builders.GenerasjonerBuilder.Byggetilstand.Initiell
import no.nav.helse.serde.api.speil.builders.GenerasjonerBuilder.Byggetilstand.Utbetalinger
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingslinjer.Utbetalingtype

internal typealias BeregningId = UUID
internal typealias KorrelasjonsId = UUID

// Besøker hele arbeidsgiver-treet
internal class GenerasjonerBuilder(
    private val hendelser: List<HendelseDTO>,
    private val alder: Alder,
    private val arbeidsgiver: Arbeidsgiver,
    private val vilkårsgrunnlaghistorikk: IVilkårsgrunnlagHistorikk
) : ArbeidsgiverVisitor {
    private val tidslinjeberegninger = Tidslinjeberegninger()
    private var tilstand: Byggetilstand = Initiell

    init {
        arbeidsgiver.accept(this)
    }

    // todo: speilbuilder bør regne ut dette selv slik at
    // vi kan mykne opp bindingen tilbake til modellen
    private fun periodetype(periode: Periode) =
        arbeidsgiver.periodetype(periode)

    internal fun build(): List<GenerasjonDTO> {
        val perioder = tidslinjeberegninger.build(alder, vilkårsgrunnlaghistorikk)
        return Generasjoner().apply {
            perioder
                .sorterEtterHendelse()
                .forEach { periode -> periode.tilGenerasjon(this) }
        }.build()
    }

    private fun byggForkastetVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        vedtaksperiodeId: UUID,
        tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime,
        periode: Periode,
        skjæringstidspunkt: LocalDate,
        hendelseIder: Set<Dokumentsporing>,
    ) {
        byggVedtaksperiode(vedtaksperiode, true, vedtaksperiodeId, tilstand, opprettet, oppdatert, periode, skjæringstidspunkt, hendelseIder)
    }

    private fun byggVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        forkastet: Boolean,
        vedtaksperiodeId: UUID,
        tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime,
        periode: Periode,
        skjæringstidspunkt: LocalDate,
        hendelseIder: Set<Dokumentsporing>,
    ) {
        val sykdomstidslinje = VedtaksperiodeSykdomstidslinjeBuilder(vedtaksperiode).build()
        val utbetalinger = UtbetalingerBuilder(vedtaksperiode).build()
        val aktivetsloggForPeriode = Vedtaksperiode.aktivitetsloggMedForegåendeUtenUtbetaling(vedtaksperiode)
        val filtrerteHendelser = hendelser.filter { it.id in hendelseIder.ider().map(UUID::toString) }
        if (forkastet) {
            return tidslinjeberegninger.leggTilForkastetVedtaksperiode(
                vedtaksperiode = vedtaksperiodeId,
                fom = periode.start,
                tom = periode.endInclusive,
                hendelser = filtrerteHendelser,
                utbetalinger = utbetalinger,
                periodetype = periodetype(periode),
                sykdomstidslinje = sykdomstidslinje,
                opprettet = opprettet,
                oppdatert = oppdatert,
                tilstand = tilstand,
                skjæringstidspunkt = skjæringstidspunkt,
                aktivitetsloggForPeriode = aktivetsloggForPeriode
            )
        }
        tidslinjeberegninger.leggTilVedtaksperiode(
            vedtaksperiode = vedtaksperiodeId,
            fom = periode.start,
            tom = periode.endInclusive,
            hendelser = filtrerteHendelser,
            utbetalinger = utbetalinger,
            periodetype = periodetype(periode),
            sykdomstidslinje = sykdomstidslinje,
            opprettet = opprettet,
            oppdatert = oppdatert,
            tilstand = tilstand,
            skjæringstidspunkt = skjæringstidspunkt,
            aktivitetsloggForPeriode = aktivetsloggForPeriode
        )
    }

    override fun preVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
        this.tilstand = AktivePerioder
    }

    override fun postVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
        this.tilstand = Initiell
    }

    override fun preVisitForkastedePerioder(vedtaksperioder: List<ForkastetVedtaksperiode>) {
        this.tilstand = ForkastedePerioder
    }

    override fun postVisitForkastedePerioder(vedtaksperioder: List<ForkastetVedtaksperiode>) {
        this.tilstand = Initiell
    }

    override fun preVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime,
        periode: Periode,
        opprinneligPeriode: Periode,
        skjæringstidspunkt: () -> LocalDate,
        skjæringstidspunktFraInfotrygd: LocalDate?,
        forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
        hendelseIder: Set<Dokumentsporing>,
        inntektsmeldingInfo: InntektsmeldingInfo?
    ) {
        this.tilstand.besøkVedtaksperiode(this, vedtaksperiode, id, tilstand, opprettet, oppdatert, periode, skjæringstidspunkt(), hendelseIder)
    }

    override fun preVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
        this.tilstand = Utbetalinger
    }

    override fun postVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
        this.tilstand = Initiell
    }

    override fun preVisitUtbetaling(
        utbetaling: Utbetaling,
        id: UUID,
        korrelasjonsId: UUID,
        type: Utbetalingtype,
        utbetalingstatus: Utbetalingstatus,
        periode: Periode,
        tidsstempel: LocalDateTime,
        oppdatert: LocalDateTime,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        maksdato: LocalDate,
        forbrukteSykedager: Int?,
        gjenståendeSykedager: Int?,
        stønadsdager: Int,
        beregningId: UUID,
        overføringstidspunkt: LocalDateTime?,
        avsluttet: LocalDateTime?,
        avstemmingsnøkkel: Long?,
        annulleringer: Set<UUID>
    ) {
        if (utbetalingstatus == Utbetalingstatus.FORKASTET) return
        tilstand.besøkUtbetaling(
            builder = this,
            tidslinjeberegninger = tidslinjeberegninger,
            internutbetaling = utbetaling,
            id = id,
            korrelasjonsId = korrelasjonsId,
            type = type,
            utbetalingstatus = utbetalingstatus,
            tidsstempel = tidsstempel,
            arbeidsgiverNettoBeløp = arbeidsgiverNettoBeløp,
            personNettoBeløp = personNettoBeløp,
            maksdato = maksdato,
            forbrukteSykedager = forbrukteSykedager,
            gjenståendeSykedager = gjenståendeSykedager,
            beregningId = beregningId,
            annulleringer = annulleringer
        )
    }

    override fun preVisitUtbetalingstidslinjeberegning(
        id: UUID,
        tidsstempel: LocalDateTime,
        organisasjonsnummer: String,
        sykdomshistorikkElementId: UUID,
        vilkårsgrunnlagHistorikkInnslagId: UUID
    ) {
        tidslinjeberegninger.leggTil(id, sykdomshistorikkElementId)
    }

    override fun preVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element, id: UUID, hendelseId: UUID?, tidsstempel: LocalDateTime) {
        tidslinjeberegninger.leggTil(id, SykdomshistorikkBuilder(element).build())
    }

    private interface Byggetilstand {
        fun besøkUtbetaling(
            builder: GenerasjonerBuilder,
            tidslinjeberegninger: Tidslinjeberegninger,
            internutbetaling: Utbetaling,
            id: UUID,
            korrelasjonsId: UUID,
            type: Utbetalingtype,
            utbetalingstatus: Utbetalingstatus,
            tidsstempel: LocalDateTime,
            arbeidsgiverNettoBeløp: Int,
            personNettoBeløp: Int,
            maksdato: LocalDate,
            forbrukteSykedager: Int?,
            gjenståendeSykedager: Int?,
            beregningId: UUID,
            annulleringer: Set<UUID>
        ) {}
        fun besøkVedtaksperiode(
            builder: GenerasjonerBuilder,
            vedtaksperiode: Vedtaksperiode,
            vedtaksperiodeId: UUID,
            tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
            opprettet: LocalDateTime,
            oppdatert: LocalDateTime,
            periode: Periode,
            skjæringstidspunkt: LocalDate,
            hendelseIder: Set<Dokumentsporing>
        ) {
            throw IllegalStateException("a-hoy! dette var ikke forventet gitt!")
        }

        object Initiell : Byggetilstand
        object Utbetalinger : Byggetilstand {
            override fun besøkUtbetaling(
                builder: GenerasjonerBuilder,
                tidslinjeberegninger: Tidslinjeberegninger,
                internutbetaling: Utbetaling,
                id: UUID,
                korrelasjonsId: UUID,
                type: Utbetalingtype,
                utbetalingstatus: Utbetalingstatus,
                tidsstempel: LocalDateTime,
                arbeidsgiverNettoBeløp: Int,
                personNettoBeløp: Int,
                maksdato: LocalDate,
                forbrukteSykedager: Int?,
                gjenståendeSykedager: Int?,
                beregningId: UUID,
                annulleringer: Set<UUID>
            ) {
                when (type) {
                    Utbetalingtype.UTBETALING -> builder.tidslinjeberegninger.leggTilUtbetaling(
                        internutbetaling = internutbetaling,
                        id = id,
                        korrelasjonsId = korrelasjonsId,
                        type = type,
                        utbetalingstatus = utbetalingstatus,
                        tidsstempel = tidsstempel,
                        arbeidsgiverNettoBeløp = arbeidsgiverNettoBeløp,
                        personNettoBeløp = personNettoBeløp,
                        maksdato = maksdato,
                        forbrukteSykedager = forbrukteSykedager,
                        gjenståendeSykedager = gjenståendeSykedager,
                        beregningId = beregningId,
                        annulleringer = annulleringer
                    )
                    Utbetalingtype.REVURDERING -> builder.tidslinjeberegninger.leggTilRevurdering(
                        internutbetaling = internutbetaling,
                        id = id,
                        korrelasjonsId = korrelasjonsId,
                        type = type,
                        utbetalingstatus = utbetalingstatus,
                        tidsstempel = tidsstempel,
                        arbeidsgiverNettoBeløp = arbeidsgiverNettoBeløp,
                        personNettoBeløp = personNettoBeløp,
                        maksdato = maksdato,
                        forbrukteSykedager = forbrukteSykedager,
                        gjenståendeSykedager = gjenståendeSykedager,
                        beregningId = beregningId,
                        annulleringer = annulleringer
                    )
                    Utbetalingtype.ANNULLERING -> builder.tidslinjeberegninger.leggTilAnnullering(
                        internutbetaling = internutbetaling,
                        id = id,
                        korrelasjonsId = korrelasjonsId,
                        type = type,
                        utbetalingstatus = utbetalingstatus,
                        tidsstempel = tidsstempel,
                        arbeidsgiverNettoBeløp = arbeidsgiverNettoBeløp,
                        personNettoBeløp = personNettoBeløp,
                        maksdato = maksdato,
                        forbrukteSykedager = forbrukteSykedager,
                        gjenståendeSykedager = gjenståendeSykedager,
                        beregningId = beregningId
                    )
                    else -> { /* ignorer */ }
                }
            }
        }
        object AktivePerioder : Byggetilstand {
            override fun besøkVedtaksperiode(
                builder: GenerasjonerBuilder,
                vedtaksperiode: Vedtaksperiode,
                vedtaksperiodeId: UUID,
                tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
                opprettet: LocalDateTime,
                oppdatert: LocalDateTime,
                periode: Periode,
                skjæringstidspunkt: LocalDate,
                hendelseIder: Set<Dokumentsporing>
            ) {
                builder.byggVedtaksperiode(vedtaksperiode, false, vedtaksperiodeId, tilstand, opprettet, oppdatert, periode, skjæringstidspunkt, hendelseIder)
            }
        }
        object ForkastedePerioder : Byggetilstand {
            override fun besøkVedtaksperiode(
                builder: GenerasjonerBuilder,
                vedtaksperiode: Vedtaksperiode,
                vedtaksperiodeId: UUID,
                tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
                opprettet: LocalDateTime,
                oppdatert: LocalDateTime,
                periode: Periode,
                skjæringstidspunkt: LocalDate,
                hendelseIder: Set<Dokumentsporing>,
            ) {
                builder.byggForkastetVedtaksperiode(vedtaksperiode, vedtaksperiodeId, tilstand, opprettet, oppdatert, periode, skjæringstidspunkt, hendelseIder)
            }
        }
    }
}
