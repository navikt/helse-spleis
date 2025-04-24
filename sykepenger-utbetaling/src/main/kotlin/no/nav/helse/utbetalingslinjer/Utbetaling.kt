package no.nav.helse.utbetalingslinjer

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.dto.EndringskodeDto
import no.nav.helse.dto.KlassekodeDto
import no.nav.helse.dto.UtbetalingTilstandDto
import no.nav.helse.dto.UtbetalingVurderingDto
import no.nav.helse.dto.UtbetalingtypeDto
import no.nav.helse.dto.deserialisering.UtbetalingInnDto
import no.nav.helse.dto.serialisering.UtbetalingUtDto
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.AnnullerUtbetalingHendelse
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.SimuleringHendelse
import no.nav.helse.hendelser.UtbetalingmodulHendelse
import no.nav.helse.hendelser.UtbetalingpåminnelseHendelse
import no.nav.helse.hendelser.UtbetalingsavgjørelseHendelse
import no.nav.helse.hendelser.valider
import no.nav.helse.hendelser.vurdering
import no.nav.helse.nesteDag
import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_11
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_12
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_21
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_6
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_7
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_9
import no.nav.helse.utbetalingslinjer.Oppdrag.Companion.trekkerTilbakePenger
import no.nav.helse.utbetalingslinjer.Oppdrag.Companion.valider
import no.nav.helse.utbetalingslinjer.Utbetalingtype.ANNULLERING
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
            annulleringer = annulleringer.map { it.id }
        )

    constructor(
        korrelerendeUtbetaling: Utbetaling?,
        periode: Periode,
        utbetalingstidslinje: Utbetalingstidslinje,
        arbeidsgiverOppdrag: Oppdrag,
        personOppdrag: Oppdrag,
        type: Utbetalingtype,
        maksdato: LocalDate,
        forbrukteSykedager: Int?,
        gjenståendeSykedager: Int?,
        annulleringer: List<Utbetaling> = emptyList()
    ) : this(
        UUID.randomUUID(),
        korrelerendeUtbetaling?.takeIf { arbeidsgiverOppdrag.tilhører(it.arbeidsgiverOppdrag) || personOppdrag.tilhører(it.personOppdrag) }?.korrelasjonsId ?: UUID.randomUUID(),
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
        annulleringer,
        null,
        null,
        null,
        null
    ) {
        check(annulleringer.all { it.type == ANNULLERING }) { "skal bare ha annulleringer" }
    }

    internal var tilstand: Tilstand = tilstand
        private set

    private val stønadsdager get() = Oppdrag.stønadsdager(arbeidsgiverOppdrag, personOppdrag)
    private val observers = mutableSetOf<UtbetalingObserver>()
    private var forrigeHendelse: UtbetalingmodulHendelse? = null

    private fun harHåndtert(hendelse: UtbetalingmodulHendelse) =
        (hendelse === forrigeHendelse).also { forrigeHendelse = hendelse }

    fun registrer(observer: UtbetalingObserver) {
        observers.add(observer)
    }

    fun periode() = periode
    private fun gyldig() = tilstand !in setOf(Ny, Forkastet)
    fun erUbetalt() = tilstand == Ubetalt
    private fun erUtbetalt() = tilstand == Utbetalt || tilstand == Annullert
    private fun erAktiv() = erAvsluttet() || erInFlight()
    private fun erAktivEllerUbetalt() = erAktiv() || erUbetalt()
    fun erInFlight() = tilstand == Overført || annulleringer.any { it.tilstand == Overført }
    fun erAnnulleringInFlight() = erAnnullering() && erInFlight()
    fun erAvsluttet() = erUtbetalt() || tilstand == GodkjentUtenUtbetaling
    fun erAvvist() = tilstand == IkkeGodkjent
    private fun erAnnullering() = type == ANNULLERING

    fun trekkerTilbakePenger() = listOf(arbeidsgiverOppdrag, personOppdrag).trekkerTilbakePenger()

    // this kan revurdere other gitt at fagsystemId == other.fagsystemId,
    // og at this er lik den siste aktive utbetalingen for fagsystemIden
    fun hørerSammen(other: Utbetaling) =
        this.korrelasjonsId == other.korrelasjonsId

    fun harUtbetalinger() =
        harOppdragMedUtbetalinger() || annulleringer.any { it.harOppdragMedUtbetalinger() }

    private fun harOppdragMedUtbetalinger() =
        arbeidsgiverOppdrag.harUtbetalinger() || personOppdrag.harUtbetalinger()

    fun harDelvisRefusjon() = arbeidsgiverOppdrag.harUtbetalinger() && personOppdrag.harUtbetalinger()

    fun erKlarForGodkjenning() = personOppdrag.erKlarForGodkjenning() && arbeidsgiverOppdrag.erKlarForGodkjenning()

    fun opprett(aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedUtbetalingkontekst = aktivitetslogg.kontekst(this)
        tilstand.opprett(this, aktivitetsloggMedUtbetalingkontekst)
    }

    fun håndter(hendelse: UtbetalingsavgjørelseHendelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedUtbetalingkontekst = aktivitetslogg.kontekst(this)
        if (hendelse.utbetalingId != this.id) return
        hendelse.valider(aktivitetsloggMedUtbetalingkontekst)
        godkjenn(aktivitetsloggMedUtbetalingkontekst, hendelse.vurdering)
    }

    fun håndter(utbetaling: UtbetalingmodulHendelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedUtbetalingkontekst = aktivitetslogg.kontekst(this)
        if (!relevantFor(utbetaling)) return håndterKvitteringForAnnullering(utbetaling, aktivitetsloggMedUtbetalingkontekst)
        if (harHåndtert(utbetaling)) return
        tilstand.kvittér(this, utbetaling, aktivitetsloggMedUtbetalingkontekst)
    }

    private fun håndterKvitteringForAnnullering(hendelse: UtbetalingmodulHendelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedUtbetalingkontekst = aktivitetslogg.kontekst(this)
        if (annulleringer.none { it.relevantFor(hendelse) }) return
        tilstand.kvittérAnnullering(this, hendelse, aktivitetsloggMedUtbetalingkontekst)
    }

    private fun relevantFor(utbetaling: UtbetalingmodulHendelse) =
        utbetaling.utbetalingId == this.id && (utbetaling.fagsystemId in setOf(this.arbeidsgiverOppdrag.fagsystemId, this.personOppdrag.fagsystemId))

    fun håndter(simulering: SimuleringHendelse) {
        if (simulering.utbetalingId != this.id) return
        personOppdrag.håndter(simulering)
        arbeidsgiverOppdrag.håndter(simulering)
    }

    fun simuler(aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedUtbetalingkontekst = aktivitetslogg.kontekst(this)
        tilstand.simuler(this, aktivitetsloggMedUtbetalingkontekst)
    }

    fun håndter(påminnelse: UtbetalingpåminnelseHendelse, aktivitetslogg: IAktivitetslogg) {
        if (påminnelse.utbetalingId != this.id) return
        val aktivitetsloggMedUtbetalingkontekst = aktivitetslogg.kontekst(this)
        if (påminnelse.status != this.tilstand.status) return
        tilstand.håndterPåminnelse(this, aktivitetsloggMedUtbetalingkontekst)
    }

    fun gjelderFor(hendelse: UtbetalingmodulHendelse) =
        relevantFor(hendelse) || annulleringer.any { it.relevantFor(hendelse) }

    fun gjelderFor(hendelse: UtbetalingsavgjørelseHendelse) = hendelse.utbetalingId == this.id

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

    fun håndter(hendelse: AnnullerUtbetalingHendelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedUtbetalingkontekst = aktivitetslogg.kontekst(this)
        godkjenn(aktivitetsloggMedUtbetalingkontekst, hendelse.vurdering)
    }

    fun annuller(hendelse: AnnullerUtbetalingHendelse, aktivitetslogg: IAktivitetslogg, alleUtbetalinger: List<Utbetaling>): Utbetaling? {
        val korrelerendeUtbetaling = alleUtbetalinger.firstOrNull { it.id == hendelse.utbetalingId } ?: return null
        if (korrelerendeUtbetaling.korrelasjonsId != this.korrelasjonsId) return null

        val aktiveUtbetalinger = alleUtbetalinger.aktive()

        val sisteUtbetalteForUtbetaling = checkNotNull(aktiveUtbetalinger.singleOrNull { it.hørerSammen(this) }) {
            "Det er gjort forsøk på å annullere en utbetaling som ikke lenger er aktiv"
        }

        val aktivitetsloggMedUtbetalingkontekst = aktivitetslogg.kontekst(sisteUtbetalteForUtbetaling)
        return sisteUtbetalteForUtbetaling.opphør(aktivitetsloggMedUtbetalingkontekst)
    }

    private fun opphør(aktivitetslogg: IAktivitetslogg) =
        tilstand.annuller(this, aktivitetslogg)

    fun forkast(aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedUtbetalingkontekst = aktivitetslogg.kontekst(this)
        tilstand.forkast(this, aktivitetsloggMedUtbetalingkontekst)
    }

    fun arbeidsgiverOppdrag() = arbeidsgiverOppdrag

    fun personOppdrag() = personOppdrag

    override fun toSpesifikkKontekst() =
        SpesifikkKontekst("Utbetaling", mapOf("utbetalingId" to "$id"))

    private fun godkjenn(aktivitetslogg: IAktivitetslogg, vurdering: Vurdering) {
        val aktivitetsloggMedUtbetalingkontekst = aktivitetslogg.kontekst(this)
        tilstand.godkjenn(this, aktivitetsloggMedUtbetalingkontekst, vurdering)
    }

    private fun tilstand(neste: Tilstand, aktivitetslogg: IAktivitetslogg) {
        oppdatert = LocalDateTime.now()
        if (Oppdrag.ingenFeil(arbeidsgiverOppdrag, personOppdrag) && !Oppdrag.synkronisert(
                arbeidsgiverOppdrag,
                personOppdrag
            )
        ) return aktivitetslogg.info("Venter på status på det andre oppdraget før vi kan gå videre")
        val forrigeTilstand = tilstand
        tilstand = neste
        observers.forEach {
            it.utbetalingEndret(
                id,
                type,
                arbeidsgiverOppdrag,
                personOppdrag,
                forrigeTilstand.status,
                neste.status,
                korrelasjonsId
            )
        }
        tilstand.entering(this, aktivitetslogg)
    }

    private fun nyUtbetaling(
        aktivitetslogg: IAktivitetslogg,
        type: Utbetalingtype,
        utbetalingsaken: Utbetalingsak,
        vedtaksperiode: Periode,
        kladd: Utbetalingkladd,
        maksdato: LocalDate,
        forbrukteSykedager: Int,
        gjenståendeSykedager: Int,
        annulleringer: List<Utbetaling>
    ): Utbetaling {
        val nyttArbeidsgiveroppdrag = byggViderePåOppdrag(aktivitetslogg, vedtaksperiode, utbetalingsaken, this.arbeidsgiverOppdrag, kladd.arbeidsgiveroppdrag)
        val nyttPersonoppdrag = byggViderePåOppdrag(aktivitetslogg, vedtaksperiode, utbetalingsaken, this.personOppdrag, kladd.personoppdrag)

        val nyereUtbetalingstidslinje = this.utbetalingstidslinje.subset(kladd.utbetalingsperiode)
        val tidligereUtbetalingstidslinje = nyereUtbetalingstidslinje.fremTilOgMed(vedtaksperiode.start.forrigeDag)
        val nyUtbetalingstidslinje = tidligereUtbetalingstidslinje + kladd.utbetalingstidslinje + nyereUtbetalingstidslinje.fraOgMed(vedtaksperiode.endInclusive.nesteDag)

        return Utbetaling(
            korrelerendeUtbetaling = this,
            periode = kladd.utbetalingsperiode,
            utbetalingstidslinje = nyUtbetalingstidslinje,
            arbeidsgiverOppdrag = nyttArbeidsgiveroppdrag,
            personOppdrag = nyttPersonoppdrag,
            type = type,
            maksdato = maksdato,
            forbrukteSykedager = forbrukteSykedager,
            gjenståendeSykedager = gjenståendeSykedager,
            annulleringer = annulleringer
        )
    }

    private fun byggViderePåOppdrag(aktivitetslogg: IAktivitetslogg, vedtaksperiode: Periode, utbetalingsaken: Utbetalingsak, historiskOppdrag: Oppdrag, nyttOppdrag: Oppdrag): Oppdrag {
        /* ta bort eventuell hale som er avkortet */
        val linjerFremTilOgMedUtbetalingsaken = historiskOppdrag.begrensTil(utbetalingsaken.omsluttendePeriode.endInclusive)

        /* linjer forut før perioden */
        val linjerFørVedtaksperioden = linjerFremTilOgMedUtbetalingsaken.begrensTil(vedtaksperiode.start.forrigeDag)
        val linjerEtterVedtaksperioden = linjerFremTilOgMedUtbetalingsaken.begrensFra(vedtaksperiode.endInclusive.nesteDag)

        /* legg inn endringer fra perioden og sy sammen */
        val nyeArbeidsgiverlinjer = linjerFørVedtaksperioden + nyttOppdrag + linjerEtterVedtaksperioden

        return Oppdrag(historiskOppdrag.mottaker, historiskOppdrag.fagområde, nyeArbeidsgiverlinjer).minus(historiskOppdrag, aktivitetslogg)
    }

    companion object {

        val log: Logger = LoggerFactory.getLogger("Utbetaling")
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        private const val systemident = "SPLEIS"

        fun lagUtbetaling(
            utbetalinger: List<Utbetaling>,
            fødselsnummer: String,
            organisasjonsnummer: String,
            utbetalingstidslinje: Utbetalingstidslinje,
            periode: Periode,
            utbetalingsaker: List<Utbetalingsak>,
            aktivitetslogg: IAktivitetslogg,
            maksdato: LocalDate,
            forbrukteSykedager: Int,
            gjenståendeSykedager: Int,
            type: Utbetalingtype = UTBETALING
        ): Pair<Utbetaling, List<Utbetaling>> {
            val utbetalingsaken = utbetalingsaker.first { periode in it.vedtaksperioder }
            val utbetalingsperiode = periode.oppdaterFom(utbetalingsaken.omsluttendePeriode)

            val vedtaksperiodekladd = UtbetalingkladdBuilder(utbetalingsperiode, utbetalingstidslinje.subset(periode), organisasjonsnummer, fødselsnummer).build()

            val forrigeUtbetalte = utbetalinger.aktive(utbetalingsaken.omsluttendePeriode)
            val korrelerendeUtbetaling = forrigeUtbetalte.firstOrNull()
            val annulleringer = forrigeUtbetalte
                .filterNot { it === korrelerendeUtbetaling }
                .mapNotNull { it.opphør(aktivitetslogg) }

            if (annulleringer.isNotEmpty()) aktivitetslogg.varsel(RV_UT_21)

            val utbetalingen = korrelerendeUtbetaling?.nyUtbetaling(
                aktivitetslogg = aktivitetslogg,
                type = type,
                utbetalingsaken = utbetalingsaken,
                vedtaksperiode = periode,
                kladd = vedtaksperiodekladd,
                maksdato = maksdato,
                forbrukteSykedager = forbrukteSykedager,
                gjenståendeSykedager = gjenståendeSykedager,
                annulleringer = annulleringer
            ) ?: Utbetaling(
                korrelerendeUtbetaling = null,
                periode = vedtaksperiodekladd.utbetalingsperiode,
                utbetalingstidslinje = utbetalingstidslinje.subset(vedtaksperiodekladd.utbetalingsperiode),
                arbeidsgiverOppdrag = vedtaksperiodekladd.arbeidsgiveroppdrag,
                personOppdrag = vedtaksperiodekladd.personoppdrag,
                type = type,
                maksdato = maksdato,
                forbrukteSykedager = forbrukteSykedager,
                gjenståendeSykedager = gjenståendeSykedager,
                annulleringer = annulleringer
            )
            listOf(utbetalingen.arbeidsgiverOppdrag, utbetalingen.personOppdrag).valider(aktivitetslogg)
            return utbetalingen to annulleringer
        }

        fun List<Utbetaling>.aktive() = grupperUtbetalinger(Utbetaling::erAktiv).filterNot { it.tilstand is Annullert  }
        private fun List<Utbetaling>.aktiveMedUbetalte() = grupperUtbetalinger(Utbetaling::erAktivEllerUbetalt)
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
            if (other.erAnnullering()) return true // må godta annulleringer ettersom de vil rydde opp i nettopp overlappende utbetalinger
            val overlappendeUtbetalingsperioder = overlappendeUtbetalingsperioder(other)
            if (overlappendeUtbetalingsperioder.isNotEmpty()) {
                sikkerlogg.warn("Vi har opprettet en utbetaling med periode ${other.periode} & korrelasjonsId ${other.korrelasjonsId} som overlapper med eksisterende utbetalinger $overlappendeUtbetalingsperioder")
            }
            return overlappendeUtbetalingsperioder.isEmpty()
        }

        private fun List<Utbetaling>.overlappendeUtbetalingsperioder(other: Utbetaling): List<Periode> {
            return aktiveMedUbetalte()
                .filterNot { it.hørerSammen(other) }
                .filter { it.periode.overlapperMed(other.periode) }
                .map { it.periode }
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
                    UtbetalingTilstandDto.GODKJENT -> Godkjent
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
                    UtbetalingtypeDto.ETTERUTBETALING -> Utbetalingtype.ETTERUTBETALING
                    UtbetalingtypeDto.FERIEPENGER -> Utbetalingtype.FERIEPENGER
                    UtbetalingtypeDto.REVURDERING -> Utbetalingtype.REVURDERING
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

    private fun håndterKvittering(hendelse: UtbetalingmodulHendelse, aktivitetslogg: IAktivitetslogg) {
        when (hendelse.status) {
            Oppdragstatus.OVERFØRT,
            Oppdragstatus.AKSEPTERT -> {} // all is good
            Oppdragstatus.AKSEPTERT_MED_FEIL -> aktivitetslogg.info("Utbetalingen ble gjennomført, men med advarsel")
            Oppdragstatus.AVVIST,
            Oppdragstatus.FEIL -> aktivitetslogg.info("Utbetaling feilet med status ${hendelse.status}. Feilmelding fra Oppdragsystemet: ${hendelse.melding}")
        }

        val skalForsøkesIgjen = hendelse.status in setOf(Oppdragstatus.AVVIST, Oppdragstatus.FEIL)
        val nesteTilstand = when {
            skalForsøkesIgjen || Oppdrag.harFeil(arbeidsgiverOppdrag, personOppdrag) -> return // utbetaling gjør retry ved neste påminnelse
            type == ANNULLERING -> Annullert
            else -> Utbetalt
        }
        tilstand(nesteTilstand, aktivitetslogg)
    }

    fun nyVedtaksperiodeUtbetaling(vedtaksperiodeId: UUID) {
        observers.forEach { it.nyVedtaksperiodeUtbetaling(this.id, vedtaksperiodeId) }
    }

    fun overlapperMed(other: Periode): Boolean {
        return this.periode.overlapperMed(other)
    }

    fun overlapperMed(other: Utbetaling): Boolean {
        return this.periode.overlapperMed(other.periode)
    }

    fun erNyereEnn(other: LocalDateTime): Boolean {
        return other <= tidsstempel
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
        fun forkast(utbetaling: Utbetaling, aktivitetslogg: IAktivitetslogg) {}

        fun opprett(utbetaling: Utbetaling, aktivitetslogg: IAktivitetslogg) {
            aktivitetslogg.info("Forventet ikke å opprette utbetaling i tilstand=${this::class.simpleName}")
            aktivitetslogg.funksjonellFeil(RV_UT_6)
        }

        fun godkjenn(
            utbetaling: Utbetaling,
            aktivitetslogg: IAktivitetslogg,
            vurdering: Vurdering
        ) {
            aktivitetslogg.info("Forventet ikke godkjenning på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
            aktivitetslogg.funksjonellFeil(RV_UT_7)
        }

        fun annuller(utbetaling: Utbetaling, aktivitetslogg: IAktivitetslogg): Utbetaling? {
            aktivitetslogg.info("Forventet ikke å annullere på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
            aktivitetslogg.funksjonellFeil(RV_UT_9)
            return null
        }

        fun kvittér(utbetaling: Utbetaling, hendelse: UtbetalingmodulHendelse, aktivitetslogg: IAktivitetslogg) {
            aktivitetslogg.info("Forventet ikke kvittering på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
            aktivitetslogg.funksjonellFeil(RV_UT_11)
        }

        fun kvittérAnnullering(utbetaling: Utbetaling, hendelse: UtbetalingmodulHendelse, aktivitetslogg: IAktivitetslogg) {
            aktivitetslogg.info("Forventet ikke kvittering for annullering på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
            aktivitetslogg.funksjonellFeil(RV_UT_11)
        }

        fun håndterPåminnelse(utbetaling: Utbetaling, påminnelse: IAktivitetslogg) {
            påminnelse.info("Utbetaling ble påminnet, men gjør ingenting")
        }

        fun simuler(utbetaling: Utbetaling, aktivitetslogg: IAktivitetslogg) {
            aktivitetslogg.info("Forventet ikke simulering på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
            aktivitetslogg.funksjonellFeil(RV_UT_12)
        }

        fun entering(utbetaling: Utbetaling, aktivitetslogg: IAktivitetslogg) {}
    }

    internal data object Ny : Tilstand {
        override val status = Utbetalingstatus.NY
        override fun opprett(utbetaling: Utbetaling, aktivitetslogg: IAktivitetslogg) {
            utbetaling.tilstand(Ubetalt, aktivitetslogg)
        }
    }

    internal data object Ubetalt : Tilstand {
        override val status = Utbetalingstatus.IKKE_UTBETALT
        override fun forkast(utbetaling: Utbetaling, aktivitetslogg: IAktivitetslogg) {
            utbetaling.annulleringer.forEach { it.forkast(aktivitetslogg) }
            aktivitetslogg.info("Forkaster utbetaling")
            utbetaling.tilstand(Forkastet, aktivitetslogg)
        }

        override fun godkjenn(utbetaling: Utbetaling, aktivitetslogg: IAktivitetslogg, vurdering: Vurdering) {
            utbetaling.vurdering = vurdering
            utbetaling.annulleringer.forEach { it.godkjenn(aktivitetslogg, vurdering) }
            utbetaling.tilstand(vurdering.avgjør(utbetaling), aktivitetslogg)
        }

        override fun simuler(utbetaling: Utbetaling, aktivitetslogg: IAktivitetslogg) {
            utbetaling.annulleringer.forEach { it.simuler(aktivitetslogg) }

            utbetaling.arbeidsgiverOppdrag.simuler(aktivitetslogg, utbetaling.maksdato, systemident)
            utbetaling.personOppdrag.simuler(aktivitetslogg, utbetaling.maksdato, systemident)
        }
    }

    internal data object Godkjent : Tilstand {
        override val status = Utbetalingstatus.GODKJENT

        override fun entering(utbetaling: Utbetaling, aktivitetslogg: IAktivitetslogg) {
            check(utbetaling.annulleringer.isNotEmpty())
        }

        override fun håndterPåminnelse(utbetaling: Utbetaling, påminnelse: IAktivitetslogg) {
            vurderNesteTilstand(utbetaling, påminnelse)
        }

        override fun kvittérAnnullering(utbetaling: Utbetaling, hendelse: UtbetalingmodulHendelse, aktivitetslogg: IAktivitetslogg) {
            vurderNesteTilstand(utbetaling, aktivitetslogg)
        }

        private fun vurderNesteTilstand(utbetaling: Utbetaling, aktivitetslogg: IAktivitetslogg) {
            if (utbetaling.annulleringer.any { !it.erAvsluttet() }) return
            utbetaling.tilstand(
                when {
                    utbetaling.harOppdragMedUtbetalinger() -> Overført
                    else -> GodkjentUtenUtbetaling
                }, aktivitetslogg
            )
        }
    }

    internal data object GodkjentUtenUtbetaling : Tilstand {
        override val status = Utbetalingstatus.GODKJENT_UTEN_UTBETALING
        override fun entering(utbetaling: Utbetaling, aktivitetslogg: IAktivitetslogg) {
            check(!utbetaling.harOppdragMedUtbetalinger())
            utbetaling.vurdering?.avsluttetUtenUtbetaling(utbetaling)
            utbetaling.avsluttet = LocalDateTime.now()
        }

        override fun annuller(utbetaling: Utbetaling, aktivitetslogg: IAktivitetslogg) = Utbetaling(
            utbetaling,
            utbetaling.periode,
            utbetaling.utbetalingstidslinje,
            utbetaling.arbeidsgiverOppdrag.annuller(aktivitetslogg),
            utbetaling.personOppdrag.annuller(aktivitetslogg),
            ANNULLERING,
            LocalDate.MAX,
            null,
            null
        ).also { aktivitetslogg.info("Oppretter annullering med id ${it.id}") }
    }

    internal data object Overført : Tilstand {
        override val status = Utbetalingstatus.OVERFØRT
        override fun entering(utbetaling: Utbetaling, aktivitetslogg: IAktivitetslogg) {
            utbetaling.overførBegge(aktivitetslogg)
        }

        override fun håndterPåminnelse(utbetaling: Utbetaling, påminnelse: IAktivitetslogg) {
            utbetaling.overførBegge(påminnelse)
        }

        override fun kvittér(utbetaling: Utbetaling, hendelse: UtbetalingmodulHendelse, aktivitetslogg: IAktivitetslogg) {
            utbetaling.lagreOverføringsinformasjon(aktivitetslogg, hendelse.avstemmingsnøkkel, hendelse.overføringstidspunkt)
            utbetaling.arbeidsgiverOppdrag.lagreOverføringsinformasjon(hendelse)
            utbetaling.personOppdrag.lagreOverføringsinformasjon(hendelse)
            utbetaling.håndterKvittering(hendelse, aktivitetslogg)
        }
    }

    internal data object Annullert : Tilstand {
        override val status = Utbetalingstatus.ANNULLERT
        override fun entering(utbetaling: Utbetaling, aktivitetslogg: IAktivitetslogg) {
            utbetaling.vurdering?.annullert(utbetaling)
            utbetaling.avsluttet = LocalDateTime.now()
        }
    }

    internal data object Utbetalt : Tilstand {
        override val status = Utbetalingstatus.UTBETALT
        override fun entering(utbetaling: Utbetaling, aktivitetslogg: IAktivitetslogg) {
            utbetaling.vurdering?.utbetalt(utbetaling)
            utbetaling.avsluttet = LocalDateTime.now()
        }

        override fun annuller(utbetaling: Utbetaling, aktivitetslogg: IAktivitetslogg) =
            Utbetaling(
                utbetaling,
                utbetaling.periode,
                utbetaling.utbetalingstidslinje,
                utbetaling.arbeidsgiverOppdrag.annuller(aktivitetslogg),
                utbetaling.personOppdrag.annuller(aktivitetslogg),
                ANNULLERING,
                LocalDate.MAX,
                null,
                null
            ).also { aktivitetslogg.info("Oppretter annullering med id ${it.id}") }
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
        fun annullert(utbetaling: Utbetaling) {
            utbetaling.observers.forEach {
                it.utbetalingAnnullert(
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
        }

        fun utbetalt(utbetaling: Utbetaling) {
            utbetaling.observers.forEach {
                it.utbetalingUtbetalt(
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
        }

        fun avsluttetUtenUtbetaling(utbetaling: Utbetaling) {
            utbetaling.observers.forEach {
                it.utbetalingUtenUtbetaling(
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
        }

        fun overfør(aktivitetslogg: IAktivitetslogg, oppdrag: Oppdrag, maksdato: LocalDate?) {
            oppdrag.overfør(aktivitetslogg, maksdato, ident)
        }

        internal fun avgjør(utbetaling: Utbetaling) =
            when {
                !godkjent -> IkkeGodkjent
                utbetaling.annulleringer.any { it.harUtbetalinger() } -> Godkjent
                utbetaling.harOppdragMedUtbetalinger() -> Overført
                utbetaling.type == ANNULLERING -> Annullert
                else -> GodkjentUtenUtbetaling
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
            Godkjent -> UtbetalingTilstandDto.GODKJENT
            GodkjentUtenUtbetaling -> UtbetalingTilstandDto.GODKJENT_UTEN_UTBETALING
            IkkeGodkjent -> UtbetalingTilstandDto.IKKE_GODKJENT
            Ny -> UtbetalingTilstandDto.NY
            Overført -> UtbetalingTilstandDto.OVERFØRT
            Ubetalt -> UtbetalingTilstandDto.IKKE_UTBETALT
            Utbetalt -> UtbetalingTilstandDto.UTBETALT
        },
        type = when (type) {
            UTBETALING -> UtbetalingtypeDto.UTBETALING
            Utbetalingtype.ETTERUTBETALING -> UtbetalingtypeDto.ETTERUTBETALING
            ANNULLERING -> UtbetalingtypeDto.ANNULLERING
            Utbetalingtype.REVURDERING -> UtbetalingtypeDto.REVURDERING
            Utbetalingtype.FERIEPENGER -> UtbetalingtypeDto.FERIEPENGER
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

enum class Utbetalingtype { UTBETALING, ETTERUTBETALING, ANNULLERING, REVURDERING, FERIEPENGER }
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
    RefusjonFeriepengerIkkeOpplysningspliktig(verdi = "SPREFAGFER-IOP"),
    SykepengerArbeidstakerOrdinær(verdi = "SPATORD"),
    SykepengerArbeidstakerFeriepenger(verdi = "SPATFER");

    companion object {
        private val map = entries.associateBy(Klassekode::verdi)
        fun from(verdi: String) = requireNotNull(map[verdi]) { "Støtter ikke klassekode: $verdi" }
        fun gjenopprett(dto: KlassekodeDto) = when (dto) {
            KlassekodeDto.RefusjonFeriepengerIkkeOpplysningspliktig -> RefusjonFeriepengerIkkeOpplysningspliktig
            KlassekodeDto.RefusjonIkkeOpplysningspliktig -> RefusjonIkkeOpplysningspliktig
            KlassekodeDto.SykepengerArbeidstakerFeriepenger -> SykepengerArbeidstakerFeriepenger
            KlassekodeDto.SykepengerArbeidstakerOrdinær -> SykepengerArbeidstakerOrdinær
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
    val annulleringer: List<UUID>
)
