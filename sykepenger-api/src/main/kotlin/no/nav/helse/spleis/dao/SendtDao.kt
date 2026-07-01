package no.nav.helse.spleis.dao

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.mapNotNull
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import com.github.navikt.tbd_libs.sql_dsl.string
import com.github.navikt.tbd_libs.sql_dsl.stringOrNull
import java.util.UUID
import javax.sql.DataSource
import org.intellij.lang.annotations.Language

data class SendteMeldinger(
    val antallMeldinger: Int,
    val meldinger: List<SendtMelding>
)

data class SendtMelding(
    val key: String?,
    val json: ObjectNode,
    val mottaker: String
)

data class SendtDao(private val dataSource: () -> DataSource) {

    fun sendteMeldinger(forarsaketAv: UUID): SendteMeldinger {
        @Language("PostgreSQL")
        val sql = """
            SELECT key, json, mottaker, sendt
            FROM sendt
            WHERE forarsaket_av = :forarsaket_av
        """

        val meldinger = dataSource().connection {
            prepareStatementWithNamedParameters(sql) {
                withParameter("forarsaket_av", forarsaketAv)
            }.mapNotNull { row ->
                SendtMelding(
                    key = row.stringOrNull("key"),
                    json = row.string("json").let { objectMapper.readTree(it) as ObjectNode },
                    mottaker = row.string("mottaker")
                )
            }
        }
        return SendteMeldinger(meldinger.size, meldinger)
    }

    internal companion object {
        private val objectMapper = jacksonObjectMapper()
        internal fun SendteMeldinger.responseJson() = objectMapper.writeValueAsString(this)
    }
}
