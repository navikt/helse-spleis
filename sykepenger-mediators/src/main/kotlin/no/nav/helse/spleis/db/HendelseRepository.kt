package no.nav.helse.spleis.db

import com.github.navikt.tbd_libs.sql_dsl.boolean
import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.mapNotNull
import com.github.navikt.tbd_libs.sql_dsl.offsetDateTime
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import com.github.navikt.tbd_libs.sql_dsl.single
import com.github.navikt.tbd_libs.sql_dsl.string
import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.serde.migration.Hendelse
import no.nav.helse.spleis.PostgresProbe
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.ANMODNING_OM_FORKASTING
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.AVBRUTT_SØKNAD
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.DØDSMELDING
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.FERIEPENGEUTBETALING
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.FORKAST_SYKMELDINGSPERIODER
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.GRUNNBELØPSREGULERING
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.IDENT_OPPHØRT
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.INNTEKTSMELDING
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.INNTEKTSMELDINGER_REPLAY
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.KANSELLER_UTBETALING
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.MINIMUM_SYKDOMSGRAD_VURDERT
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.NAV_NO_INNTEKTSMELDING
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.NAV_NO_KORRIGERT_INNTEKTSMELDING
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.NAV_NO_SELVBESTEMT_INNTEKTSMELDING
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.NY_SØKNAD
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.NY_SØKNAD_ARBEIDSLEDIG
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.NY_SØKNAD_FRILANS
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.NY_SØKNAD_SELVSTENDIG
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.NY_SØKNAD_TIDLIGERE_ARBEIDSTAKER
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.OVERSTYRARBEIDSFORHOLD
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.OVERSTYRARBEIDSGIVEROPPLYSNINGER
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.OVERSTYRTIDSLINJE
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.SENDT_SØKNAD_ARBEIDSGIVER
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.SENDT_SØKNAD_ARBEIDSLEDIG
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.SENDT_SØKNAD_ARBEIDSLEDIG_TIDLIGERE_ARBEIDSTAKER
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.SENDT_SØKNAD_FRILANS
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.SENDT_SØKNAD_NAV
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.SENDT_SØKNAD_SELVSTENDIG
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.SIMULERING
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.SKJØNNSMESSIG_FASTSETTELSE
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.SYKEPENGEGRUNNLAG_FOR_ARBEIDSGIVER
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.UTBETALING
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.UTBETALINGPÅMINNELSE
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.UTBETALINGSGODKJENNING
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.UTBETALINGSHISTORIKK_ETTER_IT_ENDRING
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.UTBETALINGSHISTORIKK_FOR_FERIEPENGER
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.VILKÅRSGRUNNLAG
import no.nav.helse.spleis.db.HendelseRepository.Meldingstype.YTELSER
import no.nav.helse.spleis.meldinger.model.AnmodningOmForkastingMessage
import no.nav.helse.spleis.meldinger.model.AnnulleringMessage
import no.nav.helse.spleis.meldinger.model.AvbruttArbeidsledigSøknadMessage
import no.nav.helse.spleis.meldinger.model.AvbruttArbeidsledigTidligereArbeidstakerSøknadMessage
import no.nav.helse.spleis.meldinger.model.AvbruttSøknadMessage
import no.nav.helse.spleis.meldinger.model.AvstemmingMessage
import no.nav.helse.spleis.meldinger.model.DødsmeldingMessage
import no.nav.helse.spleis.meldinger.model.FeriepengeutbetalingMessage
import no.nav.helse.spleis.meldinger.model.ForkastSykmeldingsperioderMessage
import no.nav.helse.spleis.meldinger.model.GrunnbeløpsreguleringMessage
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import no.nav.helse.spleis.meldinger.model.IdentOpphørtMessage
import no.nav.helse.spleis.meldinger.model.InfotrygdendringMessage
import no.nav.helse.spleis.meldinger.model.InntektsendringerMessage
import no.nav.helse.spleis.meldinger.model.InntektsmeldingMessage
import no.nav.helse.spleis.meldinger.model.InntektsmeldingerReplayMessage
import no.nav.helse.spleis.meldinger.model.MigrateMessage
import no.nav.helse.spleis.meldinger.model.MinimumSykdomsgradVurdertMessage
import no.nav.helse.spleis.meldinger.model.NavNoInntektsmeldingMessage
import no.nav.helse.spleis.meldinger.model.NavNoKorrigertInntektsmeldingMessage
import no.nav.helse.spleis.meldinger.model.NavNoSelvbestemtInntektsmeldingMessage
import no.nav.helse.spleis.meldinger.model.NyArbeidsledigSøknadMessage
import no.nav.helse.spleis.meldinger.model.NyArbeidsledigTidligereArbeidstakerSøknadMessage
import no.nav.helse.spleis.meldinger.model.NyFrilansSøknadMessage
import no.nav.helse.spleis.meldinger.model.NySelvstendigSøknadMessage
import no.nav.helse.spleis.meldinger.model.NySøknadMessage
import no.nav.helse.spleis.meldinger.model.OverstyrArbeidsforholdMessage
import no.nav.helse.spleis.meldinger.model.OverstyrArbeidsgiveropplysningerMessage
import no.nav.helse.spleis.meldinger.model.OverstyrTidslinjeMessage
import no.nav.helse.spleis.meldinger.model.PersonPåminnelseMessage
import no.nav.helse.spleis.meldinger.model.PåminnelseMessage
import no.nav.helse.spleis.meldinger.model.SendtSøknadArbeidsgiverMessage
import no.nav.helse.spleis.meldinger.model.SendtSøknadArbeidsledigMessage
import no.nav.helse.spleis.meldinger.model.SendtSøknadArbeidsledigTidligereArbeidstakerMessage
import no.nav.helse.spleis.meldinger.model.SendtSøknadFrilansMessage
import no.nav.helse.spleis.meldinger.model.SendtSøknadNavMessage
import no.nav.helse.spleis.meldinger.model.SendtSøknadSelvstendigMessage
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.helse.spleis.meldinger.model.SkjønnsmessigFastsettelseMessage
import no.nav.helse.spleis.meldinger.model.SykepengegrunnlagForArbeidsgiverMessage
import no.nav.helse.spleis.meldinger.model.UtbetalingMessage
import no.nav.helse.spleis.meldinger.model.UtbetalingpåminnelseMessage
import no.nav.helse.spleis.meldinger.model.UtbetalingsgodkjenningMessage
import no.nav.helse.spleis.meldinger.model.UtbetalingshistorikkEtterInfotrygdendringMessage
import no.nav.helse.spleis.meldinger.model.UtbetalingshistorikkForFeriepengerMessage
import no.nav.helse.spleis.meldinger.model.UtbetalingshistorikkMessage
import no.nav.helse.spleis.meldinger.model.VilkårsgrunnlagMessage
import no.nav.helse.spleis.meldinger.model.YtelserMessage
import org.intellij.lang.annotations.Language

