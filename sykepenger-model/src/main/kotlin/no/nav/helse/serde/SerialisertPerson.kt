package no.nav.helse.serde

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.helse.person.Person
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.serde.migration.JsonMigration
import no.nav.helse.serde.migration.JsonMigrationException
import no.nav.helse.serde.migration.MeldingerSupplier
import no.nav.helse.serde.migration.V100SlettEnkeltFeriepengeutbetaling
import no.nav.helse.serde.migration.V101SlettEnkeltFeriepengeutbetaling
import no.nav.helse.serde.migration.V102LeggerFeriepengerSendTilOppdragFelt
import no.nav.helse.serde.migration.V103FjernerFeriepengeUtbetalingerMedTalletNullSomBeløp
import no.nav.helse.serde.migration.V104SlettOpphørMedFeilKlassekode
import no.nav.helse.serde.migration.V105VilkårsgrunnlagMedGenerasjoner
import no.nav.helse.serde.migration.V106FjernerTommeInnslagIVilkårsgrunnlagHistorikken
import no.nav.helse.serde.migration.V107None
import no.nav.helse.serde.migration.V108None
import no.nav.helse.serde.migration.V109UtvidetUtbetalingstidslinjeBeregning
import no.nav.helse.serde.migration.V10EndreNavnPåSykdomstidslinjer
import no.nav.helse.serde.migration.V110SletteArbeidsforhold
import no.nav.helse.serde.migration.V111RiktigStatusAnnullerteUtbetalinger
import no.nav.helse.serde.migration.V112FjernTrailingCarriageReturnFraFagsystemId
import no.nav.helse.serde.migration.V113MigrerVekkFraFjernetTilstandTilAnnullering
import no.nav.helse.serde.migration.V114LagreSykepengegrunnlag
import no.nav.helse.serde.migration.V115LagreVilkårsgrunnlagFraInfotrygd
import no.nav.helse.serde.migration.V116LeggTilRefusjonshistorikk
import no.nav.helse.serde.migration.V117LeggTilGradPåUgyldigeInfotrygdHistorikkPerioder
import no.nav.helse.serde.migration.V118LeggTilBegrensningPåSykepengegrunnlag
import no.nav.helse.serde.migration.V119SletteSkatteopplydsningFraITVilkårsgrunnlag
import no.nav.helse.serde.migration.V11LeggeTilForlengelseFraInfotrygd
import no.nav.helse.serde.migration.V120BrukeInntektsmeldingOverSkattIVilkårsgrunnlag
import no.nav.helse.serde.migration.V121SletteVilkårsgrunnlagUtenNødvendigInntekt
import no.nav.helse.serde.migration.V122FikseNullverdierForAvstemmingsnøkkel
import no.nav.helse.serde.migration.V123FlytteDataInnIOppdrag
import no.nav.helse.serde.migration.V124SetteOppdragSomUendret
import no.nav.helse.serde.migration.V125KorrelasjonsIdPåUtbetaling
import no.nav.helse.serde.migration.V126FjernePaddingPåFagsystemId
import no.nav.helse.serde.migration.V127DefaultArbeidsgiverRefusjonsbeløp
import no.nav.helse.serde.migration.V128FjerneUgyldigSimuleringsResultat
import no.nav.helse.serde.migration.V129KopiereSimuleringsResultatTilOppdrag
import no.nav.helse.serde.migration.V12Aktivitetslogg
import no.nav.helse.serde.migration.V130TrimmeUtbetalingstidslinje
import no.nav.helse.serde.migration.V131FjernHelgedagFraSykdomstidslinje
import no.nav.helse.serde.migration.V132None
import no.nav.helse.serde.migration.V133VilkårsgrunnlagIdPåVilkårsgrunnlag
import no.nav.helse.serde.migration.V134OppdragErSimulert
import no.nav.helse.serde.migration.V135UtbetalingslinjeGrad
import no.nav.helse.serde.migration.V136MigrereTilstanderPåForkastede
import no.nav.helse.serde.migration.V137InntektsmeldingInfoHistorikk
import no.nav.helse.serde.migration.V138SammenligningsgrunnlagPerArbeidsgiverIVilkårsgrunnlag
import no.nav.helse.serde.migration.V139EndreTommeInntektsopplysningerTilIkkeRapportert
import no.nav.helse.serde.migration.V13NettoBeløpIOppdrag
import no.nav.helse.serde.migration.V140OppdaterFelterIArbeidsforhold
import no.nav.helse.serde.migration.V141RenameErAktivtTilDeaktivert
import no.nav.helse.serde.migration.V142DeaktiverteArbeidsforholdPåSykepengegrunnlag
import no.nav.helse.serde.migration.V143DummyMigrerHendelseInnPåVedtaksperiode
import no.nav.helse.serde.migration.V144TyperPåHendelserIVedtaksperiode
import no.nav.helse.serde.migration.V145DummyLagreArbeidsforholdForOpptjening
import no.nav.helse.serde.migration.V146DummyLagreArbeidsforholdForOpptjening
import no.nav.helse.serde.migration.V147LagreArbeidsforholdForOpptjening
import no.nav.helse.serde.migration.V148OpprettSykmeldingsperioder
import no.nav.helse.serde.migration.V149ResetLåstePerioder
import no.nav.helse.serde.migration.V14NettoBeløpIVedtaksperiode
import no.nav.helse.serde.migration.V15ØkonomiSykdomstidslinjer
import no.nav.helse.serde.migration.V16StatusIUtbetaling
import no.nav.helse.serde.migration.V17ForkastedePerioder
import no.nav.helse.serde.migration.V18UtbetalingstidslinjeØkonomi
import no.nav.helse.serde.migration.V19KlippOverlappendeVedtaksperioder
import no.nav.helse.serde.migration.V1EndreKunArbeidsgiverSykedagEnum
import no.nav.helse.serde.migration.V20AvgrensVedtaksperiode
import no.nav.helse.serde.migration.V21FjernGruppeId
import no.nav.helse.serde.migration.V22FjernFelterFraSykdomstidslinje
import no.nav.helse.serde.migration.V23None
import no.nav.helse.serde.migration.V24None
import no.nav.helse.serde.migration.V25ManglendeForlengelseFraInfotrygd
import no.nav.helse.serde.migration.V26SykdomshistorikkMerge
import no.nav.helse.serde.migration.V27CachetSykdomstidslinjePåVedtaksperiode
import no.nav.helse.serde.migration.V28HendelsesIderPåVedtaksperiode
import no.nav.helse.serde.migration.V29LeggerTilInntektsKildeType
import no.nav.helse.serde.migration.V2Medlemskapstatus
import no.nav.helse.serde.migration.V30AvviksprosentSomNullable
import no.nav.helse.serde.migration.V31LeggerTilInntektendringTidsstempel
import no.nav.helse.serde.migration.V32SletterForkastedePerioderUtenHistorikk
import no.nav.helse.serde.migration.V33BehovtypeAktivitetslogg
import no.nav.helse.serde.migration.V34OpprinneligPeriodePåVedtaksperiode
import no.nav.helse.serde.migration.V35ÅrsakTilForkasting
import no.nav.helse.serde.migration.V36BonkersNavnPåForkastedePerioder
import no.nav.helse.serde.migration.V37None
import no.nav.helse.serde.migration.V38InntektshistorikkVol2
import no.nav.helse.serde.migration.V39SetterAutomatiskBehandlingPåVedtaksperiode
import no.nav.helse.serde.migration.V3BeregnerGjenståendeSykedagerFraMaksdato
import no.nav.helse.serde.migration.V40RenamerFørsteFraværsdag
import no.nav.helse.serde.migration.V41RenamerBeregningsdato
import no.nav.helse.serde.migration.V42AnnulleringIUtbetaling
import no.nav.helse.serde.migration.V43RenamerSkjæringstidspunkt
import no.nav.helse.serde.migration.V44MaksdatoIkkeNullable
import no.nav.helse.serde.migration.V45InntektsmeldingId
import no.nav.helse.serde.migration.V46GamleAnnulleringsforsøk
import no.nav.helse.serde.migration.V47BeregnetUtbetalingstidslinjer
import no.nav.helse.serde.migration.V48OppdragTidsstempel
import no.nav.helse.serde.migration.V49UtbetalingId
import no.nav.helse.serde.migration.V4LeggTilNySykdomstidslinje
import no.nav.helse.serde.migration.V50None
import no.nav.helse.serde.migration.V51PatcheGamleAnnulleringer
import no.nav.helse.serde.migration.V52None
import no.nav.helse.serde.migration.V53KnytteVedtaksperiodeTilUtbetaling
import no.nav.helse.serde.migration.V54UtvideUtbetaling
import no.nav.helse.serde.migration.V55UtvideUtbetalingMedVurdering
import no.nav.helse.serde.migration.V56UtvideUtbetalingMedAvstemmingsnøkkel
import no.nav.helse.serde.migration.V57UtbetalingAvsluttet
import no.nav.helse.serde.migration.V58Utbetalingtype
import no.nav.helse.serde.migration.V59UtbetalingNesteForrige
import no.nav.helse.serde.migration.V5BegrensGradTilMellom0Og100
import no.nav.helse.serde.migration.V60FiksStatusPåUtbetalinger
import no.nav.helse.serde.migration.V61MigrereUtbetaltePerioderITilInfotrygd
import no.nav.helse.serde.migration.V62VurderingGodkjentBoolean
import no.nav.helse.serde.migration.V63EndreUtbetalingId
import no.nav.helse.serde.migration.V64None
import no.nav.helse.serde.migration.V65None
import no.nav.helse.serde.migration.V66SjekkeEtterutbetalingerForFeil
import no.nav.helse.serde.migration.V67FeilStatusOgTypePåAnnulleringer
import no.nav.helse.serde.migration.V68FikseØdelagteUtbetalinger
import no.nav.helse.serde.migration.V69SetteOpprettetOgOppdatertTidspunkt
import no.nav.helse.serde.migration.V6LeggTilNySykdomstidslinje
import no.nav.helse.serde.migration.V70SetteOppdatertTidspunkt
import no.nav.helse.serde.migration.V71SetteOpprettetTidspunkt
import no.nav.helse.serde.migration.V72RetteUtbetalingPåEnkeltperson
import no.nav.helse.serde.migration.V73MergeAvventerGapOgAvventerInntektsmeldingFerdigGap
import no.nav.helse.serde.migration.V74SykdomshistorikkElementId
import no.nav.helse.serde.migration.V75UtbetalingstidslinjeberegningId
import no.nav.helse.serde.migration.V76UtbetalingstidslinjeberegningSykdomshistorikkElementId
import no.nav.helse.serde.migration.V77UtbetalingBeregningId
import no.nav.helse.serde.migration.V78VedtaksperiodeListeOverUtbetalinger
import no.nav.helse.serde.migration.V79IdIInntektshistorikk
import no.nav.helse.serde.migration.V7DagsatsSomHeltall
import no.nav.helse.serde.migration.V80InntektskildePåVedtaksperiode
import no.nav.helse.serde.migration.V81ForkastetUtbetalinger
import no.nav.helse.serde.migration.V82VilkårsgrunnlagHistorikk
import no.nav.helse.serde.migration.V83VilkårsvurderingerForInfotrygdperioder
import no.nav.helse.serde.migration.V84Infotrygdhistorikk
import no.nav.helse.serde.migration.V85FjernerAvsluttetUtenUtbetalingMedInntektsmelding
import no.nav.helse.serde.migration.V86ForkastetPeriodeIFjernetState
import no.nav.helse.serde.migration.V87InfotrygdhistorikkStatslønn
import no.nav.helse.serde.migration.V88InfotrygdhistorikkInntekterLagret
import no.nav.helse.serde.migration.V89RetteOppFeilTilstand
import no.nav.helse.serde.migration.V8LeggerTilLønnIUtbetalingslinjer
import no.nav.helse.serde.migration.V90HendelsekildeTidsstempel
import no.nav.helse.serde.migration.V91AvvistDagerBegrunnelser
import no.nav.helse.serde.migration.V92VilkårsvurderingMinimumInntekt
import no.nav.helse.serde.migration.V93MeldingsreferanseIdPåGrunnlagsdata
import no.nav.helse.serde.migration.V94AvvistDagerBegrunnelser2
import no.nav.helse.serde.migration.V95ArbeidsforholdId
import no.nav.helse.serde.migration.V96RetteOppFeilUtbetalingPeker
import no.nav.helse.serde.migration.V97RenameDagsatsTilSats
import no.nav.helse.serde.migration.V98SletterITCacheMedUtbetalingsperioder
import no.nav.helse.serde.migration.V99LeggerTilSatstypePåUtbetalingslinjene
import no.nav.helse.serde.migration.V9FjernerGamleSykdomstidslinjer
import no.nav.helse.serde.migration.migrate

