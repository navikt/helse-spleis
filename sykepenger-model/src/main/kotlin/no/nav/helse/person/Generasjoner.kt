package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.etterlevelse.SubsumsjonObserver.Companion.NullObserver
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.utbetaling.AnnullerUtbetaling
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.Utbetalingsavgjørelse
import no.nav.helse.hendelser.utbetaling.avvist
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
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.GodkjenningsbehovBuilder
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.PeriodeMedSammeSkjæringstidspunkt
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.sykdomstidslinje.SykdomshistorikkHendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.harId
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.Maksdatosituasjon
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

internal class Generasjoner private constructor(generasjoner: List<Generasjon>) {
    internal constructor() : this(emptyList())
    companion object {
        // for PersonData
        fun ferdigGenerasjoner(generasjoner: List<Generasjon>) = Generasjoner(generasjoner)
    }
    private val utbetalingene get() = generasjoner.mapNotNull(Generasjon::utbetaling)
    private val generasjoner = generasjoner.toMutableList()
    private val siste get() = generasjoner.lastOrNull()?.utbetaling()

    private val observatører = mutableListOf<GenerasjonObserver>()

    internal fun initiellGenerasjon(sykmeldingsperiode: Periode, sykdomstidslinje: Sykdomstidslinje, dokumentsporing: Dokumentsporing, søknad: Søknad) {
        check(generasjoner.isEmpty())
        val generasjon = Generasjon.nyGenerasjon(this.observatører, sykdomstidslinje, dokumentsporing, sykmeldingsperiode, søknad)
        leggTilNyGenerasjon(generasjon)
    }

    internal fun addObserver(observatør: GenerasjonObserver) {
        observatører.add(observatør)
        generasjoner.forEach { it.addObserver(observatør) }
    }

    internal fun accept(visitor: GenerasjonerVisitor) {
        visitor.preVisitGenerasjoner(generasjoner)
        generasjoner.forEach { generasjon ->
            generasjon.accept(visitor)
        }
        visitor.postVisitGenerasjoner(generasjoner)
    }

    internal fun sykdomstidslinje() = generasjoner.last().sykdomstidslinje()

    internal fun trekkerTilbakePenger() = siste?.trekkerTilbakePenger() == true
    internal fun utbetales() = generasjoner.any { it.erInFlight() }
    internal fun erAvsluttet() = generasjoner.last().erAvsluttet()
    internal fun erAvvist() = siste?.erAvvist() == true
    internal fun harUtbetalinger() = siste?.harUtbetalinger() == true
    internal fun erUbetalt() = siste?.erUbetalt() == true

    internal fun kanForkastes(hendelse: IAktivitetslogg, arbeidsgiverUtbetalinger: List<Utbetaling>) =
        generasjoner.last().kanForkastes(hendelse, arbeidsgiverUtbetalinger)
    internal fun harId(utbetalingId: UUID) = utbetalingene.harId(utbetalingId)
    internal fun håndterUtbetalinghendelse(hendelse: UtbetalingHendelse) = generasjoner.any { it.håndterUtbetalinghendelse(hendelse) }

    internal fun lagreTidsnæreInntekter(
        arbeidsgiver: Arbeidsgiver,
        skjæringstidspunkt: LocalDate,
        hendelse: IAktivitetslogg,
        oppholdsperiodeMellom: Periode?
    ) {
        generasjoner.lagreTidsnæreInntekter(arbeidsgiver, skjæringstidspunkt, hendelse, oppholdsperiodeMellom)
    }

    internal fun gjelderIkkeFor(hendelse: Utbetalingsavgjørelse) = siste?.gjelderFor(hendelse) != true

    internal fun erHistorikkEndretSidenBeregning(infotrygdhistorikk: Infotrygdhistorikk) =
        infotrygdhistorikk.harEndretHistorikk(siste!!)

    internal fun overlapperMed(other: Generasjoner): Boolean {
        if (!this.harUtbetalinger() || !other.harUtbetalinger()) return false
        return this.siste!!.overlapperMed(other.siste!!)
    }

    internal fun valider(simulering: Simulering) {
        siste!!.valider(simulering)
    }

    internal fun erKlarForGodkjenning() = siste?.erKlarForGodkjenning() ?: false

    internal fun simuler(hendelse: IAktivitetslogg) = siste!!.simuler(hendelse)

    internal fun godkjenning(hendelse: IAktivitetslogg, erForlengelse: Boolean, perioderMedSammeSkjæringstidspunkt: List<Pair<UUID, Generasjoner>>, kanForkastes: Boolean) {
        val generasjonerMedSammeSkjæringstidspunkt = perioderMedSammeSkjæringstidspunkt.map { it.first to it.second.generasjoner.last() }
        generasjoner.last().godkjenning(hendelse, erForlengelse, generasjonerMedSammeSkjæringstidspunkt, kanForkastes)
    }

    internal fun håndterAnnullering(arbeidsgiver: Arbeidsgiver, hendelse: AnnullerUtbetaling, andreGenerasjoner: List<Generasjoner>): Boolean {
        val annullering = generasjoner.last().annuller(arbeidsgiver, hendelse, this.generasjoner.toList()) ?: return false
        andreGenerasjoner.forEach {
            it.kobleAnnulleringTilAndre(arbeidsgiver, hendelse, annullering)
        }
        return true
    }

    private fun kobleAnnulleringTilAndre(arbeidsgiver: Arbeidsgiver, hendelse: AnnullerUtbetaling, annullering: Utbetaling) {
        leggTilNyGenerasjon(generasjoner.last().annuller(arbeidsgiver, hendelse, annullering, generasjoner.toList()))
    }

    internal fun nyUtbetaling(
        vedtaksperiodeSomLagerUtbetaling: UUID,
        fødselsnummer: String,
        arbeidsgiver: Arbeidsgiver,
        grunnlagsdata: VilkårsgrunnlagElement,
        hendelse: IAktivitetslogg,
        maksimumSykepenger: Maksdatosituasjon,
        utbetalingstidslinje: Utbetalingstidslinje
    ): Utbetalingstidslinje {
        return generasjoner.last().utbetaling(vedtaksperiodeSomLagerUtbetaling, fødselsnummer, arbeidsgiver, grunnlagsdata, hendelse, maksimumSykepenger, utbetalingstidslinje)
    }

