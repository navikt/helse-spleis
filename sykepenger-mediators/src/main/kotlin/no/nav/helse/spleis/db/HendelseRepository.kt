package no.nav.helse.spleis.db

import com.github.navikt.tbd_libs.sql_dsl.*
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.serde.migration.Hendelse
import no.nav.helse.spleis.PostgresProbe
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.*
import no.nav.helse.spleis.meldinger.model.*
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

internal class HendelseRepository(
    private val dataSource: DataSource,
) {
    fun lagreMelding(melding: HendelseMessage) {
        melding.lagreMelding(this)
    }

    internal fun lagreMelding(
        melding: HendelseMessage,
        personidentifikator: Personidentifikator,
        meldingId: MeldingsreferanseId,
        json: String,
    ) {
        val meldingtype = meldingstype(melding) ?: return

        @Language("PostgreSQL")
        val sql = "INSERT INTO melding (fnr, melding_id, melding_type, data) VALUES (:fnr, :meldingId, :meldingType, cast(:data as json)) ON CONFLICT(melding_id) DO NOTHING"
        dataSource
            .connection {
                prepareStatementWithNamedParameters(sql) {
                    withParameter("fnr", personidentifikator.toLong())
                    withParameter("meldingId", meldingId.id)
                    withParameter("meldingType", meldingtype.name)
                    withParameter("data", json)
                }.use { stmt ->
                    stmt.execute()
                }
            }.also {
                PostgresProbe.hendelseSkrevetTilDb()
            }
    }

    fun markerSomBehandlet(meldingId: MeldingsreferanseId) {
        @Language("PostgreSQL")
        val sql = "UPDATE melding SET behandlet_tidspunkt=now() WHERE melding_id = cast(:meldingId as text) AND behandlet_tidspunkt IS NULL"
        dataSource.connection {
            prepareStatementWithNamedParameters(sql) {
                withParameter("meldingId", meldingId.id)
            }.use { stmt ->
                stmt.execute()
            }
        }
    }

    fun erBehandlet(meldingId: MeldingsreferanseId) =
        dataSource.connection {
            @Language("PostgreSQL")
            val sql = "SELECT exists(select 1 FROM melding WHERE melding_id = CAST(:meldingId as text) and behandlet_tidspunkt is not null)"
            true ==
                prepareStatementWithNamedParameters(sql) {
                    withParameter("meldingId", meldingId.id)
                }.single { rs -> rs.boolean(1) }
        }

    private fun meldingstype(melding: HendelseMessage) =
        when (melding) {
            is NySøknadMessage -> NY_SØKNAD
            is NyFrilansSøknadMessage -> NY_SØKNAD_FRILANS
            is NySelvstendigSøknadMessage -> NY_SØKNAD_SELVSTENDIG
            is NyArbeidsledigSøknadMessage -> NY_SØKNAD_ARBEIDSLEDIG
            is NyArbeidsledigTidligereArbeidstakerSøknadMessage -> NY_SØKNAD_TIDLIGERE_ARBEIDSTAKER
            is SendtSøknadArbeidsgiverMessage -> SENDT_SØKNAD_ARBEIDSGIVER
            is SendtSøknadNavMessage -> SENDT_SØKNAD_NAV
            is SendtSøknadFrilansMessage -> SENDT_SØKNAD_FRILANS
            is SendtSøknadSelvstendigMessage -> SENDT_SØKNAD_SELVSTENDIG
            is SendtSøknadFiskerMessage -> SENDT_SØKNAD_FISKER
            is SendtSøknadAnnetMessage -> SENDT_SØKNAD_ANNET
            is SendtSøknadArbeidsledigTidligereArbeidstakerMessage -> SENDT_SØKNAD_ARBEIDSLEDIG_TIDLIGERE_ARBEIDSTAKER
            is SendtSøknadArbeidsledigMessage -> SENDT_SØKNAD_ARBEIDSLEDIG
            is NavNoSelvbestemtInntektsmeldingMessage -> NAV_NO_SELVBESTEMT_INNTEKTSMELDING
            is NavNoKorrigertInntektsmeldingMessage -> NAV_NO_KORRIGERT_INNTEKTSMELDING
            is NavNoInntektsmeldingMessage -> NAV_NO_INNTEKTSMELDING
            is YtelserMessage -> YTELSER
            is VilkårsgrunnlagMessage -> VILKÅRSGRUNNLAG
            is SimuleringMessage -> SIMULERING
            is UtbetalingsgodkjenningMessage -> UTBETALINGSGODKJENNING
            is UtbetalingMessage -> UTBETALING
            is FeriepengeutbetalingMessage -> FERIEPENGEUTBETALING
            is AnnulleringMessage -> KANSELLER_UTBETALING
            is GrunnbeløpsreguleringMessage -> GRUNNBELØPSREGULERING
            is OverstyrTidslinjeMessage -> OVERSTYRTIDSLINJE
            is OverstyrArbeidsforholdMessage -> OVERSTYRARBEIDSFORHOLD
            is OverstyrArbeidsgiveropplysningerMessage -> OVERSTYRARBEIDSGIVEROPPLYSNINGER
            is UtbetalingshistorikkForFeriepengerMessage -> UTBETALINGSHISTORIKK_FOR_FERIEPENGER
            is UtbetalingshistorikkEtterInfotrygdendringMessage -> UTBETALINGSHISTORIKK_ETTER_IT_ENDRING
            is DødsmeldingMessage -> DØDSMELDING
            is ForkastSykmeldingsperioderMessage -> FORKAST_SYKMELDINGSPERIODER
            is AnmodningOmForkastingMessage -> ANMODNING_OM_FORKASTING
            is IdentOpphørtMessage -> IDENT_OPPHØRT
            is SkjønnsmessigFastsettelseMessage -> SKJØNNSMESSIG_FASTSETTELSE
            is AvbruttSøknadMessage -> AVBRUTT_SØKNAD
            is InntektsmeldingerReplayMessage -> INNTEKTSMELDINGER_REPLAY
            is MinimumSykdomsgradVurdertMessage -> MINIMUM_SYKDOMSGRAD_VURDERT
            is InntektsopplysningerFraLagretInntektsmeldingMessage -> INNTEKTSOPPLYSNINGER_FRA_LAGRET_INNTEKTSMELDING

            is MigrateMessage,
            is AvstemmingMessage,
            is PersonPåminnelseMessage,
            is PåminnelseMessage,
            is GjenopptaBehandlingMessage,
            is UtbetalingshistorikkMessage,
            is InfotrygdendringMessage,
            is EndretVurderingPåSkjæringstidspunktMessage,
            is EndretGrunnlagForBeregningMessage,
            -> null // Disse trenger vi ikke å lagre
        }

    internal fun hentAlleHendelser(personidentifikator: Personidentifikator): Map<UUID, Hendelse> {
        @Language("PostgreSQL")
        val sql = "SELECT melding_id, melding_type, lest_dato FROM melding WHERE fnr = :fnr"
        return dataSource
            .connection {
                prepareStatementWithNamedParameters(sql) {
                    withParameter("fnr", personidentifikator.toLong())
                }.mapNotNull { row ->
                    Hendelse(
                        meldingsreferanseId = UUID.fromString(row.string("melding_id")),
                        meldingstype = row.string("melding_type"),
                        lestDato = row.offsetDateTime("lest_dato").toLocalDateTime(),
                    )
                }
            }.associateBy { it.meldingsreferanseId }
    }

    internal fun hentInntektsmelding(
        personidentifikator: Personidentifikator,
        meldingId: MeldingsreferanseId,
    ): String? {
        @Language("PostgreSQL")
        val sql = "SELECT data FROM melding WHERE fnr = :fnr AND melding_id = :meldingId AND (melding_type='INNTEKTSMELDING' OR melding_type = 'NAV_NO_SELVBESTEMT_INNTEKTSMELDING' OR melding_type = 'NAV_NO_KORRIGERT_INNTEKTSMELDING' OR melding_type = 'NAV_NO_INNTEKTSMELDING')"
        return dataSource.connection {
            prepareStatementWithNamedParameters(sql) {
                withParameter("fnr", personidentifikator.toLong())
                withParameter("meldingId", meldingId.id.toString())
            }.singleOrNull { row ->
                row.string("data")
            }
        }
    }

    private enum class Meldingstype {
        NY_SØKNAD,
        NY_SØKNAD_FRILANS,
        NY_SØKNAD_SELVSTENDIG,
        NY_SØKNAD_JORDBRUKER,
        NY_SØKNAD_ARBEIDSLEDIG,
        NY_SØKNAD_TIDLIGERE_ARBEIDSTAKER,
        SENDT_SØKNAD_ARBEIDSGIVER,
        SENDT_SØKNAD_NAV,
        SENDT_SØKNAD_FRILANS,
        SENDT_SØKNAD_SELVSTENDIG,
        SENDT_SØKNAD_JORDBRUKER,
        SENDT_SØKNAD_FISKER,
        SENDT_SØKNAD_ANNET,
        SENDT_SØKNAD_ARBEIDSLEDIG,
        SENDT_SØKNAD_ARBEIDSLEDIG_TIDLIGERE_ARBEIDSTAKER,
        INNTEKTSMELDING,
        NAV_NO_SELVBESTEMT_INNTEKTSMELDING,
        NAV_NO_KORRIGERT_INNTEKTSMELDING,
        NAV_NO_INNTEKTSMELDING,
        PÅMINNELSE,
        PERSONPÅMINNELSE,
        OVERSTYRTIDSLINJE,
        OVERSTYRINNTEKT,
        OVERSTYRARBEIDSFORHOLD,
        MANUELL_SAKSBEHANDLING,
        UTBETALINGPÅMINNELSE,
        YTELSER,
        UTBETALINGSGRUNNLAG,
        UTBETALING_OVERFØRT,
        VILKÅRSGRUNNLAG,
        UTBETALINGSGODKJENNING,
        UTBETALING,
        FERIEPENGEUTBETALING,
        SIMULERING,
        ROLLBACK,
        KANSELLER_UTBETALING,
        GRUNNBELØPSREGULERING,
        UTBETALINGSHISTORIKK_FOR_FERIEPENGER,
        UTBETALINGSHISTORIKK_ETTER_IT_ENDRING,
        DØDSMELDING,
        OVERSTYRARBEIDSGIVEROPPLYSNINGER,
        FORKAST_SYKMELDINGSPERIODER,
        ANMODNING_OM_FORKASTING,
        GJENOPPLIV_VILKÅRSGRUNNLAG,
        IDENT_OPPHØRT,
        SKJØNNSMESSIG_FASTSETTELSE,
        AVBRUTT_SØKNAD,
        INNTEKTSMELDINGER_REPLAY,
        MINIMUM_SYKDOMSGRAD_VURDERT,
        SYKEPENGEGRUNNLAG_FOR_ARBEIDSGIVER,
        INNTEKTSOPPLYSNINGER_FRA_LAGRET_INNTEKTSMELDING,
    }
}
