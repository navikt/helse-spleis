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
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.InntektsmeldingInfo
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.serde.api.dto.Generasjon
import no.nav.helse.serde.api.dto.HendelseDTO
import no.nav.helse.serde.api.speil.AnnulleringerAkkumulator
import no.nav.helse.serde.api.speil.ForkastetVedtaksperiodeAkkumulator
import no.nav.helse.serde.api.speil.GenerasjonIderAkkumulator
import no.nav.helse.serde.api.speil.Generasjoner
import no.nav.helse.serde.api.speil.IVedtaksperiode
import no.nav.helse.serde.api.speil.SykdomshistorikkAkkumulator
import no.nav.helse.serde.api.speil.Tidslinjeberegninger
import no.nav.helse.serde.api.speil.Tidslinjeperioder
import no.nav.helse.serde.api.speil.VedtaksperiodeAkkumulator
import no.nav.helse.serde.api.speil.builders.GenerasjonerBuilder.Byggetilstand.AktivePerioder
import no.nav.helse.serde.api.speil.builders.GenerasjonerBuilder.Byggetilstand.ForkastedePerioder
import no.nav.helse.serde.api.speil.builders.GenerasjonerBuilder.Byggetilstand.Initiell
import no.nav.helse.serde.api.speil.builders.GenerasjonerBuilder.Byggetilstand.Utbetalinger
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingslinjer.Utbetalingtype

internal data class GenerasjonIder(
    val beregningId: BeregningId,
    val sykdomshistorikkId: SykdomshistorikkId,
    val vilkårsgrunnlagshistorikkId: VilkårsgrunnlagshistorikkId
)

internal typealias BeregningId = UUID
internal typealias SykdomshistorikkId = UUID
internal typealias VilkårsgrunnlagshistorikkId = UUID
internal typealias KorrelasjonsId = UUID
internal typealias InntektsmeldingId = UUID

