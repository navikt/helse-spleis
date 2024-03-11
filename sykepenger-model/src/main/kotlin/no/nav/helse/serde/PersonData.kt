package no.nav.helse.serde

import com.fasterxml.jackson.annotation.JsonUnwrapped
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.Alder
import no.nav.helse.erHelg
import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.SimuleringResultat
import no.nav.helse.hendelser.Subsumsjon
import no.nav.helse.hendelser.til
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.ForkastetVedtaksperiode
import no.nav.helse.person.Generasjoner
import no.nav.helse.person.Opptjening
import no.nav.helse.person.Person
import no.nav.helse.person.Sykmeldingsperioder
import no.nav.helse.person.TilstandType
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.Yrkesaktivitet.Companion.tilYrkesaktivitet
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.person.infotrygdhistorikk.PersonUtbetalingsperiode
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag
import no.nav.helse.person.inntekt.IkkeRapportert
import no.nav.helse.person.inntekt.Infotrygd
import no.nav.helse.person.inntekt.Inntektshistorikk
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.person.inntekt.Refusjonshistorikk
import no.nav.helse.person.inntekt.Refusjonsopplysning
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger.Companion.gjennopprett
import no.nav.helse.person.inntekt.Saksbehandler
import no.nav.helse.person.inntekt.Sammenligningsgrunnlag
import no.nav.helse.person.inntekt.SkattSykepengegrunnlag
import no.nav.helse.person.inntekt.Skatteopplysning
import no.nav.helse.person.inntekt.SkjønnsmessigFastsatt
import no.nav.helse.person.inntekt.Sykepengegrunnlag
import no.nav.helse.serde.PersonData.ArbeidsgiverData.RefusjonData.Companion.parseRefusjon
import no.nav.helse.serde.PersonData.ArbeidsgiverData.RefusjonData.EndringIRefusjonData.Companion.parseEndringerIRefusjon
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.Companion.tilModellobjekt
import no.nav.helse.serde.PersonData.InfotrygdhistorikkElementData.Companion.tilModellObjekt
import no.nav.helse.serde.PersonData.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningData.Companion.parseArbeidsgiverInntektsopplysninger
import no.nav.helse.serde.PersonData.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagData.Companion.parseArbeidsgiverInntektsopplysninger
import no.nav.helse.serde.PersonData.VilkårsgrunnlagElementData.OpptjeningData.ArbeidsgiverOpptjeningsgrunnlagData.Companion.tilArbeidsgiverOpptjeningsgrunnlag
import no.nav.helse.serde.PersonData.VilkårsgrunnlagInnslagData.Companion.tilModellObjekt
import no.nav.helse.serde.mapping.JsonMedlemskapstatus
import no.nav.helse.somPersonidentifikator
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.SykdomshistorikkHendelse.Hendelseskilde
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.Feriepengeutbetaling
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Satstype
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetalingslinje
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.InfotrygdArbeidsgiver
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.InfotrygdPerson
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.SpleisArbeidsgiver
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.SpleisPerson
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi

internal data class PersonData(
    val aktørId: String,
    val fødselsnummer: String,
    val fødselsdato: LocalDate,
    val arbeidsgivere: List<ArbeidsgiverData>,
    val opprettet: LocalDateTime,
    val infotrygdhistorikk: List<InfotrygdhistorikkElementData>,
    val vilkårsgrunnlagHistorikk: List<VilkårsgrunnlagInnslagData>,
    val dødsdato: LocalDate?
) {
    private val arbeidsgivereliste = mutableListOf<Arbeidsgiver>()
    private val fnr by lazy { fødselsnummer.somPersonidentifikator() }
    private val alder by lazy { Alder(fødselsdato, dødsdato) }
    private val vilkårsgrunnlaghistorikkBuilder = VilkårsgrunnlaghistorikkBuilder(vilkårsgrunnlagHistorikk)

    private fun person(jurist: MaskinellJurist, tidligereBehandlinger: List<Person> = emptyList()) = Person.ferdigPerson(
        aktørId = aktørId,
        personidentifikator = fnr,
        alder = alder,
        arbeidsgivere = arbeidsgivereliste,
        aktivitetslogg = Aktivitetslogg(),
        opprettet = opprettet,
        infotrygdhistorikk = infotrygdhistorikk.tilModellObjekt(),
        vilkårsgrunnlaghistorikk = vilkårsgrunnlaghistorikkBuilder.build(alder),
        tidligereBehandlinger = tidligereBehandlinger,
        jurist = jurist
    )

    internal fun createPerson(jurist: MaskinellJurist, tidligereBehandlinger: List<Person> = emptyList()): Person {
        val personJurist = jurist.medFødselsnummer(fødselsnummer.somPersonidentifikator().toString())
        val person = person(personJurist, tidligereBehandlinger)
        arbeidsgivereliste.addAll(this.arbeidsgivere.map {
            it.konverterTilArbeidsgiver(
                person,
                this.aktørId,
                this.fødselsnummer,
                this.alder,
                vilkårsgrunnlaghistorikkBuilder::hentVilkårsgrunnlagelement,
                personJurist
            )
        })
        return person
    }

    data class InfotrygdhistorikkElementData(
        val id: UUID,
        val tidsstempel: LocalDateTime,
        val hendelseId: UUID?,
        val ferieperioder: List<FerieperiodeData>,
        val arbeidsgiverutbetalingsperioder: List<ArbeidsgiverutbetalingsperiodeData>,
        val personutbetalingsperioder: List<PersonutbetalingsperiodeData>,
        val inntekter: List<InntektsopplysningData>,
        val arbeidskategorikoder: Map<String, LocalDate>,
        val oppdatert: LocalDateTime
    ) {
        internal companion object {
            fun List<InfotrygdhistorikkElementData>.tilModellObjekt() =
                Infotrygdhistorikk.ferdigInfotrygdhistorikk(map { it.parseInfotrygdhistorikkElement() })
        }

        internal fun parseInfotrygdhistorikkElement() = InfotrygdhistorikkElement.ferdigElement(
            id = id,
            tidsstempel = tidsstempel,
            hendelseId = hendelseId,
            infotrygdperioder = arbeidsgiverutbetalingsperioder.map { it.parsePeriode() }
                    + personutbetalingsperioder.map { it.parsePeriode() }
                    + ferieperioder.map { it.parsePeriode() },
            inntekter = inntekter.map { it.parseInntektsopplysning() },
            arbeidskategorikoder = arbeidskategorikoder,
            oppdatert = oppdatert
        )

        data class FerieperiodeData(
            val fom: LocalDate,
            val tom: LocalDate
        ) {
            internal fun parsePeriode() = Friperiode(fom, tom)
        }

        data class PersonutbetalingsperiodeData(
            val orgnr: String,
            val fom: LocalDate,
            val tom: LocalDate,
            val grad: Double,
            val inntekt: Int
        ) {
            internal fun parsePeriode() = PersonUtbetalingsperiode(
                orgnr = orgnr,
                fom = fom,
                tom = tom,
                grad = grad.prosent,
                inntekt = inntekt.daglig
            )
        }

        data class ArbeidsgiverutbetalingsperiodeData(
            val orgnr: String,
            val fom: LocalDate,
            val tom: LocalDate,
            val grad: Double,
            val inntekt: Int
        ) {
            internal fun parsePeriode() = ArbeidsgiverUtbetalingsperiode(
                orgnr = orgnr,
                fom = fom,
                tom = tom,
                grad = grad.prosent,
                inntekt = inntekt.daglig
            )
        }

        data class InntektsopplysningData(
            val orgnr: String,
            val sykepengerFom: LocalDate,
            val inntekt: Double,
            val refusjonTilArbeidsgiver: Boolean,
            val refusjonTom: LocalDate?,
            val lagret: LocalDateTime?
        ) {
            internal fun parseInntektsopplysning() = Inntektsopplysning.ferdigInntektsopplysning(
                orgnummer = orgnr,
                sykepengerFom = sykepengerFom,
                inntekt = inntekt.månedlig,
                refusjonTilArbeidsgiver = refusjonTilArbeidsgiver,
                refusjonTom = refusjonTom,
                lagret = lagret
            )
        }
    }

    internal class VilkårsgrunnlaghistorikkBuilder(private val historikk: List<VilkårsgrunnlagInnslagData>) {
        private val inntekter = mutableMapOf<UUID, no.nav.helse.person.inntekt.Inntektsopplysning>()
        private val elementer = mutableMapOf<UUID, VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement>()

        internal fun registrerVilkårsgrunnlagElement(id: UUID, element: () -> VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement) =
            elementer.getOrPut(id, element)
        internal fun registrerInntekt(id: UUID, inntektsopplysning: () -> no.nav.helse.person.inntekt.Inntektsopplysning) =
            inntekter.getOrPut(id, inntektsopplysning)

        fun build(alder: Alder) =
            historikk.tilModellObjekt(this, alder)

        internal fun hentVilkårsgrunnlagelement(id: UUID) = elementer.getValue(id)
        internal fun hentInntekt(id: UUID) = inntekter.getValue(id)
    }

    data class VilkårsgrunnlagInnslagData(
        val id: UUID,
        val opprettet: LocalDateTime,
        val vilkårsgrunnlag: List<VilkårsgrunnlagElementData>
    ) {
        private fun tilModellobjekt(builder: VilkårsgrunnlaghistorikkBuilder, alder: Alder) = id to VilkårsgrunnlagHistorikk.Innslag.gjenopprett(
            id = id,
            opprettet = opprettet,
            elementer = vilkårsgrunnlag.associate { it.parseDataForVilkårsvurdering(builder, alder) }
        )

        internal companion object {
            private fun List<VilkårsgrunnlagInnslagData>.parseVilkårsgrunnlag(builder: VilkårsgrunnlaghistorikkBuilder, alder: Alder): List<VilkårsgrunnlagHistorikk.Innslag> {
                // parser vilkårsgrunnlag fra eldste innslag (bakerst i listen) til nyeste (først i listen) fordi nyere inntekter kan peke på eldre inntekter (overstyringer)
                val mellombelsResultat = this
                    .asReversed()
                    .map { innslagData -> innslagData.tilModellobjekt(builder, alder) }
                    // snu rekkefølgen tilbake igjen
                    .reversed()
                this.bekreftRekkefølgeErIvaretatt(mellombelsResultat.map { it.first })
                return mellombelsResultat.map { it.second }
            }

            private fun List<VilkårsgrunnlagInnslagData>.bekreftRekkefølgeErIvaretatt(rekkefølge: List<UUID>) {
                check(this.size == rekkefølge.size) { "antall innslag har blitt endret" }
                check(this.zip(rekkefølge) { a, b -> a.id == b }.all { it }) { "rekkefølgen har endret seg, minst ett innslag ligger feil!" }
            }

            internal fun List<VilkårsgrunnlagInnslagData>.tilModellObjekt(builder: VilkårsgrunnlaghistorikkBuilder, alder: Alder) = VilkårsgrunnlagHistorikk.ferdigVilkårsgrunnlagHistorikk(parseVilkårsgrunnlag(builder, alder))
        }
    }

    data class VilkårsgrunnlagElementData(
        val skjæringstidspunkt: LocalDate,
        val type: GrunnlagsdataType,
        val sykepengegrunnlag: SykepengegrunnlagData,
        val opptjening: OpptjeningData?,
        val medlemskapstatus: JsonMedlemskapstatus?,
        val vurdertOk: Boolean?,
        val meldingsreferanseId: UUID?,
        val vilkårsgrunnlagId: UUID
    ) {
        internal fun parseDataForVilkårsvurdering(
            builder: VilkårsgrunnlaghistorikkBuilder,
            alder: Alder,
        ) = skjæringstidspunkt to builder.registrerVilkårsgrunnlagElement(vilkårsgrunnlagId) {
            when (type) {
                GrunnlagsdataType.Vilkårsprøving -> VilkårsgrunnlagHistorikk.Grunnlagsdata(
                    skjæringstidspunkt = skjæringstidspunkt,
                    sykepengegrunnlag = sykepengegrunnlag.parseSykepengegrunnlag(builder, alder, skjæringstidspunkt),
                    opptjening = opptjening!!.tilOpptjening(skjæringstidspunkt),
                    medlemskapstatus = when (medlemskapstatus!!) {
                        JsonMedlemskapstatus.JA -> Medlemskapsvurdering.Medlemskapstatus.Ja
                        JsonMedlemskapstatus.NEI -> Medlemskapsvurdering.Medlemskapstatus.Nei
                        JsonMedlemskapstatus.UAVKLART_MED_BRUKERSPØRSMÅL -> Medlemskapsvurdering.Medlemskapstatus.UavklartMedBrukerspørsmål
                        JsonMedlemskapstatus.VET_IKKE -> Medlemskapsvurdering.Medlemskapstatus.VetIkke
                    },
                    vurdertOk = vurdertOk!!,
                    meldingsreferanseId = meldingsreferanseId,
                    vilkårsgrunnlagId = vilkårsgrunnlagId
                )
                GrunnlagsdataType.Infotrygd -> VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag(
                    skjæringstidspunkt = skjæringstidspunkt,
                    sykepengegrunnlag = sykepengegrunnlag.parseSykepengegrunnlagInfotrygd(builder, alder, skjæringstidspunkt),
                    vilkårsgrunnlagId = vilkårsgrunnlagId
                )
            }
        }

        enum class GrunnlagsdataType {
            Infotrygd,
            Vilkårsprøving
        }

        data class SykepengegrunnlagData(
            val grunnbeløp: Double?,
            val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysningData>,
            val sammenligningsgrunnlag: SammenligningsgrunnlagData?,
            val deaktiverteArbeidsforhold: List<ArbeidsgiverInntektsopplysningData>,
            val vurdertInfotrygd: Boolean
        ) {
            internal fun parseSykepengegrunnlag(
                builder: VilkårsgrunnlaghistorikkBuilder,
                alder: Alder,
                skjæringstidspunkt: LocalDate
            ) = Sykepengegrunnlag.ferdigSykepengegrunnlag(
                alder = alder,
                skjæringstidspunkt = skjæringstidspunkt,
                arbeidsgiverInntektsopplysninger = arbeidsgiverInntektsopplysninger.parseArbeidsgiverInntektsopplysninger(builder),
                sammenligningsgrunnlag = sammenligningsgrunnlag!!.parseSammenligningsgrunnlag(),
                deaktiverteArbeidsforhold = deaktiverteArbeidsforhold.parseArbeidsgiverInntektsopplysninger(builder),
                vurdertInfotrygd = vurdertInfotrygd,
                `6G` = grunnbeløp?.årlig
            )

            internal fun parseSykepengegrunnlagInfotrygd(
                builder: VilkårsgrunnlaghistorikkBuilder,
                alder: Alder,
                skjæringstidspunkt: LocalDate
            ) = Sykepengegrunnlag.ferdigSykepengegrunnlag(
                alder = alder,
                skjæringstidspunkt = skjæringstidspunkt,
                arbeidsgiverInntektsopplysninger = arbeidsgiverInntektsopplysninger.parseArbeidsgiverInntektsopplysninger(builder),
                sammenligningsgrunnlag = Sammenligningsgrunnlag(emptyList()),
                deaktiverteArbeidsforhold = deaktiverteArbeidsforhold.parseArbeidsgiverInntektsopplysninger(builder),
                vurdertInfotrygd = vurdertInfotrygd,
                `6G` = grunnbeløp?.årlig
            )
        }

        data class SammenligningsgrunnlagData(
            val sammenligningsgrunnlag: Double,
            val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagData>,
        ) {

            internal fun parseSammenligningsgrunnlag(): Sammenligningsgrunnlag = Sammenligningsgrunnlag(
                sammenligningsgrunnlag.årlig,
                arbeidsgiverInntektsopplysninger.parseArbeidsgiverInntektsopplysninger()
            )
        }

        data class ArbeidsgiverInntektsopplysningData(
            val orgnummer: String,
            val fom: LocalDate,
            val tom: LocalDate,
            val inntektsopplysning: InntektsopplysningData,
            val refusjonsopplysninger: List<ArbeidsgiverData.RefusjonsopplysningData>
        ) {
            companion object {
                private fun List<ArbeidsgiverData.RefusjonsopplysningData>.tilModellobjekt() =
                    map { it.tilModellobjekt() }

                internal fun List<ArbeidsgiverInntektsopplysningData>.parseArbeidsgiverInntektsopplysninger(builder: VilkårsgrunnlaghistorikkBuilder) =
                    map {
                        ArbeidsgiverInntektsopplysning(it.orgnummer, it.fom til it.tom, it.inntektsopplysning.tilModellobjekt(builder), it.refusjonsopplysninger.tilModellobjekt().gjennopprett())
                    }
            }

            data class SkatteopplysningData(
                val hendelseId: UUID,
                val beløp: Double,
                val måned: YearMonth,
                val type: String,
                val fordel: String,
                val beskrivelse: String,
                val tidsstempel: LocalDateTime
            ) {
                internal fun tilModellobjekt() = Skatteopplysning(hendelseId, beløp.månedlig, måned, enumValueOf(type), fordel, beskrivelse, tidsstempel)
            }

            data class InntektsopplysningData(
                val id: UUID,
                val dato: LocalDate,
                val hendelseId: UUID,
                val beløp: Double?,
                val kilde: String,
                val forklaring: String?,
                val subsumsjon: SubsumsjonData?,
                val tidsstempel: LocalDateTime,
                val overstyrtInntektId: UUID?,
                val skatteopplysninger: List<SkatteopplysningData>?
            ) {
                data class SubsumsjonData(
                    val paragraf: String,
                    val ledd: Int?,
                    val bokstav: String?
                ) {
                    internal fun tilModellobjekt() = Subsumsjon(paragraf, ledd, bokstav)
                }
                internal fun tilModellobjekt(builder: VilkårsgrunnlaghistorikkBuilder): no.nav.helse.person.inntekt.Inntektsopplysning {
                    val opplysning = builder.registrerInntekt(id) {
                        when (kilde.let(Inntektsopplysningskilde::valueOf)) {
                            Inntektsopplysningskilde.INFOTRYGD -> somInfotrygd()
                            Inntektsopplysningskilde.INNTEKTSMELDING -> somInntektsmelding()
                            Inntektsopplysningskilde.IKKE_RAPPORTERT -> somIkkeRapportert()
                            Inntektsopplysningskilde.SAKSBEHANDLER -> somSaksbehandler(builder)
                            Inntektsopplysningskilde.SKJØNNSMESSIG_FASTSATT -> somSkjønnsmessigFastsatt(builder)
                            Inntektsopplysningskilde.SKATT_SYKEPENGEGRUNNLAG -> somSkattSykepengegrunnlag()
                            else -> error("Fant ${kilde}. Det er ugyldig for sykepengegrunnlag")
                        }
                    }
                    return opplysning
                }

                private fun somSaksbehandler(builder: VilkårsgrunnlaghistorikkBuilder) =
                    Saksbehandler(
                        id = id,
                        dato = dato,
                        hendelseId = hendelseId,
                        beløp = requireNotNull(beløp).månedlig,
                        forklaring = forklaring,
                        subsumsjon = subsumsjon?.tilModellobjekt(),
                        overstyrtInntekt = builder.hentInntekt(requireNotNull(overstyrtInntektId)),
                        tidsstempel = tidsstempel
                    )
                private fun somSkjønnsmessigFastsatt(builder: VilkårsgrunnlaghistorikkBuilder) =
                    SkjønnsmessigFastsatt(
                        id = id,
                        dato = dato,
                        hendelseId = hendelseId,
                        beløp = requireNotNull(beløp).månedlig,
                        overstyrtInntekt = builder.hentInntekt(requireNotNull(overstyrtInntektId)),
                        tidsstempel = tidsstempel
                    )
                private fun somIkkeRapportert() = IkkeRapportert(
                    id = id,
                    hendelseId = hendelseId,
                    dato = dato,
                    tidsstempel = tidsstempel
                )
                private fun somInntektsmelding() = Inntektsmelding(
                    id = id,
                    dato = dato,
                    hendelseId = hendelseId,
                    beløp = requireNotNull(beløp).månedlig,
                    tidsstempel = tidsstempel
                )
                private fun somInfotrygd() = Infotrygd(
                    id = id,
                    dato = dato,
                    hendelseId = hendelseId,
                    beløp = requireNotNull(beløp).månedlig,
                    tidsstempel = tidsstempel
                )
                private fun somSkattSykepengegrunnlag() = SkattSykepengegrunnlag.ferdigSkattSykepengegrunnlag(
                    id = id,
                    dato = dato,
                    inntektsopplysninger = requireNotNull(skatteopplysninger).map { skatteData ->
                        skatteData.tilModellobjekt()
                    },
                    ansattPerioder = emptyList(),
                    tidsstempel = tidsstempel,
                    hendelseId = hendelseId
                )
            }
        }

        data class ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagData(
            val orgnummer: String,
            val skatteopplysninger: List<SammenligningsgrunnlagInntektsopplysningData>
        ) {
            companion object {
                internal fun List<ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagData>.parseArbeidsgiverInntektsopplysninger(): List<ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag> =
                    map {
                        ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag(it.orgnummer, it.skatteopplysninger.map { it.tilModellobjekt() })
                    }
            }
            data class SammenligningsgrunnlagInntektsopplysningData(
                val hendelseId: UUID,
                val beløp: Double,
                val måned: YearMonth,
                val type: InntekttypeData,
                val fordel: String,
                val beskrivelse: String,
                val tidsstempel: LocalDateTime,
            ) {
                internal enum class InntekttypeData {
                    LØNNSINNTEKT,
                    NÆRINGSINNTEKT,
                    PENSJON_ELLER_TRYGD,
                    YTELSE_FRA_OFFENTLIGE;

                    fun tilModellenum() = when(this) {
                        LØNNSINNTEKT -> Skatteopplysning.Inntekttype.LØNNSINNTEKT
                        NÆRINGSINNTEKT -> Skatteopplysning.Inntekttype.NÆRINGSINNTEKT
                        PENSJON_ELLER_TRYGD -> Skatteopplysning.Inntekttype.PENSJON_ELLER_TRYGD
                        YTELSE_FRA_OFFENTLIGE -> Skatteopplysning.Inntekttype.YTELSE_FRA_OFFENTLIGE
                    }
                }
                fun tilModellobjekt(): Skatteopplysning =
                    Skatteopplysning(
                        hendelseId = hendelseId,
                        beløp = beløp.månedlig,
                        måned = måned,
                        type = type.tilModellenum(),
                        fordel = fordel,
                        beskrivelse = beskrivelse,
                        tidsstempel = tidsstempel
                    )
            }
        }

        data class OpptjeningData(
            val opptjeningFom: LocalDate,
            val opptjeningTom: LocalDate,
            val arbeidsforhold: List<ArbeidsgiverOpptjeningsgrunnlagData>
        ) {
            fun tilOpptjening(skjæringstidspunkt: LocalDate) = Opptjening.gjenopprett(
                skjæringstidspunkt = skjæringstidspunkt,
                arbeidsforhold = arbeidsforhold.tilArbeidsgiverOpptjeningsgrunnlag(),
                opptjeningsperiode = opptjeningFom til opptjeningTom
            )

            data class ArbeidsgiverOpptjeningsgrunnlagData(
                val orgnummer: String,
                val ansattPerioder: List<ArbeidsforholdData>
            ) {
                data class ArbeidsforholdData(
                    val ansattFom: LocalDate,
                    val ansattTom: LocalDate?,
                    val deaktivert: Boolean
                ) {
                    internal fun tilArbeidsforhold() =
                        Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Arbeidsforhold(ansattFom, ansattTom, deaktivert)
                }

                companion object {
                    fun List<ArbeidsgiverOpptjeningsgrunnlagData>.tilArbeidsgiverOpptjeningsgrunnlag() =
                        map { arbeidsgiverOpptjeningsgrunnlag ->
                            Opptjening.ArbeidsgiverOpptjeningsgrunnlag(
                                arbeidsgiverOpptjeningsgrunnlag.orgnummer,
                                arbeidsgiverOpptjeningsgrunnlag.ansattPerioder.map { it.tilArbeidsforhold() }
                            )
                        }
                }
            }
        }
    }

    data class ArbeidsgiverData(
        val organisasjonsnummer: String,
        val id: UUID,
        val inntektshistorikk: List<InntektsmeldingData> = listOf(),
        val sykdomshistorikk: List<SykdomshistorikkData>,
        val sykmeldingsperioder: List<SykmeldingsperiodeData>,
        val vedtaksperioder: List<VedtaksperiodeData>,
        val forkastede: List<ForkastetVedtaksperiodeData>,
        val utbetalinger: List<UtbetalingData>,
        val feriepengeutbetalinger: List<FeriepengeutbetalingData> = emptyList(),
        val refusjonshistorikk: List<RefusjonData>
    ) {
        private val modelSykdomshistorikk by lazy { SykdomshistorikkData.parseSykdomshistorikk(sykdomshistorikk) }
        private val vedtaksperiodeliste = mutableListOf<Vedtaksperiode>()
        private val forkastedeliste = mutableListOf<ForkastetVedtaksperiode>()
        private val modelUtbetalinger by lazy {
            utbetalinger.fold(emptyList<Pair<UUID, Utbetaling>>()) { andreUtbetalinger, utbetalingen ->
                andreUtbetalinger.plus(utbetalingen.id to utbetalingen.konverterTilUtbetaling(andreUtbetalinger))
            }.map(Pair<*, Utbetaling>::second)
        }
        private val utbetalingMap by lazy {
            utbetalinger.zip(modelUtbetalinger) { data, utbetaling -> data.id to utbetaling }.toMap()
        }

        internal fun konverterTilArbeidsgiver(
            person: Person,
            aktørId: String,
            fødselsnummer: String,
            alder: Alder,
            grunnlagoppslag: (UUID) -> VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement,
            jurist: MaskinellJurist
        ): Arbeidsgiver {
            val arbeidsgiverJurist = jurist.medOrganisasjonsnummer(organisasjonsnummer)
            val arbeidsgiver = Arbeidsgiver.JsonRestorer.restore(
                person,
                organisasjonsnummer,
                id,
                Inntektshistorikk.gjenopprett(inntektshistorikk.map { it.tilModellobjekt() }),
                modelSykdomshistorikk,
                Sykmeldingsperioder(sykmeldingsperioder.map { it.tilPeriode() }),
                vedtaksperiodeliste,
                forkastedeliste,
                modelUtbetalinger,
                feriepengeutbetalinger.map { it.createFeriepengeutbetaling(alder) },
                refusjonshistorikk.parseRefusjon(),
                organisasjonsnummer.tilYrkesaktivitet(),
                arbeidsgiverJurist
            )

            vedtaksperiodeliste.addAll(this.vedtaksperioder.map {
                it.createVedtaksperiode(
                    person,
                    arbeidsgiver,
                    aktørId,
                    fødselsnummer,
                    this.organisasjonsnummer,
                    grunnlagoppslag,
                    utbetalingMap,
                    arbeidsgiverJurist
                )
            })

            forkastedeliste.addAll(this.forkastede.map { periode ->
                ForkastetVedtaksperiode(
                    periode.vedtaksperiode.createVedtaksperiode(
                        person,
                        arbeidsgiver,
                        aktørId,
                        fødselsnummer,
                        this.organisasjonsnummer,
                        grunnlagoppslag,
                        utbetalingMap,
                        arbeidsgiverJurist
                    )
                )
            })
            vedtaksperiodeliste.sort()
            return arbeidsgiver
        }

        data class InntektsmeldingData(
            val id: UUID,
            val dato: LocalDate,
            val hendelseId: UUID,
            val beløp: Double,
            val tidsstempel: LocalDateTime
        ) {

            internal fun tilModellobjekt() = Inntektsmelding(
                id = id,
                dato = dato,
                hendelseId = hendelseId,
                beløp = beløp.månedlig,
                tidsstempel = tidsstempel
            )
        }

        data class RefusjonsopplysningData(
            val meldingsreferanseId: UUID,
            val fom: LocalDate,
            val tom: LocalDate?,
            val beløp: Double
        ) {
            internal fun tilModellobjekt() = Refusjonsopplysning(meldingsreferanseId, fom, tom, beløp.månedlig)
        }

        data class PeriodeData(val fom: LocalDate, val tom: LocalDate) {
            fun tilModellobjekt() = Periode(fom, tom)
        }
        data class SykdomstidslinjeData(
            val dager: List<DagData>,
            val periode: PeriodeData?,
            val låstePerioder: List<PeriodeData>?
        ) {
            private val dagerMap: Map<LocalDate, Dag> = DagData.parseDager(dager)

            internal fun createSykdomstidslinje(): Sykdomstidslinje =
                Sykdomstidslinje.ferdigSykdomstidslinje(
                    dager = dagerMap,
                    periode = periode?.tilModellobjekt(),
                    perioder = låstePerioder?.map { it.tilModellobjekt() } ?: mutableListOf()
                )

            data class DagData(
                val type: JsonDagType,
                val kilde: KildeData,
                val grad: Double,
                val other: KildeData?,
                val melding: String?
            ) {
                @JsonUnwrapped
                lateinit var datoer: DateRange

                internal companion object {
                    internal fun parseDager(dager: List<DagData>): Map<LocalDate, Dag> =
                        dager.filterNot { it.type == JsonDagType.UKJENT_DAG }
                            .flatMap { it.datoer.dates().map { dato -> dato to it.parseDag(dato) } }
                            .toMap()
                            .toSortedMap()
                }

                private val økonomi get() = Økonomi.sykdomsgrad(grad.prosent)

                private val hendelseskilde get() = kilde.parseKilde()

                internal fun parseDag(dato: LocalDate): Dag = when (type) {
                    JsonDagType.ARBEIDSDAG -> Dag.Arbeidsdag(dato, hendelseskilde)
                    JsonDagType.ARBEIDSGIVERDAG -> if (dato.erHelg()) {
                        Dag.ArbeidsgiverHelgedag(dato, økonomi, hendelseskilde)
                    } else {
                        Dag.Arbeidsgiverdag(dato, økonomi, hendelseskilde)
                    }
                    JsonDagType.FERIEDAG -> Dag.Feriedag(dato, hendelseskilde)
                    JsonDagType.ARBEID_IKKE_GJENOPPTATT_DAG -> Dag.ArbeidIkkeGjenopptattDag(dato, hendelseskilde)
                    JsonDagType.FRISK_HELGEDAG -> Dag.FriskHelgedag(dato, hendelseskilde)
                    JsonDagType.FORELDET_SYKEDAG -> Dag.ForeldetSykedag(dato, økonomi, hendelseskilde)
                    JsonDagType.PERMISJONSDAG -> Dag.Permisjonsdag(dato, hendelseskilde)
                    JsonDagType.PROBLEMDAG -> Dag.ProblemDag(dato, hendelseskilde, other!!.parseKilde(), melding!!)
                    JsonDagType.SYKEDAG -> if (dato.erHelg()) {
                        Dag.SykHelgedag(dato, økonomi, hendelseskilde)
                    } else {
                        Dag.Sykedag(dato, økonomi, hendelseskilde)
                    }
                    JsonDagType.SYKEDAG_NAV -> Dag.SykedagNav(dato, økonomi, hendelseskilde)
                    JsonDagType.ANDRE_YTELSER_FORELDREPENGER -> Dag.AndreYtelser(dato, hendelseskilde, Dag.AndreYtelser.AnnenYtelse.Foreldrepenger)
                    JsonDagType.ANDRE_YTELSER_AAP -> Dag.AndreYtelser(dato, hendelseskilde, Dag.AndreYtelser.AnnenYtelse.AAP)
                    JsonDagType.ANDRE_YTELSER_OMSORGSPENGER -> Dag.AndreYtelser(dato, hendelseskilde, Dag.AndreYtelser.AnnenYtelse.Omsorgspenger)
                    JsonDagType.ANDRE_YTELSER_PLEIEPENGER -> Dag.AndreYtelser(dato, hendelseskilde, Dag.AndreYtelser.AnnenYtelse.Pleiepenger)
                    JsonDagType.ANDRE_YTELSER_SVANGERSKAPSPENGER -> Dag.AndreYtelser(dato, hendelseskilde, Dag.AndreYtelser.AnnenYtelse.Svangerskapspenger)
                    JsonDagType.ANDRE_YTELSER_OPPLÆRINGSPENGER -> Dag.AndreYtelser(dato, hendelseskilde, Dag.AndreYtelser.AnnenYtelse.Opplæringspenger)
                    JsonDagType.ANDRE_YTELSER_DAGPENGER -> Dag.AndreYtelser(dato, hendelseskilde, Dag.AndreYtelser.AnnenYtelse.Dagpenger)
                    JsonDagType.UKJENT_DAG -> throw IllegalStateException("Deserialisering av $type er ikke støttet")
                }
            }

            enum class JsonDagType {
                ARBEIDSDAG,
                ARBEIDSGIVERDAG,

                FERIEDAG,
                ARBEID_IKKE_GJENOPPTATT_DAG,
                FRISK_HELGEDAG,
                FORELDET_SYKEDAG,
                PERMISJONSDAG,
                PROBLEMDAG,
                SYKEDAG,
                SYKEDAG_NAV,
                ANDRE_YTELSER_FORELDREPENGER,
                ANDRE_YTELSER_AAP,
                ANDRE_YTELSER_OMSORGSPENGER,
                ANDRE_YTELSER_PLEIEPENGER,
                ANDRE_YTELSER_SVANGERSKAPSPENGER,
                ANDRE_YTELSER_OPPLÆRINGSPENGER,
                ANDRE_YTELSER_DAGPENGER,

                UKJENT_DAG
            }

            data class KildeData(
                val type: String,
                val id: UUID,
                val tidsstempel: LocalDateTime
            ) {
                internal fun parseKilde() = Hendelseskilde(type, id, tidsstempel)
            }
        }

        data class ForkastetVedtaksperiodeData(
            val vedtaksperiode: VedtaksperiodeData
        )

        data class FeriepengeutbetalingData(
            val infotrygdFeriepengebeløpPerson: Double,
            val infotrygdFeriepengebeløpArbeidsgiver: Double,
            val spleisFeriepengebeløpArbeidsgiver: Double,
            val spleisFeriepengebeløpPerson: Double,
            val oppdrag: OppdragData,
            val personoppdrag: OppdragData,
            val opptjeningsår: Year,
            val utbetalteDager: List<UtbetaltDagData>,
            val feriepengedager: List<UtbetaltDagData>,
            val utbetalingId: UUID,
            val sendTilOppdrag: Boolean,
            val sendPersonoppdragTilOS: Boolean,
        ) {
            internal fun createFeriepengeutbetaling(alder: Alder): Feriepengeutbetaling {
                val feriepengeberegner = createFeriepengeberegner(alder)
                return Feriepengeutbetaling.ferdigFeriepengeutbetaling(
                    feriepengeberegner = feriepengeberegner,
                    infotrygdFeriepengebeløpPerson = infotrygdFeriepengebeløpPerson,
                    infotrygdFeriepengebeløpArbeidsgiver = infotrygdFeriepengebeløpArbeidsgiver,
                    spleisFeriepengebeløpArbeidsgiver = spleisFeriepengebeløpArbeidsgiver,
                    spleisFeriepengebeløpPerson = spleisFeriepengebeløpPerson,
                    oppdrag = oppdrag.konverterTilOppdrag(),
                    personoppdrag = personoppdrag.konverterTilOppdrag(),
                    utbetalingId = utbetalingId,
                    sendTilOppdrag = sendTilOppdrag,
                    sendPersonoppdragTilOS = sendPersonoppdragTilOS,
                )
            }

            private fun createFeriepengeberegner(alder: Alder): Feriepengeberegner {
                return Feriepengeberegner.ferdigFeriepengeberegner(
                    alder = alder,
                    opptjeningsår = opptjeningsår,
                    utbetalteDager = utbetalteDager.sortedBy { it.type }.map { it.createUtbetaltDag() }
                )
            }

            data class UtbetaltDagData(
                val type: String,
                val orgnummer: String,
                val dato: LocalDate,
                val beløp: Int,
            ) {
                internal fun createUtbetaltDag() =
                    when (type) {
                        "InfotrygdPersonDag" -> InfotrygdPerson(orgnummer, dato, beløp)
                        "InfotrygdArbeidsgiverDag" -> InfotrygdArbeidsgiver(orgnummer, dato, beløp)
                        "SpleisArbeidsgiverDag" -> SpleisArbeidsgiver(orgnummer, dato, beløp)
                        "SpleisPersonDag" -> SpleisPerson(orgnummer, dato, beløp)
                        else -> throw IllegalArgumentException("Støtter ikke denne dagtypen: $type")
                    }

            }
        }

        data class SykmeldingsperiodeData(
            val fom: LocalDate,
            val tom: LocalDate
        ) {
            internal fun tilPeriode() = fom til tom
        }

        data class VedtaksperiodeData(
            val id: UUID,
            val tilstand: TilstandType,
            val generasjoner: List<GenerasjonData>,
            val opprettet: LocalDateTime,
            val oppdatert: LocalDateTime
        ) {
            data class DokumentsporingData(
                val dokumentId: UUID,
                val dokumenttype: DokumentTypeData
            ) {
                fun tilModellobjekt() = dokumenttype.tilModelltype(dokumentId)
            }
            enum class DokumentTypeData(private val modelltype: (UUID) -> Dokumentsporing) {
                Sykmelding(Dokumentsporing::sykmelding),
                Søknad(Dokumentsporing::søknad),
                InntektsmeldingInntekt(Dokumentsporing::inntektsmeldingInntekt),
                InntektsmeldingDager(Dokumentsporing::inntektsmeldingDager),
                OverstyrTidslinje(Dokumentsporing::overstyrTidslinje),
                OverstyrInntekt(Dokumentsporing::overstyrInntekt),
                OverstyrRefusjon(Dokumentsporing::overstyrRefusjon),
                OverstyrArbeidsgiveropplysninger(Dokumentsporing::overstyrArbeidsgiveropplysninger),
                OverstyrArbeidsforhold(Dokumentsporing::overstyrArbeidsforhold),
                SkjønnsmessigFastsettelse(Dokumentsporing::skjønnsmessigFastsettelse);

                fun tilModelltype(dokumentId: UUID) = modelltype(dokumentId)
            }

            data class GenerasjonData(
                val id: UUID,
                val tilstand: TilstandData,
                val vedtakFattet: LocalDateTime?,
                val avsluttet: LocalDateTime?,
                val kilde: KildeData,
                val endringer: List<EndringData>
            ) {
                internal enum class TilstandData {
                    UBEREGNET, UBEREGNET_OMGJØRING, UBEREGNET_REVURDERING, BEREGNET, BEREGNET_OMGJØRING, BEREGNET_REVURDERING, VEDTAK_FATTET, REVURDERT_VEDTAK_AVVIST, VEDTAK_IVERKSATT, AVSLUTTET_UTEN_VEDTAK, TIL_INFOTRYGD;
                    fun tilModellobjekt() = mapping.getValue(this)

                    companion object {
                        private val mapping = mapOf(
                            UBEREGNET to Generasjoner.Generasjon.Tilstand.Uberegnet,
                            UBEREGNET_OMGJØRING to Generasjoner.Generasjon.Tilstand.UberegnetOmgjøring,
                            UBEREGNET_REVURDERING to Generasjoner.Generasjon.Tilstand.UberegnetRevurdering,
                            BEREGNET to Generasjoner.Generasjon.Tilstand.Beregnet,
                            BEREGNET_OMGJØRING to Generasjoner.Generasjon.Tilstand.BeregnetOmgjøring,
                            BEREGNET_REVURDERING to Generasjoner.Generasjon.Tilstand.BeregnetRevurdering,
                            VEDTAK_FATTET to Generasjoner.Generasjon.Tilstand.VedtakFattet,
                            VEDTAK_IVERKSATT to Generasjoner.Generasjon.Tilstand.VedtakIverksatt,
                            REVURDERT_VEDTAK_AVVIST to Generasjoner.Generasjon.Tilstand.RevurdertVedtakAvvist,
                            AVSLUTTET_UTEN_VEDTAK to Generasjoner.Generasjon.Tilstand.AvsluttetUtenVedtak,
                            TIL_INFOTRYGD to Generasjoner.Generasjon.Tilstand.TilInfotrygd
                        )
                        fun tilEnum(tilstand: Generasjoner.Generasjon.Tilstand) = mapping.entries.single { (_, obj) -> obj == tilstand }.key
                    }
                }
                internal enum class AvsenderData {
                    SYKMELDT, ARBEIDSGIVER, SAKSBEHANDLER, SYSTEM;
                    fun tilModellobjekt() = mapping.getValue(this)

                    companion object {
                        private val mapping = mapOf(
                            SYKMELDT to Avsender.SYKMELDT,
                            ARBEIDSGIVER to Avsender.ARBEIDSGIVER,
                            SAKSBEHANDLER to Avsender.SAKSBEHANDLER,
                            SYSTEM to Avsender.SYSTEM
                        )
                        fun tilEnum(avsender: Avsender) = mapping.entries.single { (_, obj) -> obj == avsender }.key
                    }
                }

                internal data class KildeData(
                    val meldingsreferanseId: UUID,
                    val innsendt: LocalDateTime,
                    val registrert: LocalDateTime,
                    val avsender: AvsenderData
                ) {
                    fun tilModellObjekt() = Generasjoner.Generasjonkilde(
                        meldingsreferanseId = meldingsreferanseId,
                        innsendt = innsendt,
                        registert = registrert,
                        avsender = avsender.tilModellobjekt()
                    )
                }

                internal fun tilModellobjekt(grunnlagoppslag: (UUID) -> VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement, utbetalinger: Map<UUID, Utbetaling>) = Generasjoner.Generasjon.ferdigGenerasjon(
                    id = id,
                    tilstand = tilstand.tilModellobjekt(),
                    endringer = endringer.map { it.tilModellobjekt(grunnlagoppslag, utbetalinger) }.toMutableList(),
                    vedtakFattet = vedtakFattet,
                    avsluttet = avsluttet,
                    kilde = kilde.tilModellObjekt()
                )
                companion object {
                    fun List<GenerasjonData>.tilModellobjekt(grunnlagoppslag: (UUID) -> VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement, utbetalinger: Map<UUID, Utbetaling>) =
                        this.map { it.tilModellobjekt(grunnlagoppslag, utbetalinger) }
                }

                data class EndringData(
                    val id: UUID,
                    val tidsstempel: LocalDateTime,
                    val sykmeldingsperiodeFom: LocalDate,
                    val sykmeldingsperiodeTom: LocalDate,
                    val fom: LocalDate,
                    val tom: LocalDate,
                    val utbetalingId: UUID?,
                    val vilkårsgrunnlagId: UUID?,
                    val sykdomstidslinje: SykdomstidslinjeData,
                    val dokumentsporing: DokumentsporingData
                ) {
                    fun tilModellobjekt(grunnlagoppslag: (UUID) -> VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement, utbetalinger: Map<UUID, Utbetaling>) =
                        Generasjoner.Generasjon.Endring(
                            id = id,
                            tidsstempel = tidsstempel,
                            sykmeldingsperiode = sykmeldingsperiodeFom til sykmeldingsperiodeTom,
                            periode = fom til tom,
                            grunnlagsdata = vilkårsgrunnlagId?.let(grunnlagoppslag),
                            utbetaling = utbetalingId?.let(utbetalinger::getValue),
                            dokumentsporing = dokumentsporing.tilModellobjekt(),
                            sykdomstidslinje = sykdomstidslinje.createSykdomstidslinje()
                        )
                }
            }
            internal fun createVedtaksperiode(
                person: Person,
                arbeidsgiver: Arbeidsgiver,
                aktørId: String,
                fødselsnummer: String,
                organisasjonsnummer: String,
                grunnlagoppslag: (UUID) -> VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement,
                utbetalinger: Map<UUID, Utbetaling>,
                jurist: MaskinellJurist
            ): Vedtaksperiode {
                return Vedtaksperiode.ferdigVedtaksperiode(
                    person = person,
                    arbeidsgiver = arbeidsgiver,
                    id = id,
                    aktørId = aktørId,
                    fødselsnummer = fødselsnummer,
                    organisasjonsnummer = organisasjonsnummer,
                    tilstand = parseTilstand(this.tilstand),
                    generasjoner = Generasjoner.ferdigGenerasjoner(
                        generasjoner = this.generasjoner.tilModellobjekt(grunnlagoppslag, utbetalinger)
                    ),
                    opprettet = opprettet,
                    oppdatert = oppdatert,
                    medOrganisasjonsnummer = jurist
                )
            }

            private fun parseTilstand(tilstand: TilstandType) = when (tilstand) {
                TilstandType.AVVENTER_HISTORIKK -> Vedtaksperiode.AvventerHistorikk
                TilstandType.AVVENTER_GODKJENNING -> Vedtaksperiode.AvventerGodkjenning
                TilstandType.AVVENTER_SIMULERING -> Vedtaksperiode.AvventerSimulering
                TilstandType.TIL_UTBETALING -> Vedtaksperiode.TilUtbetaling
                TilstandType.AVSLUTTET -> Vedtaksperiode.Avsluttet
                TilstandType.AVSLUTTET_UTEN_UTBETALING -> Vedtaksperiode.AvsluttetUtenUtbetaling
                TilstandType.REVURDERING_FEILET -> Vedtaksperiode.RevurderingFeilet
                TilstandType.TIL_INFOTRYGD -> Vedtaksperiode.TilInfotrygd
                TilstandType.START -> Vedtaksperiode.Start
                TilstandType.AVVENTER_INFOTRYGDHISTORIKK -> Vedtaksperiode.AvventerInfotrygdHistorikk
                TilstandType.AVVENTER_VILKÅRSPRØVING -> Vedtaksperiode.AvventerVilkårsprøving
                TilstandType.AVVENTER_INNTEKTSMELDING -> Vedtaksperiode.AvventerInntektsmelding
                TilstandType.AVVENTER_BLOKKERENDE_PERIODE -> Vedtaksperiode.AvventerBlokkerendePeriode
                TilstandType.AVVENTER_REVURDERING -> Vedtaksperiode.AvventerRevurdering
                TilstandType.AVVENTER_HISTORIKK_REVURDERING -> Vedtaksperiode.AvventerHistorikkRevurdering
                TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING -> Vedtaksperiode.AvventerVilkårsprøvingRevurdering
                TilstandType.AVVENTER_SIMULERING_REVURDERING -> Vedtaksperiode.AvventerSimuleringRevurdering
                TilstandType.AVVENTER_GODKJENNING_REVURDERING -> Vedtaksperiode.AvventerGodkjenningRevurdering
            }

            data class DataForSimuleringData(
                val totalbeløp: Int,
                val perioder: List<SimulertPeriode>
            ) {
                internal fun parseDataForSimulering() = SimuleringResultat(
                    totalbeløp = totalbeløp,
                    perioder = perioder.map { it.parsePeriode() }
                )

                data class SimulertPeriode(
                    val fom: LocalDate,
                    val tom: LocalDate,
                    val utbetalinger: List<SimulertUtbetaling>
                ) {

                    internal fun parsePeriode(): SimuleringResultat.SimulertPeriode {
                        return SimuleringResultat.SimulertPeriode(
                            periode = Periode(fom, tom),
                            utbetalinger = utbetalinger.map { it.parseUtbetaling() }
                        )
                    }
                }

                data class SimulertUtbetaling(
                    val forfallsdato: LocalDate,
                    val utbetalesTil: Mottaker,
                    val feilkonto: Boolean,
                    val detaljer: List<Detaljer>
                ) {
                    internal fun parseUtbetaling(): SimuleringResultat.SimulertUtbetaling {
                        return SimuleringResultat.SimulertUtbetaling(
                            forfallsdato = forfallsdato,
                            utbetalesTil = SimuleringResultat.Mottaker(
                                id = utbetalesTil.id,
                                navn = utbetalesTil.navn
                            ),
                            feilkonto = feilkonto,
                            detaljer = detaljer.map { it.parseDetaljer() }
                        )
                    }
                }

                data class Detaljer(
                    val fom: LocalDate,
                    val tom: LocalDate,
                    val konto: String,
                    val beløp: Int,
                    val klassekode: Klassekode,
                    val uføregrad: Int,
                    val utbetalingstype: String,
                    val tilbakeføring: Boolean,
                    val sats: Sats,
                    val refunderesOrgnummer: String
                ) {
                    internal fun parseDetaljer(): SimuleringResultat.Detaljer {
                        return SimuleringResultat.Detaljer(
                            periode = Periode(fom, tom),
                            konto = konto,
                            beløp = beløp,
                            klassekode = SimuleringResultat.Klassekode(
                                kode = klassekode.kode,
                                beskrivelse = klassekode.beskrivelse
                            ),
                            uføregrad = uføregrad,
                            utbetalingstype = utbetalingstype,
                            tilbakeføring = tilbakeføring,
                            sats = SimuleringResultat.Sats(
                                sats = sats.sats,
                                antall = sats.antall,
                                type = sats.type
                            ),
                            refunderesOrgnummer = refunderesOrgnummer
                        )
                    }
                }

                data class Sats(
                    val sats: Double,
                    val antall: Int,
                    val type: String
                )

                data class Klassekode(
                    val kode: String,
                    val beskrivelse: String
                )

                data class Mottaker(
                    val id: String,
                    val navn: String
                )
            }
        }

        data class RefusjonData(
            val meldingsreferanseId: UUID,
            val førsteFraværsdag: LocalDate?,
            val arbeidsgiverperioder: List<PeriodeData>,
            val beløp: Double?,
            val sisteRefusjonsdag: LocalDate?,
            val endringerIRefusjon: List<EndringIRefusjonData>,
            val tidsstempel: LocalDateTime
        ) {
            internal companion object {
                internal fun List<RefusjonData>.parseRefusjon() = Refusjonshistorikk().apply {
                    forEach {
                        leggTilRefusjon(
                            Refusjonshistorikk.Refusjon(
                                meldingsreferanseId = it.meldingsreferanseId,
                                førsteFraværsdag = it.førsteFraværsdag,
                                arbeidsgiverperioder = it.arbeidsgiverperioder.map { it.tilModellobjekt() },
                                beløp = it.beløp?.månedlig,
                                sisteRefusjonsdag = it.sisteRefusjonsdag,
                                endringerIRefusjon = it.endringerIRefusjon.parseEndringerIRefusjon(),
                                tidsstempel = it.tidsstempel
                            )
                        )
                    }
                }
            }

            data class EndringIRefusjonData(
                val beløp: Double,
                val endringsdato: LocalDate
            ) {
                internal companion object {
                    internal fun List<EndringIRefusjonData>.parseEndringerIRefusjon() = map {
                        Refusjonshistorikk.Refusjon.EndringIRefusjon(
                            beløp = it.beløp.månedlig,
                            endringsdato = it.endringsdato
                        )
                    }
                }
            }
        }
    }

    data class SykdomshistorikkData(
        val tidsstempel: LocalDateTime,
        val id: UUID,
        val hendelseId: UUID?,
        val hendelseSykdomstidslinje: ArbeidsgiverData.SykdomstidslinjeData,
        val beregnetSykdomstidslinje: ArbeidsgiverData.SykdomstidslinjeData
    ) {

        internal companion object {
            internal fun parseSykdomshistorikk(data: List<SykdomshistorikkData>) =
                Sykdomshistorikk.ferdigSykdomshistorikk(data.map { it.parseSykdomshistorikk() })
        }

        internal fun parseSykdomshistorikk(): Sykdomshistorikk.Element {
            return Sykdomshistorikk.Element.ferdigSykdomshistorikkElement(
                id = id,
                hendelseId = hendelseId,
                tidsstempel = tidsstempel,
                hendelseSykdomstidslinje = hendelseSykdomstidslinje.createSykdomstidslinje(),
                beregnetSykdomstidslinje = beregnetSykdomstidslinje.createSykdomstidslinje()
            )
        }
    }

    data class UtbetalingData(
        val id: UUID,
        val korrelasjonsId: UUID,
        val fom: LocalDate,
        val tom: LocalDate,
        val annulleringer: List<UUID>?,
        val utbetalingstidslinje: UtbetalingstidslinjeData,
        val arbeidsgiverOppdrag: OppdragData,
        val personOppdrag: OppdragData,
        val tidsstempel: LocalDateTime,
        val type: String,
        val status: String,
        val maksdato: LocalDate,
        val forbrukteSykedager: Int?,
        val gjenståendeSykedager: Int?,
        val vurdering: VurderingData?,
        val overføringstidspunkt: LocalDateTime?,
        val avstemmingsnøkkel: Long?,
        val avsluttet: LocalDateTime?,
        val oppdatert: LocalDateTime
    ) {

        internal fun konverterTilUtbetaling(andreUtbetalinger: List<Pair<UUID, Utbetaling>>) = Utbetaling.ferdigUtbetaling(
            id = id,
            korrelasjonsId = korrelasjonsId,
            annulleringer = annulleringer?.map { annulleringen -> andreUtbetalinger.single { it.first == annulleringen }.second } ?: emptyList(),
            opprinneligPeriode = fom til tom,
            utbetalingstidslinje = utbetalingstidslinje.konverterTilUtbetalingstidslinje(),
            arbeidsgiverOppdrag = arbeidsgiverOppdrag.konverterTilOppdrag(),
            personOppdrag = personOppdrag.konverterTilOppdrag(),
            tidsstempel = tidsstempel,
            utbetalingstatus = enumValueOf(status),
            utbetalingtype = enumValueOf(type),
            maksdato = maksdato,
            forbrukteSykedager = forbrukteSykedager,
            gjenståendeSykedager = gjenståendeSykedager,
            vurdering = vurdering?.konverterTilVurdering(),
            overføringstidspunkt = overføringstidspunkt,
            avstemmingsnøkkel = avstemmingsnøkkel,
            avsluttet = avsluttet,
            oppdatert = oppdatert
        )

        data class VurderingData(
            val godkjent: Boolean,
            val ident: String,
            val epost: String,
            val tidspunkt: LocalDateTime,
            val automatiskBehandling: Boolean
        ) {
            internal fun konverterTilVurdering() = Utbetaling.ferdigVurdering(
                godkjent = godkjent,
                ident = ident,
                epost = epost,
                tidspunkt = tidspunkt,
                automatiskBehandling = automatiskBehandling
            )
        }
    }

    data class OppdragData(
        val mottaker: String,
        val fagområde: String,
        val linjer: List<UtbetalingslinjeData>,
        val fagsystemId: String,
        val endringskode: String,
        val tidsstempel: LocalDateTime,
        val nettoBeløp: Int,
        val avstemmingsnøkkel: Long?,
        val status: Oppdragstatus?,
        val overføringstidspunkt: LocalDateTime?,
        val erSimulert: Boolean,
        val simuleringsResultat: ArbeidsgiverData.VedtaksperiodeData.DataForSimuleringData?
    ) {
        internal fun konverterTilOppdrag(): Oppdrag = Oppdrag.ferdigOppdrag(
            mottaker = mottaker,
            from = Fagområde.from(fagområde),
            utbetalingslinjer = linjer.map { it.konverterTilUtbetalingslinje() },
            fagsystemId = fagsystemId,
            endringskode = Endringskode.valueOf(endringskode),
            nettoBeløp = nettoBeløp,
            overføringstidspunkt = overføringstidspunkt,
            avstemmingsnøkkel = avstemmingsnøkkel,
            status = status,
            tidsstempel = tidsstempel,
            erSimulert = erSimulert,
            simuleringResultat = simuleringsResultat?.parseDataForSimulering()
        )
    }

    data class UtbetalingslinjeData(
        val fom: LocalDate,
        val tom: LocalDate,
        val satstype: String,
        val sats: Int,
        val grad: Int?,
        val refFagsystemId: String?,
        val delytelseId: Int,
        val refDelytelseId: Int?,
        val endringskode: String,
        val klassekode: String,
        val datoStatusFom: LocalDate?
    ) {

        internal fun konverterTilUtbetalingslinje(): Utbetalingslinje = Utbetalingslinje.ferdigUtbetalingslinje(
            fom = fom,
            tom = tom,
            satstype = Satstype.fromString(satstype),
            sats = sats,
            grad = grad,
            refFagsystemId = refFagsystemId,
            delytelseId = delytelseId,
            refDelytelseId = refDelytelseId,
            endringskode = Endringskode.valueOf(endringskode),
            klassekode = Klassekode.from(klassekode),
            datoStatusFom = datoStatusFom
        )
    }

    data class UtbetalingstidslinjeData(
        val dager: List<UtbetalingsdagData>
    ) {
        internal fun konverterTilUtbetalingstidslinje(): Utbetalingstidslinje =
            Utbetalingstidslinje.ferdigUtbetalingstidslinje(dager.flatMap { it.parseDager() })

        enum class BegrunnelseData {
            SykepengedagerOppbrukt,
            SykepengedagerOppbruktOver67,
            MinimumInntekt,
            MinimumInntektOver67,
            EgenmeldingUtenforArbeidsgiverperiode,
            MinimumSykdomsgrad,
            AndreYtelserAap,
            AndreYtelserDagpenger,
            AndreYtelserForeldrepenger,
            AndreYtelserOmsorgspenger,
            AndreYtelserOpplaringspenger,
            AndreYtelserPleiepenger,
            AndreYtelserSvangerskapspenger,
            EtterDødsdato,
            ManglerMedlemskap,
            ManglerOpptjening,
            Over70,
            NyVilkårsprøvingNødvendig;

            fun tilBegrunnelse() = when (this) {
                SykepengedagerOppbrukt -> Begrunnelse.SykepengedagerOppbrukt
                SykepengedagerOppbruktOver67 -> Begrunnelse.SykepengedagerOppbruktOver67
                MinimumSykdomsgrad -> Begrunnelse.MinimumSykdomsgrad
                EgenmeldingUtenforArbeidsgiverperiode -> Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode
                AndreYtelserAap -> Begrunnelse.AndreYtelserAap
                AndreYtelserDagpenger -> Begrunnelse.AndreYtelserDagpenger
                AndreYtelserForeldrepenger -> Begrunnelse.AndreYtelserForeldrepenger
                AndreYtelserOmsorgspenger -> Begrunnelse.AndreYtelserOmsorgspenger
                AndreYtelserOpplaringspenger -> Begrunnelse.AndreYtelserOpplaringspenger
                AndreYtelserPleiepenger -> Begrunnelse.AndreYtelserPleiepenger
                AndreYtelserSvangerskapspenger -> Begrunnelse.AndreYtelserSvangerskapspenger
                MinimumInntekt -> Begrunnelse.MinimumInntekt
                MinimumInntektOver67 -> Begrunnelse.MinimumInntektOver67
                EtterDødsdato -> Begrunnelse.EtterDødsdato
                ManglerMedlemskap -> Begrunnelse.ManglerMedlemskap
                ManglerOpptjening -> Begrunnelse.ManglerOpptjening
                Over70 -> Begrunnelse.Over70
                NyVilkårsprøvingNødvendig -> Begrunnelse.NyVilkårsprøvingNødvendig
            }

            internal companion object {
                fun fraBegrunnelse(begrunnelse: Begrunnelse) = when (begrunnelse) {
                    is Begrunnelse.SykepengedagerOppbrukt -> SykepengedagerOppbrukt
                    is Begrunnelse.SykepengedagerOppbruktOver67 -> SykepengedagerOppbruktOver67
                    is Begrunnelse.MinimumSykdomsgrad -> MinimumSykdomsgrad
                    is Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode -> EgenmeldingUtenforArbeidsgiverperiode
                    is Begrunnelse.AndreYtelserAap -> AndreYtelserAap
                    is Begrunnelse.AndreYtelserDagpenger -> AndreYtelserDagpenger
                    is Begrunnelse.AndreYtelserForeldrepenger -> AndreYtelserForeldrepenger
                    is Begrunnelse.AndreYtelserOmsorgspenger -> AndreYtelserOmsorgspenger
                    is Begrunnelse.AndreYtelserOpplaringspenger -> AndreYtelserOpplaringspenger
                    is Begrunnelse.AndreYtelserPleiepenger -> AndreYtelserPleiepenger
                    is Begrunnelse.AndreYtelserSvangerskapspenger -> AndreYtelserSvangerskapspenger
                    is Begrunnelse.MinimumInntekt -> MinimumInntekt
                    is Begrunnelse.MinimumInntektOver67 -> MinimumInntektOver67
                    is Begrunnelse.EtterDødsdato -> EtterDødsdato
                    is Begrunnelse.ManglerMedlemskap -> ManglerMedlemskap
                    is Begrunnelse.ManglerOpptjening -> ManglerOpptjening
                    is Begrunnelse.Over70 -> Over70
                    is Begrunnelse.NyVilkårsprøvingNødvendig -> NyVilkårsprøvingNødvendig
                }
            }
        }

        enum class TypeData {
            ArbeidsgiverperiodeDag,
            NavDag,
            NavHelgDag,
            Arbeidsdag,
            Fridag,
            AvvistDag,
            UkjentDag,
            ForeldetDag,
            ArbeidsgiverperiodedagNav
        }

        data class UtbetalingsdagData(
            val type: TypeData,
            val aktuellDagsinntekt: Double,
            val beregningsgrunnlag: Double,
            val dekningsgrunnlag: Double,
            val grunnbeløpgrense: Double?,
            val begrunnelser: List<BegrunnelseData>?,
            val grad: Double,
            val totalGrad: Double,
            val arbeidsgiverRefusjonsbeløp: Double,
            val arbeidsgiverbeløp: Double?,
            val personbeløp: Double?,
            val er6GBegrenset: Boolean?
        ) {
            private val builder: Økonomi.Builder = Økonomi.Builder()

            init {
                builder.grad(grad)
                    .totalGrad(totalGrad)
                    .aktuellDagsinntekt(aktuellDagsinntekt)
                    .beregningsgrunnlag(beregningsgrunnlag)
                    .dekningsgrunnlag(dekningsgrunnlag)
                    .grunnbeløpsgrense(grunnbeløpgrense)
                    .arbeidsgiverRefusjonsbeløp(arbeidsgiverRefusjonsbeløp)
                    .arbeidsgiverbeløp(arbeidsgiverbeløp)
                    .personbeløp(personbeløp)
                    .er6GBegrenset(er6GBegrenset)
                    .tilstand(
                        when {
                            arbeidsgiverbeløp == null && type == TypeData.AvvistDag -> Økonomi.Tilstand.Låst
                            arbeidsgiverbeløp == null -> Økonomi.Tilstand.HarInntekt
                            type == TypeData.AvvistDag -> Økonomi.Tilstand.LåstMedBeløp
                            else -> Økonomi.Tilstand.HarBeløp
                        }
                    )
            }

            // Gjør så vi kan ha dato/fom og tom på samme nivå som resten av verdiene i utbetalingsdata
            @JsonUnwrapped
            lateinit var datoer: DateRange

            private val økonomi get() = builder.build()

            internal fun parseDager() = datoer.dates().map(::parseDag)

            internal fun parseDag(dato: LocalDate) =
                when (type) {
                    TypeData.ArbeidsgiverperiodeDag -> {
                        Utbetalingsdag.ArbeidsgiverperiodeDag(dato = dato, økonomi = økonomi)
                    }
                    TypeData.NavDag -> {
                        Utbetalingsdag.NavDag(dato, økonomi)
                    }
                    TypeData.NavHelgDag -> {
                        Utbetalingsdag.NavHelgDag(dato = dato, økonomi = økonomi)
                    }
                    TypeData.Arbeidsdag -> {
                        Utbetalingsdag.Arbeidsdag(dato = dato, økonomi = økonomi)
                    }
                    TypeData.Fridag -> {
                        Utbetalingsdag.Fridag(dato = dato, økonomi = økonomi)
                    }
                    TypeData.AvvistDag -> {
                        Utbetalingsdag.AvvistDag(
                            dato = dato,
                            økonomi = økonomi,
                            begrunnelser = begrunnelser?.map { it.tilBegrunnelse() } ?: error("Prøver å deserialisere avvist dag uten begrunnelse")
                        )
                    }
                    TypeData.UkjentDag -> {
                        Utbetalingsdag.UkjentDag(dato = dato, økonomi = økonomi)
                    }
                    TypeData.ForeldetDag -> {
                        Utbetalingsdag.ForeldetDag(dato = dato, økonomi = økonomi)
                    }
                    TypeData.ArbeidsgiverperiodedagNav -> {
                        Utbetalingsdag.ArbeidsgiverperiodedagNav(dato = dato, økonomi = økonomi)
                    }
                }
        }
    }
}
