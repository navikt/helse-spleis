package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Alder
import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.Utbetalingsgodkjenning
import no.nav.helse.person.Dokumentsporing.Companion.ider
import no.nav.helse.person.Dokumentsporing.Companion.sisteInntektsmeldingId
import no.nav.helse.person.Dokumentsporing.Companion.søknadIder
import no.nav.helse.person.Dokumentsporing.Companion.tilSubsumsjonsformat
import no.nav.helse.person.Generasjoner.Generasjon.Companion.dokumentsporing
import no.nav.helse.person.Generasjoner.Generasjon.Companion.erUtbetaltPåForskjelligeUtbetalinger
import no.nav.helse.person.Generasjoner.Generasjon.Companion.jurist
import no.nav.helse.person.Generasjoner.Generasjon.Companion.lagreTidsnæreInntekter
import no.nav.helse.person.Generasjoner.Generasjon.Endring.Companion.dokumentsporing
import no.nav.helse.person.VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.GodkjenningsbehovBuilder
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.builders.VedtakFattetBuilder
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.sykdomstidslinje.SykdomshistorikkHendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.harId
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

internal class Generasjoner(generasjoner: List<Generasjon>) {
    internal constructor(sykmeldingsperiode: Periode, sykdomstidslinje: Sykdomstidslinje, dokumentsporing: Dokumentsporing) : this(mutableListOf(Generasjon.nyGenerasjon(sykdomstidslinje, dokumentsporing, sykmeldingsperiode)))

    private val utbetalingene get() = generasjoner.mapNotNull(Generasjon::utbetaling)
    private val generasjoner = generasjoner.toMutableList()
    private val siste get() = generasjoner.lastOrNull()?.utbetaling()

    internal fun accept(visitor: GenerasjonerVisistor) {
        visitor.preVisitGenerasjoner(generasjoner)
        generasjoner.forEach { generasjon ->
            generasjon.accept(visitor)
        }
        visitor.postVisitGenerasjoner(generasjoner)
    }

    internal fun sykdomstidslinje() = generasjoner.last().sykdomstidslinje()

    internal fun harUtbetaling() = siste != null && siste!!.gyldig()
    internal fun trekkerTilbakePenger() = siste?.trekkerTilbakePenger() == true
    internal fun utbetales() = generasjoner.any { it.erInFlight() }
    internal fun erAvsluttet() = generasjoner.last().erAvsluttet()
    internal fun erAvsluttet(hendelse: UtbetalingHendelse) = generasjoner.any { it.erAvsluttet(hendelse) }
    internal fun erAvvist() = siste?.erAvvist() == true
    internal fun harUtbetalinger() = siste?.harUtbetalinger() == true
    internal fun erUtbetalt() = siste?.erUtbetalt() == true
    internal fun erUbetalt() = siste?.erUbetalt() == true

    internal fun kanForkastes(arbeidsgiverUtbetalinger: List<Utbetaling>) = generasjoner.all { generasjon ->
        generasjon.kanForkastes(arbeidsgiverUtbetalinger)
    }
    internal fun harAvsluttede() = generasjoner.any { generasjon -> generasjon.utbetaling()?.erAvsluttet() == true }
    internal fun harId(utbetalingId: UUID) = utbetalingene.harId(utbetalingId)
    internal fun hørerIkkeSammenMed(other: Utbetaling) = generasjoner.lastOrNull { generasjon  -> generasjon.utbetaling()?.gyldig() == true }?.utbetaling()?.hørerSammen(other) == false
    internal fun hørerIkkeSammenMed(other: Generasjoner) = other.siste != null && hørerIkkeSammenMed(other.siste!!)
    internal fun håndterUtbetalinghendelse(hendelse: UtbetalingHendelse) = generasjoner.any { it.håndterUtbetalinghendelse(hendelse) }

    internal fun lagreTidsnæreInntekter(
        arbeidsgiver: Arbeidsgiver,
        skjæringstidspunkt: LocalDate,
        hendelse: IAktivitetslogg,
        oppholdsperiodeMellom: Periode?
    ) {
        generasjoner.lagreTidsnæreInntekter(arbeidsgiver, skjæringstidspunkt, hendelse, oppholdsperiodeMellom)
    }

    internal fun gjelderIkkeFor(hendelse: Utbetalingsgodkjenning) = siste?.gjelderFor(hendelse) != true

    internal fun erHistorikkEndretSidenBeregning(infotrygdhistorikk: Infotrygdhistorikk) =
        infotrygdhistorikk.harEndretHistorikk(siste!!)

    internal fun build(builder: VedtakFattetBuilder) {
        if (!harUtbetaling()) return
        siste?.build(builder)
    }

    internal fun overlapperMed(other: Generasjoner): Boolean {
        if (!this.harUtbetalinger() || !other.harUtbetalinger()) return false
        return this.siste!!.overlapperMed(other.siste!!)
    }

    internal fun valider(simulering: Simulering) {
        siste!!.valider(simulering)
    }

    internal fun erKlarForGodkjenning() = siste?.erKlarForGodkjenning() ?: false

    internal fun simuler(hendelse: IAktivitetslogg) = siste!!.simuler(hendelse)