internal val serdeObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .registerModule(SimpleModule().addSerializer(SetSerializer(Set::class.java)))
    .registerModule(SimpleModule().addDeserializer(Set::class.java, SetDeserializer(Set::class.java)))
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

class SerialisertPerson(val json: String) {
    internal companion object {
        private val migrations = listOf(
            V1EndreKunArbeidsgiverSykedagEnum(),
            V2Medlemskapstatus(),
            V3BeregnerGjenståendeSykedagerFraMaksdato(),
            V4LeggTilNySykdomstidslinje(),
            V5BegrensGradTilMellom0Og100(),
            V6LeggTilNySykdomstidslinje(),
            V7DagsatsSomHeltall(),
            V8LeggerTilLønnIUtbetalingslinjer(),
            V9FjernerGamleSykdomstidslinjer(),
            V10EndreNavnPåSykdomstidslinjer(),
            V11LeggeTilForlengelseFraInfotrygd(),
            V12Aktivitetslogg(),
            V13NettoBeløpIOppdrag(),
            V14NettoBeløpIVedtaksperiode(),
            V15ØkonomiSykdomstidslinjer(),
            V16StatusIUtbetaling(),
            V17ForkastedePerioder(),
            V18UtbetalingstidslinjeØkonomi(),
            V19KlippOverlappendeVedtaksperioder(),
            V20AvgrensVedtaksperiode(),
            V21FjernGruppeId(),
            V22FjernFelterFraSykdomstidslinje(),
            V23None(),
            V24None(),
            V25ManglendeForlengelseFraInfotrygd(),
            V26SykdomshistorikkMerge(),
            V27CachetSykdomstidslinjePåVedtaksperiode(),
            V28HendelsesIderPåVedtaksperiode(),
            V29LeggerTilInntektsKildeType(),
            V30AvviksprosentSomNullable(),
            V31LeggerTilInntektendringTidsstempel(),
            V32SletterForkastedePerioderUtenHistorikk(),
            V33BehovtypeAktivitetslogg(),
            V34OpprinneligPeriodePåVedtaksperiode(),
            V35ÅrsakTilForkasting(),
            V36BonkersNavnPåForkastedePerioder(),
            V37None(),
            V38InntektshistorikkVol2(),
            V39SetterAutomatiskBehandlingPåVedtaksperiode(),
            V40RenamerFørsteFraværsdag(),
            V41RenamerBeregningsdato(),
            V42AnnulleringIUtbetaling(),
            V43RenamerSkjæringstidspunkt(),
            V44MaksdatoIkkeNullable(),
            V45InntektsmeldingId(),
            V46GamleAnnulleringsforsøk(),
            V47BeregnetUtbetalingstidslinjer(),
            V48OppdragTidsstempel(),
            V49UtbetalingId(),
            V50None(),
            V51PatcheGamleAnnulleringer(),
            V52None(),
            V53KnytteVedtaksperiodeTilUtbetaling(),
            V54UtvideUtbetaling(),
            V55UtvideUtbetalingMedVurdering(),
            V56UtvideUtbetalingMedAvstemmingsnøkkel(),
            V57UtbetalingAvsluttet(),
            V58Utbetalingtype(),
            V59UtbetalingNesteForrige(),
            V60FiksStatusPåUtbetalinger(),
            V61MigrereUtbetaltePerioderITilInfotrygd(),
            V62VurderingGodkjentBoolean(),
            V63EndreUtbetalingId(),
            V64None(),
            V65None(),
            V66SjekkeEtterutbetalingerForFeil(),
            V67FeilStatusOgTypePåAnnulleringer(),
            V68FikseØdelagteUtbetalinger(),
            V69SetteOpprettetOgOppdatertTidspunkt(),
            V70SetteOppdatertTidspunkt(),
            V71SetteOpprettetTidspunkt(),
            V72RetteUtbetalingPåEnkeltperson(),
            V73MergeAvventerGapOgAvventerInntektsmeldingFerdigGap(),
            V74SykdomshistorikkElementId(),
            V75UtbetalingstidslinjeberegningId(),
            V76UtbetalingstidslinjeberegningSykdomshistorikkElementId(),
            V77UtbetalingBeregningId(),
            V78VedtaksperiodeListeOverUtbetalinger(),
            V79IdIInntektshistorikk(),
            V80InntektskildePåVedtaksperiode(),
            V81ForkastetUtbetalinger(),
            V82VilkårsgrunnlagHistorikk(),
            V83VilkårsvurderingerForInfotrygdperioder(),
            V84Infotrygdhistorikk(),
            V85FjernerAvsluttetUtenUtbetalingMedInntektsmelding(),
            V86ForkastetPeriodeIFjernetState(),
            V87InfotrygdhistorikkStatslønn(),
            V88InfotrygdhistorikkInntekterLagret(),
            V89RetteOppFeilTilstand(),
            V90HendelsekildeTidsstempel(),
            V91AvvistDagerBegrunnelser(),
            V92VilkårsvurderingMinimumInntekt(),
            V93MeldingsreferanseIdPåGrunnlagsdata(),
            V94AvvistDagerBegrunnelser2(),
            V95ArbeidsforholdId(),
            V96RetteOppFeilUtbetalingPeker(),
            V97RenameDagsatsTilSats(),
            V98SletterITCacheMedUtbetalingsperioder(),
            V99LeggerTilSatstypePåUtbetalingslinjene(),
            V100SlettEnkeltFeriepengeutbetaling(),
            V101SlettEnkeltFeriepengeutbetaling(),
            V102LeggerFeriepengerSendTilOppdragFelt(),
            V103FjernerFeriepengeUtbetalingerMedTalletNullSomBeløp(),
            V104SlettOpphørMedFeilKlassekode(),
            V105VilkårsgrunnlagMedGenerasjoner(),
            V106FjernerTommeInnslagIVilkårsgrunnlagHistorikken(),
            V107None(),
            V108None(),
            V109UtvidetUtbetalingstidslinjeBeregning(),
            V110SletteArbeidsforhold(),
            V111RiktigStatusAnnullerteUtbetalinger(),
            V112FjernTrailingCarriageReturnFraFagsystemId(),
            V113MigrerVekkFraFjernetTilstandTilAnnullering(),
            V114LagreSykepengegrunnlag(),
            V115LagreVilkårsgrunnlagFraInfotrygd(),
            V116LeggTilRefusjonshistorikk(),
            V117LeggTilGradPåUgyldigeInfotrygdHistorikkPerioder(),
            V118LeggTilBegrensningPåSykepengegrunnlag(),
            V119SletteSkatteopplydsningFraITVilkårsgrunnlag(),
            V120BrukeInntektsmeldingOverSkattIVilkårsgrunnlag(),
            V121SletteVilkårsgrunnlagUtenNødvendigInntekt(),
            V122FikseNullverdierForAvstemmingsnøkkel(),
            V123FlytteDataInnIOppdrag(),
            V124SetteOppdragSomUendret(),
            V125KorrelasjonsIdPåUtbetaling(),
            V126FjernePaddingPåFagsystemId(),
            V127DefaultArbeidsgiverRefusjonsbeløp(),
            V128FjerneUgyldigSimuleringsResultat(),
            V129KopiereSimuleringsResultatTilOppdrag(),
            V130TrimmeUtbetalingstidslinje(),
            V131FjernHelgedagFraSykdomstidslinje(),
            V132None(),
            V133VilkårsgrunnlagIdPåVilkårsgrunnlag(),
            V134OppdragErSimulert(),
            V135UtbetalingslinjeGrad(),
            V136MigrereTilstanderPåForkastede(),
            V137InntektsmeldingInfoHistorikk(),
            V138SammenligningsgrunnlagPerArbeidsgiverIVilkårsgrunnlag(),
            V139EndreTommeInntektsopplysningerTilIkkeRapportert(),
            V140OppdaterFelterIArbeidsforhold(),
            V141RenameErAktivtTilDeaktivert(),
            V142DeaktiverteArbeidsforholdPåSykepengegrunnlag(),
            V143DummyMigrerHendelseInnPåVedtaksperiode(),
            V144TyperPåHendelserIVedtaksperiode(),
            V145DummyLagreArbeidsforholdForOpptjening(),
            V146DummyLagreArbeidsforholdForOpptjening(),
            V147LagreArbeidsforholdForOpptjening(),
            V148OpprettSykmeldingsperioder(),
            V149ResetLåstePerioder()
        )

        fun gjeldendeVersjon() = JsonMigration.gjeldendeVersjon(migrations)
        fun medSkjemaversjon(jsonNode: JsonNode) = JsonMigration.medSkjemaversjon(migrations, jsonNode)
    }

