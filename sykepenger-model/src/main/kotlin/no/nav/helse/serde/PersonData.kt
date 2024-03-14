package no.nav.helse.serde

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.Alder
import no.nav.helse.dto.AlderDto
import no.nav.helse.dto.ArbeidsforholdDto
import no.nav.helse.dto.ArbeidsgiverInntektsopplysningDto
import no.nav.helse.dto.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagDto
import no.nav.helse.dto.ArbeidsgiverOpptjeningsgrunnlagDto
import no.nav.helse.dto.AvsenderDto
import no.nav.helse.dto.BegrunnelseDto
import no.nav.helse.dto.DokumentsporingDto
import no.nav.helse.dto.DokumenttypeDto
import no.nav.helse.dto.EndringIRefusjonDto
import no.nav.helse.dto.EndringskodeDto
import no.nav.helse.dto.FagområdeDto
import no.nav.helse.dto.FeriepengeberegnerDto
import no.nav.helse.dto.GenerasjonDto
import no.nav.helse.dto.GenerasjonEndringDto
import no.nav.helse.dto.GenerasjonTilstandDto
import no.nav.helse.dto.GenerasjonerDto
import no.nav.helse.dto.GenerasjonkildeDto
import no.nav.helse.dto.HendelseskildeDto
import no.nav.helse.dto.InfotrygdArbeidsgiverutbetalingsperiodeDto
import no.nav.helse.dto.InfotrygdFerieperiodeDto
import no.nav.helse.dto.InfotrygdInntektsopplysningDto
import no.nav.helse.dto.InfotrygdPersonutbetalingsperiodeDto
import no.nav.helse.dto.InfotrygdhistorikkDto
import no.nav.helse.dto.InfotrygdhistorikkelementDto
import no.nav.helse.dto.InntektDto
import no.nav.helse.dto.InntektshistorikkDto
import no.nav.helse.dto.InntektsopplysningDto
import no.nav.helse.dto.InntekttypeDto
import no.nav.helse.dto.KlassekodeDto
import no.nav.helse.dto.OppdragstatusDto
import no.nav.helse.dto.OpptjeningDto
import no.nav.helse.dto.PeriodeDto
import no.nav.helse.dto.ProsentdelDto
import no.nav.helse.dto.RefusjonDto
import no.nav.helse.dto.RefusjonshistorikkDto
import no.nav.helse.dto.RefusjonsopplysningDto
import no.nav.helse.dto.RefusjonsopplysningerDto
import no.nav.helse.dto.SammenligningsgrunnlagDto
import no.nav.helse.dto.SatstypeDto
import no.nav.helse.dto.SkatteopplysningDto
import no.nav.helse.dto.SubsumsjonDto
import no.nav.helse.dto.SykdomshistorikkDto
import no.nav.helse.dto.SykdomshistorikkElementDto
import no.nav.helse.dto.SykdomstidslinjeDagDto
import no.nav.helse.dto.SykdomstidslinjeDto
import no.nav.helse.dto.serialisering.SykepengegrunnlagUtDto
import no.nav.helse.dto.SykmeldingsperioderDto
import no.nav.helse.dto.UtbetalingTilstandDto
import no.nav.helse.dto.UtbetalingVurderingDto
import no.nav.helse.dto.UtbetalingsdagDto
import no.nav.helse.dto.UtbetalingstidslinjeDto
import no.nav.helse.dto.UtbetalingtypeDto
import no.nav.helse.dto.UtbetaltDagDto
import no.nav.helse.dto.VedtaksperiodetilstandDto
import no.nav.helse.dto.serialisering.VilkårsgrunnlagUtDto
import no.nav.helse.dto.serialisering.VilkårsgrunnlagInnslagUtDto
import no.nav.helse.dto.serialisering.VilkårsgrunnlaghistorikkUtDto
import no.nav.helse.dto.deserialisering.ArbeidsgiverInnDto
import no.nav.helse.dto.deserialisering.FeriepengeInnDto
import no.nav.helse.dto.deserialisering.ForkastetVedtaksperiodeInnDto
import no.nav.helse.dto.deserialisering.OppdragInnDto
import no.nav.helse.dto.deserialisering.PersonInnDto
import no.nav.helse.dto.deserialisering.SykepengegrunnlagInnDto
import no.nav.helse.dto.deserialisering.UtbetalingInnDto
import no.nav.helse.dto.deserialisering.UtbetalingslinjeInnDto
import no.nav.helse.dto.deserialisering.VedtaksperiodeInnDto
import no.nav.helse.dto.deserialisering.VilkårsgrunnlagInnDto
import no.nav.helse.dto.deserialisering.VilkårsgrunnlagInnslagInnDto
import no.nav.helse.dto.deserialisering.VilkårsgrunnlaghistorikkInnDto
import no.nav.helse.dto.ØkonomiDto
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

    fun tilPersonDto() = PersonInnDto(
        aktørId = this.aktørId,
        fødselsnummer = this.fødselsnummer,
        alder = AlderDto(fødselsdato = this.fødselsdato, dødsdato = this.dødsdato),
        opprettet = this.opprettet,
        arbeidsgivere = this.arbeidsgivere.map { it.tilDto() },
        infotrygdhistorikk = InfotrygdhistorikkDto(this.infotrygdhistorikk.map { it.tilDto() }),
        vilkårsgrunnlagHistorikk = VilkårsgrunnlaghistorikkInnDto(vilkårsgrunnlagHistorikk.map { it.tilDto() })
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

        fun tilDto() = InfotrygdhistorikkelementDto(
            id = this.id,
            tidsstempel = this.tidsstempel,
            hendelseId = this.hendelseId,
            ferieperioder = this.ferieperioder.map { it.tilDto() },
            arbeidsgiverutbetalingsperioder = this.arbeidsgiverutbetalingsperioder.map { it.tilDto() },
            personutbetalingsperioder = this.personutbetalingsperioder.map { it.tilDto() },
            inntekter = this.inntekter.map { it.tilDto() },
            arbeidskategorikoder = this.arbeidskategorikoder,
            oppdatert = this.oppdatert
        )

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
            fun tilDto() = InfotrygdFerieperiodeDto(
                periode = PeriodeDto(
                    fom = this.fom,
                    tom = this.tom
                )
            )
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
            fun tilDto() = InfotrygdPersonutbetalingsperiodeDto(
                orgnr = this.orgnr,
                periode = PeriodeDto(
                    fom = this.fom,
                    tom = this.tom
                ),
                grad = ProsentdelDto(prosent = this.grad),
                inntekt = InntektDto.DagligInt(this.inntekt)
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
            fun tilDto() = InfotrygdArbeidsgiverutbetalingsperiodeDto(
                orgnr = this.orgnr,
                periode = PeriodeDto(fom = this.fom, tom = this.tom),
                grad = ProsentdelDto(prosent = grad),
                inntekt = InntektDto.DagligInt(inntekt)
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
            fun tilDto() = InfotrygdInntektsopplysningDto(
                orgnummer = this.orgnr,
                sykepengerFom = this.sykepengerFom,
                inntekt = InntektDto.MånedligDouble(inntekt),
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

        fun tilDto() = VilkårsgrunnlagInnslagInnDto(
            id = this.id,
            opprettet = this.opprettet,
            vilkårsgrunnlag = this.vilkårsgrunnlag.map { it.tilDto() }
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
        fun tilDto() = when (type) {
            GrunnlagsdataType.Infotrygd -> VilkårsgrunnlagInnDto.Infotrygd(
                vilkårsgrunnlagId = this.vilkårsgrunnlagId,
                skjæringstidspunkt = this.skjæringstidspunkt,
                sykepengegrunnlag = sykepengegrunnlag.tilInfotrygdDto()
            )
            GrunnlagsdataType.Vilkårsprøving -> VilkårsgrunnlagInnDto.Spleis(
                vilkårsgrunnlagId = this.vilkårsgrunnlagId,
                skjæringstidspunkt = this.skjæringstidspunkt,
                sykepengegrunnlag = this.sykepengegrunnlag.tilSpleisDto(),
                opptjening = this.opptjening!!.tilDto(),
                medlemskapstatus = this.medlemskapstatus!!.tilDto(),
                vurdertOk = this.vurdertOk!!,
                meldingsreferanseId = this.meldingsreferanseId
            )
        }
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
            fun tilSpleisDto() = SykepengegrunnlagInnDto(
                arbeidsgiverInntektsopplysninger = this.arbeidsgiverInntektsopplysninger.map { it.tilDto() },
                deaktiverteArbeidsforhold = this.deaktiverteArbeidsforhold.map { it.tilDto() },
                vurdertInfotrygd = this.vurdertInfotrygd,
                sammenligningsgrunnlag = this.sammenligningsgrunnlag!!.tilDto(),
                `6G` = InntektDto.Årlig(grunnbeløp!!)
            )
            fun tilInfotrygdDto() = SykepengegrunnlagInnDto(
                arbeidsgiverInntektsopplysninger = this.arbeidsgiverInntektsopplysninger.map { it.tilDto() },
                deaktiverteArbeidsforhold = this.deaktiverteArbeidsforhold.map { it.tilDto() },
                vurdertInfotrygd = this.vurdertInfotrygd,
                sammenligningsgrunnlag = SammenligningsgrunnlagDto(InntektDto.Årlig(0.0), emptyList()),
                `6G` = InntektDto.Årlig(grunnbeløp!!)
            )
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
            fun tilDto() = SammenligningsgrunnlagDto(
                sammenligningsgrunnlag = InntektDto.Årlig(sammenligningsgrunnlag),
                arbeidsgiverInntektsopplysninger = this.arbeidsgiverInntektsopplysninger.map { it.tilDto() }
            )
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
            fun tilDto() = ArbeidsgiverInntektsopplysningDto(
                orgnummer = this.orgnummer,
                gjelder = PeriodeDto(fom = this.fom, tom = this.tom),
                inntektsopplysning = this.inntektsopplysning.tilDto(),
                refusjonsopplysninger = RefusjonsopplysningerDto(opplysninger = this.refusjonsopplysninger.map { it.tilDto() })
            )
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
                val type: InntekttypeData,
                val fordel: String,
                val beskrivelse: String,
                val tidsstempel: LocalDateTime
            ) {
                enum class InntekttypeData {
                    LØNNSINNTEKT,
                    NÆRINGSINNTEKT,
                    PENSJON_ELLER_TRYGD,
                    YTELSE_FRA_OFFENTLIGE
                }
                internal fun tilModellobjekt() = Skatteopplysning(hendelseId, beløp.månedlig, måned, enumValueOf(type.name), fordel, beskrivelse, tidsstempel)
                fun tilDto() = SkatteopplysningDto(
                    hendelseId = this.hendelseId,
                    beløp = InntektDto.MånedligDouble(beløp = beløp),
                    måned = this.måned,
                    type = when (type) {
                        InntekttypeData.LØNNSINNTEKT -> InntekttypeDto.LØNNSINNTEKT
                        InntekttypeData.NÆRINGSINNTEKT -> InntekttypeDto.NÆRINGSINNTEKT
                        InntekttypeData.PENSJON_ELLER_TRYGD -> InntekttypeDto.PENSJON_ELLER_TRYGD
                        InntekttypeData.YTELSE_FRA_OFFENTLIGE -> InntekttypeDto.YTELSE_FRA_OFFENTLIGE
                    },
                    fordel = fordel,
                    beskrivelse = beskrivelse,
                    tidsstempel = tidsstempel
                )
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
                fun tilDto() = when (kilde.let(Inntektsopplysningskilde::valueOf)) {
                    Inntektsopplysningskilde.INFOTRYGD -> InntektsopplysningDto.InfotrygdDto(
                        id = this.id,
                        hendelseId = this.hendelseId,
                        dato = this.dato,
                        beløp = InntektDto.MånedligDouble(beløp = beløp!!),
                        tidsstempel = this.tidsstempel
                    )
                    Inntektsopplysningskilde.INNTEKTSMELDING -> InntektsopplysningDto.InntektsmeldingDto(
                        id = this.id,
                        hendelseId = this.hendelseId,
                        dato = this.dato,
                        beløp = InntektDto.MånedligDouble(beløp = beløp!!),
                        tidsstempel = this.tidsstempel
                    )
                    Inntektsopplysningskilde.IKKE_RAPPORTERT -> InntektsopplysningDto.IkkeRapportertDto(
                        id = this.id,
                        hendelseId = this.hendelseId,
                        dato = this.dato,
                        tidsstempel = this.tidsstempel
                    )
                    Inntektsopplysningskilde.SAKSBEHANDLER -> InntektsopplysningDto.SaksbehandlerDto(
                        id = this.id,
                        hendelseId = this.hendelseId,
                        dato = this.dato,
                        beløp = InntektDto.MånedligDouble(beløp = beløp!!),
                        tidsstempel = this.tidsstempel,
                        overstyrtInntekt = this.overstyrtInntektId!!,
                        forklaring = this.forklaring,
                        subsumsjon = this.subsumsjon?.tilDto()
                    )
                    Inntektsopplysningskilde.SKJØNNSMESSIG_FASTSATT -> InntektsopplysningDto.SkjønnsmessigFastsattDto(
                        id = this.id,
                        hendelseId = this.hendelseId,
                        dato = this.dato,
                        beløp = InntektDto.MånedligDouble(beløp = beløp!!),
                        tidsstempel = this.tidsstempel,
                        overstyrtInntekt = this.overstyrtInntektId!!
                    )
                    Inntektsopplysningskilde.SKATT_SYKEPENGEGRUNNLAG -> InntektsopplysningDto.SkattSykepengegrunnlagDto(
                        id = this.id,
                        hendelseId = this.hendelseId,
                        dato = this.dato,
                        tidsstempel = this.tidsstempel,
                        inntektsopplysninger = this.skatteopplysninger!!.map { it.tilDto() },
                        ansattPerioder = emptyList()
                    )
                    else -> error("Fant ${kilde}. Det er ugyldig for sykepengegrunnlag")
                }

                data class SubsumsjonData(
                    val paragraf: String,
                    val ledd: Int?,
                    val bokstav: String?
                ) {
                    internal fun tilModellobjekt() = Subsumsjon(paragraf, ledd, bokstav)
                    fun tilDto() = SubsumsjonDto(paragraf, ledd, bokstav)
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
            fun tilDto() = ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagDto(
                orgnummer = this.orgnummer,
                inntektsopplysninger = this.skatteopplysninger.map { it.tilDto() }
            )
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
                fun tilDto() = SkatteopplysningDto(
                    hendelseId = this.hendelseId,
                    beløp = InntektDto.MånedligDouble(beløp = beløp),
                    måned = this.måned,
                    type = when (type) {
                        InntekttypeData.LØNNSINNTEKT -> InntekttypeDto.LØNNSINNTEKT
                        InntekttypeData.NÆRINGSINNTEKT -> InntekttypeDto.NÆRINGSINNTEKT
                        InntekttypeData.PENSJON_ELLER_TRYGD -> InntekttypeDto.PENSJON_ELLER_TRYGD
                        InntekttypeData.YTELSE_FRA_OFFENTLIGE -> InntekttypeDto.YTELSE_FRA_OFFENTLIGE
                    },
                    fordel = fordel,
                    beskrivelse = beskrivelse,
                    tidsstempel = tidsstempel
                )
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
            fun tilDto() = OpptjeningDto(
                arbeidsforhold = this.arbeidsforhold.map { it.tilDto() },
                opptjeningsperiode = PeriodeDto(fom = this.opptjeningFom, tom = this.opptjeningTom)
            )
            fun tilOpptjening(skjæringstidspunkt: LocalDate) = Opptjening.gjenopprett(
                skjæringstidspunkt = skjæringstidspunkt,
                arbeidsforhold = arbeidsforhold.tilArbeidsgiverOpptjeningsgrunnlag(),
                opptjeningsperiode = opptjeningFom til opptjeningTom
            )

            data class ArbeidsgiverOpptjeningsgrunnlagData(
                val orgnummer: String,
                val ansattPerioder: List<ArbeidsforholdData>
            ) {
                fun tilDto() = ArbeidsgiverOpptjeningsgrunnlagDto(
                    orgnummer = this.orgnummer,
                    ansattPerioder = this.ansattPerioder.map { it.tilDto() }
                )
                data class ArbeidsforholdData(
                    val ansattFom: LocalDate,
                    val ansattTom: LocalDate?,
                    val deaktivert: Boolean
                ) {
                    fun tilDto() = ArbeidsforholdDto(
                        ansattFom = ansattFom, ansattTom = ansattTom, deaktivert = deaktivert
                    )
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
        val inntektshistorikk: List<InntektsmeldingData>,
        val sykdomshistorikk: List<SykdomshistorikkData>,
        val sykmeldingsperioder: List<SykmeldingsperiodeData>,
        val vedtaksperioder: List<VedtaksperiodeData>,
        val forkastede: List<ForkastetVedtaksperiodeData>,
        val utbetalinger: List<UtbetalingData>,
        val feriepengeutbetalinger: List<FeriepengeutbetalingData>,
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

        fun tilDto() = ArbeidsgiverInnDto(
            id = this.id,
            organisasjonsnummer = this.organisasjonsnummer,
            inntektshistorikk = InntektshistorikkDto(this.inntektshistorikk.map { it.tilDto() }),
            sykdomshistorikk = SykdomshistorikkDto(this.sykdomshistorikk.map { it.tilDto() }),
            sykmeldingsperioder = SykmeldingsperioderDto(this.sykmeldingsperioder.map { it.tilDto() }),
            vedtaksperioder = this.vedtaksperioder.map { it.tilDto() },
            forkastede = this.forkastede.map { it.tilDto() },
            utbetalinger = this.utbetalinger.map { it.tilDto() },
            feriepengeutbetalinger = this.feriepengeutbetalinger.map { it.tilDto() },
            refusjonshistorikk = RefusjonshistorikkDto(this.refusjonshistorikk.map { it.tilDto() })
        )

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

            fun tilDto() = InntektsopplysningDto.InntektsmeldingDto(
                id = this.id,
                hendelseId = this.hendelseId,
                dato = this.dato,
                beløp = InntektDto.MånedligDouble(beløp = this.beløp),
                tidsstempel = this.tidsstempel
            )
        }

        data class RefusjonsopplysningData(
            val meldingsreferanseId: UUID,
            val fom: LocalDate,
            val tom: LocalDate?,
            val beløp: Double
        ) {
            internal fun tilModellobjekt() = Refusjonsopplysning(meldingsreferanseId, fom, tom, beløp.månedlig)
            fun tilDto() = RefusjonsopplysningDto(
                meldingsreferanseId = this.meldingsreferanseId,
                fom = this.fom,
                tom = this.tom,
                beløp = InntektDto.MånedligDouble(beløp)
            )
        }

        data class PeriodeData(val fom: LocalDate, val tom: LocalDate) {
            fun tilDto() = PeriodeDto(fom = this.fom, tom = this.tom)
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

            fun tilDto() = SykdomstidslinjeDto(
                dager = this.dager.flatMap { it.tilDto() },
                periode = this.periode?.tilDto(),
                låstePerioder = this.låstePerioder?.map { it.tilDto() } ?: emptyList()
            )

            data class DagData(
                val type: JsonDagType,
                val kilde: KildeData,
                val grad: Double,
                val other: KildeData?,
                val melding: String?,
                val fom: LocalDate?,
                val tom: LocalDate?,
                val dato: LocalDate?
            ) {
                init {
                    check (dato != null || (fom != null && tom != null)) {
                        "enten må dato være satt eller så må både fom og tom være satt"
                    }
                }

                private val datoer = if (dato != null) DateRange.Single(dato) else DateRange.Range(fom!!, tom!!)

                internal fun tilDto(): List<SykdomstidslinjeDagDto> {
                    val kilde = this.kilde.tilDto()
                    return datoer.dates().map { tilDto(it, kilde) }
                }
                private fun tilDto(dagen: LocalDate, kilde: HendelseskildeDto) = when (type) {
                    JsonDagType.ARBEIDSDAG -> SykdomstidslinjeDagDto.ArbeidsdagDto(dato = dagen, kilde = kilde)
                    JsonDagType.ARBEIDSGIVERDAG -> if (dagen.erHelg())
                        SykdomstidslinjeDagDto.ArbeidsgiverHelgedagDto(dato = dagen, kilde = kilde, grad = ProsentdelDto(grad))
                    else
                        SykdomstidslinjeDagDto.ArbeidsgiverdagDto(dato = dagen, kilde = kilde, grad = ProsentdelDto(grad))
                    JsonDagType.FERIEDAG -> SykdomstidslinjeDagDto.FeriedagDto(dato = dagen, kilde = kilde)
                    JsonDagType.ARBEID_IKKE_GJENOPPTATT_DAG -> SykdomstidslinjeDagDto.ArbeidIkkeGjenopptattDagDto(dato = dagen, kilde = kilde)
                    JsonDagType.FRISK_HELGEDAG -> SykdomstidslinjeDagDto.FriskHelgedagDto(dato = dagen, kilde = kilde)
                    JsonDagType.FORELDET_SYKEDAG -> SykdomstidslinjeDagDto.ForeldetSykedagDto(dato = dagen, kilde = kilde, grad = ProsentdelDto(grad))
                    JsonDagType.PERMISJONSDAG -> SykdomstidslinjeDagDto.PermisjonsdagDto(dato = dagen, kilde = kilde)
                    JsonDagType.PROBLEMDAG -> SykdomstidslinjeDagDto.ProblemDagDto(dato = dagen, kilde = kilde, other = this.other!!.tilDto(), melding = this.melding!!)
                    JsonDagType.SYKEDAG -> if (dagen.erHelg())
                        SykdomstidslinjeDagDto.SykHelgedagDto(dato = dagen, kilde = kilde, grad = ProsentdelDto(grad))
                    else
                        SykdomstidslinjeDagDto.SykedagDto(dato = dagen, kilde = kilde, grad = ProsentdelDto(grad))
                    JsonDagType.SYKEDAG_NAV -> SykdomstidslinjeDagDto.SykedagNavDto(dato = dagen, kilde = kilde, grad = ProsentdelDto(grad))
                    JsonDagType.ANDRE_YTELSER_FORELDREPENGER -> SykdomstidslinjeDagDto.AndreYtelserDto(dato = dagen, kilde = kilde, SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto.Foreldrepenger)
                    JsonDagType.ANDRE_YTELSER_AAP -> SykdomstidslinjeDagDto.AndreYtelserDto(dato = dagen, kilde = kilde, ytelse = SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto.AAP)
                    JsonDagType.ANDRE_YTELSER_OMSORGSPENGER -> SykdomstidslinjeDagDto.AndreYtelserDto(dato = dagen, kilde = kilde, ytelse = SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto.Omsorgspenger)
                    JsonDagType.ANDRE_YTELSER_PLEIEPENGER -> SykdomstidslinjeDagDto.AndreYtelserDto(dato = dagen, kilde = kilde, ytelse = SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto.Pleiepenger)
                    JsonDagType.ANDRE_YTELSER_SVANGERSKAPSPENGER -> SykdomstidslinjeDagDto.AndreYtelserDto(dato = dagen, kilde = kilde, ytelse = SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto.Svangerskapspenger)
                    JsonDagType.ANDRE_YTELSER_OPPLÆRINGSPENGER -> SykdomstidslinjeDagDto.AndreYtelserDto(dato = dagen, kilde = kilde, ytelse = SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto.Opplæringspenger)
                    JsonDagType.ANDRE_YTELSER_DAGPENGER -> SykdomstidslinjeDagDto.AndreYtelserDto(dato = dagen, kilde = kilde, ytelse = SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto.Dagpenger)
                    JsonDagType.UKJENT_DAG -> SykdomstidslinjeDagDto.UkjentDagDto(dato = dagen, kilde = kilde)
                }

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
                fun tilDto() = HendelseskildeDto(
                    type = this.type,
                    meldingsreferanseId = this.id,
                    tidsstempel = this.tidsstempel
                )
            }
        }

        data class ForkastetVedtaksperiodeData(
            val vedtaksperiode: VedtaksperiodeData
        ) {
            fun tilDto() = ForkastetVedtaksperiodeInnDto(vedtaksperiode = this.vedtaksperiode.tilDto())
        }

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
            fun tilDto() = FeriepengeInnDto(
                feriepengeberegner = FeriepengeberegnerDto(
                    opptjeningsår = this.opptjeningsår,
                    utbetalteDager = this.utbetalteDager.map { it.tilDto() },
                    feriepengedager = this.feriepengedager.map { it.tilDto() }
                ),
                infotrygdFeriepengebeløpPerson = this.infotrygdFeriepengebeløpPerson,
                infotrygdFeriepengebeløpArbeidsgiver = this.infotrygdFeriepengebeløpArbeidsgiver,
                spleisFeriepengebeløpPerson = this.spleisFeriepengebeløpPerson,
                spleisFeriepengebeløpArbeidsgiver = this.spleisFeriepengebeløpArbeidsgiver,
                oppdrag = this.oppdrag.tilDto(),
                personoppdrag = this.personoppdrag.tilDto(),
                utbetalingId = this.utbetalingId,
                sendTilOppdrag = this.sendTilOppdrag,
                sendPersonoppdragTilOS = this.sendPersonoppdragTilOS
            )
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
                fun tilDto(): UtbetaltDagDto = when (type) {
                    "InfotrygdPersonDag" -> UtbetaltDagDto.InfotrygdPerson(
                        orgnummer = orgnummer,
                        dato = dato,
                        beløp = beløp
                    )
                    "InfotrygdArbeidsgiverDag" -> UtbetaltDagDto.InfotrygdArbeidsgiver(
                        orgnummer = orgnummer,
                        dato = dato,
                        beløp = beløp
                    )
                    "SpleisArbeidsgiverDag" -> UtbetaltDagDto.SpleisArbeidsgiver(
                        orgnummer = orgnummer,
                        dato = dato,
                        beløp = beløp
                    )
                    "SpleisPersonDag" -> UtbetaltDagDto.SpleisPerson(
                        orgnummer = orgnummer,
                        dato = dato,
                        beløp = beløp
                    )
                    else -> error("Ukjent utbetaltdag-type: $type")
                }
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
            fun tilDto() = PeriodeDto(fom = fom, tom = tom)
            internal fun tilPeriode() = fom til tom
        }

        data class VedtaksperiodeData(
            val id: UUID,
            val tilstand: TilstandType,
            val generasjoner: List<GenerasjonData>,
            val opprettet: LocalDateTime,
            val oppdatert: LocalDateTime
        ) {
            fun tilDto() = VedtaksperiodeInnDto(
                id = this.id,
                tilstand = when (tilstand) {
                    TilstandType.AVVENTER_HISTORIKK -> VedtaksperiodetilstandDto.AVVENTER_HISTORIKK
                    TilstandType.AVVENTER_GODKJENNING -> VedtaksperiodetilstandDto.AVVENTER_GODKJENNING
                    TilstandType.AVVENTER_SIMULERING -> VedtaksperiodetilstandDto.AVVENTER_SIMULERING
                    TilstandType.TIL_UTBETALING -> VedtaksperiodetilstandDto.TIL_UTBETALING
                    TilstandType.TIL_INFOTRYGD -> VedtaksperiodetilstandDto.TIL_INFOTRYGD
                    TilstandType.AVSLUTTET -> VedtaksperiodetilstandDto.AVSLUTTET
                    TilstandType.AVSLUTTET_UTEN_UTBETALING -> VedtaksperiodetilstandDto.AVSLUTTET_UTEN_UTBETALING
                    TilstandType.REVURDERING_FEILET -> VedtaksperiodetilstandDto.REVURDERING_FEILET
                    TilstandType.START -> VedtaksperiodetilstandDto.START
                    TilstandType.AVVENTER_INFOTRYGDHISTORIKK -> VedtaksperiodetilstandDto.AVVENTER_INFOTRYGDHISTORIKK
                    TilstandType.AVVENTER_INNTEKTSMELDING -> VedtaksperiodetilstandDto.AVVENTER_INNTEKTSMELDING
                    TilstandType.AVVENTER_BLOKKERENDE_PERIODE -> VedtaksperiodetilstandDto.AVVENTER_BLOKKERENDE_PERIODE
                    TilstandType.AVVENTER_VILKÅRSPRØVING -> VedtaksperiodetilstandDto.AVVENTER_VILKÅRSPRØVING
                    TilstandType.AVVENTER_REVURDERING -> VedtaksperiodetilstandDto.AVVENTER_REVURDERING
                    TilstandType.AVVENTER_HISTORIKK_REVURDERING -> VedtaksperiodetilstandDto.AVVENTER_HISTORIKK_REVURDERING
                    TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING -> VedtaksperiodetilstandDto.AVVENTER_VILKÅRSPRØVING_REVURDERING
                    TilstandType.AVVENTER_SIMULERING_REVURDERING -> VedtaksperiodetilstandDto.AVVENTER_SIMULERING_REVURDERING
                    TilstandType.AVVENTER_GODKJENNING_REVURDERING -> VedtaksperiodetilstandDto.AVVENTER_GODKJENNING_REVURDERING
                },
                generasjoner = GenerasjonerDto(this.generasjoner.map { it.tilDto() }),
                opprettet = opprettet,
                oppdatert = oppdatert
            )
            data class DokumentsporingData(
                val dokumentId: UUID,
                val dokumenttype: DokumentTypeData
            ) {
                fun tilDto() = DokumentsporingDto(
                    id = this.dokumentId,
                    type = when (dokumenttype) {
                        DokumentTypeData.Sykmelding -> DokumenttypeDto.Sykmelding
                        DokumentTypeData.Søknad -> DokumenttypeDto.Søknad
                        DokumentTypeData.InntektsmeldingInntekt -> DokumenttypeDto.InntektsmeldingInntekt
                        DokumentTypeData.InntektsmeldingDager -> DokumenttypeDto.InntektsmeldingDager
                        DokumentTypeData.OverstyrTidslinje -> DokumenttypeDto.OverstyrTidslinje
                        DokumentTypeData.OverstyrInntekt -> DokumenttypeDto.OverstyrInntekt
                        DokumentTypeData.OverstyrRefusjon -> DokumenttypeDto.OverstyrRefusjon
                        DokumentTypeData.OverstyrArbeidsgiveropplysninger -> DokumenttypeDto.OverstyrArbeidsgiveropplysninger
                        DokumentTypeData.OverstyrArbeidsforhold -> DokumenttypeDto.OverstyrArbeidsforhold
                        DokumentTypeData.SkjønnsmessigFastsettelse -> DokumenttypeDto.SkjønnsmessigFastsettelse
                    }
                )
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
                fun tilDto() = GenerasjonDto(
                    id = this.id,
                    tilstand = when (tilstand) {
                        TilstandData.UBEREGNET -> GenerasjonTilstandDto.UBEREGNET
                        TilstandData.UBEREGNET_OMGJØRING -> GenerasjonTilstandDto.UBEREGNET_OMGJØRING
                        TilstandData.UBEREGNET_REVURDERING -> GenerasjonTilstandDto.UBEREGNET_REVURDERING
                        TilstandData.BEREGNET -> GenerasjonTilstandDto.BEREGNET
                        TilstandData.BEREGNET_OMGJØRING -> GenerasjonTilstandDto.BEREGNET_OMGJØRING
                        TilstandData.BEREGNET_REVURDERING -> GenerasjonTilstandDto.BEREGNET_REVURDERING
                        TilstandData.VEDTAK_FATTET -> GenerasjonTilstandDto.VEDTAK_FATTET
                        TilstandData.REVURDERT_VEDTAK_AVVIST -> GenerasjonTilstandDto.REVURDERT_VEDTAK_AVVIST
                        TilstandData.VEDTAK_IVERKSATT -> GenerasjonTilstandDto.VEDTAK_IVERKSATT
                        TilstandData.AVSLUTTET_UTEN_VEDTAK -> GenerasjonTilstandDto.AVSLUTTET_UTEN_VEDTAK
                        TilstandData.TIL_INFOTRYGD -> GenerasjonTilstandDto.TIL_INFOTRYGD
                    },
                    vedtakFattet = this.vedtakFattet,
                    avsluttet = this.avsluttet,
                    kilde = this.kilde.tilDto(),
                    endringer = this.endringer.map { it.tilDto() }
                )
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
                    fun tilDto() = when (this) {
                        SYKMELDT -> AvsenderDto.SYKMELDT
                        ARBEIDSGIVER -> AvsenderDto.ARBEIDSGIVER
                        SAKSBEHANDLER -> AvsenderDto.SAKSBEHANDLER
                        SYSTEM -> AvsenderDto.SYSTEM
                    }

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
                    fun tilDto() = GenerasjonkildeDto(
                        meldingsreferanseId = this.meldingsreferanseId,
                        innsendt = this.innsendt,
                        registert = this.registrert,
                        avsender = this.avsender.tilDto()
                    )
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
                    fun tilDto() = GenerasjonEndringDto(
                        id = this.id,
                        tidsstempel = this.tidsstempel,
                        sykmeldingsperiode = PeriodeDto(fom = sykmeldingsperiodeFom, tom = sykmeldingsperiodeTom),
                        periode = PeriodeDto(fom = this.fom, tom = this.tom),
                        vilkårsgrunnlagId = this.vilkårsgrunnlagId,
                        utbetalingId = this.utbetalingId,
                        dokumentsporing = this.dokumentsporing.tilDto(),
                        sykdomstidslinje = this.sykdomstidslinje.tilDto()
                    )
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
            fun tilDto() = RefusjonDto(
                meldingsreferanseId = this.meldingsreferanseId,
                førsteFraværsdag = this.førsteFraværsdag,
                arbeidsgiverperioder = this.arbeidsgiverperioder.map { it.tilDto() },
                beløp = this.beløp?.let { InntektDto.MånedligDouble(it) },
                sisteRefusjonsdag = this.sisteRefusjonsdag,
                endringerIRefusjon = this.endringerIRefusjon.map { it.tilDto() },
                tidsstempel = this.tidsstempel
            )
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
                fun tilDto() = EndringIRefusjonDto(beløp = InntektDto.MånedligDouble(this.beløp), endringsdato = this.endringsdato)
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

        fun tilDto() = SykdomshistorikkElementDto(
            id = this.id,
            hendelseId = this.hendelseId,
            tidsstempel = this.tidsstempel,
            hendelseSykdomstidslinje = hendelseSykdomstidslinje.tilDto(),
            beregnetSykdomstidslinje = beregnetSykdomstidslinje.tilDto()
        )

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
        val type: UtbetalingtypeData,
        val status: UtbetalingstatusData,
        val maksdato: LocalDate,
        val forbrukteSykedager: Int?,
        val gjenståendeSykedager: Int?,
        val vurdering: VurderingData?,
        val overføringstidspunkt: LocalDateTime?,
        val avstemmingsnøkkel: Long?,
        val avsluttet: LocalDateTime?,
        val oppdatert: LocalDateTime
    ) {
        enum class UtbetalingtypeData { UTBETALING, ETTERUTBETALING, ANNULLERING, REVURDERING, FERIEPENGER }
        enum class UtbetalingstatusData {
            NY,
            IKKE_UTBETALT,
            IKKE_GODKJENT,
            OVERFØRT,
            UTBETALT,
            GODKJENT,
            GODKJENT_UTEN_UTBETALING,
            ANNULLERT,
            FORKASTET
        }
        fun tilDto() = UtbetalingInnDto(
            id = this.id,
            korrelasjonsId = this.korrelasjonsId,
            periode = PeriodeDto(fom = this.fom, tom = this.tom),
            utbetalingstidslinje = this.utbetalingstidslinje.tilDto(),
            arbeidsgiverOppdrag = this.arbeidsgiverOppdrag.tilDto(),
            personOppdrag = this.personOppdrag.tilDto(),
            tidsstempel = this.tidsstempel,
            tilstand = when (status) {
                UtbetalingstatusData.NY -> UtbetalingTilstandDto.NY
                UtbetalingstatusData.IKKE_UTBETALT -> UtbetalingTilstandDto.IKKE_UTBETALT
                UtbetalingstatusData.IKKE_GODKJENT -> UtbetalingTilstandDto.IKKE_GODKJENT
                UtbetalingstatusData.OVERFØRT -> UtbetalingTilstandDto.OVERFØRT
                UtbetalingstatusData.UTBETALT -> UtbetalingTilstandDto.UTBETALT
                UtbetalingstatusData.GODKJENT -> UtbetalingTilstandDto.GODKJENT
                UtbetalingstatusData.GODKJENT_UTEN_UTBETALING -> UtbetalingTilstandDto.GODKJENT_UTEN_UTBETALING
                UtbetalingstatusData.ANNULLERT -> UtbetalingTilstandDto.ANNULLERT
                UtbetalingstatusData.FORKASTET -> UtbetalingTilstandDto.FORKASTET
            },
            type = when (type) {
                UtbetalingtypeData.UTBETALING -> UtbetalingtypeDto.UTBETALING
                UtbetalingtypeData.ETTERUTBETALING -> UtbetalingtypeDto.ETTERUTBETALING
                UtbetalingtypeData.ANNULLERING -> UtbetalingtypeDto.ANNULLERING
                UtbetalingtypeData.REVURDERING -> UtbetalingtypeDto.REVURDERING
                UtbetalingtypeData.FERIEPENGER -> UtbetalingtypeDto.FERIEPENGER
            },
            maksdato = this.maksdato,
            forbrukteSykedager = this.forbrukteSykedager,
            gjenståendeSykedager = this.gjenståendeSykedager,
            annulleringer = this.annulleringer ?: emptyList(),
            vurdering = this.vurdering?.tilDto(),
            overføringstidspunkt = overføringstidspunkt,
            avstemmingsnøkkel = avstemmingsnøkkel,
            avsluttet = avsluttet,
            oppdatert = oppdatert
        )
        internal fun konverterTilUtbetaling(andreUtbetalinger: List<Pair<UUID, Utbetaling>>) = Utbetaling.ferdigUtbetaling(
            id = id,
            korrelasjonsId = korrelasjonsId,
            annulleringer = annulleringer?.map { annulleringen -> andreUtbetalinger.single { it.first == annulleringen }.second } ?: emptyList(),
            opprinneligPeriode = fom til tom,
            utbetalingstidslinje = utbetalingstidslinje.konverterTilUtbetalingstidslinje(),
            arbeidsgiverOppdrag = arbeidsgiverOppdrag.konverterTilOppdrag(),
            personOppdrag = personOppdrag.konverterTilOppdrag(),
            tidsstempel = tidsstempel,
            utbetalingstatus = enumValueOf(status.name),
            utbetalingtype = enumValueOf(type.name),
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
            fun tilDto() = UtbetalingVurderingDto(
                godkjent = godkjent,
                ident = ident,
                epost = epost,
                tidspunkt = tidspunkt,
                automatiskBehandling = automatiskBehandling
            )
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
        val status: OppdragstatusData?,
        val overføringstidspunkt: LocalDateTime?,
        val erSimulert: Boolean,
        val simuleringsResultat: ArbeidsgiverData.VedtaksperiodeData.DataForSimuleringData?
    ) {
        enum class OppdragstatusData { OVERFØRT, AKSEPTERT, AKSEPTERT_MED_FEIL, AVVIST, FEIL }
        fun tilDto() = OppdragInnDto(
            mottaker = this.mottaker,
            fagområde = when (fagområde) {
                "SPREF" -> FagområdeDto.SPREF
                "SP" -> FagområdeDto.SP
                else -> error("Ukjent fagområde: $fagområde")
            },
            linjer = this.linjer.map { it.tilDto() },
            fagsystemId = this.fagsystemId,
            endringskode = when (endringskode) {
                "NY" -> EndringskodeDto.NY
                "ENDR" -> EndringskodeDto.ENDR
                "UEND" -> EndringskodeDto.UEND
                else -> error("Ukjent endringskode: $endringskode")
            },
            nettoBeløp = this.nettoBeløp,
            overføringstidspunkt = this.overføringstidspunkt,
            avstemmingsnøkkel = this.avstemmingsnøkkel,
            status = when (status) {
                OppdragstatusData.OVERFØRT -> OppdragstatusDto.OVERFØRT
                OppdragstatusData.AKSEPTERT -> OppdragstatusDto.AKSEPTERT
                OppdragstatusData.AKSEPTERT_MED_FEIL -> OppdragstatusDto.AKSEPTERT_MED_FEIL
                OppdragstatusData.AVVIST -> OppdragstatusDto.AVVIST
                OppdragstatusData.FEIL -> OppdragstatusDto.FEIL
                null -> null
            },
            tidsstempel = this.tidsstempel,
            erSimulert = this.erSimulert,
            simuleringsResultat = this.simuleringsResultat?.parseDataForSimulering()
        )
        internal fun konverterTilOppdrag(): Oppdrag = Oppdrag.ferdigOppdrag(
            mottaker = mottaker,
            from = Fagområde.from(fagområde),
            utbetalingslinjer = linjer.map { it.konverterTilUtbetalingslinje() },
            fagsystemId = fagsystemId,
            endringskode = Endringskode.valueOf(endringskode),
            nettoBeløp = nettoBeløp,
            overføringstidspunkt = overføringstidspunkt,
            avstemmingsnøkkel = avstemmingsnøkkel,
            status = status?.let { Oppdragstatus.valueOf(it.name) },
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
        fun tilDto() = UtbetalingslinjeInnDto(
            fom = this.fom,
            tom = this.tom,
            satstype = when (this.satstype) {
                "ENG" -> SatstypeDto.Engang
                "DAG" -> SatstypeDto.Daglig
                else -> error("Ukjent satstype: $satstype")
            },
            beløp = this.sats,
            grad = this.grad,
            refFagsystemId = this.refFagsystemId,
            delytelseId = this.delytelseId,
            refDelytelseId = this.refDelytelseId,
            endringskode = when (this.endringskode) {
                "NY" -> EndringskodeDto.NY
                "ENDR" -> EndringskodeDto.ENDR
                "UEND" -> EndringskodeDto.UEND
                else -> error("Ukjent endringskode: $endringskode")
            },
            klassekode = when (this.klassekode) {
                "SPREFAG-IOP" -> KlassekodeDto.RefusjonIkkeOpplysningspliktig
                "SPREFAGFER-IOP" -> KlassekodeDto.RefusjonFeriepengerIkkeOpplysningspliktig
                "SPATORD" -> KlassekodeDto.SykepengerArbeidstakerOrdinær
                "SPATFER" -> KlassekodeDto.SykepengerArbeidstakerFeriepenger
                else -> error("Ukjent klassekode: ${this.klassekode}")
            },
            datoStatusFom = this.datoStatusFom
        )
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
        fun tilDto() = UtbetalingstidslinjeDto(dager = this.dager.flatMap { it.tilDto() })
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

            fun tilDto() = when (this) {
                SykepengedagerOppbrukt -> BegrunnelseDto.SykepengedagerOppbrukt
                SykepengedagerOppbruktOver67 -> BegrunnelseDto.SykepengedagerOppbruktOver67
                MinimumInntekt -> BegrunnelseDto.MinimumInntekt
                MinimumInntektOver67 -> BegrunnelseDto.MinimumInntektOver67
                EgenmeldingUtenforArbeidsgiverperiode -> BegrunnelseDto.EgenmeldingUtenforArbeidsgiverperiode
                MinimumSykdomsgrad -> BegrunnelseDto.MinimumSykdomsgrad
                AndreYtelserAap -> BegrunnelseDto.AndreYtelserAap
                AndreYtelserDagpenger -> BegrunnelseDto.AndreYtelserDagpenger
                AndreYtelserForeldrepenger -> BegrunnelseDto.AndreYtelserForeldrepenger
                AndreYtelserOmsorgspenger -> BegrunnelseDto.AndreYtelserOmsorgspenger
                AndreYtelserOpplaringspenger -> BegrunnelseDto.AndreYtelserOpplaringspenger
                AndreYtelserPleiepenger -> BegrunnelseDto.AndreYtelserPleiepenger
                AndreYtelserSvangerskapspenger -> BegrunnelseDto.AndreYtelserSvangerskapspenger
                EtterDødsdato -> BegrunnelseDto.EtterDødsdato
                ManglerMedlemskap -> BegrunnelseDto.ManglerMedlemskap
                ManglerOpptjening -> BegrunnelseDto.ManglerOpptjening
                Over70 -> BegrunnelseDto.Over70
                NyVilkårsprøvingNødvendig -> BegrunnelseDto.NyVilkårsprøvingNødvendig
            }

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
            val er6GBegrenset: Boolean?,
            val dato: LocalDate?,
            val fom: LocalDate?,
            val tom: LocalDate?
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

            private val datoer: DateRange = if (dato != null) DateRange.Single(dato) else DateRange.Range(fom!!, tom!!)

            private val økonomi get() = builder.build()
            private val økonomiDto = ØkonomiDto(
                grad = ProsentdelDto(grad),
                totalGrad = ProsentdelDto(totalGrad),
                arbeidsgiverRefusjonsbeløp = InntektDto.DagligDouble(this.arbeidsgiverRefusjonsbeløp),
                aktuellDagsinntekt = InntektDto.DagligDouble(this.aktuellDagsinntekt),
                beregningsgrunnlag = InntektDto.DagligDouble(this.beregningsgrunnlag),
                dekningsgrunnlag = InntektDto.DagligDouble(this.dekningsgrunnlag),
                grunnbeløpgrense = this.grunnbeløpgrense?.let { InntektDto.Årlig(it) },
                arbeidsgiverbeløp = this.arbeidsgiverbeløp?.let { InntektDto.DagligDouble(it) },
                personbeløp = this.personbeløp?.let { InntektDto.DagligDouble(it) },
                er6GBegrenset = this.er6GBegrenset
            )

            fun tilDto() = datoer.dates().map { tilDto(it) }
            private fun tilDto(dato: LocalDate): UtbetalingsdagDto {
                return when (type) {
                    TypeData.ArbeidsgiverperiodeDag -> UtbetalingsdagDto.ArbeidsgiverperiodeDagDto(dato = dato, økonomi = økonomiDto)
                    TypeData.NavDag -> UtbetalingsdagDto.NavDagDto(dato = dato, økonomi = økonomiDto)
                    TypeData.NavHelgDag -> UtbetalingsdagDto.NavHelgDagDto(dato = dato, økonomi = økonomiDto)
                    TypeData.Arbeidsdag -> UtbetalingsdagDto.ArbeidsdagDto(dato = dato, økonomi = økonomiDto)
                    TypeData.Fridag -> UtbetalingsdagDto.FridagDto(dato = dato, økonomi = økonomiDto)
                    TypeData.AvvistDag -> UtbetalingsdagDto.AvvistDagDto(dato = dato, økonomi = økonomiDto, begrunnelser = begrunnelser!!.map { it.tilDto() })
                    TypeData.UkjentDag -> UtbetalingsdagDto.UkjentDagDto(dato = dato, økonomi = økonomiDto)
                    TypeData.ForeldetDag -> UtbetalingsdagDto.ForeldetDagDto(dato = dato, økonomi = økonomiDto)
                    TypeData.ArbeidsgiverperiodedagNav -> UtbetalingsdagDto.ArbeidsgiverperiodeDagNavDto(dato = dato, økonomi = økonomiDto)
                }
            }
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