    internal fun godkjenning(hendelse: IAktivitetslogg, builder: GodkjenningsbehovBuilder) {
        siste!!.godkjenning(hendelse, builder)
    }

    internal fun nyUtbetaling(
        vedtaksperiodeSomLagerUtbetaling: UUID,
        fødselsnummer: String,
        arbeidsgiver: Arbeidsgiver,
        arbeidsgiverSomBeregner: Arbeidsgiver,
        grunnlagsdata: VilkårsgrunnlagElement,
        periode: Periode,
        hendelse: IAktivitetslogg,
        maksimumSykepenger: Alder.MaksimumSykepenger,
        utbetalingstidslinje: Utbetalingstidslinje
    ): Utbetalingstidslinje {
        val strategi = if (this.harAvsluttede()) Arbeidsgiver::lagRevurdering else Arbeidsgiver::lagUtbetaling
        val denNyeUtbetalingen = strategi(arbeidsgiver, hendelse, fødselsnummer, arbeidsgiverSomBeregner, utbetalingstidslinje, maksimumSykepenger.sisteDag(), maksimumSykepenger.forbrukteDager(), maksimumSykepenger.gjenståendeDager(), periode)
        denNyeUtbetalingen.nyVedtaksperiodeUtbetaling(vedtaksperiodeSomLagerUtbetaling)
        // leggTilNyGenerasjon(generasjonkladd.somGenerasjon(denNyeUtbetalingen))

        // Bytter ut siste generasjon med en oppdatert en.. Fiffig!
        // generasjoner[generasjoner.lastIndex] = generasjoner.last().utbetaling(denNyeUtbetalingen, grunnlagsdata)
        generasjoner.last().utbetaling(denNyeUtbetalingen, grunnlagsdata)
        return utbetalingstidslinje.subset(periode)
    }

    internal fun forkast(hendelse: IAktivitetslogg) {
        leggTilNyGenerasjon(generasjoner.last().forkastVedtaksperiode())
    }
    internal fun forkastUtbetaling(hendelse: IAktivitetslogg) {
        generasjoner.last().forkastUtbetaling(hendelse)
    }
    internal fun harIkkeUtbetaling() = generasjoner.last().harIkkeUtbetaling()


    fun vedtakFattet(utbetalingsgodkjenning: Utbetalingsgodkjenning) {
        this.generasjoner.last().vedtakFattet(utbetalingsgodkjenning)
    }
    fun avslutt() {
        this.generasjoner.last().avslutt()
    }

    internal fun sykmeldingsperiode() = this.generasjoner.first().sykmeldingsperiode()
    internal fun periode() = this.generasjoner.last().periode()

    // sørger for ny generasjon når vedtaksperioden går ut av Avsluttet/AUU,
    // men bare hvis det ikke er laget en ny allerede fra før
    fun sikreNyGenerasjon() {
        leggTilNyGenerasjon(generasjoner.last().sikreNyGenerasjon())
    }

    private fun leggTilNyGenerasjon(generasjon: Generasjon?) {
        if (generasjon == null) return
        check(generasjoner.last().tillaterNyGenerasjon(generasjon)) {
            "siste generasjon ${generasjoner.last()} tillater ikke opprettelse av ny generasjon $generasjon"
        }
        this.generasjoner.add(generasjon)
    }

    fun klarForUtbetaling(): Boolean {
        return generasjoner.last().klarForUtbetaling()
    }

    fun harÅpenGenerasjon(): Boolean {
        return generasjoner.last().harÅpenGenerasjon()
    }

    internal fun jurist(jurist: MaskinellJurist, vedtaksperiodeId: UUID) =
        generasjoner.jurist(jurist, vedtaksperiodeId)

    internal fun hendelseIder() = generasjoner.dokumentsporing
    internal fun dokumentsporing() = generasjoner.dokumentsporing.ider()

    internal fun søknadIder() = generasjoner.dokumentsporing.søknadIder()
    internal fun sisteInntektsmeldingId() = generasjoner.dokumentsporing.sisteInntektsmeldingId()

    internal fun oppdaterDokumentsporing(dokument: Dokumentsporing): Boolean {
        return generasjoner.last().oppdaterDokumentsporing(dokument)
    }

    fun dokumentHåndtert(dokumentsporing: Dokumentsporing) =
        generasjoner.any { it.dokumentHåndtert(dokumentsporing) }

    fun håndterEndring(arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse) {
        val generasjon = generasjoner.last().håndterEndring(arbeidsgiver, hendelse) ?: return
        // lagde ny generasjon for å håndtere endringen
        generasjoner.add(generasjon)
    }

    fun erFattetVedtak(): Boolean {
        return generasjoner.last().erFattetVedtak()
    }

    fun erUtbetaltPåForskjelligeUtbetalinger(other: Generasjoner): Boolean {
        return this.generasjoner.erUtbetaltPåForskjelligeUtbetalinger(other.generasjoner)
    }