    fun metrikker(): List<Metrikk> = Metrikk.metrikkerAv(
        json,
        listOf("aktivitetslogg"),
        listOf("aktivitetslogg", "aktiviteter"),
        listOf("aktivitetslogg", "kontekster"),
        listOf("arbeidsgivere"),
        listOf("arbeidsgivere", "sykdomshistorikk"),
        listOf("vilkårsgrunnlagHistorikk"),
        listOf("infotrygdhistorikk")
    )

    val skjemaVersjon = gjeldendeVersjon()

    private fun migrate(jsonNode: JsonNode, meldingerSupplier: MeldingerSupplier) {
        try {
            migrations.migrate(jsonNode, meldingerSupplier)
        } catch (err: Exception) {
            throw JsonMigrationException("Feil under migrering: ${err.message}", err)
        }
    }

    fun deserialize(jurist: MaskinellJurist, meldingerSupplier: MeldingerSupplier = MeldingerSupplier.empty): Person {
        val jsonNode = serdeObjectMapper.readTree(json)

        migrate(jsonNode, meldingerSupplier)

        try {
            val personData: PersonData = requireNotNull(serdeObjectMapper.treeToValue(jsonNode))
            return personData.createPerson(jurist)
        } catch (err: Exception) {
            throw DeserializationException("Feil under oversetting til modellobjekter: ${err.message}", err)
        }
    }
}
