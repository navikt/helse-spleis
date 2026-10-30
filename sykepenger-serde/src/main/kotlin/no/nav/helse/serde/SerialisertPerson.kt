package no.nav.helse.serde

import no.nav.helse.dto.deserialisering.PersonInnDto
import no.nav.helse.serde.migration.*
import tools.jackson.module.kotlin.readValue

class SerialisertPerson(
    val json: String,
    val skjemaVersjon: Int,
) {
    companion object {
        private val migrations =
            listOf(
                V279AvsluttettidspunktVedtakFattet(),
                V280HengendeRevurderinger(),
                V281ForkasteAvsluttedePerioderMedUberegnetGenerasjon(),
                V282HengendeRevurderinger(),
                V283BeregningsgrunnlagPåØkonomi(),
                V284GjelderPeriodeArbeidsgiverInntektsopplysning(),
                V285LoggeRareAnnulleringer(),
                V286AnnullerteÅpneRevurderinger(),
                V287AnnullerteÅpneRevurderingerEnGangTil(),
                V288FjerneOverflødigeUberegnedeRevurderinger(),
                V289AvsluttetTidspunktForkastedeGenerasjoner(),
                V290FikseForbrukteDagerSomErNull(),
                V291FikseTidligereOmgjøringerSomErRevurderingFeilet(),
                V292AnnullertPeriode(),
                V293AvsluttetUberegnedeOmgjøringer(),
                V294RenameTilBehandlinger(),
                V295BumpVersjon(),
                V296SkjæringstidspunktPåBehandlinger(),
                V297IdentifiserFerdigBehandledePerioderMedÅpenBehandling(),
                V298EgenmeldingsdagerPåVedtaksperiode(),
                V299EgenmeldingerFraSykdomstidslinjeTilVedtaksperiode(),
                V302MaksdatoresultatPåBehandling(),
                V303KopiereMaksdatoFraUtbetalingTilBehandling(),
                V304FjerneArbeidsledigSykmeldingsperioder(),
                V305RenameSykepengegrunnlagTilInntektsgrunnlag(),
                V306RefusjonstidslinjePåBehandling(),
                V307RefusjonstidslinjePåBehandlingsendring(),
                V308HendelseIdPåInfotrygdhistorikk(),
                V309LeggeTilUbrukteRefusjonsopplysninger(),
                V311AvsenderOgTidsstempelPåRefusjonsopplysning(),
                V312AvsenderOgTidsstempelPåRefusjonsopplysningForDeaktiverteArbeidsforhold(),
                V313OppryddingIUbrukteRefusjonsopplysninger(),
                V314SkjæringstidspunkterSomListe(),
                V315AuuerSomIkkeRapportertInntekt(),
                V316BumpVersjon(),
                V317SykNavIEgenListe(),
                V318FjerneSykNavSomSykdomstidslinjedagtype(),
                V319BumpVersjon(),
                V320ManglendeRefusjonsopplysninger(),
                V321EgenmeldingsdagerPåBehandling(),
                V322YrkesaktivitetstypePåArbeidsgiver(),
                V323FikseFomPåUtbetalinger(),
                V324FjerneLinjerMed0KrSats(),
                V325InntektjusteringPåØkonomi(),
                V326DekningsgradPåØkonomi(),
                V327ProsenterSomTallMellomNullOgEn(),
                V328InntektjusteringerPåBehandling(),
                V329ArbeidssituasjonPåBehandling(),
                V330EpochSomArbeidsgiverperiodeForInfotrygdsaker(),
                V331ArbeidsgiverperiodeFerdigAvklartBoolean(),
                V332ArbeidsgiverperiodeFerdigAvklartBoolean(),
                V333ArbeidsgiverperioderPåYrkesaktivitet(),
                V334ArbeidsgiverperioderKunPåArbeidstakere(),
                V335PerioderUtenNavAnsvar(),
                V336PerioderUtenNavAnsvarPåBehandling(),
                V337FjernerRevurderingFeilet(),
                V339JustereFomPåSpesifikkeUtbetalinger(),
                V340KonsekventNavngivningFaktaavklartInntekt(),
                V341KonsekventNavngivningFaktaavklartInntektPåDeaktiverteArbeidsforhold(),
                V342RenameAvventersøknadForTidligereEllerOverlappendePeriode(),
                V343JordbrukerTilSelvstendigYrkesaktivitetstype(),
                V344LeggeTilBeregningId(),
                V345LeggeTilOpptjeningsvurderingId(),
                V346OverskrivOpptjeningsvurderingId(),
                V347RenameSelvstendigAvsluttetTilAvsluttet(),
                V348LeggeTilMedlemskapsvurderingId(),
                V349LeggeTilRettighetskategoriId(),
            )

        fun gjeldendeVersjon() = JsonMigration.gjeldendeVersjon(migrations)
    }

    private fun migrate(meldingerSupplier: MeldingerSupplier): Pair<Int, String> {
        try {
            return migrations.migrate(skjemaVersjon, json, meldingerSupplier)
        } catch (err: Exception) {
            throw JsonMigrationException("Feil under migrering: ${err.message}", err)
        }
    }

    fun tilPersonDto(meldingerSupplier: MeldingerSupplier = MeldingerSupplier.empty): PersonInnDto {
        val (nySkjemaverjon, migratedJson) = migrate(meldingerSupplier)

        try {
            val personData = serdeObjectMapper.readValue<PersonData>(migratedJson)
            return personData.tilPersonDto()
        } catch (err: Exception) {
            throw DeserializationException(
                "Feil under oversetting til modellobjekter fra skjemaversjon $skjemaVersjon til $nySkjemaverjon: ${err.message}",
                err,
            )
        }
    }
}
