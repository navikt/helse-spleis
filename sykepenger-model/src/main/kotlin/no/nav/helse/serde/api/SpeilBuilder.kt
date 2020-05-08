package no.nav.helse.serde.api

import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.*
import no.nav.helse.serde.api.SykdomstidslinjedagDTO.KildeDTO
import no.nav.helse.serde.mapping.SpeilDagtype
import no.nav.helse.serde.mapping.SpeilKildetype
import no.nav.helse.serde.mapping.SpeilKildetype.*
import no.nav.helse.serde.reflection.ArbeidsgiverReflect
import no.nav.helse.serde.reflection.PersonReflect
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.serde.reflection.VedtaksperiodeReflect
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*


fun serializePersonForSpeil(person: Person, hendelser: List<HendelseDTO> = emptyList()): PersonDTO {
    val jsonBuilder = SpeilBuilder(hendelser)
    person.accept(jsonBuilder)
    return jsonBuilder.toJson()
}

internal class SpeilBuilder(private val hendelser: List<HendelseDTO>) : PersonVisitor {

    private val stack: Stack<JsonState> = Stack()
    private val rootState = Root()
    private val currentState: JsonState get() = stack.peek()

    init {
        stack.push(rootState)
    }

    internal fun toJson() = rootState.toJson()

    private fun pushState(state: JsonState) {
        currentState.leaving()
        stack.push(state)
        currentState.entering()
    }

    private fun popState() {
        currentState.leaving()
        stack.pop()
        currentState.entering()
    }

    override fun preVisitPerson(
        person: Person,
        aktørId: String,
        fødselsnummer: String
    ) = currentState.preVisitPerson(person, aktørId, fødselsnummer)

    override fun visitWarn(
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitetslogg.Aktivitet.Warn,
        melding: String,
        tidsstempel: String
    ) = currentState.visitWarn(kontekster, aktivitet, melding, tidsstempel)

    override fun postVisitPerson(
        person: Person,
        aktørId: String,
        fødselsnummer: String
    ) = currentState.postVisitPerson(person, aktørId, fødselsnummer)

