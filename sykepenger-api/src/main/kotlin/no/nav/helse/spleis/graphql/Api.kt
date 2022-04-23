package no.nav.helse.spleis.graphql

import com.apurebase.kgraphql.GraphQL
import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.authenticate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.Toggle
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.spleis.dao.PersonDao
import no.nav.helse.spleis.dto.håndterPerson
import no.nav.helse.spleis.graphql.dto.GraphQLArbeidsgiver
import no.nav.helse.spleis.graphql.dto.GraphQLGenerasjon
import no.nav.helse.spleis.graphql.dto.GraphQLGhostPeriode
import no.nav.helse.spleis.graphql.dto.GraphQLPerson
import no.nav.helse.spleis.graphql.dto.arbeidsgiverTypes
import no.nav.helse.spleis.graphql.dto.hendelseTypes
import no.nav.helse.spleis.graphql.dto.inntektsgrunnlagTypes
import no.nav.helse.spleis.graphql.dto.personTypes
import no.nav.helse.spleis.graphql.dto.simuleringTypes
import no.nav.helse.spleis.graphql.dto.tidslinjeperiodeTypes
import no.nav.helse.spleis.graphql.dto.vilkarsgrunnlagTypes

internal fun SchemaBuilder.personSchema(personDao: PersonDao, hendelseDao: HendelseDao) {
    query("person") {
        resolver { fnr: String ->
            personDao.hentPersonFraFnr(fnr.toLong())
                ?.deserialize(
                    jurist = MaskinellJurist(),
                    meldingerSupplier = { hendelseDao.hentAlleHendelser(fnr.toLong()) }
                )
                ?.let { håndterPerson(it, hendelseDao) }
                ?.let { person ->
                    GraphQLPerson(
                        aktorId = person.aktørId,
                        fodselsnummer = person.fødselsnummer,
                        arbeidsgivere = person.arbeidsgivere.map { arbeidsgiver ->
                            GraphQLArbeidsgiver(
                                organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
                                id = arbeidsgiver.id,
                                generasjoner = arbeidsgiver.generasjoner.map { generasjon ->
                                    GraphQLGenerasjon(
                                        id = generasjon.id,
                                        perioder = generasjon.perioder.map { periode -> mapTidslinjeperiode(periode) }
                                    )
                                },
                                ghostPerioder = arbeidsgiver.ghostPerioder.map { periode ->
                                    GraphQLGhostPeriode(
                                        id = periode.id,
                                        fom = periode.fom,
                                        tom = periode.tom,
                                        skjaeringstidspunkt = periode.skjæringstidspunkt,
                                        vilkarsgrunnlaghistorikkId = periode.vilkårsgrunnlagHistorikkInnslagId,
                                        deaktivert = periode.deaktivert,
                                        organisasjonsnummer = arbeidsgiver.organisasjonsnummer
                                    )
                                }
                            )
                        },
                        inntektsgrunnlag = person.inntektsgrunnlag.map { inntektsgrunnlag -> mapInntektsgrunnlag(inntektsgrunnlag) },
                        vilkarsgrunnlaghistorikk = person.vilkårsgrunnlagHistorikk.entries.map { (id, dateMap) ->
                            mapVilkårsgrunnlag(id, dateMap.values.toList())
                        },
                        dodsdato = person.dødsdato,
                        versjon = person.versjon
                    )
                }
        }
    }

    personTypes()
    arbeidsgiverTypes()
    hendelseTypes()
    inntektsgrunnlagTypes()
    simuleringTypes()
    tidslinjeperiodeTypes()
    vilkarsgrunnlagTypes()

    stringScalar<UUID> {
        deserialize = { uuid: String -> UUID.fromString(uuid) }
        serialize = { uuid: UUID -> uuid.toString() }
    }
    stringScalar<LocalDate> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        deserialize = { date: String -> LocalDate.parse(date, formatter) }
        serialize = { date: LocalDate -> date.format(formatter) }
    }
    stringScalar<LocalDateTime> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        deserialize = { date: String -> LocalDateTime.parse(date, formatter) }
        serialize = { date: LocalDateTime -> date.format(formatter) }
    }
    stringScalar<YearMonth> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM")
        deserialize = { date: String -> YearMonth.parse(date, formatter) }
        serialize = { date: YearMonth -> date.format(formatter) }
    }
}

fun Application.installGraphQLApi(dataSource: DataSource, authProviderName: String) {
    val personDao = PersonDao(dataSource)
    val hendelseDao = HendelseDao(dataSource)

    install(GraphQL) {
        endpoint = "/graphql"

        if (Toggle.GraphQLPlayground.enabled) {
            playground = true
        } else {
            wrap {
                authenticate(authProviderName, build = it)
            }
        }

        schema {
            personSchema(personDao, hendelseDao)
        }
    }
}