internal class HendelseRepository(private val dataSource: DataSource) {
    fun lagreMelding(melding: HendelseMessage) {
        melding.lagreMelding(this)
    }

    internal fun lagreMelding(melding: HendelseMessage, personidentifikator: Personidentifikator, meldingId: MeldingsreferanseId, json: String) {
        val meldingtype = meldingstype(melding) ?: return

        @Language("PostgreSQL")
        val sql = "INSERT INTO melding (fnr, melding_id, melding_type, data) VALUES (:fnr, :meldingId, :meldingType, cast(:data as json)) ON CONFLICT(melding_id) DO NOTHING"
        dataSource.connection {
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

    fun erBehandlet(meldingId: MeldingsreferanseId) = dataSource.connection {
        @Language("PostgreSQL")
        val sql = "SELECT exists(select 1 FROM melding WHERE melding_id = CAST(:meldingId as text) and behandlet_tidspunkt is not null)"
        true == prepareStatementWithNamedParameters(sql) {
            withParameter("meldingId", meldingId.id)
        }.single { rs -> rs.boolean(1) }
    }

    private fun meldingstype(melding: HendelseMessage) = when (melding) {
        is NySøknadMessage -> NY_SØKNAD
        is NyFrilansSøknadMessage -> NY_SØKNAD_FRILANS
        is NySelvstendigSøknadMessage -> NY_SØKNAD_SELVSTENDIG
        is NyArbeidsledigSøknadMessage -> NY_SØKNAD_ARBEIDSLEDIG
        is NyArbeidsledigTidligereArbeidstakerSøknadMessage -> NY_SØKNAD_TIDLIGERE_ARBEIDSTAKER
        is SendtSøknadArbeidsgiverMessage -> SENDT_SØKNAD_ARBEIDSGIVER
        is SendtSøknadNavMessage -> SENDT_SØKNAD_NAV
        is SendtSøknadFrilansMessage -> SENDT_SØKNAD_FRILANS
        is SendtSøknadSelvstendigMessage -> SENDT_SØKNAD_SELVSTENDIG
        is SendtSøknadArbeidsledigTidligereArbeidstakerMessage -> SENDT_SØKNAD_ARBEIDSLEDIG_TIDLIGERE_ARBEIDSTAKER
        is SendtSøknadArbeidsledigMessage -> SENDT_SØKNAD_ARBEIDSLEDIG
        is NavNoSelvbestemtInntektsmeldingMessage -> NAV_NO_SELVBESTEMT_INNTEKTSMELDING
        is NavNoKorrigertInntektsmeldingMessage -> NAV_NO_KORRIGERT_INNTEKTSMELDING
        is NavNoInntektsmeldingMessage -> NAV_NO_INNTEKTSMELDING
        is InntektsmeldingMessage -> INNTEKTSMELDING
        is UtbetalingpåminnelseMessage -> UTBETALINGPÅMINNELSE
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
        is SykepengegrunnlagForArbeidsgiverMessage -> SYKEPENGEGRUNNLAG_FOR_ARBEIDSGIVER
        is MigrateMessage,
        is AvstemmingMessage,
        is PersonPåminnelseMessage,
        is PåminnelseMessage,
        is UtbetalingshistorikkMessage,
        is InfotrygdendringMessage,
        is InntektsendringerMessage,
        is AvbruttArbeidsledigTidligereArbeidstakerSøknadMessage,
        is AvbruttArbeidsledigSøknadMessage -> null // Disse trenger vi ikke å lagre

    }

    internal fun hentAlleHendelser(personidentifikator: Personidentifikator): Map<UUID, Hendelse> {
        @Language("PostgreSQL")
        val sql = "SELECT melding_id, melding_type, lest_dato FROM melding WHERE fnr = :fnr"
        return dataSource.connection {
            prepareStatementWithNamedParameters(sql) {
                withParameter("fnr", personidentifikator.toLong())
            }.mapNotNull { row ->
                Hendelse(
                    meldingsreferanseId = UUID.fromString(row.string("melding_id")),
                    meldingstype = row.string("melding_type"),
                    lestDato = row.offsetDateTime("lest_dato").toLocalDateTime()
                )
            }
        }.associateBy { it.meldingsreferanseId }
    }

    private enum class Meldingstype {
        NY_SØKNAD,
        NY_SØKNAD_FRILANS,
        NY_SØKNAD_SELVSTENDIG,
        NY_SØKNAD_ARBEIDSLEDIG,
        NY_SØKNAD_TIDLIGERE_ARBEIDSTAKER,
        SENDT_SØKNAD_ARBEIDSGIVER,
        SENDT_SØKNAD_NAV,
        SENDT_SØKNAD_FRILANS,
        SENDT_SØKNAD_SELVSTENDIG,
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
        SYKEPENGEGRUNNLAG_FOR_ARBEIDSGIVER
    }
}