    internal fun forkast(arbeidsgiver: Arbeidsgiver, hendelse: Hendelse) {
        leggTilNyGenerasjon(generasjoner.last().forkastVedtaksperiode(arbeidsgiver, hendelse))
        generasjoner.last().forkastetGenerasjon(hendelse)
    }
    internal fun forkastUtbetaling(hendelse: IAktivitetslogg) {
        generasjoner.last().forkastUtbetaling(hendelse)
    }
    internal fun harIkkeUtbetaling() = generasjoner.last().harIkkeUtbetaling()


    fun vedtakFattet(arbeidsgiver: Arbeidsgiver, utbetalingsavgjørelse: Utbetalingsavgjørelse) {
        this.generasjoner.last().vedtakFattet(arbeidsgiver, utbetalingsavgjørelse)
    }
    fun bekreftAvsluttetGenerasjonMedVedtak(arbeidsgiver: Arbeidsgiver) {
        bekreftAvsluttetGenerasjon(arbeidsgiver)
        check(erFattetVedtak()) {
            "forventer at generasjonen skal ha fattet vedtak"
        }
    }
    private fun erFattetVedtak(): Boolean {
        return generasjoner.last().erFattetVedtak()
    }
    private fun bekreftAvsluttetGenerasjon(arbeidsgiver: Arbeidsgiver) {
        arbeidsgiver.bekreftErLåst(periode())
        check(erAvsluttet()) {
            "forventer at utbetaling skal være avsluttet"
        }
    }
    fun avsluttUtenVedtak(arbeidsgiver: Arbeidsgiver, hendelse: IAktivitetslogg) {
        check(generasjoner.last().utbetaling() == null) { "Forventet ikke at perioden har fått utbetaling: kun perioder innenfor arbeidsgiverperioden skal sendes hit. " }
        this.generasjoner.last().avsluttUtenVedtak(arbeidsgiver, hendelse)
        bekreftAvsluttetGenerasjon(arbeidsgiver)
    }

    internal fun sykmeldingsperiode() = this.generasjoner.first().sykmeldingsperiode()
    internal fun periode() = this.generasjoner.last().periode()

    // sørger for ny generasjon når vedtaksperioden går ut av Avsluttet/AUU,
    // men bare hvis det ikke er laget en ny allerede fra før
    fun sikreNyGenerasjon(arbeidsgiver: Arbeidsgiver, hendelse: Hendelse) {
        leggTilNyGenerasjon(generasjoner.last().sikreNyGenerasjon(arbeidsgiver, hendelse))
    }

    private fun leggTilNyGenerasjon(generasjon: Generasjon?) {
        if (generasjon == null) return
        if (generasjoner.isNotEmpty())
            check(generasjoner.last().tillaterNyGenerasjon(generasjon)) {
                "siste generasjon ${generasjoner.last()} tillater ikke opprettelse av ny generasjon $generasjon"
            }
        this.generasjoner.add(generasjon)
    }

    fun klarForUtbetaling(): Boolean {
        return generasjoner.last().klarForUtbetaling()
    }