// Besøker hele arbeidsgiver-treet
internal class GenerasjonerBuilder(
    private val hendelser: List<HendelseDTO>,
    private val alder: Alder,
    private val arbeidsgiver: Arbeidsgiver,
    private val vilkårsgrunnlaghistorikk: IVilkårsgrunnlagHistorikk
) : ArbeidsgiverVisitor {
    private val vedtaksperiodeAkkumulator = VedtaksperiodeAkkumulator()
    private val forkastetVedtaksperiodeAkkumulator = ForkastetVedtaksperiodeAkkumulator()
    private val generasjonIderAkkumulator = GenerasjonIderAkkumulator()
    private val sykdomshistorikkAkkumulator = SykdomshistorikkAkkumulator()
    private val annulleringer = AnnulleringerAkkumulator()
    private var tilstand: Byggetilstand = Initiell

    init {
        arbeidsgiver.accept(this)
    }

    // todo: speilbuilder bør regne ut dette selv slik at
    // vi kan mykne opp bindingen tilbake til modellen
    private fun periodetype(periode: Periode) =
        arbeidsgiver.periodetype(periode)

    fun build(): List<Generasjon> {
        vedtaksperiodeAkkumulator.supplerMedAnnulleringer(annulleringer)
        val tidslinjeberegninger = Tidslinjeberegninger(generasjonIderAkkumulator.toList(), sykdomshistorikkAkkumulator)
        val tidslinjeperioder = Tidslinjeperioder(
            alder = alder,
            forkastetVedtaksperiodeIder = forkastetVedtaksperiodeAkkumulator.toList(),
            vedtaksperioder = vedtaksperiodeAkkumulator.toList(),
            tidslinjeberegninger = tidslinjeberegninger,
            vilkårsgrunnlaghistorikk = vilkårsgrunnlaghistorikk
        )
        return Generasjoner(tidslinjeperioder).build()
    }

    private fun byggVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        vedtaksperiodeId: UUID,
        tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
        oppdatert: LocalDateTime,
        periode: Periode,
        skjæringstidspunkt: LocalDate,
        hendelseIder: Set<Dokumentsporing>,
        inntektskilde: Inntektskilde
    ) {
        val sykdomstidslinje = VedtaksperiodeSykdomstidslinjeBuilder(vedtaksperiode).build()
        val utbetalinger = UtbetalingerBuilder(vedtaksperiode, tilstand).build(vedtaksperiodeId)
        val aktivetsloggForPeriode = Vedtaksperiode.aktivitetsloggMedForegåendeUtenUtbetaling(vedtaksperiode)
        vedtaksperiodeAkkumulator.leggTil(
            IVedtaksperiode(
                vedtaksperiodeId,
                periode.start,
                periode.endInclusive,
                inntektskilde = inntektskilde,
                hendelser = hendelser.filter { it.id in hendelseIder.ider().map(UUID::toString) },
                utbetalinger = utbetalinger,
                periodetype = periodetype(periode),
                sykdomstidslinje = sykdomstidslinje,
                oppdatert = oppdatert,
                tilstand = tilstand,
                skjæringstidspunkt = skjæringstidspunkt,
                aktivitetsloggForPeriode = aktivetsloggForPeriode
            )
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
        inntektsmeldingInfo: InntektsmeldingInfo?,
        inntektskilde: () -> Inntektskilde
    ) {
        this.tilstand.besøkVedtaksperiode(this, vedtaksperiode, id, tilstand, oppdatert, periode, skjæringstidspunkt(), hendelseIder, inntektskilde())
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
        tilstand.besøkUtbetaling(this, utbetaling, type, annulleringer)
    }

    override fun preVisitUtbetalingstidslinjeberegning(
        id: UUID,
        tidsstempel: LocalDateTime,
        organisasjonsnummer: String,
        sykdomshistorikkElementId: UUID,
        vilkårsgrunnlagHistorikkInnslagId: UUID
    ) {
        generasjonIderAkkumulator.leggTil(id, GenerasjonIder(id, sykdomshistorikkElementId, vilkårsgrunnlagHistorikkInnslagId))
    }

    override fun preVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element, id: UUID, hendelseId: UUID?, tidsstempel: LocalDateTime) {
        SykdomshistorikkBuilder(id, element).build().also { (id, tidslinje) ->
            sykdomshistorikkAkkumulator.leggTil(id, tidslinje)
        }
    }

    private interface Byggetilstand {
        fun besøkUtbetaling(builder: GenerasjonerBuilder, utbetaling: Utbetaling, type: Utbetalingtype, annulleringer: Set<UUID>) {}
        fun besøkVedtaksperiode(
            builder: GenerasjonerBuilder,
            vedtaksperiode: Vedtaksperiode,
            vedtaksperiodeId: UUID,
            tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
            oppdatert: LocalDateTime,
            periode: Periode,
            skjæringstidspunkt: LocalDate,
            hendelseIder: Set<Dokumentsporing>,
            inntektskilde: Inntektskilde
        ) {
            throw IllegalStateException("a-hoy! dette var ikke forventet gitt!")
        }

        object Initiell : Byggetilstand
        object Utbetalinger : Byggetilstand {
            override fun besøkUtbetaling(builder: GenerasjonerBuilder, utbetaling: Utbetaling, type: Utbetalingtype, annulleringer: Set<UUID>) {
                if (type != Utbetalingtype.ANNULLERING) return builder.annulleringer.fjerne(annulleringer)
                builder.annulleringer.leggTil(UtbetalingBuilder(utbetaling).build())
            }
        }
        object AktivePerioder : Byggetilstand {
            override fun besøkVedtaksperiode(
                builder: GenerasjonerBuilder,
                vedtaksperiode: Vedtaksperiode,
                vedtaksperiodeId: UUID,
                tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
                oppdatert: LocalDateTime,
                periode: Periode,
                skjæringstidspunkt: LocalDate,
                hendelseIder: Set<Dokumentsporing>,
                inntektskilde: Inntektskilde
            ) {
                builder.byggVedtaksperiode(vedtaksperiode, vedtaksperiodeId, tilstand, oppdatert, periode, skjæringstidspunkt, hendelseIder, inntektskilde)
            }
        }
        object ForkastedePerioder : Byggetilstand {
            override fun besøkVedtaksperiode(
                builder: GenerasjonerBuilder,
                vedtaksperiode: Vedtaksperiode,
                vedtaksperiodeId: UUID,
                tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
                oppdatert: LocalDateTime,
                periode: Periode,
                skjæringstidspunkt: LocalDate,
                hendelseIder: Set<Dokumentsporing>,
                inntektskilde: Inntektskilde
            ) {
                builder.forkastetVedtaksperiodeAkkumulator.leggTil(vedtaksperiodeId)

                if (!skalForkastetPeriodeFåPølse(tilstand)) return
                builder.byggVedtaksperiode(vedtaksperiode, vedtaksperiodeId, tilstand, oppdatert, periode, skjæringstidspunkt, hendelseIder, inntektskilde)
            }

            private fun skalForkastetPeriodeFåPølse(tilstand: Vedtaksperiode.Vedtaksperiodetilstand): Boolean {
                // todo: speil vil lage pølser for annullerte (forkastede) perioder,
                // derfor bør vi heller sjekke utbetalingene til vedtaksperioden fremfor tilstanden
                return tilstand != Vedtaksperiode.TilInfotrygd
            }
        }
    }
}
