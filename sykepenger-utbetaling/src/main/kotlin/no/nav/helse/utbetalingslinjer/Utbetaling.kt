package no.nav.helse.utbetalingslinjer

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.dto.EndringskodeDto
import no.nav.helse.dto.KlassekodeDto
import no.nav.helse.dto.UtbetalingTilstandDto
import no.nav.helse.dto.UtbetalingVurderingDto
import no.nav.helse.dto.UtbetalingtypeDto
import no.nav.helse.dto.deserialisering.UtbetalingInnDto
import no.nav.helse.dto.serialisering.UtbetalingUtDto
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.SimuleringHendelse
import no.nav.helse.hendelser.UtbetalingmodulHendelse
import no.nav.helse.hendelser.til
import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.utbetalingslinjer.Utbetalingtype.ANNULLERING
import no.nav.helse.utbetalingslinjer.Utbetalingtype.ETTERUTBETALING
import no.nav.helse.utbetalingslinjer.Utbetalingtype.REVURDERING
import no.nav.helse.utbetalingslinjer.Utbetalingtype.UTBETALING
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Utbetaling private constructor(
    val id: UUID,
    private val korrelasjonsId: UUID,
    private val periode: Periode,
    val utbetalingstidslinje: Utbetalingstidslinje,
    val arbeidsgiverOppdrag: Oppdrag,
    val personOppdrag: Oppdrag,
    private val tidsstempel: LocalDateTime,
    tilstand: Tilstand,
    val type: Utbetalingtype,
    private val maksdato: LocalDate,
    private val forbrukteSykedager: Int?,
    private val gjenståendeSykedager: Int?,
    // annulleringer brukes ikke mer, men finnes av historiske årsaker da listen kan være "ikke-tom" på tidligere utbetalinger
    private val annulleringer: List<Utbetaling>,
    private var vurdering: Vurdering?,
    private var overføringstidspunkt: LocalDateTime?,
    private var avstemmingsnøkkel: Long?,
    private var avsluttet: LocalDateTime?,
    private var oppdatert: LocalDateTime = tidsstempel
) : Aktivitetskontekst {
    val view
        get() = UtbetalingView(
            id = id,
            korrelasjonsId = korrelasjonsId,
            periode = periode,
            utbetalingstidslinje = utbetalingstidslinje,
            arbeidsgiverOppdrag = arbeidsgiverOppdrag,
            personOppdrag = personOppdrag,
            status = tilstand.status,
            type = type,
            annulleringer = annulleringer.map { it.id },
            erAvsluttet = erAvsluttet()
        )

    constructor(
        periode: Periode,
        utbetalingstidslinje: Utbetalingstidslinje,
        arbeidsgiverOppdrag: Oppdrag,
        personOppdrag: Oppdrag,
        type: Utbetalingtype,
        maksdato: LocalDate,
        forbrukteSykedager: Int?,
        gjenståendeSykedager: Int?,
        korrelasjonsId: UUID = UUID.randomUUID()
    ) : this(
        UUID.randomUUID(),
        korrelasjonsId,
        periode,
        utbetalingstidslinje,
        arbeidsgiverOppdrag,
        personOppdrag,
        LocalDateTime.now(),
        Ny,
        type,
        maksdato,
        forbrukteSykedager,
        gjenståendeSykedager,
        emptyList(),
        null,
        null,
        null,
        null
    )

    internal var tilstand: Tilstand = tilstand
        private set

    private val stønadsdager get() = Oppdrag.stønadsdager(arbeidsgiverOppdrag, personOppdrag)
    private var forrigeHendelse: UtbetalingmodulHendelse? = null

    private fun harHåndtert(hendelse: UtbetalingmodulHendelse) =
        (hendelse === forrigeHendelse).also { forrigeHendelse = hendelse }

    fun periode() = periode
    private fun gyldig() = tilstand !in setOf(Ny, Forkastet)
    private fun erUbetalt() = tilstand == Ubetalt
    private fun erUtbetalt() = tilstand == Utbetalt || tilstand == Annullert
    private fun erAktiv() = erAvsluttet() || erInFlight()
    private fun erAktivEllerUbetalt() = erAktiv() || erUbetalt()
    fun erInFlight() = tilstand == Overført
    fun erAnnulleringInFlight() = erAnnullering() && erInFlight()
    fun erAvsluttet() = erUtbetalt() || tilstand == GodkjentUtenUtbetaling
    private fun erAnnullering() = type == ANNULLERING

    // this kan revurdere other gitt at fagsystemId == other.fagsystemId,
    // og at this er lik den siste aktive utbetalingen for fagsystemIden
    fun hørerSammen(other: Utbetaling) =
        this.korrelasjonsId == other.korrelasjonsId

    fun harOppdragMedUtbetalinger() =
        arbeidsgiverOppdrag.harUtbetalinger() || personOppdrag.harUtbetalinger()

    fun harDelvisRefusjon() = arbeidsgiverOppdrag.harUtbetalinger() && personOppdrag.harUtbetalinger()

    fun erKlarForGodkjenning() = personOppdrag.erKlarForGodkjenning() && arbeidsgiverOppdrag.erKlarForGodkjenning()

    fun opprett(eventBus: UtbetalingEventBus, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedUtbetalingkontekst = aktivitetslogg.kontekst(this)
        tilstand.opprett(this, eventBus, aktivitetsloggMedUtbetalingkontekst)
    }

    fun håndterUtbetalingmodulHendelse(eventBus: UtbetalingEventBus, utbetaling: UtbetalingmodulHendelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedUtbetalingkontekst = aktivitetslogg.kontekst(this)
        if (!relevantFor(utbetaling)) return
        if (harHåndtert(utbetaling)) return
        tilstand.kvittér(this, eventBus, utbetaling, aktivitetsloggMedUtbetalingkontekst)
    }

    private fun relevantFor(utbetaling: UtbetalingmodulHendelse) =
        utbetaling.utbetalingId == this.id && (utbetaling.fagsystemId in setOf(this.arbeidsgiverOppdrag.fagsystemId, this.personOppdrag.fagsystemId))

    fun håndterSimuleringHendelse(simulering: SimuleringHendelse) {
        if (simulering.utbetalingId != this.id) return
        personOppdrag.håndterSimulering(simulering)
        arbeidsgiverOppdrag.håndterSimulering(simulering)
    }

    fun simuler(aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedUtbetalingkontekst = aktivitetslogg.kontekst(this)
        tilstand.simuler(this, aktivitetsloggMedUtbetalingkontekst)
    }

    fun håndterUtbetalingpåminnelse(aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedUtbetalingkontekst = aktivitetslogg.kontekst(this)
        tilstand.håndterPåminnelse(this, aktivitetsloggMedUtbetalingkontekst)
    }

    fun valider(simulering: SimuleringHendelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedUtbetalingkontekst = aktivitetslogg.kontekst(this)
        validerSimuleringsresultat(simulering, aktivitetsloggMedUtbetalingkontekst, arbeidsgiverOppdrag)
        validerSimuleringsresultat(simulering, aktivitetsloggMedUtbetalingkontekst, personOppdrag)
    }

    private fun validerSimuleringsresultat(simuleringHendelse: SimuleringHendelse, aktivitetslogg: IAktivitetslogg, oppdrag: Oppdrag) {
        if (simuleringHendelse.fagsystemId != oppdrag.fagsystemId) return
        if (simuleringHendelse.fagområde != oppdrag.fagområde) return
        if (!simuleringHendelse.simuleringOK) return aktivitetslogg.info("Feil under simulering: ${simuleringHendelse.melding}")
        val simuleringsResultat = simuleringHendelse.simuleringsResultat ?: return aktivitetslogg.info("Ingenting ble simulert")
        val harNegativtTotalbeløp = simuleringsResultat.totalbeløp < 0
        if (harNegativtTotalbeløp) aktivitetslogg.varsel(Varselkode.RV_SI_3)
    }

    fun lagAnnulleringsutbetaling(aktivitetslogg: IAktivitetslogg): Utbetaling {
        val aktivitetsloggMedUtbetalingkontekst = aktivitetslogg.kontekst(this)
        return lagAnnullering(aktivitetsloggMedUtbetalingkontekst)
    }

    private fun lagAnnullering(aktivitetslogg: IAktivitetslogg): Utbetaling {
        return when (tilstand) {
            Utbetalt,
            GodkjentUtenUtbetaling -> {
                Utbetaling(
                    periode = periode,
                    utbetalingstidslinje = utbetalingstidslinje,
                    arbeidsgiverOppdrag = arbeidsgiverOppdrag.annuller(aktivitetslogg),
                    personOppdrag = personOppdrag.annuller(aktivitetslogg),
                    type = ANNULLERING,
                    maksdato = LocalDate.MAX,
                    forbrukteSykedager = null,
                    gjenståendeSykedager = null,
                    korrelasjonsId = korrelasjonsId
                ).also { aktivitetslogg.info("Oppretter annullering med id ${it.id}") }
            }

            Annullert,
            Forkastet,
            IkkeGodkjent,
            Ny,
            Overført,
            Ubetalt -> error("Forventet ikke å annullere på utbetaling=${id} i tilstand=${this::class.simpleName}")
        }
    }

    fun forkast(eventBus: UtbetalingEventBus, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedUtbetalingkontekst = aktivitetslogg.kontekst(this)
        tilstand.forkast(this, eventBus, aktivitetsloggMedUtbetalingkontekst)
    }

    fun arbeidsgiverOppdrag() = arbeidsgiverOppdrag

    fun personOppdrag() = personOppdrag

    override fun toSpesifikkKontekst() =
        SpesifikkKontekst("Utbetaling", mapOf("utbetalingId" to "$id"))

    fun ikkeGodkjent(eventBus: UtbetalingEventBus, aktivitetslogg: IAktivitetslogg, vurdering: Vurdering) {
        val aktivitetsloggMedUtbetalingkontekst = aktivitetslogg.kontekst(this)
        tilstand.ikkeGodkjent(this, eventBus, aktivitetsloggMedUtbetalingkontekst, vurdering)
    }

    fun godkjent(eventBus: UtbetalingEventBus, aktivitetslogg: IAktivitetslogg, vurdering: Vurdering) {
        val aktivitetsloggMedUtbetalingkontekst = aktivitetslogg.kontekst(this)
        tilstand.godkjent(this, eventBus, aktivitetsloggMedUtbetalingkontekst, vurdering)
    }

    private fun tilstand(eventBus: UtbetalingEventBus, neste: Tilstand, aktivitetslogg: IAktivitetslogg) {
        oppdatert = LocalDateTime.now()
        if (Oppdrag.ingenFeil(arbeidsgiverOppdrag, personOppdrag) && !Oppdrag.synkronisert(
                arbeidsgiverOppdrag,
                personOppdrag
            )
        ) return aktivitetslogg.info("Venter på status på det andre oppdraget før vi kan gå videre")
        val forrigeTilstand = tilstand
        tilstand = neste
        eventBus.utbetalingEndret(
            id,
            type,
            arbeidsgiverOppdrag,
            personOppdrag,
            forrigeTilstand.status,
            neste.status,
            korrelasjonsId
        )
        tilstand.entering(this, eventBus, aktivitetslogg)
    }

    private fun nyUtbetaling(
        aktivitetslogg: IAktivitetslogg,
        type: Utbetalingtype,
        vedtaksperiode: Periode,
        utbetalingstidslinje: Utbetalingstidslinje,
        kladd: Utbetalingkladd,
        maksdato: LocalDate,
        forbrukteSykedager: Int,
        gjenståendeSykedager: Int
    ): Utbetaling {
        val nyttArbeidsgiveroppdrag = byggViderePåOppdrag(aktivitetslogg, vedtaksperiode, this.arbeidsgiverOppdrag, kladd.arbeidsgiveroppdrag)
        val nyttPersonoppdrag = byggViderePåOppdrag(aktivitetslogg, vedtaksperiode, this.personOppdrag, kladd.personoppdrag)

        val tidligereUtbetalingstidslinje = this.utbetalingstidslinje.fremTilOgMed(vedtaksperiode.start.forrigeDag)
        val nyUtbetalingstidslinje = tidligereUtbetalingstidslinje + utbetalingstidslinje

        return Utbetaling(
            korrelasjonsId = this.korrelasjonsId,
            periode = this.periode.start til vedtaksperiode.endInclusive,
            utbetalingstidslinje = nyUtbetalingstidslinje,
            arbeidsgiverOppdrag = nyttArbeidsgiveroppdrag,
            personOppdrag = nyttPersonoppdrag,
            type = type,
            maksdato = maksdato,
            forbrukteSykedager = forbrukteSykedager,
            gjenståendeSykedager = gjenståendeSykedager
        )
    }

    private fun byggViderePåOppdrag(aktivitetslogg: IAktivitetslogg, vedtaksperiode: Periode, historiskOppdrag: Oppdrag, nyttOppdrag: Oppdrag): Oppdrag {
        /* ta bort eventuell hale som er avkortet */
        val linjerFremTilOgMedUtbetalingsaken = historiskOppdrag.begrensTil(vedtaksperiode.endInclusive)

        /* linjer forut før perioden */
        val linjerFørVedtaksperioden = linjerFremTilOgMedUtbetalingsaken.begrensTil(vedtaksperiode.start.forrigeDag)

        /* legg inn endringer fra perioden og sy sammen */
        val nyeArbeidsgiverlinjer = linjerFørVedtaksperioden + nyttOppdrag

        return Oppdrag(historiskOppdrag.mottaker, historiskOppdrag.fagområde, nyeArbeidsgiverlinjer).minus(historiskOppdrag, aktivitetslogg)
    }

    companion object {

        val log: Logger = LoggerFactory.getLogger("Utbetaling")
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        private const val systemident = "SPLEIS"

        fun lagUtbetaling(
            utbetalinger: List<Utbetaling>,
            vedtaksperiodekladd: Utbetalingkladd,
            utbetalingstidslinje: Utbetalingstidslinje,
            periode: Periode,
            aktivitetslogg: IAktivitetslogg,
            maksdato: LocalDate,
            forbrukteSykedager: Int,
            gjenståendeSykedager: Int,
            type: Utbetalingtype = UTBETALING
        ): Utbetaling {
            check(utbetalingstidslinje.periode() == periode) {
                "forventer ikke at utbetalingstidslinje skal være forskjellig fra vedtaksperioden"
            }

            val forrigeUtbetalte = utbetalinger.aktive(periode)
            check(forrigeUtbetalte.size <= 1) { "finner flere enn én korrelerende utbetaling for periode $periode: ${forrigeUtbetalte.map { it.id }}" }
            val korrelerendeUtbetaling = forrigeUtbetalte.firstOrNull()

            val utbetalingen = korrelerendeUtbetaling?.nyUtbetaling(
                aktivitetslogg = aktivitetslogg,
                type = type,
                vedtaksperiode = periode,
                utbetalingstidslinje = utbetalingstidslinje,
                kladd = vedtaksperiodekladd,
                maksdato = maksdato,
                forbrukteSykedager = forbrukteSykedager,
                gjenståendeSykedager = gjenståendeSykedager
            ) ?: Utbetaling(
                periode = periode,
                utbetalingstidslinje = utbetalingstidslinje,
                arbeidsgiverOppdrag = vedtaksperiodekladd.arbeidsgiveroppdrag,
                personOppdrag = vedtaksperiodekladd.personoppdrag,
                type = type,
                maksdato = maksdato,
                forbrukteSykedager = forbrukteSykedager,
                gjenståendeSykedager = gjenståendeSykedager
            )
            return utbetalingen
        }

        fun lagTomUtbetaling(vedtaksperiodekladd: Utbetalingkladd, periode: Periode, type: Utbetalingtype) = Utbetaling(
            periode = periode,
            utbetalingstidslinje = Utbetalingstidslinje(),
            arbeidsgiverOppdrag = vedtaksperiodekladd.arbeidsgiveroppdrag,
            personOppdrag = vedtaksperiodekladd.personoppdrag,
            type = type,
            maksdato = LocalDate.MAX,
            forbrukteSykedager = null,
            gjenståendeSykedager = null
        )

        fun List<Utbetaling>.aktive() = grupperUtbetalinger(Utbetaling::erAktiv)
        fun List<Utbetaling>.aktiveMedUbetalte() = grupperUtbetalinger(Utbetaling::erAktivEllerUbetalt)
        fun List<Utbetaling>.aktive(periode: Periode) = this
            .aktive()
            .filter { utbetaling -> utbetaling.periode.overlapperMed(periode) }

        private fun Collection<Utbetaling>.grupperUtbetalinger(filter: (Utbetaling) -> Boolean) =
            this
                .asSequence()
                .filter { it.gyldig() }
                .groupBy { it.korrelasjonsId }
                .filter { (_, utbetalinger) -> utbetalinger.any(filter) }
                .map { (_, utbetalinger) -> utbetalinger.sortedBy { it.tidsstempel } }
                .map { it.last(filter) }
                .sortedBy { it.periode.endInclusive }
                .filterNot(Utbetaling::erAnnullering)
                .toList()

        fun List<Utbetaling>.tillaterOpprettelseAvUtbetaling(other: Utbetaling): Boolean {
            return !harOverlappendeUtbetalingsperioder(other)
        }

        private fun List<Utbetaling>.harOverlappendeUtbetalingsperioder(nyUtbetaling: Utbetaling): Boolean {
            return this
                .aktiveMedUbetalte()
                .filterNot { it.hørerSammen(nyUtbetaling) }
                .flatMap { other ->
                    other.arbeidsgiverOppdrag.overlappendeLinjer(nyUtbetaling.arbeidsgiverOppdrag) + other.personOppdrag.overlappendeLinjer(nyUtbetaling.personOppdrag)
                }
                .also { overlappendeLinjer ->
                    if (overlappendeLinjer.isNotEmpty()) {
                        val feilmelding = "Vi har opprettet en utbetaling med periode ${nyUtbetaling.periode} & " +
                            "korrelasjonsId ${nyUtbetaling.korrelasjonsId} som overlapper med " +
                            "oppdragslinjer i eksisterende utbetalinger:\n" +
                            overlappendeLinjer.joinToString(separator = "\n") { (fagområde, fagsystemId, linje) -> "* $fagområde - $fagsystemId - ${linje.periode}" }
                        sikkerlogg.error(feilmelding, kv("fødselsnummer", nyUtbetaling.personOppdrag.mottaker))
                    }
                }
                .isNotEmpty()
        }

        fun List<Utbetaling>.validerNyUtbetaling(nyUtbetaling: Utbetaling) {
            if (nyUtbetaling.erAnnullering()) return
            val ikkeUtbetalt = filter { it.tilstand is Ubetalt }.takeUnless { it.isEmpty() } ?: return
            error("Hvordan kan det ha seg at vi lager en ny utbetaling for ${nyUtbetaling.periode} samtidig som utbetalingene ${ikkeUtbetalt.joinToString { "${it.id}" }} står som IKKE_UTBETALT? Hvorfor er ikke disse forkastet?")
        }

        fun List<Utbetaling>.kunEnIkkeUtbetalt() {
            val ikkeUtbetalte = this
                .filterNot { it.erAnnullering() }
                .filter { it.erUbetalt() }
            check(ikkeUtbetalte.size <= 1) {
                "Det er mer enn én utbetaling som er IKKE_UTBETALT:\n${ikkeUtbetalte.joinToString(separator = "\n") { "* ${it.id} - ${it.periode}" }}"
            }
        }

        private fun Oppdrag.overlappendeLinjer(nyttOppdrag: Oppdrag): List<Triple<Fagområde, String, Utbetalingslinje>> {
            return nyttOppdrag.linjerUtenOpphør().flatMap { linje ->
                this
                    .linjerUtenOpphør()
                    .filter { linje.periode.overlapperMed(it.periode) }
                    .map { Triple(this.fagområde, this.fagsystemId, it) }
            }
        }

        // kan forkaste dersom ingen utbetalinger er utbetalt/in flight, eller de er annullert
        fun kanForkastes(vedtaksperiodeUtbetalinger: List<Utbetaling>, arbeidsgiverUtbetalinger: List<Utbetaling>): Boolean {
            val annulleringer = arbeidsgiverUtbetalinger.filter { it.erAnnullering() && it.tilstand != Forkastet }
            return vedtaksperiodeUtbetalinger.filter { it.erAktiv() }.all { utbetaling ->
                annulleringer.any { annullering -> annullering.hørerSammen(utbetaling) }
            }
        }

        fun gjenopprett(dto: UtbetalingInnDto, utbetalinger: List<Utbetaling>): Utbetaling {
            return Utbetaling(
                id = dto.id,
                korrelasjonsId = dto.korrelasjonsId,
                periode = Periode.gjenopprett(dto.periode),
                utbetalingstidslinje = Utbetalingstidslinje.gjenopprett(dto.utbetalingstidslinje),
                arbeidsgiverOppdrag = Oppdrag.gjenopprett(dto.arbeidsgiverOppdrag),
                personOppdrag = Oppdrag.gjenopprett(dto.personOppdrag),
                tidsstempel = dto.tidsstempel,
                tilstand = when (dto.tilstand) {
                    UtbetalingTilstandDto.ANNULLERT -> Annullert
                    UtbetalingTilstandDto.FORKASTET -> Forkastet
                    UtbetalingTilstandDto.GODKJENT_UTEN_UTBETALING -> GodkjentUtenUtbetaling
                    UtbetalingTilstandDto.IKKE_GODKJENT -> IkkeGodkjent
                    UtbetalingTilstandDto.IKKE_UTBETALT -> Ubetalt
                    UtbetalingTilstandDto.NY -> Ny
                    UtbetalingTilstandDto.OVERFØRT -> Overført
                    UtbetalingTilstandDto.UTBETALT -> Utbetalt
                },
                type = when (dto.type) {
                    UtbetalingtypeDto.UTBETALING -> UTBETALING
                    UtbetalingtypeDto.ANNULLERING -> ANNULLERING
                    UtbetalingtypeDto.ETTERUTBETALING -> ETTERUTBETALING
                    UtbetalingtypeDto.REVURDERING -> REVURDERING
                },
                maksdato = dto.maksdato,
                forbrukteSykedager = dto.forbrukteSykedager,
                gjenståendeSykedager = dto.gjenståendeSykedager,
                annulleringer = dto.annulleringer.map { annulleringId -> utbetalinger.single { it.id == annulleringId } },
                vurdering = dto.vurdering?.let { Vurdering.gjenopprett(it) },
                overføringstidspunkt = dto.overføringstidspunkt,
                avstemmingsnøkkel = dto.avstemmingsnøkkel,
                avsluttet = dto.avsluttet,
                oppdatert = dto.oppdatert
            )
        }
    }

    private fun overførBegge(aktivitetslogg: IAktivitetslogg) {
        vurdering?.overfør(aktivitetslogg, arbeidsgiverOppdrag, maksdato.takeUnless { type == ANNULLERING })
        vurdering?.overfør(aktivitetslogg, personOppdrag, maksdato.takeUnless { type == ANNULLERING })
    }

    private fun håndterKvittering(eventBus: UtbetalingEventBus, hendelse: UtbetalingmodulHendelse, aktivitetslogg: IAktivitetslogg) {
        when (hendelse.status) {
            Oppdragstatus.OVERFØRT,
            Oppdragstatus.AKSEPTERT -> {} // all is good
            Oppdragstatus.AKSEPTERT_MED_FEIL -> aktivitetslogg.info("Utbetalingen ble gjennomført, men med advarsel")
            Oppdragstatus.AVVIST,
            Oppdragstatus.FEIL -> aktivitetslogg.info("Utbetaling feilet med status ${hendelse.status}. Feilmelding fra Oppdragsystemet: ${hendelse.melding}")
        }

        if (Oppdrag.harFeil(arbeidsgiverOppdrag, personOppdrag)) return

        val nesteTilstand = when (hendelse.status) {
            Oppdragstatus.OVERFØRT,
            Oppdragstatus.AVVIST,
            Oppdragstatus.FEIL -> return // utbetaling gjør retry ved neste påminnelse

            Oppdragstatus.AKSEPTERT,
            Oppdragstatus.AKSEPTERT_MED_FEIL -> when (Oppdrag.harFeil(arbeidsgiverOppdrag, personOppdrag)) {
                true -> return // må vente på at begge oppdrag er uten feil
                false -> when (type) {
                    UTBETALING,
                    REVURDERING -> Utbetalt
                    ANNULLERING -> Annullert
                    ETTERUTBETALING -> error("Forventer ikke denne typen her")
                }
            }
        }
        tilstand(eventBus, nesteTilstand, aktivitetslogg)
    }

    fun overlapperMed(other: Periode): Boolean {
        return this.periode.overlapperMed(other)
    }

    fun overlapperMed(other: Utbetaling): Boolean {
        return this.periode.overlapperMed(other.periode)
    }

    private fun lagreOverføringsinformasjon(aktivitetslogg: IAktivitetslogg, avstemmingsnøkkel: Long, tidspunkt: LocalDateTime) {
        aktivitetslogg.info("Utbetalingen ble overført til Oppdrag/UR $tidspunkt, og har fått avstemmingsnøkkel $avstemmingsnøkkel.\n")
        if (this.avstemmingsnøkkel != null && this.avstemmingsnøkkel != avstemmingsnøkkel)
            aktivitetslogg.info("Avstemmingsnøkkel har endret seg.\nTidligere verdi: ${this.avstemmingsnøkkel}")
        if (this.overføringstidspunkt == null) this.overføringstidspunkt = tidspunkt
        if (this.avstemmingsnøkkel == null) this.avstemmingsnøkkel = avstemmingsnøkkel
    }

    override fun toString() = "$type(${tilstand.status}) - $periode"

    internal sealed interface Tilstand {
        val status: Utbetalingstatus
        fun forkast(utbetaling: Utbetaling, eventBus: UtbetalingEventBus, aktivitetslogg: IAktivitetslogg) {}

        fun opprett(utbetaling: Utbetaling, eventBus: UtbetalingEventBus, aktivitetslogg: IAktivitetslogg) {
            error("Forventet ikke å opprette utbetaling i tilstand=${this::class.simpleName}")
        }

        fun ikkeGodkjent(
            utbetaling: Utbetaling,
            eventBus: UtbetalingEventBus,
            aktivitetslogg: IAktivitetslogg,
            vurdering: Vurdering
        ) {
            error("Forventet ikke godkjenning på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
        }

        fun godkjent(
            utbetaling: Utbetaling,
            eventBus: UtbetalingEventBus,
            aktivitetslogg: IAktivitetslogg,
            vurdering: Vurdering
        ) {
            error("Forventet ikke godkjenning på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
        }

        fun kvittér(utbetaling: Utbetaling, eventBus: UtbetalingEventBus, hendelse: UtbetalingmodulHendelse, aktivitetslogg: IAktivitetslogg) {
            error("Forventet ikke kvittering på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
        }

        fun håndterPåminnelse(utbetaling: Utbetaling, påminnelse: IAktivitetslogg) {
            påminnelse.info("Utbetaling ble påminnet, men gjør ingenting")
        }

        fun simuler(utbetaling: Utbetaling, aktivitetslogg: IAktivitetslogg) {
            error("Forventet ikke simulering på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
        }

        fun entering(utbetaling: Utbetaling, eventBus: UtbetalingEventBus, aktivitetslogg: IAktivitetslogg) {}
    }

    internal data object Ny : Tilstand {
        override val status = Utbetalingstatus.NY
        override fun opprett(utbetaling: Utbetaling, eventBus: UtbetalingEventBus, aktivitetslogg: IAktivitetslogg) {
            utbetaling.tilstand(eventBus, Ubetalt, aktivitetslogg)
        }
    }

    internal data object Ubetalt : Tilstand {
        override val status = Utbetalingstatus.IKKE_UTBETALT
        override fun forkast(utbetaling: Utbetaling, eventBus: UtbetalingEventBus, aktivitetslogg: IAktivitetslogg) {
            aktivitetslogg.info("Forkaster utbetaling")
            utbetaling.tilstand(eventBus, Forkastet, aktivitetslogg)
        }

        override fun ikkeGodkjent(utbetaling: Utbetaling, eventBus: UtbetalingEventBus, aktivitetslogg: IAktivitetslogg, vurdering: Vurdering) {
            utbetaling.vurdering = vurdering
            utbetaling.tilstand(eventBus, IkkeGodkjent, aktivitetslogg)
        }

        override fun godkjent(utbetaling: Utbetaling, eventBus: UtbetalingEventBus, aktivitetslogg: IAktivitetslogg, vurdering: Vurdering) {
            utbetaling.vurdering = vurdering
            utbetaling.tilstand(eventBus, when {
                utbetaling.harOppdragMedUtbetalinger() -> Overført
                utbetaling.type == ANNULLERING -> Annullert
                else -> GodkjentUtenUtbetaling
            }, aktivitetslogg)
        }

        override fun simuler(utbetaling: Utbetaling, aktivitetslogg: IAktivitetslogg) {
            utbetaling.arbeidsgiverOppdrag.simuler(aktivitetslogg, utbetaling.maksdato, systemident)
            utbetaling.personOppdrag.simuler(aktivitetslogg, utbetaling.maksdato, systemident)
        }
    }

    internal data object GodkjentUtenUtbetaling : Tilstand {
        override val status = Utbetalingstatus.GODKJENT_UTEN_UTBETALING
        override fun entering(utbetaling: Utbetaling, eventBus: UtbetalingEventBus, aktivitetslogg: IAktivitetslogg) {
            check(!utbetaling.harOppdragMedUtbetalinger())
            utbetaling.vurdering?.avsluttetUtenUtbetaling(utbetaling, eventBus)
            utbetaling.avsluttet = LocalDateTime.now()
        }
    }

    internal data object Overført : Tilstand {
        override val status = Utbetalingstatus.OVERFØRT
        override fun entering(utbetaling: Utbetaling, eventBus: UtbetalingEventBus, aktivitetslogg: IAktivitetslogg) {
            utbetaling.overførBegge(aktivitetslogg)
        }

        override fun håndterPåminnelse(utbetaling: Utbetaling, påminnelse: IAktivitetslogg) {
            utbetaling.overførBegge(påminnelse)
        }

        override fun kvittér(utbetaling: Utbetaling, eventBus: UtbetalingEventBus, hendelse: UtbetalingmodulHendelse, aktivitetslogg: IAktivitetslogg) {
            utbetaling.lagreOverføringsinformasjon(aktivitetslogg, hendelse.avstemmingsnøkkel, hendelse.overføringstidspunkt)
            utbetaling.arbeidsgiverOppdrag.lagreOverføringsinformasjon(hendelse)
            utbetaling.personOppdrag.lagreOverføringsinformasjon(hendelse)
            utbetaling.håndterKvittering(eventBus, hendelse, aktivitetslogg)
        }
    }

    internal data object Annullert : Tilstand {
        override val status = Utbetalingstatus.ANNULLERT
        override fun entering(utbetaling: Utbetaling, eventBus: UtbetalingEventBus, aktivitetslogg: IAktivitetslogg) {
            utbetaling.vurdering?.annullert(utbetaling, eventBus)
            utbetaling.avsluttet = LocalDateTime.now()
        }
    }

    internal data object Utbetalt : Tilstand {
        override val status = Utbetalingstatus.UTBETALT
        override fun entering(utbetaling: Utbetaling, eventBus: UtbetalingEventBus, aktivitetslogg: IAktivitetslogg) {
            utbetaling.vurdering?.utbetalt(utbetaling, eventBus)
            utbetaling.avsluttet = LocalDateTime.now()
        }
    }

    internal data object IkkeGodkjent : Tilstand {
        override val status = Utbetalingstatus.IKKE_GODKJENT
    }

    internal data object Forkastet : Tilstand {
        override val status = Utbetalingstatus.FORKASTET
    }

    class Vurdering(
        private val godkjent: Boolean,
        private val ident: String,
        private val epost: String,
        private val tidspunkt: LocalDateTime,
        private val automatiskBehandling: Boolean
    ) {
        fun annullert(utbetaling: Utbetaling, eventBus: UtbetalingEventBus) {
            eventBus.utbetalingAnnullert(
                id = utbetaling.id,
                korrelasjonsId = utbetaling.korrelasjonsId,
                periode = utbetaling.periode,
                personFagsystemId = utbetaling.personOppdrag.fagsystemId,
                godkjenttidspunkt = tidspunkt,
                saksbehandlerEpost = epost,
                saksbehandlerIdent = ident,
                arbeidsgiverFagsystemId = utbetaling.arbeidsgiverOppdrag.fagsystemId
            )
        }

        fun utbetalt(utbetaling: Utbetaling, eventBus: UtbetalingEventBus) {
            eventBus.utbetalingUtbetalt(
                utbetaling.id,
                utbetaling.korrelasjonsId,
                utbetaling.type,
                utbetaling.periode,
                utbetaling.maksdato,
                utbetaling.forbrukteSykedager!!,
                utbetaling.gjenståendeSykedager!!,
                utbetaling.stønadsdager,
                utbetaling.arbeidsgiverOppdrag,
                utbetaling.personOppdrag,
                epost,
                tidspunkt,
                automatiskBehandling,
                utbetaling.utbetalingstidslinje,
                ident
            )
        }

        fun avsluttetUtenUtbetaling(utbetaling: Utbetaling, eventBus: UtbetalingEventBus) {
            eventBus.utbetalingUtenUtbetaling(
                utbetaling.id,
                utbetaling.korrelasjonsId,
                utbetaling.type,
                utbetaling.periode,
                utbetaling.maksdato,
                utbetaling.forbrukteSykedager!!,
                utbetaling.gjenståendeSykedager!!,
                utbetaling.stønadsdager,
                utbetaling.personOppdrag,
                ident,
                utbetaling.arbeidsgiverOppdrag,
                tidspunkt,
                automatiskBehandling,
                utbetaling.utbetalingstidslinje,
                epost
            )
        }

        fun overfør(aktivitetslogg: IAktivitetslogg, oppdrag: Oppdrag, maksdato: LocalDate?) {
            oppdrag.overfør(aktivitetslogg, maksdato, ident)
        }

        fun dto() = UtbetalingVurderingDto(
            godkjent = godkjent,
            ident = ident,
            epost = epost,
            tidspunkt = tidspunkt,
            automatiskBehandling = automatiskBehandling
        )

        internal companion object {
            fun gjenopprett(dto: UtbetalingVurderingDto): Vurdering {
                return Vurdering(
                    godkjent = dto.godkjent,
                    ident = dto.ident,
                    epost = dto.epost,
                    tidspunkt = dto.tidspunkt,
                    automatiskBehandling = dto.automatiskBehandling
                )
            }
        }
    }

    fun dto() = UtbetalingUtDto(
        id = this.id,
        korrelasjonsId = this.korrelasjonsId,
        periode = this.periode.dto(),
        utbetalingstidslinje = this.utbetalingstidslinje.dto(),
        arbeidsgiverOppdrag = this.arbeidsgiverOppdrag.dto(),
        personOppdrag = this.personOppdrag.dto(),
        tidsstempel = this.tidsstempel,
        tilstand = when (tilstand) {
            Annullert -> UtbetalingTilstandDto.ANNULLERT
            Forkastet -> UtbetalingTilstandDto.FORKASTET
            GodkjentUtenUtbetaling -> UtbetalingTilstandDto.GODKJENT_UTEN_UTBETALING
            IkkeGodkjent -> UtbetalingTilstandDto.IKKE_GODKJENT
            Ny -> UtbetalingTilstandDto.NY
            Overført -> UtbetalingTilstandDto.OVERFØRT
            Ubetalt -> UtbetalingTilstandDto.IKKE_UTBETALT
            Utbetalt -> UtbetalingTilstandDto.UTBETALT
        },
        type = when (type) {
            UTBETALING -> UtbetalingtypeDto.UTBETALING
            ETTERUTBETALING -> UtbetalingtypeDto.ETTERUTBETALING
            ANNULLERING -> UtbetalingtypeDto.ANNULLERING
            REVURDERING -> UtbetalingtypeDto.REVURDERING
        },
        maksdato = this.maksdato,
        forbrukteSykedager = this.forbrukteSykedager,
        gjenståendeSykedager = this.gjenståendeSykedager,
        annulleringer = this.annulleringer.map { it.id },
        vurdering = this.vurdering?.dto(),
        overføringstidspunkt = overføringstidspunkt,
        avstemmingsnøkkel = avstemmingsnøkkel,
        avsluttet = avsluttet,
        oppdatert = oppdatert
    )
}

enum class Utbetalingstatus {
    NY,
    IKKE_UTBETALT,
    IKKE_GODKJENT,
    OVERFØRT,
    UTBETALT,
    GODKJENT,
    GODKJENT_UTEN_UTBETALING,
    ANNULLERT,
    FORKASTET;
}

enum class Utbetalingtype { UTBETALING, ETTERUTBETALING, ANNULLERING, REVURDERING }
enum class Endringskode {
    NY, UEND, ENDR;

    companion object {
        fun gjenopprett(dto: EndringskodeDto) = when (dto) {
            EndringskodeDto.ENDR -> ENDR
            EndringskodeDto.NY -> NY
            EndringskodeDto.UEND -> UEND
        }
    }
}

enum class Klassekode(val verdi: String) {
    RefusjonIkkeOpplysningspliktig(verdi = "SPREFAG-IOP"),
    SykepengerArbeidstakerOrdinær(verdi = "SPATORD"),
    SelvstendigNæringsdrivendeOppgavepliktig(verdi = "SPSND-OP"),
    SelvstendigNæringsdrivendeFisker(verdi = "SPSNDFISK"),
    SelvstendigNæringsdrivendeJordbrukOgSkogbruk(verdi = "SPSNDJORD"),
    SelvstendigNæringsdrivendeBarnepasserOppgavepliktig(verdi = "SPSNDDM-OP");

    companion object {
        private val map = entries.associateBy(Klassekode::verdi)
        fun from(verdi: String) = requireNotNull(map[verdi]) { "Støtter ikke klassekode: $verdi" }
        fun gjenopprett(dto: KlassekodeDto) = when (dto) {
            KlassekodeDto.RefusjonIkkeOpplysningspliktig -> RefusjonIkkeOpplysningspliktig
            KlassekodeDto.SykepengerArbeidstakerOrdinær -> SykepengerArbeidstakerOrdinær
            KlassekodeDto.SelvstendigNæringsdrivendeOppgavepliktig -> SelvstendigNæringsdrivendeOppgavepliktig
            KlassekodeDto.SelvstendigNæringsdrivendeBarnepasserOppgavepliktig -> SelvstendigNæringsdrivendeBarnepasserOppgavepliktig
            KlassekodeDto.SelvstendigNæringsdrivendeFisker -> SelvstendigNæringsdrivendeFisker
            KlassekodeDto.SelvstendigNæringsdrivendeJordbrukOgSkogbruk -> SelvstendigNæringsdrivendeJordbrukOgSkogbruk
        }
    }
}

data class UtbetalingView(
    val id: UUID,
    val korrelasjonsId: UUID,
    val periode: Periode,
    val utbetalingstidslinje: Utbetalingstidslinje,
    val arbeidsgiverOppdrag: Oppdrag,
    val personOppdrag: Oppdrag,
    val status: Utbetalingstatus,
    val type: Utbetalingtype,
    val annulleringer: List<UUID>,
    val erAvsluttet: Boolean
)