    override fun preVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) = currentState.preVisitArbeidsgiver(arbeidsgiver, id, organisasjonsnummer)

    override fun postVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) = currentState.postVisitArbeidsgiver(arbeidsgiver, id, organisasjonsnummer)

    override fun preVisitArbeidsgivere() = currentState.preVisitArbeidsgivere()
    override fun postVisitArbeidsgivere() = currentState.postVisitArbeidsgivere()
    override fun preVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) =
        currentState.preVisitInntekthistorikk(inntekthistorikk)

    override fun postVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {
        currentState.postVisitInntekthistorikk(inntekthistorikk)
    }

    override fun visitInntekt(inntekt: Inntekthistorikk.Inntekt, id: UUID) {
        currentState.visitInntekt(inntekt, id)
    }

    override fun preVisitTidslinjer(tidslinjer: MutableList<Utbetalingstidslinje>) =
        currentState.preVisitTidslinjer(tidslinjer)

    override fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) =
        currentState.preVisitUtbetalingstidslinje(tidslinje)

    override fun visitArbeidsdag(dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag) =
        currentState.visitArbeidsdag(dag)

    override fun visitArbeidsgiverperiodeDag(dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag) =
        currentState.visitArbeidsgiverperiodeDag(dag)

    override fun visitNavDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag) =
        currentState.visitNavDag(dag)

    override fun visitNavHelgDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag) =
        currentState.visitNavHelgDag(dag)

    override fun visitFridag(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag) =
        currentState.visitFridag(dag)

    override fun visitAvvistDag(dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag) =
        currentState.visitAvvistDag(dag)

    override fun visitUkjentDag(dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag) =
        currentState.visitUkjentDag(dag)

    override fun visitForeldetDag(dag: Utbetalingstidslinje.Utbetalingsdag.ForeldetDag) =
        currentState.visitForeldetDag(dag)

    override fun postVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) =
        currentState.postVisitUtbetalingstidslinje(tidslinje)

    override fun preVisitPerioder() = currentState.preVisitPerioder()
    override fun preVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        gruppeId: UUID
    ) {
        currentState.preVisitVedtaksperiode(vedtaksperiode, id, gruppeId)
    }

    override fun postVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
        currentState.postVisitUtbetalinger(utbetalinger)
    }

    override fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) =
        currentState.preVisitSykdomshistorikk(sykdomshistorikk)

    override fun postVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
        currentState.postVisitSykdomshistorikk(sykdomshistorikk)
    }

    override fun preVisitSykdomshistorikkElement(
        element: Sykdomshistorikk.Element,
        id: UUID,
        tidsstempel: LocalDateTime
    ) {
        currentState.preVisitSykdomshistorikkElement(element, id, tidsstempel)
    }

    override fun preVisitHendelseSykdomstidslinje(tidslinje: Sykdomstidslinje) =
        currentState.preVisitHendelseSykdomstidslinje(tidslinje)

    override fun postVisitHendelseSykdomstidslinje(tidslinje: Sykdomstidslinje) =
        currentState.postVisitHendelseSykdomstidslinje(tidslinje)

    override fun preVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) =
        currentState.preVisitBeregnetSykdomstidslinje(tidslinje)

    override fun postVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) =
        currentState.postVisitBeregnetSykdomstidslinje(tidslinje)

    override fun preVisitSykdomstidslinje(tidslinje: Sykdomstidslinje) =
        currentState.preVisitSykdomstidslinje(tidslinje)

    override fun postVisitSykdomstidslinje(tidslinje: Sykdomstidslinje) =
        currentState.postVisitSykdomstidslinje(tidslinje)

    override fun postVisitSykdomshistorikkElement(
        element: Sykdomshistorikk.Element,
        id: UUID,
        tidsstempel: LocalDateTime
    ) =
        currentState.postVisitSykdomshistorikkElement(element, id, tidsstempel)

    override fun visitDataForSimulering(dataForSimuleringResultat: Simulering.SimuleringResultat?) {
        currentState.visitDataForSimulering(dataForSimuleringResultat)
    }

    override fun postVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        gruppeId: UUID
    ) =
        currentState.postVisitVedtaksperiode(vedtaksperiode, id, gruppeId)

    override fun visitArbeidsdag(dag: Arbeidsdag.Inntektsmelding) = currentState.visitArbeidsdag(dag)
    override fun visitArbeidsdag(dag: Arbeidsdag.Søknad) = currentState.visitArbeidsdag(dag)
    override fun visitEgenmeldingsdag(dag: Egenmeldingsdag.Inntektsmelding) = currentState.visitEgenmeldingsdag(dag)

    override fun visitEgenmeldingsdag(dag: Egenmeldingsdag.Søknad) = currentState.visitEgenmeldingsdag(dag)

    override fun visitFeriedag(dag: Feriedag.Inntektsmelding) = currentState.visitFeriedag(dag)
    override fun visitFeriedag(dag: Feriedag.Søknad) = currentState.visitFeriedag(dag)

    override fun visitFriskHelgedag(dag: FriskHelgedag.Søknad) = currentState.visitFriskHelgedag(dag)
    override fun visitFriskHelgedag(dag: FriskHelgedag.Inntektsmelding) = currentState.visitFriskHelgedag(dag)

    override fun visitImplisittDag(dag: ImplisittDag) = currentState.visitImplisittDag(dag)
    override fun visitPermisjonsdag(dag: Permisjonsdag.Søknad) = currentState.visitPermisjonsdag(dag)

    override fun visitPermisjonsdag(dag: Permisjonsdag.Aareg) = currentState.visitPermisjonsdag(dag)
    override fun visitStudiedag(dag: Studiedag) = currentState.visitStudiedag(dag)
    override fun visitSykHelgedag(dag: SykHelgedag.Søknad) = currentState.visitSykHelgedag(dag)
    override fun visitSykHelgedag(dag: SykHelgedag.Sykmelding) = currentState.visitSykHelgedag(dag)
    override fun visitSykedag(dag: Sykedag.Sykmelding) = currentState.visitSykedag(dag)
    override fun visitSykedag(dag: Sykedag.Søknad) = currentState.visitSykedag(dag)
    override fun visitUbestemt(dag: Ubestemtdag) = currentState.visitUbestemt(dag)
    override fun visitUtenlandsdag(dag: Utenlandsdag) = currentState.visitUtenlandsdag(dag)
    override fun visitForeldetSykedag(dag: ForeldetSykedag) =
        currentState.visitForeldetSykedag(dag)

    override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) = currentState.visitTilstand(tilstand)

    private interface JsonState : PersonVisitor {
        fun entering() {}
        fun leaving() {}
    }

    private inner class Root : JsonState {
        private val personMap = mutableMapOf<String, Any?>()

        override fun preVisitPerson(
            person: Person,
            aktørId: String,
            fødselsnummer: String
        ) {
            pushState(PersonState(person, personMap, fødselsnummer))
        }

        override fun toString() = personMap.toString()

        fun toJson() = personMap.mapTilPersonDto()
    }

    private inner class PersonState(
        person: Person,
        private val personMap: MutableMap<String, Any?>,
        private val fødselsnummer: String
    ) : JsonState {
        init {
            personMap.putAll(PersonReflect(person).toSpeilMap())
        }

        private val arbeidsgivere = mutableListOf<ArbeidsgiverDTO>()
        private val aktivitetslogg = mutableListOf<AktivitetDTO>()

        override fun preVisitArbeidsgivere() {
            personMap["arbeidsgivere"] = arbeidsgivere
        }

        override fun visitWarn(
            kontekster: List<SpesifikkKontekst>,
            aktivitet: Aktivitetslogg.Aktivitet.Warn,
            melding: String,
            tidsstempel: String
        ) {
            kontekster.find { it.kontekstType == "Vedtaksperiode" }
                ?.let { it.kontekstMap["vedtaksperiodeId"] }
                ?.let(UUID::fromString)
                ?.also { vedtaksperiodeId ->
                    aktivitetslogg.add(AktivitetDTO(vedtaksperiodeId, "W", melding, tidsstempel))
                }
        }

        override fun preVisitArbeidsgiver(
            arbeidsgiver: Arbeidsgiver,
            id: UUID,
            organisasjonsnummer: String
        ) {
            val arbeidsgiverMap = mutableMapOf<String, Any?>()
            pushState(ArbeidsgiverState(arbeidsgiver, arbeidsgiverMap, fødselsnummer, arbeidsgivere, aktivitetslogg))
        }

        override fun postVisitPerson(
            person: Person,
            aktørId: String,
            fødselsnummer: String
        ) {
            popState()
        }
    }

    private inner class ArbeidsgiverState(
        private val arbeidsgiver: Arbeidsgiver,
        private val arbeidsgiverMap: MutableMap<String, Any?>,
        private val fødselsnummer: String,
        private val arbeidsgivere: MutableList<ArbeidsgiverDTO>,
        private val aktivitetslogg: List<AktivitetDTO>
    ) : JsonState {
        init {
            arbeidsgiverMap.putAll(ArbeidsgiverReflect(arbeidsgiver).toSpeilMap())
        }

        private val vedtaksperioder = mutableListOf<VedtaksperiodeDTOBase>()
        lateinit var utbetalinger: List<Utbetaling>
        var vedtaksperiodeMap = mutableMapOf<String, Any?>()
        val fellesGrunnlagsdata = mutableMapOf<UUID, GrunnlagsdataDTO>()
        val fellesOpptjening = mutableMapOf<UUID, OpptjeningDTO>()
        var inntekter = mutableListOf<Inntekthistorikk.Inntekt>()


        override fun postVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
            this.utbetalinger = utbetalinger
        }

        override fun preVisitPerioder() {
            arbeidsgiverMap["vedtaksperioder"] = vedtaksperioder
        }

        override fun visitInntekt(inntekt: Inntekthistorikk.Inntekt, id: UUID) {
            inntekter.add(inntekt)
        }

        override fun preVisitVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            id: UUID,
            gruppeId: UUID
        ) {
            pushState(
                VedtaksperiodeState(
                    vedtaksperiode = vedtaksperiode,
                    arbeidsgiver = arbeidsgiver,
                    vedtaksperiodeMap = vedtaksperiodeMap,
                    fellesGrunnlagsdata = fellesGrunnlagsdata,
                    fellesOpptjening = fellesOpptjening,
                    vedtaksperioder = vedtaksperioder,
                    fødselsnummer = fødselsnummer,
                    inntekter = inntekter,
                    utbetalinger = utbetalinger,
                    aktivitetslogg = aktivitetslogg
                )
            )
        }

        override fun postVisitArbeidsgiver(
            arbeidsgiver: Arbeidsgiver,
            id: UUID,
            organisasjonsnummer: String
        ) {

            val arbeidsgiverDTO = arbeidsgiverMap.mapTilArbeidsgiverDto()

            val mappedeArbeidsgivere = arbeidsgiverDTO.copy(
                vedtaksperioder = arbeidsgiverDTO.vedtaksperioder
                    .map { vedtaksperiode ->
                        if (vedtaksperiode.fullstendig) {
                            (vedtaksperiode as VedtaksperiodeDTO).copy(
                                dataForVilkårsvurdering = fellesGrunnlagsdata[vedtaksperiode.gruppeId],
                                vilkår = vedtaksperiode.vilkår.copy(opptjening = fellesOpptjening[vedtaksperiode.gruppeId])
                            )
                        } else {
                            vedtaksperiode
                        }
                    }
            )

            arbeidsgivere.add(mappedeArbeidsgivere)
            popState()
        }
    }

    private inner class VedtaksperiodeState(
        vedtaksperiode: Vedtaksperiode,
        arbeidsgiver: Arbeidsgiver,
        private val vedtaksperiodeMap: MutableMap<String, Any?>,
        private val fellesGrunnlagsdata: MutableMap<UUID, GrunnlagsdataDTO>,
        private val fellesOpptjening: MutableMap<UUID, OpptjeningDTO>,
        private val vedtaksperioder: MutableList<VedtaksperiodeDTOBase>,
        private val fødselsnummer: String,
        private val inntekter: List<Inntekthistorikk.Inntekt>,
        private val utbetalinger: List<Utbetaling>,
        aktivitetslogg: List<AktivitetDTO>
    ) : JsonState {
        private var fullstendig = false
        private val vedtaksperiodehendelser = mutableListOf<HendelseDTO>()
        private val beregnetSykdomstidslinje = mutableListOf<SykdomstidslinjedagDTO>()
        private val totalbeløpakkumulator = mutableListOf<Int>()
        private val vedtaksperiode = vedtaksperiode
        private val dataForVilkårsvurdering = vedtaksperiode
            .get<Vilkårsgrunnlag.Grunnlagsdata?>("dataForVilkårsvurdering")
            ?.let { mapDataForVilkårsvurdering(it) }

        init {
            val vedtaksperiodeReflect = VedtaksperiodeReflect(vedtaksperiode)
            vedtaksperiodeMap.putAll(vedtaksperiodeReflect.toSpeilMap(arbeidsgiver))
            vedtaksperiodeMap["sykdomstidslinje"] = beregnetSykdomstidslinje
            vedtaksperiodeMap["hendelser"] = vedtaksperiodehendelser
            vedtaksperiodeMap["dataForVilkårsvurdering"] = dataForVilkårsvurdering
            vedtaksperiodeMap["aktivitetslogg"] =
                aktivitetslogg.filter { it.vedtaksperiodeId == vedtaksperiodeReflect.id }
        }

        override fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
            pushState(SykdomshistorikkState(vedtaksperiodehendelser, beregnetSykdomstidslinje))
        }

        override fun preVisitSykdomshistorikkElement(
            element: Sykdomshistorikk.Element,
            id: UUID,
            tidsstempel: LocalDateTime
        ) {
            hendelser.find { it.id == id.toString() }?.also { vedtaksperiodehendelser.add(it) }
        }

        private var førsteSykepengedag: LocalDate? = null
        private var sisteSykepengedag: LocalDate? = null

        override fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {
            val utbetalingstidslinje = mutableListOf<UtbetalingstidslinjedagDTO>()
            vedtaksperiodeMap["utbetalingstidslinje"] = utbetalingstidslinje
            førsteSykepengedag = tidslinje.førsteSykepengedag()
            sisteSykepengedag = tidslinje.sisteSykepengedag()
            pushState(UtbetalingstidslinjeState(utbetalingstidslinje, totalbeløpakkumulator))
        }

        override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) {
            vedtaksperiodeMap["tilstand"] =
                mapTilstander(tilstand = tilstand.type, utbetalt = (totalbeløpakkumulator.sum() > 0))
            if (tilstand.type in listOf(
                    TilstandType.AVSLUTTET,
                    TilstandType.AVVENTER_GODKJENNING,
                    TilstandType.UTBETALING_FEILET,
                    TilstandType.TIL_UTBETALING
                )
            ) {
                fullstendig = true
            } else {
                vedtaksperioder.add(vedtaksperiodeMap.mapTilUfullstendigVedtaksperiodeDto())
            }
        }

        override fun visitDataForSimulering(dataForSimuleringResultat: Simulering.SimuleringResultat?) {
            dataForSimuleringResultat?.let {
                vedtaksperiodeMap["dataForSimulering"] = it.mapTilSimuleringsdataDto()
            }
        }

        override fun postVisitVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            id: UUID,
            gruppeId: UUID
        ) {
            vedtaksperiodeMap["totalbeløpArbeidstaker"] = totalbeløpakkumulator.sum()
            vedtaksperiodeMap["utbetalinger"] = byggUtbetalingerForPeriode()
            if (fullstendig) {
                vedtaksperioder.add(
                    vedtaksperiodeMap.mapTilVedtaksperiodeDto(
                        fødselsnummer,
                        inntekter,
                        førsteSykepengedag,
                        sisteSykepengedag
                    )
                )
            }
            samleFellesdata(gruppeId)
            popState()
        }

        private fun samleFellesdata(gruppeId: UUID) {
            val førsteFraværsdag = vedtaksperiodeMap["førsteFraværsdag"] as? LocalDate
            val opptjening = førsteFraværsdag?.let {
                dataForVilkårsvurdering?.let {
                    mapOpptjening(førsteFraværsdag, it)
                }
            }
            dataForVilkårsvurdering?.let { fellesGrunnlagsdata.putIfAbsent(gruppeId, it) }
            opptjening?.let { fellesOpptjening.putIfAbsent(gruppeId, it) }
        }

        private fun byggUtbetalingerForPeriode(): UtbetalingerDTO =
            UtbetalingerDTO(
                utbetalinger.byggUtbetaling(
                    vedtaksperiode.arbeidsgiverFagsystemId(),
                    Utbetaling::arbeidsgiverOppdrag
                ),
                utbetalinger.byggUtbetaling(
                    vedtaksperiode.personFagsystemId(),
                    Utbetaling::personOppdrag
                )
            )

        private fun List<Utbetaling>.byggUtbetaling(fagsystemId: String?, oppdragStrategy: (Utbetaling) -> Oppdrag) =
            fagsystemId?.let {
                val linjer = this.filter { utbetaling ->
                    oppdragStrategy(utbetaling).fagsystemId() == fagsystemId
                }.flatMap { utbetaling ->
                    oppdragStrategy(utbetaling).map { linje ->
                        UtbetalingerDTO.UtbetalingslinjeDTO(
                            fom = linje.fom,
                            tom = linje.tom,
                            dagsats = linje.dagsats,
                            grad = linje.grad
                        )
                    }
                }
                UtbetalingerDTO.UtbetalingDTO(linjer, it)
            }
    }

    private inner class UtbetalingstidslinjeState(
        private val utbetalingstidslinjeMap: MutableList<UtbetalingstidslinjedagDTO>,
        private var utbetalinger: MutableList<Int>
    ) : JsonState {

        override fun visitArbeidsdag(dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag) {
            utbetalingstidslinjeMap.add(
                UtbetalingsdagDTO(
                    type = TypeDataDTO.Arbeidsdag,
                    inntekt = dag.dagsats,
                    dato = dag.dato
                )
            )
        }

        override fun visitArbeidsgiverperiodeDag(dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag) {
            utbetalingstidslinjeMap.add(
                UtbetalingsdagDTO(
                    type = TypeDataDTO.ArbeidsgiverperiodeDag,
                    inntekt = dag.dagsats,
                    dato = dag.dato
                )
            )
        }

        override fun visitNavDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag) {
            utbetalingstidslinjeMap.add(
                NavDagDTO(
                    type = TypeDataDTO.NavDag,
                    inntekt = dag.dagsats,
                    dato = dag.dato,
                    utbetaling = dag.utbetaling,
                    grad = dag.grad
                )
            )
            utbetalinger.add(dag.utbetaling)
        }

        override fun visitNavHelgDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag) {
            utbetalingstidslinjeMap.add(
                UtbetalingsdagMedGradDTO(
                    type = TypeDataDTO.NavHelgDag,
                    inntekt = dag.dagsats,
                    dato = dag.dato,
                    grad = dag.grad
                )
            )
        }

        override fun visitFridag(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag) {
            utbetalingstidslinjeMap.add(
                UtbetalingsdagDTO(
                    type = if (dag.dato.erHelg()) TypeDataDTO.Helgedag else TypeDataDTO.Feriedag,
                    inntekt = dag.dagsats,
                    dato = dag.dato
                )
            )
        }

        override fun visitUkjentDag(dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag) {
            utbetalingstidslinjeMap.add(
                UtbetalingsdagDTO(
                    type = TypeDataDTO.UkjentDag,
                    inntekt = dag.dagsats,
                    dato = dag.dato
                )
            )
        }

        override fun visitAvvistDag(dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag) {
            utbetalingstidslinjeMap.add(
                AvvistDagDTO(
                    type = TypeDataDTO.AvvistDag,
                    inntekt = dag.dagsats,
                    dato = dag.dato,
                    begrunnelse = mapBegrunnelse(dag.begrunnelse),
                    grad = dag.grad
                )
            )
        }

        override fun visitForeldetDag(dag: Utbetalingstidslinje.Utbetalingsdag.ForeldetDag) {
            utbetalingstidslinjeMap.add(
                UtbetalingsdagDTO(
                    type = TypeDataDTO.ForeldetDag,
                    inntekt = dag.dagsats,
                    dato = dag.dato
                )
            )
        }

        override fun postVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {
            popState()
        }
    }

    private inner class SykdomshistorikkState(
        private val vedtaksperiodehendelser: MutableList<HendelseDTO>,
        private val sykdomstidslinjeListe: MutableList<SykdomstidslinjedagDTO>
    ) : JsonState {

        override fun preVisitSykdomshistorikkElement(
            element: Sykdomshistorikk.Element,
            id: UUID,
            tidsstempel: LocalDateTime
        ) {
            hendelser.find { it.id == id.toString() }?.also { vedtaksperiodehendelser.add(it) }
        }

        override fun preVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) {
            pushState(SykdomstidslinjeState(sykdomstidslinjeListe))
        }

        override fun postVisitSykdomshistorikkElement(
            element: Sykdomshistorikk.Element,
            id: UUID,
            tidsstempel: LocalDateTime
        ) {
            popState()
        }

        override fun postVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
            popState()
        }

        override fun preVisitHendelseSykdomstidslinje(tidslinje: Sykdomstidslinje) {}

        override fun postVisitHendelseSykdomstidslinje(tidslinje: Sykdomstidslinje) {}

        override fun postVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) {}
    }

    private inner class SykdomstidslinjeState(private val sykdomstidslinjeListe: MutableList<SykdomstidslinjedagDTO>) :
        JsonState {

        override fun visitArbeidsdag(dag: Arbeidsdag.Inntektsmelding) =
            leggTilDag(SpeilDagtype.ARBEIDSDAG, dag, Inntektsmelding)

        override fun visitArbeidsdag(dag: Arbeidsdag.Søknad) =
            leggTilDag(SpeilDagtype.ARBEIDSDAG, dag, Søknad)

        override fun visitEgenmeldingsdag(dag: Egenmeldingsdag.Inntektsmelding) =
            leggTilDag(SpeilDagtype.ARBEIDSGIVERDAG, dag, Inntektsmelding)

        override fun visitEgenmeldingsdag(dag: Egenmeldingsdag.Søknad) =
            leggTilDag(SpeilDagtype.ARBEIDSGIVERDAG, dag, Søknad)

        override fun visitFeriedag(dag: Feriedag.Inntektsmelding) =
            leggTilDag(SpeilDagtype.FERIEDAG, dag, Inntektsmelding)

        override fun visitFeriedag(dag: Feriedag.Søknad) =
            leggTilDag(SpeilDagtype.FERIEDAG, dag, Søknad)

        override fun visitFriskHelgedag(dag: FriskHelgedag.Søknad) =
            leggTilDag(SpeilDagtype.FRISK_HELGEDAG, dag, Søknad)

        override fun visitFriskHelgedag(dag: FriskHelgedag.Inntektsmelding) =
            leggTilDag(SpeilDagtype.FRISK_HELGEDAG, dag, Inntektsmelding)

        override fun visitImplisittDag(dag: ImplisittDag) =
            leggTilDag(SpeilDagtype.IMPLISITT_DAG, dag)

        override fun visitPermisjonsdag(dag: Permisjonsdag.Søknad) =
            leggTilDag(SpeilDagtype.PERMISJONSDAG, dag, Søknad)

        override fun visitPermisjonsdag(dag: Permisjonsdag.Aareg) =
            leggTilDag(SpeilDagtype.PERMISJONSDAG, dag)

        override fun visitStudiedag(dag: Studiedag) =
            leggTilDag(SpeilDagtype.STUDIEDAG, dag, Søknad)

        override fun visitSykHelgedag(dag: SykHelgedag.Sykmelding) =
            leggTilSykedag(SpeilDagtype.SYK_HELGEDAG, dag, Sykmelding)

        override fun visitSykHelgedag(dag: SykHelgedag.Søknad) =
            leggTilSykedag(SpeilDagtype.SYK_HELGEDAG, dag, Søknad)

        override fun visitSykedag(dag: Sykedag.Sykmelding) =
            leggTilSykedag(SpeilDagtype.SYKEDAG, dag, Sykmelding)

        override fun visitSykedag(dag: Sykedag.Søknad) =
            leggTilSykedag(SpeilDagtype.SYKEDAG, dag, Søknad)

        override fun visitUbestemt(dag: Ubestemtdag) =
            leggTilDag(SpeilDagtype.UBESTEMTDAG, dag)

        override fun visitUtenlandsdag(dag: Utenlandsdag) =
            leggTilDag(SpeilDagtype.UTENLANDSDAG, dag, Søknad)

        override fun visitForeldetSykedag(dag: ForeldetSykedag) =
            leggTilSykedag(SpeilDagtype.FORELDET_SYKEDAG, dag, Søknad)

        private fun leggTilDag(dagtype: SpeilDagtype, dag: Dag, kilde: SpeilKildetype? = null) {
            sykdomstidslinjeListe.add(
                SykdomstidslinjedagDTO(dag.dagen, dagtype, kilde?.let { KildeDTO(kilde) } )
            )
        }

        private fun leggTilSykedag(dagtype: SpeilDagtype, dag: GradertDag, kilde: SpeilKildetype? = null) {
            sykdomstidslinjeListe.add(
                SykdomstidslinjedagDTO(dag.dagen, dagtype, kilde?.let { KildeDTO(kilde) }, dag.grad )
            )
        }

        override fun postVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) {
            popState()
        }
    }
}