    fun bekreftÅpenGenerasjon(arbeidsgiver: Arbeidsgiver) {
        arbeidsgiver.bekreftErÅpen(periode())
        check(generasjoner.last().harÅpenGenerasjon()) {
            "forventer at vedtaksperioden er uberegnet når den går ut av Avsluttet/AvsluttetUtenUtbetaling"
        }
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

    fun håndterEndring(person: Person, arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse) {
        val nyGenerasjon = generasjoner.last().håndterEndring(arbeidsgiver, hendelse)?.also {
            leggTilNyGenerasjon(it)
        }
        // 🤯 <OBS! NB!> 🤯
        // spesialist er -avhengig- av at sykefraværstilfelle går ut før generasjonen kan lukkes automatisk
        // Meldingen kan dessuten ikke sendes ut før generasjonen er fysisk lagt til i listen (se leggTilNyGenerasjon(it) over),
        // fordi når vedtaksperioden skal håndtere sykefraværstilfelle-signalet så avhenger den at generasjonen er på plass
        person.sykdomshistorikkEndret(hendelse)
        // 🤯 </OBS! NB!> 🤯
        nyGenerasjon?.vurderLukkeAutomatisk(arbeidsgiver, hendelse)
    }

    fun erUtbetaltPåForskjelligeUtbetalinger(other: Generasjoner): Boolean {
        return this.generasjoner.erUtbetaltPåForskjelligeUtbetalinger(other.generasjoner)
    }

    internal fun trengerArbeidsgiverperiode() = generasjoner.dokumentsporing.sisteInntektsmeldingId() == null

    internal class Generasjonkilde(
        val meldingsreferanseId: UUID,
        val innsendt: LocalDateTime,
        val registert: LocalDateTime,
        val avsender: Avsender
    ) {
        constructor(hendelse: Hendelse): this(hendelse.meldingsreferanseId(), hendelse.innsendt(), hendelse.registrert(), hendelse.avsender())

        internal fun accept(visitor: GenerasjonerVisitor) {
            visitor.preVisitGenerasjonkilde(meldingsreferanseId, innsendt, registert, avsender)
        }
    }


    internal class Generasjon private constructor(
        private val id: UUID,
        private var tilstand: Tilstand,
        private val endringer: MutableList<Endring>,
        private var vedtakFattet: LocalDateTime?,
        private var avsluttet: LocalDateTime?,
        private val kilde: Generasjonkilde,
        observatører: List<GenerasjonObserver>
    ) {
        private val observatører = observatører.toMutableList()
        private val gjeldende get() = endringer.last()
        private val periode: Periode get() = gjeldende.periode
        private val tidsstempel = endringer.first().tidsstempel
        private val dokumentsporing get() = endringer.dokumentsporing

        constructor(observatører: List<GenerasjonObserver>, tilstand: Tilstand, endringer: List<Endring>, avsluttet: LocalDateTime?, kilde: Generasjonkilde) : this(UUID.randomUUID(), tilstand, endringer.toMutableList(), null, avsluttet, kilde, observatører) {
            check(observatører.isNotEmpty()) {
                "må ha minst én observatør for å registrere en generasjon"
            }
            emitNyGenerasjonOpprettet()
        }

        init {
            check(endringer.isNotEmpty()) {
                "Må ha endringer for at det skal være vits med en generasjon"
            }
        }

        internal fun addObserver(observatør: GenerasjonObserver) {
            check(observatører.none { it === observatør }) { "observatør finnes fra før" }
            observatører.add(observatør)
        }

        override fun toString() = "$periode - $tilstand"

        fun accept(visitor: GenerasjonerVisitor) {
            visitor.preVisitGenerasjon(id, tidsstempel, tilstand, periode, vedtakFattet, avsluttet, kilde)
            endringer.forEach { it.accept(visitor) }
            kilde.accept(visitor)
            visitor.postVisitGenerasjon(id, tidsstempel, tilstand, periode, vedtakFattet, avsluttet, kilde)
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

            internal fun accept(visitor: GenerasjonerVisitor) {
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

            fun godkjenning(hendelse: IAktivitetslogg, erForlengelse: Boolean, kanForkastes: Boolean, generasjonId: UUID, perioderMedSammeSkjæringstidspunkt: List<Triple<UUID, UUID, Periode>>) {
                checkNotNull(utbetaling) { "Forventet ikke manglende utbetaling ved godkjenningsbehov" }
                checkNotNull(grunnlagsdata) { "Forventet ikke manglende vilkårsgrunnlag ved godkjennignsbehov" }
                val builder = GodkjenningsbehovBuilder(erForlengelse, kanForkastes, periode, generasjonId, perioderMedSammeSkjæringstidspunkt.map { (vedtaksperiodeId, generasjonId, periode) ->
                    PeriodeMedSammeSkjæringstidspunkt(vedtaksperiodeId, generasjonId, periode)
                })
                grunnlagsdata.byggGodkjenningsbehov(builder)
                utbetaling.byggGodkjenningsbehov(hendelse, builder, periode)
                Aktivitet.Behov.godkjenning(
                    aktivitetslogg = hendelse,
                    builder = builder
                )
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

        internal fun klarForUtbetaling() = this.tilstand in setOf(Tilstand.Uberegnet, Tilstand.UberegnetOmgjøring, Tilstand.UberegnetRevurdering)
        internal fun harÅpenGenerasjon() = this.tilstand in setOf(Tilstand.UberegnetRevurdering, Tilstand.UberegnetOmgjøring, Tilstand.AnnullertPeriode, Tilstand.TilInfotrygd)
        internal fun harIkkeUtbetaling() = this.tilstand in setOf(Tilstand.Uberegnet, Tilstand.UberegnetOmgjøring, Tilstand.TilInfotrygd)

        internal fun vedtakFattet(arbeidsgiver: Arbeidsgiver, utbetalingsavgjørelse: Utbetalingsavgjørelse) {
            if (utbetalingsavgjørelse.avvist) return tilstand.vedtakAvvist(this, arbeidsgiver, utbetalingsavgjørelse)
            tilstand.vedtakFattet(this, arbeidsgiver, utbetalingsavgjørelse)
        }

        internal fun avsluttUtenVedtak(arbeidsgiver: Arbeidsgiver, hendelse: IAktivitetslogg) {
            tilstand.avsluttUtenVedtak(this, arbeidsgiver, hendelse)
        }

        internal fun forkastVedtaksperiode(arbeidsgiver: Arbeidsgiver, hendelse: Hendelse): Generasjon? {
            return tilstand.forkastVedtaksperiode(this, arbeidsgiver, hendelse)
        }

        private fun tilstand(nyTilstand: Tilstand, hendelse: IAktivitetslogg) {
            tilstand.leaving(this)
            tilstand = nyTilstand
            tilstand.entering(this, hendelse)
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

        fun utbetaling(
            vedtaksperiodeSomLagerUtbetaling: UUID,
            fødselsnummer: String,
            arbeidsgiver: Arbeidsgiver,
            grunnlagsdata: VilkårsgrunnlagElement,
            hendelse: IAktivitetslogg,
            maksimumSykepenger: Maksdatosituasjon,
            utbetalingstidslinje: Utbetalingstidslinje
        ): Utbetalingstidslinje {
            return tilstand.utbetaling(this, vedtaksperiodeSomLagerUtbetaling, fødselsnummer, arbeidsgiver, grunnlagsdata, hendelse, maksimumSykepenger, utbetalingstidslinje)
        }

        private fun håndterAnnullering(arbeidsgiver: Arbeidsgiver, hendelse: AnnullerUtbetaling): Utbetaling? {
            val utbetaling = checkNotNull(gjeldende.utbetaling) { "forventer å ha en tidligere utbetaling" }
            return arbeidsgiver.nyAnnullering(hendelse, utbetaling)
        }

        private fun lagOmgjøring(
            vedtaksperiodeSomLagerUtbetaling: UUID,
            fødselsnummer: String,
            arbeidsgiver: Arbeidsgiver,
            grunnlagsdata: VilkårsgrunnlagElement,
            hendelse: IAktivitetslogg,
            maksimumSykepenger: Maksdatosituasjon,
            utbetalingstidslinje: Utbetalingstidslinje
        ): Utbetalingstidslinje {
            val strategi = Arbeidsgiver::lagUtbetaling
            return lagUtbetaling(
                vedtaksperiodeSomLagerUtbetaling,
                fødselsnummer,
                arbeidsgiver,
                grunnlagsdata,
                hendelse,
                maksimumSykepenger,
                utbetalingstidslinje,
                strategi,
                Tilstand.BeregnetOmgjøring
            )
        }
        private fun lagUtbetaling(
            vedtaksperiodeSomLagerUtbetaling: UUID,
            fødselsnummer: String,
            arbeidsgiver: Arbeidsgiver,
            grunnlagsdata: VilkårsgrunnlagElement,
            hendelse: IAktivitetslogg,
            maksimumSykepenger: Maksdatosituasjon,
            utbetalingstidslinje: Utbetalingstidslinje
        ): Utbetalingstidslinje {
            val strategi = Arbeidsgiver::lagUtbetaling
            return lagUtbetaling(
                vedtaksperiodeSomLagerUtbetaling,
                fødselsnummer,
                arbeidsgiver,
                grunnlagsdata,
                hendelse,
                maksimumSykepenger,
                utbetalingstidslinje,
                strategi,
                Tilstand.Beregnet
            )
        }
        private fun lagRevurdering(
            vedtaksperiodeSomLagerUtbetaling: UUID,
            fødselsnummer: String,
            arbeidsgiver: Arbeidsgiver,
            grunnlagsdata: VilkårsgrunnlagElement,
            hendelse: IAktivitetslogg,
            maksimumSykepenger: Maksdatosituasjon,
            utbetalingstidslinje: Utbetalingstidslinje
        ): Utbetalingstidslinje {
            val strategi = Arbeidsgiver::lagRevurdering
            return lagUtbetaling(
                vedtaksperiodeSomLagerUtbetaling,
                fødselsnummer,
                arbeidsgiver,
                grunnlagsdata,
                hendelse,
                maksimumSykepenger,
                utbetalingstidslinje,
                strategi,
                Tilstand.BeregnetRevurdering
            )
        }
        private fun lagUtbetaling(
            vedtaksperiodeSomLagerUtbetaling: UUID,
            fødselsnummer: String,
            arbeidsgiver: Arbeidsgiver,
            grunnlagsdata: VilkårsgrunnlagElement,
            hendelse: IAktivitetslogg,
            maksimumSykepenger: Maksdatosituasjon,
            utbetalingstidslinje: Utbetalingstidslinje,
            strategi: (Arbeidsgiver, aktivitetslogg: IAktivitetslogg, fødselsnummer: String, utbetalingstidslinje: Utbetalingstidslinje, maksdato: LocalDate, forbrukteSykedager: Int, gjenståendeSykedager: Int, periode: Periode) -> Utbetaling,
            nyTilstand: Tilstand
        ): Utbetalingstidslinje {
            val denNyeUtbetalingen = strategi(arbeidsgiver, hendelse, fødselsnummer, utbetalingstidslinje, maksimumSykepenger.maksdato, maksimumSykepenger.forbrukteDager, maksimumSykepenger.gjenståendeDager, periode)
            denNyeUtbetalingen.nyVedtaksperiodeUtbetaling(vedtaksperiodeSomLagerUtbetaling)
            nyEndring(gjeldende.kopierMedUtbetaling(denNyeUtbetalingen, grunnlagsdata))
            tilstand(nyTilstand, hendelse)
            return utbetalingstidslinje.subset(periode)
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

        fun vurderLukkeAutomatisk(arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse) {
            return tilstand.vurderLukkeAutomatisk(this, arbeidsgiver, hendelse)
        }
        private fun håndtereEndring(arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse): Endring {
            val oppdatertPeriode = hendelse.oppdaterFom(endringer.last().periode)
            val sykdomstidslinje = arbeidsgiver.oppdaterSykdom(hendelse).subset(oppdatertPeriode)
            return endringer.last().kopierMedEndring(oppdatertPeriode, hendelse.dokumentsporing(), sykdomstidslinje)
        }

        // oppdaterer seg selv med endringen
        private fun oppdaterMedEndring(arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse) {
            val endring = håndtereEndring(arbeidsgiver, hendelse)
            if (endring == gjeldende) return
            nyEndring(endring)
        }
        private fun nyGenerasjonMedEndring(arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse, starttilstand: Tilstand = Tilstand.Uberegnet): Generasjon {
            arbeidsgiver.låsOpp(periode)
            return Generasjon(
                observatører = this.observatører,
                tilstand = starttilstand,
                endringer = listOf(håndtereEndring(arbeidsgiver, hendelse)),
                avsluttet = null,
                kilde = Generasjonkilde(hendelse)
            )
        }

        private fun sikreNyGenerasjon(arbeidsgiver: Arbeidsgiver, starttilstand: Tilstand, hendelse: Hendelse): Generasjon {
            arbeidsgiver.låsOpp(periode)
            return Generasjon(
                observatører = this.observatører,
                tilstand = starttilstand,
                endringer = listOf(endringer.last().kopierUtenUtbetaling()),
                avsluttet = null,
                kilde = Generasjonkilde(hendelse)
            )
        }

        private fun nyGenerasjonTilInfotrygd(arbeidsgiver: Arbeidsgiver, hendelse: Hendelse): Generasjon {
            arbeidsgiver.låsOpp(periode)
            return Generasjon(
                observatører = this.observatører,
                tilstand = Tilstand.TilInfotrygd,
                endringer = listOf(this.gjeldende.kopierUtenUtbetaling()),
                avsluttet = LocalDateTime.now(),
                kilde = Generasjonkilde(hendelse)
            )
        }

        private fun nyAnnullertGenerasjon(arbeidsgiver: Arbeidsgiver, hendelse: Hendelse, annullering: Utbetaling, grunnlagsdata: VilkårsgrunnlagElement): Generasjon {
            arbeidsgiver.låsOpp(periode)
            return Generasjon(
                observatører = this.observatører,
                tilstand = Tilstand.AnnullertPeriode,
                endringer = listOf(this.gjeldende.kopierMedUtbetaling(annullering, grunnlagsdata)),
                avsluttet = null,
                kilde = Generasjonkilde(hendelse)
            )
        }

        fun sikreNyGenerasjon(arbeidsgiver: Arbeidsgiver, hendelse: Hendelse): Generasjon? {
            return tilstand.sikreNyGenerasjon(this, arbeidsgiver, hendelse)
        }

        fun tillaterNyGenerasjon(other: Generasjon): Boolean {
            return tilstand.tillaterNyGenerasjon(this, other)
        }

        fun håndterUtbetalinghendelse(hendelse: UtbetalingHendelse): Boolean {
            return tilstand.håndterUtbetalinghendelse(this, hendelse)
        }

        fun kanForkastes(hendelse: IAktivitetslogg, arbeidsgiverUtbetalinger: List<Utbetaling>): Boolean {
            return tilstand.kanForkastes(this, hendelse, arbeidsgiverUtbetalinger)
        }
        private fun generasjonLukket(arbeidsgiver: Arbeidsgiver, ) {
            arbeidsgiver.lås(periode)
            check(observatører.isNotEmpty()) { "generasjonen har ingen registrert observatør" }
            observatører.forEach { it.generasjonLukket(id) }
        }
        private fun vedtakIverksatt(hendelse: IAktivitetslogg) {
            check(observatører.isNotEmpty()) { "generasjonen har ingen registrert observatør" }
            observatører.forEach { it.vedtakIverksatt(hendelse, id, avsluttet!!, periode, dokumentsporing.ider(), utbetaling()!!.id, vedtakFattet!!, gjeldende.grunnlagsdata!!) }
        }

        private fun avsluttetUtenVedtak(hendelse: IAktivitetslogg) {
            check(observatører.isNotEmpty()) { "generasjonen har ingen registrert observatør" }
            observatører.forEach { it.avsluttetUtenVedtak(hendelse, id, avsluttet!!, periode, dokumentsporing.ider()) }
        }

        private fun emitNyGenerasjonOpprettet() {
            check(observatører.isNotEmpty()) { "generasjonen har ingen registrert observatør" }
            val type = when(tilstand) {
                Tilstand.UberegnetRevurdering -> PersonObserver.GenerasjonOpprettetEvent.Type.Revurdering
                Tilstand.UberegnetOmgjøring -> PersonObserver.GenerasjonOpprettetEvent.Type.Omgjøring
                is Tilstand.TilInfotrygd,
                is Tilstand.AnnullertPeriode -> PersonObserver.GenerasjonOpprettetEvent.Type.TilInfotrygd
                else -> PersonObserver.GenerasjonOpprettetEvent.Type.Søknad
            }
            observatører.forEach { it.nyGenerasjon(id, kilde.meldingsreferanseId, kilde.innsendt, kilde.registert, kilde.avsender, type) }
        }

        internal fun forkastetGenerasjon(hendelse: Hendelse) {
            check(observatører.isNotEmpty()) { "generasjonen har ingen registrert observatør" }
            check(this.tilstand === Tilstand.TilInfotrygd)
            observatører.forEach { it.generasjonForkastet(id, hendelse) }
        }

        internal fun vedtakAnnullert(hendelse: IAktivitetslogg) {
            check(observatører.isNotEmpty()) { "generasjonen har ingen registrert observatør" }
            check(this.tilstand === Tilstand.AnnullertPeriode)
            observatører.forEach { it.vedtakAnnullert(hendelse, id) }
        }

        internal fun godkjenning(hendelse: IAktivitetslogg, erForlengelse: Boolean, generasjonerMedSammeSkjæringstidspunkt: List<Pair<UUID, Generasjon>>, kanForkastes: Boolean) {
            val perioderMedSammeSkjæringstidspunkt = generasjonerMedSammeSkjæringstidspunkt.map { Triple(it.first, it.second.id, it.second.periode) }
            gjeldende.godkjenning(hendelse, erForlengelse, kanForkastes, id, perioderMedSammeSkjæringstidspunkt)
        }

        fun annuller(arbeidsgiver: Arbeidsgiver, hendelse: AnnullerUtbetaling, generasjoner: List<Generasjon>): Utbetaling? {
            val sisteVedtak = generasjoner.lastOrNull { it.erFattetVedtak() } ?: return null
            return sisteVedtak.håndterAnnullering(arbeidsgiver, hendelse)
        }
        fun annuller(arbeidsgiver: Arbeidsgiver, hendelse: AnnullerUtbetaling, annullering: Utbetaling, andreGenerasjoner: List<Generasjon>): Generasjon? {
            val sisteVedtak = andreGenerasjoner.lastOrNull { generasjonen -> generasjonen.erFattetVedtak() } ?: return null
            if (true != sisteVedtak.utbetaling()?.hørerSammen(annullering)) return null
            return tilstand.annuller(this, arbeidsgiver, hendelse, annullering, checkNotNull(sisteVedtak.gjeldende.grunnlagsdata))
        }

        private fun tillaterOverlappendeUtbetalingerForkasting(hendelse: IAktivitetslogg, arbeidsgiverUtbetalinger: List<Utbetaling>): Boolean {
            val overlappendeUtbetalinger = arbeidsgiverUtbetalinger.filter { it.overlapperMed(periode) }
            return Utbetaling.kanForkastes(overlappendeUtbetalinger, arbeidsgiverUtbetalinger).also {
                if (!it) hendelse.info("[kanForkastes] Kan i utgangspunktet ikke forkastes ettersom perioden har ${overlappendeUtbetalinger.size} overlappende utbetalinger")
            }
        }
        /* hvorvidt en AUU- (eller har vært-auu)-periode kan forkastes */
        private fun kanForkastingAvKortPeriodeTillates(hendelse: IAktivitetslogg, arbeidsgiverUtbetalinger: List<Utbetaling>): Boolean {
            return tillaterOverlappendeUtbetalingerForkasting(hendelse, arbeidsgiverUtbetalinger)
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

            fun nyGenerasjon(observatører: List<GenerasjonObserver>, sykdomstidslinje: Sykdomstidslinje, dokumentsporing: Dokumentsporing, sykmeldingsperiode: Periode, søknad: Søknad) =
                Generasjon(
                    observatører = observatører,
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
                    avsluttet = null,
                    kilde = Generasjonkilde(søknad)
                )
            // for PersonData
            fun ferdigGenerasjon(id: UUID, tilstand: Tilstand, endringer: MutableList<Endring>, vedtakFattet: LocalDateTime?, avsluttet: LocalDateTime?, kilde: Generasjonkilde) =
                Generasjon(id, tilstand, endringer, vedtakFattet, avsluttet, kilde, emptyList())

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
            fun entering(generasjon: Generasjon, hendelse: IAktivitetslogg) {}
            fun leaving(generasjon: Generasjon) {}
            fun annuller(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: AnnullerUtbetaling, annullering: Utbetaling, grunnlagsdata: VilkårsgrunnlagElement): Generasjon? {
                return null
            }
            fun forkastVedtaksperiode(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: Hendelse): Generasjon? {
                generasjon.tilstand(TilInfotrygd, hendelse)
                return null
            }
            fun håndterEndring(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse): Generasjon? {
                error("Har ikke implementert håndtering av endring i $this")
            }
            fun vurderLukkeAutomatisk(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse) {}
            fun vedtakAvvist(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, utbetalingsavgjørelse: Utbetalingsavgjørelse) {
                error("Kan ikke avvise vedtak for generasjon i $this")
            }
            fun vedtakFattet(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, utbetalingsavgjørelse: Utbetalingsavgjørelse) {
                error("Kan ikke fatte vedtak for generasjon i $this")
            }
            fun avsluttUtenVedtak(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: IAktivitetslogg) {
                error("Kan ikke avslutte uten vedtak for generasjon i $this")
            }
            fun avsluttMedVedtak(generasjon: Generasjon, hendelse: IAktivitetslogg) {
                error("Kan ikke avslutte generasjon i $this")
            }
            fun utenUtbetaling(generasjon: Generasjon, hendelse: IAktivitetslogg) {
                error("Støtter ikke å forkaste utbetaling utbetaling i $this")
            }
            fun utbetaling(
                generasjon: Generasjon,
                vedtaksperiodeSomLagerUtbetaling: UUID,
                fødselsnummer: String,
                arbeidsgiver: Arbeidsgiver,
                grunnlagsdata: VilkårsgrunnlagElement,
                hendelse: IAktivitetslogg,
                maksimumSykepenger: Maksdatosituasjon,
                utbetalingstidslinje: Utbetalingstidslinje
            ): Utbetalingstidslinje {
                error("Støtter ikke å opprette utbetaling i $this")
            }

            fun oppdaterDokumentsporing(generasjon: Generasjon, dokument: Dokumentsporing): Boolean {
                error("Støtter ikke å oppdatere dokumentsporing med $dokument i $this")
            }

            fun kanForkastes(generasjon: Generasjon, hendelse: IAktivitetslogg, arbeidsgiverUtbetalinger: List<Utbetaling>): Boolean
            fun sikreNyGenerasjon(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: Hendelse): Generasjon? {
                return null
            }
            fun tillaterNyGenerasjon(generasjon: Generasjon, other: Generasjon): Boolean = false
            fun håndterUtbetalinghendelse(generasjon: Generasjon, hendelse: UtbetalingHendelse) = false

            data object Uberegnet : Tilstand {
                override fun entering(generasjon: Generasjon, hendelse: IAktivitetslogg) {
                    check(generasjon.utbetaling() == null) { "skal ikke ha utbetaling og være uberegnet samtidig" }
                }

                override fun kanForkastes(generasjon: Generasjon, hendelse: IAktivitetslogg, arbeidsgiverUtbetalinger: List<Utbetaling>) = true

                override fun håndterEndring(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse): Generasjon? {
                    generasjon.oppdaterMedEndring(arbeidsgiver, hendelse)
                    return null
                }

                override fun oppdaterDokumentsporing(generasjon: Generasjon, dokument: Dokumentsporing) =
                    generasjon.kopierMedDokument(dokument)

                override fun utenUtbetaling(generasjon: Generasjon, hendelse: IAktivitetslogg) {}

                override fun utbetaling(
                    generasjon: Generasjon,
                    vedtaksperiodeSomLagerUtbetaling: UUID,
                    fødselsnummer: String,
                    arbeidsgiver: Arbeidsgiver,
                    grunnlagsdata: VilkårsgrunnlagElement,
                    hendelse: IAktivitetslogg,
                    maksimumSykepenger: Maksdatosituasjon,
                    utbetalingstidslinje: Utbetalingstidslinje
                ): Utbetalingstidslinje {
                    return generasjon.lagUtbetaling(vedtaksperiodeSomLagerUtbetaling, fødselsnummer, arbeidsgiver, grunnlagsdata, hendelse, maksimumSykepenger, utbetalingstidslinje)
                }

                override fun avsluttUtenVedtak(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: IAktivitetslogg) {
                    generasjon.generasjonLukket(arbeidsgiver)
                    generasjon.tilstand(AvsluttetUtenVedtak, hendelse)
                }
            }
            data object UberegnetOmgjøring : Tilstand by (Uberegnet) {
                override fun utbetaling(
                    generasjon: Generasjon,
                    vedtaksperiodeSomLagerUtbetaling: UUID,
                    fødselsnummer: String,
                    arbeidsgiver: Arbeidsgiver,
                    grunnlagsdata: VilkårsgrunnlagElement,
                    hendelse: IAktivitetslogg,
                    maksimumSykepenger: Maksdatosituasjon,
                    utbetalingstidslinje: Utbetalingstidslinje
                ): Utbetalingstidslinje {
                    return generasjon.lagOmgjøring(vedtaksperiodeSomLagerUtbetaling, fødselsnummer, arbeidsgiver, grunnlagsdata, hendelse, maksimumSykepenger, utbetalingstidslinje)
                }

                override fun kanForkastes(generasjon: Generasjon, hendelse: IAktivitetslogg, arbeidsgiverUtbetalinger: List<Utbetaling>) =
                    generasjon.kanForkastingAvKortPeriodeTillates(hendelse, arbeidsgiverUtbetalinger)

                override fun vurderLukkeAutomatisk(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse) {
                    if (!kanLukkesUtenVedtak(arbeidsgiver, generasjon)) return
                    generasjon.avsluttUtenVedtak(arbeidsgiver, hendelse)
                }

                private fun kanLukkesUtenVedtak(arbeidsgiver: Arbeidsgiver, generasjon: Generasjon): Boolean {
                    val arbeidsgiverperiode = arbeidsgiver.arbeidsgiverperiode(generasjon.periode) ?: return true
                    val forventerInntekt = Arbeidsgiverperiode.forventerInntekt(arbeidsgiverperiode, generasjon.periode, generasjon.sykdomstidslinje(), NullObserver)
                    return !forventerInntekt
                }
            }
            data object UberegnetRevurdering : Tilstand by (Uberegnet) {
                override fun kanForkastes(generasjon: Generasjon, hendelse: IAktivitetslogg, arbeidsgiverUtbetalinger: List<Utbetaling>) = false
                override fun annuller(
                    generasjon: Generasjon,
                    arbeidsgiver: Arbeidsgiver,
                    hendelse: AnnullerUtbetaling,
                    annullering: Utbetaling,
                    grunnlagsdata: VilkårsgrunnlagElement
                ): Generasjon? {
                    generasjon.nyEndring(generasjon.gjeldende.kopierMedUtbetaling(annullering, grunnlagsdata))
                    generasjon.tilstand(AnnullertPeriode, hendelse)
                    return null
                }

                override fun utbetaling(
                    generasjon: Generasjon,
                    vedtaksperiodeSomLagerUtbetaling: UUID,
                    fødselsnummer: String,
                    arbeidsgiver: Arbeidsgiver,
                    grunnlagsdata: VilkårsgrunnlagElement,
                    hendelse: IAktivitetslogg,
                    maksimumSykepenger: Maksdatosituasjon,
                    utbetalingstidslinje: Utbetalingstidslinje
                ): Utbetalingstidslinje {
                    return generasjon.lagRevurdering(vedtaksperiodeSomLagerUtbetaling, fødselsnummer, arbeidsgiver, grunnlagsdata, hendelse, maksimumSykepenger, utbetalingstidslinje)
                }
            }
            data object Beregnet : Tilstand {
                override fun entering(generasjon: Generasjon, hendelse: IAktivitetslogg) {
                    checkNotNull(generasjon.gjeldende.utbetaling)
                    checkNotNull(generasjon.gjeldende.grunnlagsdata)
                }

                override fun kanForkastes(generasjon: Generasjon, hendelse: IAktivitetslogg, arbeidsgiverUtbetalinger: List<Utbetaling>) = true

                override fun forkastVedtaksperiode(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: Hendelse): Generasjon? {
                    generasjon.gjeldende.forkastUtbetaling(hendelse)
                    return super.forkastVedtaksperiode(generasjon, arbeidsgiver, hendelse)
                }

                override fun håndterEndring(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse): Generasjon? {
                    generasjon.gjeldende.forkastUtbetaling(hendelse)
                    generasjon.oppdaterMedEndring(arbeidsgiver, hendelse)
                    generasjon.tilstand(Uberegnet, hendelse)
                    return null
                }

                override fun oppdaterDokumentsporing(generasjon: Generasjon, dokument: Dokumentsporing) =
                    generasjon.kopierMedDokument(dokument)

                override fun utenUtbetaling(generasjon: Generasjon, hendelse: IAktivitetslogg) {
                    generasjon.utenUtbetaling(hendelse)
                    generasjon.tilstand(Uberegnet, hendelse)
                }

                override fun vedtakAvvist(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, utbetalingsavgjørelse: Utbetalingsavgjørelse) {
                    // perioden kommer til å bli kastet til infotrygd
                }

                override fun vedtakFattet(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, utbetalingsavgjørelse: Utbetalingsavgjørelse) {
                    generasjon.vedtakFattet = utbetalingsavgjørelse.avgjørelsestidspunkt
                    generasjon.generasjonLukket(arbeidsgiver)
                    generasjon.tilstand(if (generasjon.gjeldende.utbetaling?.erAvsluttet() == true) VedtakIverksatt else VedtakFattet, utbetalingsavgjørelse)
                }
            }
            data object BeregnetOmgjøring : Tilstand by (Beregnet) {
                override fun håndterEndring(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse): Generasjon? {
                    generasjon.gjeldende.forkastUtbetaling(hendelse)
                    generasjon.oppdaterMedEndring(arbeidsgiver, hendelse)
                    generasjon.tilstand(UberegnetOmgjøring, hendelse)
                    return null
                }
                override fun kanForkastes(generasjon: Generasjon, hendelse: IAktivitetslogg, arbeidsgiverUtbetalinger: List<Utbetaling>) =
                    generasjon.kanForkastingAvKortPeriodeTillates(hendelse, arbeidsgiverUtbetalinger)
                override fun utenUtbetaling(generasjon: Generasjon, hendelse: IAktivitetslogg) {
                    generasjon.utenUtbetaling(hendelse)
                    generasjon.tilstand(UberegnetOmgjøring, hendelse)
                }
            }
            data object BeregnetRevurdering : Tilstand by (Beregnet) {
                override fun forkastVedtaksperiode(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: Hendelse): Generasjon? {
                    generasjon.gjeldende.forkastUtbetaling(hendelse)
                    return super.forkastVedtaksperiode(generasjon, arbeidsgiver, hendelse)
                }
                override fun kanForkastes(generasjon: Generasjon, hendelse: IAktivitetslogg, arbeidsgiverUtbetalinger: List<Utbetaling>) = false

                override fun annuller(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: AnnullerUtbetaling, annullering: Utbetaling, grunnlagsdata: VilkårsgrunnlagElement): Generasjon? {
                    generasjon.gjeldende.utbetaling!!.forkast(hendelse)
                    generasjon.nyEndring(generasjon.gjeldende.kopierMedUtbetaling(annullering, grunnlagsdata))
                    generasjon.tilstand(AnnullertPeriode, hendelse)
                    return null
                }

                override fun utenUtbetaling(generasjon: Generasjon, hendelse: IAktivitetslogg) {
                    generasjon.utenUtbetaling(hendelse)
                    generasjon.tilstand(UberegnetRevurdering, hendelse)
                }
                override fun vedtakAvvist(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, utbetalingsavgjørelse: Utbetalingsavgjørelse) {
                    generasjon.generasjonLukket(arbeidsgiver)
                    generasjon.tilstand(RevurdertVedtakAvvist, utbetalingsavgjørelse)
                }
                override fun håndterEndring(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse): Generasjon? {
                    generasjon.gjeldende.forkastUtbetaling(hendelse)
                    generasjon.oppdaterMedEndring(arbeidsgiver, hendelse)
                    generasjon.tilstand(UberegnetRevurdering, hendelse)
                    return null
                }
            }
            data object RevurdertVedtakAvvist : Tilstand {
                override fun forkastVedtaksperiode(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: Hendelse): Generasjon {
                    return generasjon.nyGenerasjonTilInfotrygd(arbeidsgiver, hendelse)
                }

                override fun sikreNyGenerasjon(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: Hendelse): Generasjon {
                    return generasjon.sikreNyGenerasjon(arbeidsgiver, UberegnetRevurdering, hendelse)
                }

                override fun annuller(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: AnnullerUtbetaling, annullering: Utbetaling, grunnlagsdata: VilkårsgrunnlagElement): Generasjon {
                    return generasjon.nyAnnullertGenerasjon(arbeidsgiver, hendelse, annullering, grunnlagsdata)
                }
                override fun kanForkastes(generasjon: Generasjon, hendelse: IAktivitetslogg, arbeidsgiverUtbetalinger: List<Utbetaling>) = false
                override fun tillaterNyGenerasjon(generasjon: Generasjon, other: Generasjon): Boolean {
                    return true
                }
            }
            data object VedtakFattet : Tilstand {
                override fun entering(generasjon: Generasjon, hendelse: IAktivitetslogg) {
                    checkNotNull(generasjon.gjeldende.utbetaling)
                    checkNotNull(generasjon.gjeldende.grunnlagsdata)
                }
                override fun kanForkastes(generasjon: Generasjon, hendelse: IAktivitetslogg, arbeidsgiverUtbetalinger: List<Utbetaling>) = false
                override fun tillaterNyGenerasjon(generasjon: Generasjon, other: Generasjon): Boolean {
                    return true
                }

                override fun sikreNyGenerasjon(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: Hendelse): Generasjon {
                    return generasjon.sikreNyGenerasjon(arbeidsgiver, UberegnetRevurdering, hendelse)
                }

                override fun håndterUtbetalinghendelse(generasjon: Generasjon, hendelse: UtbetalingHendelse): Boolean {
                    val utbetaling = checkNotNull(generasjon.gjeldende.utbetaling) { "forventer utbetaling" }
                    if (!utbetaling.gjelderFor(hendelse)) return false
                    if (utbetaling.erAvsluttet()) avsluttMedVedtak(generasjon, hendelse)
                    return true
                }

                override fun utenUtbetaling(generasjon: Generasjon, hendelse: IAktivitetslogg) {}

                override fun forkastVedtaksperiode(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: Hendelse): Generasjon? {
                    error("usikker på hvordan vi kan forkaste vedtaksperioden som har fått utbetaling godkjent, men ikke avsluttet i $this")
                }

                override fun håndterEndring(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse) =
                    generasjon.nyGenerasjonMedEndring(arbeidsgiver, hendelse, UberegnetRevurdering)

                override fun avsluttMedVedtak(generasjon: Generasjon, hendelse: IAktivitetslogg) {
                    generasjon.tilstand(VedtakIverksatt, hendelse)
                }
            }

            data object AvsluttetUtenVedtak : Tilstand {
                override fun entering(generasjon: Generasjon, hendelse: IAktivitetslogg) {
                    generasjon.vedtakFattet = null // det fattes ikke vedtak i AUU
                    generasjon.avsluttet = LocalDateTime.now()
                    generasjon.avsluttetUtenVedtak(hendelse)
                }
                override fun forkastVedtaksperiode(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: Hendelse): Generasjon {
                    return generasjon.nyGenerasjonTilInfotrygd(arbeidsgiver, hendelse)
                }
                override fun kanForkastes(generasjon: Generasjon, hendelse: IAktivitetslogg, arbeidsgiverUtbetalinger: List<Utbetaling>) =
                    generasjon.kanForkastingAvKortPeriodeTillates(hendelse, arbeidsgiverUtbetalinger)
                override fun tillaterNyGenerasjon(generasjon: Generasjon, other: Generasjon): Boolean {
                    return true
                }

                override fun sikreNyGenerasjon(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: Hendelse): Generasjon {
                    return generasjon.sikreNyGenerasjon(arbeidsgiver, UberegnetOmgjøring, hendelse)
                }

                override fun håndterEndring(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse): Generasjon {
                    return generasjon.nyGenerasjonMedEndring(arbeidsgiver, hendelse, UberegnetOmgjøring)
                }
            }
            data object VedtakIverksatt : Tilstand {
                override fun entering(generasjon: Generasjon, hendelse: IAktivitetslogg) {
                    generasjon.avsluttet = LocalDateTime.now()
                    generasjon.vedtakIverksatt(hendelse)
                }
                override fun annuller(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: AnnullerUtbetaling, annullering: Utbetaling, grunnlagsdata: VilkårsgrunnlagElement): Generasjon {
                    return generasjon.nyAnnullertGenerasjon(arbeidsgiver, hendelse, annullering, grunnlagsdata)
                }
                override fun forkastVedtaksperiode(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: Hendelse): Generasjon {
                    return generasjon.nyGenerasjonTilInfotrygd(arbeidsgiver, hendelse)
                }
                override fun kanForkastes(generasjon: Generasjon, hendelse: IAktivitetslogg, arbeidsgiverUtbetalinger: List<Utbetaling>) = false
                override fun tillaterNyGenerasjon(generasjon: Generasjon, other: Generasjon): Boolean {
                    return true
                }

                override fun sikreNyGenerasjon(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: Hendelse): Generasjon {
                    return generasjon.sikreNyGenerasjon(arbeidsgiver, UberegnetRevurdering, hendelse)
                }

                override fun håndterEndring(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: SykdomshistorikkHendelse) =
                    generasjon.nyGenerasjonMedEndring(arbeidsgiver, hendelse, UberegnetRevurdering)
            }
            data object AnnullertPeriode : Tilstand {
                override fun kanForkastes(generasjon: Generasjon, hendelse: IAktivitetslogg, arbeidsgiverUtbetalinger: List<Utbetaling>) = true

                override fun forkastVedtaksperiode(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: Hendelse): Generasjon? {
                    generasjon.vedtakAnnullert(hendelse)
                    // todo: beholde AnnullertPeriode som siste tilstand for annullerte perioder
                    return super.forkastVedtaksperiode(generasjon, arbeidsgiver, hendelse)
                }

                override fun annuller(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: AnnullerUtbetaling, annullering: Utbetaling, grunnlagsdata: VilkårsgrunnlagElement): Generasjon? {
                    error("forventer ikke å annullere i $this")
                }
            }
            data object TilInfotrygd : Tilstand {
                override fun entering(generasjon: Generasjon, hendelse: IAktivitetslogg) {
                    generasjon.avsluttet = LocalDateTime.now()
                }
                override fun annuller(generasjon: Generasjon, arbeidsgiver: Arbeidsgiver, hendelse: AnnullerUtbetaling, annullering: Utbetaling, grunnlagsdata: VilkårsgrunnlagElement): Generasjon? {
                    error("forventer ikke å annullere i $this")
                }
                override fun kanForkastes(generasjon: Generasjon, hendelse: IAktivitetslogg, arbeidsgiverUtbetalinger: List<Utbetaling>): Boolean {
                    error("forventer ikke å forkaste en periode som allerde er i $this")
                }
            }
        }
    }
}