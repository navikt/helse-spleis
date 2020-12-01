package no.nav.helse.serde

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.helse.person.Person
import no.nav.helse.serde.migration.*

internal val serdeObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
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
            V63EndreUtbetalingId()
        )

        fun gjeldendeVersjon() = JsonMigration.gjeldendeVersjon(migrations)
        fun medSkjemaversjon(jsonNode: JsonNode) = JsonMigration.medSkjemaversjon(migrations, jsonNode)
    }

    val skjemaVersjon = gjeldendeVersjon()

    private fun migrate(jsonNode: JsonNode) {
        try {
            migrations.migrate(jsonNode)
        } catch (err: Exception) {
            throw JsonMigrationException("Feil under migrering: ${err.message}", err)
        }
    }

    fun deserialize(): Person {
        val jsonNode = serdeObjectMapper.readTree(json)

        migrate(jsonNode)

        try {
            val personData: PersonData = requireNotNull(serdeObjectMapper.treeToValue(jsonNode))
            return personData.createPerson()
        } catch (err: Exception) {
            throw DeserializationException("Feil under oversetting til modellobjekter: ${err.message}", err)
        }
    }
}