    internal class Generasjon private constructor(
        private val id: UUID,
        private var tilstand: Tilstand,
        private val endringer: MutableList<Endring>,
        private var vedtakFattet: LocalDateTime?,
        private var avsluttet: LocalDateTime?,
        private var periode: Periode = endringer.last().periode
    ) {
        private val gjeldende get() = endringer.last()
        private val tidsstempel = endringer.first().tidsstempel
        private val dokumentsporing get() = endringer.dokumentsporing

        constructor(id: UUID, tilstand: Tilstand, endringer: List<Endring>, vedtakFattet: LocalDateTime?, avsluttet: LocalDateTime?) : this(id, tilstand, endringer.toMutableList(), vedtakFattet, avsluttet)

        init {
            check(endringer.isNotEmpty()) {
                "Må ha endringer for at det skal være vits med en generasjon"
            }
        }

        override fun toString() = "$periode - $tilstand"

        fun accept(visitor: GenerasjonerVisistor) {
            visitor.preVisitGenerasjon(id, tidsstempel, tilstand, periode, vedtakFattet, avsluttet)
            endringer.forEach { it.accept(visitor) }
            visitor.postVisitGenerasjon(id, tidsstempel, tilstand, periode, vedtakFattet, avsluttet)
        }

        fun sykmeldingsperiode() = endringer.first().sykmeldingsperiode
        fun periode() = periode

        // TODO: se på om det er nødvendig å støtte Dokumentsporing som et sett; eventuelt om Generasjon må ha et sett
        class Endring constructor(
            private val id: UUID,
            val tidsstempel: LocalDateTime,
            val sykmeldingsperiode: Periode,
            val periode: Periode,
            val grunnlagsdata: VilkårsgrunnlagElement?,
            val utbetaling: Utbetaling?,
            val dokumentsporing: Dokumentsporing,
            val sykdomstidslinje: Sykdomstidslinje,
        ) {

            internal constructor(
                grunnlagsdata: VilkårsgrunnlagElement?,
                utbetaling: Utbetaling?,
                dokumentsporing: Dokumentsporing,
                sykdomstidslinje: Sykdomstidslinje,
                sykmeldingsperiode: Periode,
                periode: Periode
            ) : this(UUID.randomUUID(), LocalDateTime.now(), sykmeldingsperiode, periode, grunnlagsdata, utbetaling, dokumentsporing, sykdomstidslinje)

            companion object {
                val List<Endring>.dokumentsporing get() = map { it.dokumentsporing }.toSet()
            }

            override fun toString() = "$periode - $dokumentsporing - ${sykdomstidslinje.toShortString()}${utbetaling?.let { " - $it" } ?: ""}"

            override fun equals(other: Any?): Boolean {
                if (other === this) return true
                if (other !is Endring) return false
                return this.dokumentsporing == other.dokumentsporing
            }

            internal fun accept(visitor: GenerasjonerVisistor) {
                visitor.preVisitGenerasjonendring(id, tidsstempel, sykmeldingsperiode, periode, grunnlagsdata, utbetaling, dokumentsporing, sykdomstidslinje)
                grunnlagsdata?.accept(visitor)
                utbetaling?.accept(visitor)
                sykdomstidslinje.accept(visitor)
                visitor.postVisitGenerasjonendring(id, tidsstempel, sykmeldingsperiode, periode, grunnlagsdata, utbetaling, dokumentsporing, sykdomstidslinje)
            }

            internal fun kopierMedEndring(periode: Periode, dokument: Dokumentsporing, sykdomstidslinje: Sykdomstidslinje) = Endring(
                grunnlagsdata = null,
                utbetaling = null,
                dokumentsporing = dokument,
                sykdomstidslinje = sykdomstidslinje,
                sykmeldingsperiode = this.sykmeldingsperiode,
                periode = periode
            )
            internal fun kopierUtenUtbetaling() = Endring(
                grunnlagsdata = null,
                utbetaling = null,
                dokumentsporing = this.dokumentsporing,
                sykdomstidslinje = this.sykdomstidslinje,
                sykmeldingsperiode = this.sykmeldingsperiode,
                periode = this.periode
            )
            internal fun kopierMedUtbetaling(utbetaling: Utbetaling, grunnlagsdata: VilkårsgrunnlagElement) = Endring(
                grunnlagsdata = grunnlagsdata,
                utbetaling = utbetaling,
                dokumentsporing = this.dokumentsporing,
                sykdomstidslinje = this.sykdomstidslinje,
                sykmeldingsperiode = this.sykmeldingsperiode,
                periode = this.periode
            )
            internal fun kopierDokument(dokument: Dokumentsporing) = Endring(
                grunnlagsdata = this.grunnlagsdata,
                utbetaling = this.utbetaling,
                dokumentsporing = dokument,
                sykdomstidslinje = this.sykdomstidslinje,
                sykmeldingsperiode = this.sykmeldingsperiode,
                periode = this.periode
            )

            fun lagreTidsnæreInntekter(
                nyttSkjæringstidspunkt: LocalDate,
                arbeidsgiver: Arbeidsgiver,
                hendelse: IAktivitetslogg,
                oppholdsperiodeMellom: Periode?
            ) {
                grunnlagsdata?.lagreTidsnæreInntekter(nyttSkjæringstidspunkt, arbeidsgiver, hendelse, oppholdsperiodeMellom)
            }

            fun forkastUtbetaling(hendelse: IAktivitetslogg) {
                utbetaling?.forkast(hendelse)
            }
        }

        internal fun sykdomstidslinje() = endringer.last().sykdomstidslinje

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is Generasjon) return false
            return this.tilstand == other.tilstand && this.dokumentsporing == other.dokumentsporing
        }

        override fun hashCode(): Int {
            return this.dokumentsporing.hashCode()
        }

        internal fun erFattetVedtak() = vedtakFattet != null
        internal fun erInFlight() = erFattetVedtak() && !erAvsluttet()
        internal fun erAvsluttet() = avsluttet != null
        internal fun erAvsluttet(utbetalingHendelse: UtbetalingHendelse) = erAvsluttet() && utbetaling()?.gjelderFor(utbetalingHendelse) == true

        internal fun klarForUtbetaling() = this.tilstand == Tilstand.Uberegnet
        internal fun harÅpenGenerasjon() = this.tilstand in setOf(Tilstand.UberegnetRevurdering, Tilstand.UberegnetOmgjøring, Tilstand.TilInfotrygd)
        internal fun harIkkeUtbetaling() = this.tilstand in setOf(Tilstand.Uberegnet, Tilstand.UberegnetOmgjøring, Tilstand.TilInfotrygd)

        internal fun vedtakFattet(utbetalingsgodkjenning: Utbetalingsgodkjenning) {
            if (!utbetalingsgodkjenning.vedtakGodkjent()) return tilstand.vedtakAvvist(this, utbetalingsgodkjenning)
            tilstand.vedtakFattet(this, utbetalingsgodkjenning)
        }

        internal fun avslutt() {
            tilstand.avslutt(this)
        }

        internal fun forkastVedtaksperiode(): Generasjon? {
            return tilstand.forkastVedtaksperiode(this)
        }

        private fun tilstand(nyTilstand: Tilstand) {
            tilstand.leaving(this)
            tilstand = nyTilstand
            tilstand.entering(this)
        }

        fun lagreTidsnæreInntekter(
            nyttSkjæringstidspunkt: LocalDate,
            arbeidsgiver: Arbeidsgiver,
            hendelse: IAktivitetslogg,
            oppholdsperiodeMellom: Periode?
        ) {
            endringer.lastOrNull { it.utbetaling != null }?.lagreTidsnæreInntekter(nyttSkjæringstidspunkt, arbeidsgiver, hendelse, oppholdsperiodeMellom)
        }

        fun forkastUtbetaling(hendelse: IAktivitetslogg) {
            tilstand.utenUtbetaling(this, hendelse)
        }

        fun utbetaling() = gjeldende.utbetaling

        fun utbetaling(utbetaling: Utbetaling, grunnlagsdata: VilkårsgrunnlagElement) {
            tilstand.utbetaling(this, utbetaling, grunnlagsdata)
        }

        private fun medUtbetaling(utbetaling: Utbetaling, grunnlagsdata: VilkårsgrunnlagElement) {
            nyEndring(gjeldende.kopierMedUtbetaling(utbetaling, grunnlagsdata))
        }

        fun dokumentHåndtert(dokumentsporing: Dokumentsporing) =
            dokumentsporing in this.dokumentsporing

        internal fun oppdaterDokumentsporing(dokument: Dokumentsporing): Boolean {
            return tilstand.oppdaterDokumentsporing(this, dokument)
        }

        private fun kopierMedDokument(dokument: Dokumentsporing): Boolean {
            if (gjeldende.dokumentsporing == dokument) return false
            nyEndring(gjeldende.kopierDokument(dokument))
            return true
        }

        private fun utenUtbetaling(hendelse: IAktivitetslogg) {
            gjeldende.utbetaling!!.forkast(hendelse)
            nyEndring(gjeldende.kopierUtenUtbetaling())
        }

        private fun nyEndring(endring: Endring?) {
            if (endring == null) return
            endringer.add(endring)
        }

        fun håndterEndring(arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse): Generasjon? {
            return tilstand.håndterEndring(this, arbeidsgiver, hendelse)
        }


        private fun håndtereEndring(arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse): Endring {
            // TODO: denne må være her ene og alene fordi 'arbeidsgiver.oppdaterSykdom()' ender opp med å sende ut sykefraværsfortellingen,
            // og da må vedtaksperiodens periode reflektere den nye perioden ellers blir det litt teit
            // derfor må <this.periode> være en 'var', men man trenger aldri deserialisere den
            this.periode = hendelse.oppdaterFom(endringer.last().periode)
            val sykdomstidslinje = arbeidsgiver.oppdaterSykdom(hendelse).subset(periode)
            return endringer.last().kopierMedEndring(this.periode, hendelse.dokumentsporing(), sykdomstidslinje)
        }
        // oppdaterer seg selv med endringen
        private fun oppdaterMedEndring(arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse) {
            val endring = håndtereEndring(arbeidsgiver, hendelse)
            if (endring == gjeldende) return
            nyEndring(endring)
        }

        private fun nyGenerasjonMedEndring(arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse, starttilstand: Tilstand = Tilstand.Uberegnet): Generasjon {
            return Generasjon(
                id = UUID.randomUUID(),
                tilstand = starttilstand,
                endringer = listOf(håndtereEndring(arbeidsgiver, hendelse)),
                vedtakFattet = null,
                avsluttet = null
            )
        }
        private fun sikreNyGenerasjon(starttilstand: Tilstand): Generasjon {
            return Generasjon(
                id = UUID.randomUUID(),
                tilstand = starttilstand,
                endringer = listOf(endringer.last().kopierUtenUtbetaling()),
                vedtakFattet = null,
                avsluttet = null
            )
        }

        private fun nyGenerasjonTilInfotrygd() = Generasjon(
            id = UUID.randomUUID(),
            tilstand = Tilstand.TilInfotrygd,
            endringer = listOf(this.gjeldende.kopierUtenUtbetaling()),
            vedtakFattet = null,
            avsluttet = LocalDateTime.now()
        )

        fun sikreNyGenerasjon(): Generasjon? {
            return tilstand.sikreNyGenerasjon(this)
        }

        fun tillaterNyGenerasjon(other: Generasjon): Boolean {
            return tilstand.tillaterNyGenerasjon(this, other)
        }

        fun håndterUtbetalinghendelse(hendelse: UtbetalingHendelse): Boolean {
            return tilstand.håndterUtbetalinghendelse(this, hendelse)
        }

        fun kanForkastes(arbeidsgiverUtbetalinger: List<Utbetaling>): Boolean {
            return tilstand.kanForkastes(this, arbeidsgiverUtbetalinger)
        }

        private fun erUtbetalingAnnullert(arbeidsgiverUtbetalinger: List<Utbetaling>): Boolean {
            val utbetalingen = checkNotNull(gjeldende.utbetaling) {
                "forventer at det er en utbetaling på denne generasjonen"
            }
            return Utbetaling.kanForkastes(utbetalingen, arbeidsgiverUtbetalinger)
        }


        /*
enum class Periodetilstand {
    TilUtbetaling,
    TilAnnullering,
    Utbetalt,
    Annullert,
    AnnulleringFeilet,
    RevurderingFeilet,
    ForberederGodkjenning,
    ManglerInformasjon,
    UtbetaltVenterPåAnnenPeriode,
    VenterPåAnnenPeriode,
    TilGodkjenning,
    IngenUtbetaling,
    TilInfotrygd;
}
         */

        internal companion object {
            val List<Generasjon>.sykmeldingsperiode get() = first().periode
            val List<Generasjon>.dokumentsporing get() = map { it.dokumentsporing }.reduce(Set<Dokumentsporing>::plus)

            fun nyGenerasjon(sykdomstidslinje: Sykdomstidslinje, dokumentsporing: Dokumentsporing, sykmeldingsperiode: Periode) =
                Generasjon(
                    id = UUID.randomUUID(),
                    tilstand = Tilstand.Uberegnet,
                    endringer = listOf(
                        Endring(
                            grunnlagsdata = null,
                            utbetaling = null,
                            dokumentsporing = dokumentsporing,
                            sykdomstidslinje = sykdomstidslinje,
                            sykmeldingsperiode = sykmeldingsperiode,
                            periode = checkNotNull(sykdomstidslinje.periode()) { "kan ikke opprette generasjon på tom sykdomstidslinje" }
                        )
                    ),
                    vedtakFattet = null,
                    avsluttet = null
                )

            fun List<Generasjon>.jurist(jurist: MaskinellJurist, vedtaksperiodeId: UUID) =
                jurist.medVedtaksperiode(vedtaksperiodeId, dokumentsporing.tilSubsumsjonsformat(), sykmeldingsperiode)

            fun List<Generasjon>.lagreTidsnæreInntekter(arbeidsgiver: Arbeidsgiver, skjæringstidspunkt: LocalDate, hendelse: IAktivitetslogg, oppholdsperiodeMellom: Periode?) {
                lastOrNull { it.endringer.any { it.utbetaling != null } }?.lagreTidsnæreInntekter(skjæringstidspunkt, arbeidsgiver, hendelse, oppholdsperiodeMellom)
            }

            // hvorvidt man delte samme utbetaling før
            fun List<Generasjon>.erUtbetaltPåForskjelligeUtbetalinger(other: List<Generasjon>): Boolean {
                val forrigeIverksatteThis = forrigeIverksatte ?: return true
                val forrigeIverksatteOther = other.forrigeIverksatte ?: return true
                // hvis forrige iverksatte på *this* har ulik korrelasjonsId som siste iverksatte på *other* -> return true
                val utbetalingThis = checkNotNull(forrigeIverksatteThis.utbetaling()) {
                    "forventer at det skal være en utbetaling på en generasjon som er iverksatt"
                }
                val utbetalingOther = forrigeIverksatteOther.utbetaling() ?: return true // forrige periode kan være AUU
                return !utbetalingOther.hørerSammen(utbetalingThis)
            }

            private val List<Generasjon>.forrigeIverksatte get() = lastOrNull { it.vedtakFattet != null }
        }
        internal sealed interface Tilstand {
            fun entering(generasjon: Generasjon) {}
            fun leaving(generasjon: Generasjon) {}
            fun forkastVedtaksperiode(generasjon: Generasjon): Generasjon? {
                generasjon.tilstand(TilInfotrygd)
                return null
            }
            fun håndterEndring(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse): Generasjon? {
                error("Har ikke implementert håndtering av endring i $this")
            }
            fun vedtakAvvist(generasjon: Generasjon, utbetalingsgodkjenning: Utbetalingsgodkjenning) {
                error("Kan ikke avvise vedtak for generasjon i $this")
            }
            fun vedtakFattet(generasjon: Generasjon, utbetalingsgodkjenning: Utbetalingsgodkjenning) {
                error("Kan ikke fatte vedtak for generasjon i $this")
            }
            fun avslutt(generasjon: Generasjon) {
                error("Kan ikke avslutte generasjon i $this")
            }
            fun utenUtbetaling(generasjon: Generasjon, hendelse: IAktivitetslogg) {
                error("Støtter ikke å forkaste utbetaling utbetaling i $this")
            }
            fun utbetaling(generasjon: Generasjon, utbetaling: Utbetaling, grunnlagsdata: VilkårsgrunnlagElement) {
                error("Støtter ikke å motta utbetaling i $this")
            }

            fun oppdaterDokumentsporing(generasjon: Generasjon, dokument: Dokumentsporing): Boolean {
                error("Støtter ikke å oppdatere dokumentsporing med $dokument i $this")
            }

            fun kanForkastes(generasjon: Generasjon, arbeidsgiverUtbetalinger: List<Utbetaling>): Boolean

            fun sikreNyGenerasjon(generasjon: Generasjon): Generasjon? {
                return null
            }
            fun tillaterNyGenerasjon(generasjon: Generasjon, other: Generasjon): Boolean = false
            fun håndterUtbetalinghendelse(generasjon: Generasjon, hendelse: UtbetalingHendelse) = false

            data object Uberegnet : Tilstand {
                override fun entering(generasjon: Generasjon) {
                    check(generasjon.utbetaling() == null) { "skal ikke ha utbetaling og være uberegnet samtidig" }
                }

                override fun kanForkastes(generasjon: Generasjon, arbeidsgiverUtbetalinger: List<Utbetaling>): Boolean {
                    return true
                }

                override fun håndterEndring(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse): Generasjon? {
                    generasjon.oppdaterMedEndring(arbeidsgiver, hendelse)
                    return null
                }

                override fun oppdaterDokumentsporing(generasjon: Generasjon, dokument: Dokumentsporing) =
                    generasjon.kopierMedDokument(dokument)

                override fun utenUtbetaling(generasjon: Generasjon, hendelse: IAktivitetslogg) {}

                override fun utbetaling(generasjon: Generasjon, utbetaling: Utbetaling, grunnlagsdata: VilkårsgrunnlagElement) {
                    generasjon.medUtbetaling(utbetaling, grunnlagsdata)
                    generasjon.tilstand(Beregnet)
                }

                override fun avslutt(generasjon: Generasjon) {
                    generasjon.tilstand(AvsluttetUtenVedtak)
                }
            }
            data object UberegnetOmgjøring : Tilstand by (Uberegnet) {
                override fun utbetaling(generasjon: Generasjon, utbetaling: Utbetaling, grunnlagsdata: VilkårsgrunnlagElement) {
                    generasjon.medUtbetaling(utbetaling, grunnlagsdata)
                    generasjon.tilstand(BeregnetOmgjøring)
                }

                override fun kanForkastes(generasjon: Generasjon, arbeidsgiverUtbetalinger: List<Utbetaling>): Boolean {
                    // TODO: her kunne vi sjekket om omgjøringen kommer til å skape problemer for andre;
                    // i så tilfelle kan vi ikke forkaste
                    return true
                }
            }
            data object UberegnetRevurdering : Tilstand by (Uberegnet) {
                override fun forkastVedtaksperiode(generasjon: Generasjon): Generasjon {
                    return generasjon.nyGenerasjonTilInfotrygd()
                }

                override fun kanForkastes(generasjon: Generasjon, arbeidsgiverUtbetalinger: List<Utbetaling>) = true

                override fun tillaterNyGenerasjon(generasjon: Generasjon, other: Generasjon): Boolean {
                    return other.tilstand == TilInfotrygd
                }
                override fun utbetaling(generasjon: Generasjon, utbetaling: Utbetaling, grunnlagsdata: VilkårsgrunnlagElement) {
                    generasjon.medUtbetaling(utbetaling, grunnlagsdata)
                    generasjon.tilstand(BeregnetRevurdering)
                }
                override fun avslutt(generasjon: Generasjon) {
                    generasjon.tilstand(AvsluttetUtenVedtakRevurdering)
                }
            }
            data object Beregnet : Tilstand {
                override fun entering(generasjon: Generasjon) {
                    checkNotNull(generasjon.gjeldende.utbetaling)
                    checkNotNull(generasjon.gjeldende.grunnlagsdata)
                }

                override fun kanForkastes(generasjon: Generasjon, arbeidsgiverUtbetalinger: List<Utbetaling>) = true


                override fun forkastVedtaksperiode(generasjon: Generasjon): Generasjon? {
                    generasjon.gjeldende.forkastUtbetaling(Aktivitetslogg())
                    return super.forkastVedtaksperiode(generasjon)
                }

                override fun håndterEndring(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse): Generasjon? {
                    generasjon.gjeldende.forkastUtbetaling(hendelse)
                    generasjon.oppdaterMedEndring(arbeidsgiver, hendelse)
                    generasjon.tilstand(Uberegnet)
                    return null
                }

                // TODO: denne overriden kan fjernes dersom utbetalingsperioder() i Vedtaksperiode hensyntar
                // bergnede perioder
                override fun utbetaling(generasjon: Generasjon, utbetaling: Utbetaling, grunnlagsdata: VilkårsgrunnlagElement) {
                    generasjon.gjeldende.forkastUtbetaling(Aktivitetslogg())
                    generasjon.medUtbetaling(utbetaling, grunnlagsdata)
                }

                override fun oppdaterDokumentsporing(generasjon: Generasjon, dokument: Dokumentsporing) =
                    generasjon.kopierMedDokument(dokument)

                override fun utenUtbetaling(generasjon: Generasjon, hendelse: IAktivitetslogg) {
                    generasjon.utenUtbetaling(hendelse)
                    generasjon.tilstand(Uberegnet)
                }

                override fun vedtakAvvist(generasjon: Generasjon, utbetalingsgodkjenning: Utbetalingsgodkjenning) {
                    // perioden kommer til å bli kastet til infotrygd
                }

                override fun vedtakFattet(generasjon: Generasjon, utbetalingsgodkjenning: Utbetalingsgodkjenning) {
                    generasjon.vedtakFattet = utbetalingsgodkjenning.vedtakFattetTidspunkt()
                    generasjon.tilstand(if (generasjon.gjeldende.utbetaling?.erAvsluttet() == true) VedtakIverksatt else VedtakFattet)
                }
            }
            data object BeregnetOmgjøring : Tilstand by (Beregnet) {
                override fun håndterEndring(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse): Generasjon? {
                    generasjon.gjeldende.forkastUtbetaling(hendelse)
                    generasjon.oppdaterMedEndring(arbeidsgiver, hendelse)
                    generasjon.tilstand(UberegnetOmgjøring)
                    return null
                }
                override fun kanForkastes(generasjon: Generasjon, arbeidsgiverUtbetalinger: List<Utbetaling>) = true
                override fun utenUtbetaling(generasjon: Generasjon, hendelse: IAktivitetslogg) {
                    generasjon.utenUtbetaling(hendelse)
                    generasjon.tilstand(UberegnetOmgjøring)
                }
            }
            data object BeregnetRevurdering : Tilstand by (Beregnet) {
                override fun forkastVedtaksperiode(generasjon: Generasjon): Generasjon {
                    generasjon.gjeldende.forkastUtbetaling(Aktivitetslogg())
                    return generasjon.nyGenerasjonTilInfotrygd()
                }
                override fun kanForkastes(generasjon: Generasjon, arbeidsgiverUtbetalinger: List<Utbetaling>) = true
                override fun tillaterNyGenerasjon(generasjon: Generasjon, other: Generasjon): Boolean {
                    return other.tilstand == TilInfotrygd
                }

                override fun utenUtbetaling(generasjon: Generasjon, hendelse: IAktivitetslogg) {
                    generasjon.utenUtbetaling(hendelse)
                    generasjon.tilstand(UberegnetRevurdering)
                }
                override fun vedtakAvvist(generasjon: Generasjon, utbetalingsgodkjenning: Utbetalingsgodkjenning) {
                    generasjon.tilstand(RevurdertVedtakAvvist)
                }
                override fun håndterEndring(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse): Generasjon? {
                    generasjon.gjeldende.forkastUtbetaling(hendelse)
                    generasjon.oppdaterMedEndring(arbeidsgiver, hendelse)
                    generasjon.tilstand(UberegnetRevurdering)
                    return null
                }
            }
            data object RevurdertVedtakAvvist : Tilstand {
                override fun forkastVedtaksperiode(generasjon: Generasjon): Generasjon {
                    return generasjon.nyGenerasjonTilInfotrygd()
                }
                override fun kanForkastes(generasjon: Generasjon, arbeidsgiverUtbetalinger: List<Utbetaling>) =
                    generasjon.erUtbetalingAnnullert(arbeidsgiverUtbetalinger)
                override fun tillaterNyGenerasjon(generasjon: Generasjon, other: Generasjon): Boolean {
                    return true
                }
            }
            data object VedtakFattet : Tilstand {
                override fun entering(generasjon: Generasjon) {
                    checkNotNull(generasjon.gjeldende.utbetaling)
                    checkNotNull(generasjon.gjeldende.grunnlagsdata)
                }
                override fun kanForkastes(generasjon: Generasjon, arbeidsgiverUtbetalinger: List<Utbetaling>) =
                    generasjon.erUtbetalingAnnullert(arbeidsgiverUtbetalinger)
                override fun tillaterNyGenerasjon(generasjon: Generasjon, other: Generasjon): Boolean {
                    return true
                }

                override fun sikreNyGenerasjon(generasjon: Generasjon): Generasjon {
                    return generasjon.sikreNyGenerasjon(UberegnetRevurdering)
                }

                override fun håndterUtbetalinghendelse(generasjon: Generasjon, hendelse: UtbetalingHendelse): Boolean {
                    val utbetaling = checkNotNull(generasjon.gjeldende.utbetaling) { "forventer utbetaling" }
                    if (!utbetaling.gjelderFor(hendelse)) return false
                    if (utbetaling.erAvsluttet()) avslutt(generasjon)
                    return true
                }

                override fun utenUtbetaling(generasjon: Generasjon, hendelse: IAktivitetslogg) {}

                override fun forkastVedtaksperiode(generasjon: Generasjon): Generasjon? {
                    error("usikker på hvordan vi kan forkaste vedtaksperioden som har fått utbetaling godkjent, men ikke avsluttet i $this")
                }

                override fun håndterEndring(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse) =
                    generasjon.nyGenerasjonMedEndring(arbeidsgiver, hendelse, UberegnetRevurdering)


                override fun oppdaterDokumentsporing(generasjon: Generasjon, dokument: Dokumentsporing): Boolean {
                    // oppdaterer ikke dokumentsporing på fattet vedtak;
                    // det må komme en endring via håndterEndring istedenfor
                    return false
                }

                override fun avslutt(generasjon: Generasjon) {
                    generasjon.tilstand(VedtakIverksatt)
                }
            }

            data object AvsluttetUtenVedtak : Tilstand {
                override fun forkastVedtaksperiode(generasjon: Generasjon): Generasjon {
                    return generasjon.nyGenerasjonTilInfotrygd()
                }
                // TODO: her kunne vi sjekket om omgjøringen kommer til å skape problemer for andre;
                // i så tilfelle kan vi ikke forkaste
                override fun kanForkastes(generasjon: Generasjon, arbeidsgiverUtbetalinger: List<Utbetaling>) =
                    true
                override fun tillaterNyGenerasjon(generasjon: Generasjon, other: Generasjon): Boolean {
                    return true
                }

                override fun avslutt(generasjon: Generasjon) {}

                override fun sikreNyGenerasjon(generasjon: Generasjon): Generasjon {
                    return generasjon.sikreNyGenerasjon(UberegnetOmgjøring)
                }

                // det deles ut dokumentsporinger til alle vedtaksperioder så snart én har håndtert hendelsen;
                // det betyr også at det kommer til å bli sendt ut et overstyring igangsatt-event som vil
                // opprette en ny Uberegnet periode for Vedtaksperioden.
                // derfor legger vi den bare til i listen uten å gjøre noe mer spesielt
                override fun oppdaterDokumentsporing(generasjon: Generasjon, dokument: Dokumentsporing) =
                    generasjon.kopierMedDokument(dokument)

                override fun håndterEndring(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse) =
                    generasjon.nyGenerasjonMedEndring(arbeidsgiver, hendelse, UberegnetOmgjøring)

                override fun entering(generasjon: Generasjon) {
                    generasjon.vedtakFattet = null // det fattes ikke vedtak i AUU
                    generasjon.avsluttet = LocalDateTime.now()
                }
            }
            data object AvsluttetUtenVedtakRevurdering : Tilstand by (AvsluttetUtenVedtak) {
                override fun håndterEndring(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse) =
                    generasjon.nyGenerasjonMedEndring(arbeidsgiver, hendelse, UberegnetRevurdering)

                override fun sikreNyGenerasjon(generasjon: Generasjon): Generasjon {
                    return generasjon.sikreNyGenerasjon(UberegnetRevurdering)
                }
            }
            data object VedtakIverksatt : Tilstand {
                override fun forkastVedtaksperiode(generasjon: Generasjon): Generasjon {
                    return generasjon.nyGenerasjonTilInfotrygd()
                }
                override fun kanForkastes(generasjon: Generasjon, arbeidsgiverUtbetalinger: List<Utbetaling>) =
                    generasjon.erUtbetalingAnnullert(arbeidsgiverUtbetalinger)
                override fun tillaterNyGenerasjon(generasjon: Generasjon, other: Generasjon): Boolean {
                    return true
                }

                override fun sikreNyGenerasjon(generasjon: Generasjon): Generasjon {
                    return generasjon.sikreNyGenerasjon(UberegnetRevurdering)
                }

                override fun håndterEndring(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse) =
                    generasjon.nyGenerasjonMedEndring(arbeidsgiver, hendelse, UberegnetRevurdering)

                // det deles ut dokumentsporinger til alle vedtaksperioder så snart én har håndtert hendelsen;
                // det betyr også at det kommer til å bli sendt ut et overstyring igangsatt-event som vil
                // opprette en ny Uberegnet periode for Vedtaksperioden.
                // derfor legger vi den bare til i listen uten å gjøre noe mer spesielt
                override fun oppdaterDokumentsporing(generasjon: Generasjon, dokument: Dokumentsporing) =
                    generasjon.kopierMedDokument(dokument)

                override fun entering(generasjon: Generasjon) {
                    generasjon.avsluttet = LocalDateTime.now()
                }
            }
            data object TilInfotrygd : Tilstand {
                override fun entering(generasjon: Generasjon) {
                    generasjon.avsluttet = LocalDateTime.now()
                }
                override fun kanForkastes(generasjon: Generasjon, arbeidsgiverUtbetalinger: List<Utbetaling>): Boolean {
                    error("forventer ikke å forkaste en periode som allerde er i $this")
                }
            }
        }
    }
}