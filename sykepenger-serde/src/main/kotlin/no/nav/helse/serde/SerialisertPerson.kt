package no.nav.helse.serde

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.dto.deserialisering.PersonInnDto
import no.nav.helse.serde.migration.JsonMigration
import no.nav.helse.serde.migration.JsonMigrationException
import no.nav.helse.serde.migration.MeldingerSupplier
import no.nav.helse.serde.migration.V279AvsluttettidspunktVedtakFattet
import no.nav.helse.serde.migration.V280HengendeRevurderinger
import no.nav.helse.serde.migration.V281ForkasteAvsluttedePerioderMedUberegnetGenerasjon
import no.nav.helse.serde.migration.V282HengendeRevurderinger
import no.nav.helse.serde.migration.V283BeregningsgrunnlagPåØkonomi
import no.nav.helse.serde.migration.V284GjelderPeriodeArbeidsgiverInntektsopplysning
import no.nav.helse.serde.migration.V285LoggeRareAnnulleringer
import no.nav.helse.serde.migration.V286AnnullerteÅpneRevurderinger
import no.nav.helse.serde.migration.V287AnnullerteÅpneRevurderingerEnGangTil
import no.nav.helse.serde.migration.V288FjerneOverflødigeUberegnedeRevurderinger
import no.nav.helse.serde.migration.V289AvsluttetTidspunktForkastedeGenerasjoner
import no.nav.helse.serde.migration.V290FikseForbrukteDagerSomErNull
import no.nav.helse.serde.migration.V291FikseTidligereOmgjøringerSomErRevurderingFeilet
import no.nav.helse.serde.migration.V292AnnullertPeriode
import no.nav.helse.serde.migration.V293AvsluttetUberegnedeOmgjøringer
import no.nav.helse.serde.migration.V294RenameTilBehandlinger
import no.nav.helse.serde.migration.V295BumpVersjon
import no.nav.helse.serde.migration.V296SkjæringstidspunktPåBehandlinger
import no.nav.helse.serde.migration.V297IdentifiserFerdigBehandledePerioderMedÅpenBehandling
import no.nav.helse.serde.migration.V298EgenmeldingsdagerPåVedtaksperiode
import no.nav.helse.serde.migration.V299EgenmeldingerFraSykdomstidslinjeTilVedtaksperiode
import no.nav.helse.serde.migration.V302MaksdatoresultatPåBehandling
import no.nav.helse.serde.migration.V303KopiereMaksdatoFraUtbetalingTilBehandling
import no.nav.helse.serde.migration.V304FjerneArbeidsledigSykmeldingsperioder
import no.nav.helse.serde.migration.V305RenameSykepengegrunnlagTilInntektsgrunnlag
import no.nav.helse.serde.migration.V306RefusjonstidslinjePåBehandling
import no.nav.helse.serde.migration.V307RefusjonstidslinjePåBehandlingsendring
import no.nav.helse.serde.migration.V308HendelseIdPåInfotrygdhistorikk
import no.nav.helse.serde.migration.V309LeggeTilUbrukteRefusjonsopplysninger
import no.nav.helse.serde.migration.V311AvsenderOgTidsstempelPåRefusjonsopplysning
import no.nav.helse.serde.migration.V312AvsenderOgTidsstempelPåRefusjonsopplysningForDeaktiverteArbeidsforhold
import no.nav.helse.serde.migration.V313OppryddingIUbrukteRefusjonsopplysninger
import no.nav.helse.serde.migration.V314SkjæringstidspunkterSomListe
import no.nav.helse.serde.migration.V315AuuerSomIkkeRapportertInntekt
import no.nav.helse.serde.migration.V316BumpVersjon
import no.nav.helse.serde.migration.V317SykNavIEgenListe
import no.nav.helse.serde.migration.V318FjerneSykNavSomSykdomstidslinjedagtype
import no.nav.helse.serde.migration.V319BumpVersjon
import no.nav.helse.serde.migration.V320ManglendeRefusjonsopplysninger
import no.nav.helse.serde.migration.V321EgenmeldingsdagerPåBehandling
import no.nav.helse.serde.migration.V322YrkesaktivitetstypePåArbeidsgiver
import no.nav.helse.serde.migration.V323FikseFomPåUtbetalinger
import no.nav.helse.serde.migration.V324FjerneLinjerMed0KrSats
import no.nav.helse.serde.migration.V325InntektjusteringPåØkonomi
import no.nav.helse.serde.migration.V326DekningsgradPåØkonomi
import no.nav.helse.serde.migration.V327ProsenterSomTallMellomNullOgEn
import no.nav.helse.serde.migration.V328InntektjusteringerPåBehandling
import no.nav.helse.serde.migration.V329ArbeidssituasjonPåBehandling
import no.nav.helse.serde.migration.V330EpochSomArbeidsgiverperiodeForInfotrygdsaker
import no.nav.helse.serde.migration.V331ArbeidsgiverperiodeFerdigAvklartBoolean
import no.nav.helse.serde.migration.migrate

class SerialisertPerson(
    val json: String,
    val skjemaVersjon: Int
) {
    companion object {
        private val migrations = listOf(
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
            V331ArbeidsgiverperiodeFerdigAvklartBoolean()
        )

        fun gjeldendeVersjon() = JsonMigration.gjeldendeVersjon(migrations)
        fun fraJson(json: String): SerialisertPerson {
            val skjemaversjon = serdeObjectMapper.readTree(json).path("skjemaVersjon").intValue()
            return SerialisertPerson(json, skjemaversjon)
        }
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
            throw DeserializationException("Feil under oversetting til modellobjekter fra skjemaversjon $skjemaVersjon til $nySkjemaverjon: ${err.message}", err)
        }
    }
}
