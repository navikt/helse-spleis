package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import org.slf4j.LoggerFactory

internal class V247ForkasteOverlappende : JsonMigration(version = 247) {
    override val description = "forkaster overlappende vedtaksperioder bestående av én dag"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val fødselsnummer = jsonNode.path("fødselsnummer").asText()
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val orgnummer = arbeidsgiver.path("organisasjonsnummer").asText()
            val vedtaksperiodeindeks = arbeidsgiver.path("vedtaksperioder").indexOfFirst { it.path("id").asText() in trøbleteVedtaksperioder }
            if (vedtaksperiodeindeks == -1) return@forEach
            val perioder = arbeidsgiver.path("vedtaksperioder") as ArrayNode
            val vedtaksperiode = perioder[vedtaksperiodeindeks].deepCopy<JsonNode>()
            perioder.remove(vedtaksperiodeindeks)
            val forkastede = arbeidsgiver.path("forkastede") as ArrayNode
            forkastede.addObject().apply {
                set<JsonNode>("vedtaksperiode", vedtaksperiode)
            }
            sikkerlogg.info("fant trøblete vedtaksperiode {} på {} for {}",
                keyValue("fødselsnummer", fødselsnummer),
                keyValue("vedtaksperiodeId", vedtaksperiode.path("id").asText()),
                keyValue("orgnummer", orgnummer)
            )
        }
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private val trøbleteVedtaksperioder = setOf(
            "079be681-b3a9-4108-8a2a-acb727f5324e",
            "f8346825-6cb0-4efd-97f7-59e480986bcc",
            "c4d0022e-fc71-43b8-8f3d-419ce8b21899",
            "189ec76a-6633-4669-9231-2b9ab4369d5a",
            "bbbe989d-afa2-4d28-ab37-ff9ff9627356",
            "2bc1ffe2-0a5b-499b-a9cd-4b44df80b799",
            "d240df30-2407-4983-bd0d-375b24bea7f1",
            "bdd4a317-9c63-4604-8d3c-2a2306823f62",
            "e8db5961-e3b9-4409-915d-f198e107a0d7",
            "982160f9-369a-4238-abdc-0b7b22537a39",
            "f45dfc67-396f-4810-8b99-d0bece2744b9",
            "83fa6b8f-d8f8-4a8c-ad06-01ddcd9525e4",
            "1835acab-fcf8-4b07-9fdd-0dd0a17a703a",
            "511cc3a5-bd99-473d-b385-9d5c634b3fe6",
            "a63e4319-77dc-4ede-93dc-cfecd85b8d67",
            "230037e0-15a3-4820-bbad-b9c5367472b7",
            "e17ca683-633d-4aba-97a2-3922a4ab7f67",
            "a89d1d64-79ce-4624-8f62-37d4cac6d5a7",
            "87540431-e4f4-4ea1-9bac-156d93c4a19c",
            "e4bafff1-69bf-4e9b-b9a8-468ad4cf29e7",
            "9d434644-fc23-4f61-ab06-e7833215f447",
            "f6460a9c-49c8-49ba-8964-3c2df28715c1",
            "64c72154-fae8-4dc3-bccc-7865a9fa1ca5",
            "5b864545-f5d2-4f83-962c-df35898e2f86",
            "bea9f733-7ee3-4431-b0d6-dcc455742c57",
            "05deaf64-c247-44f5-ac20-955707c385e6",
            "bef4c2e4-c7db-4cec-8dce-d76109612217",
            "260c2459-156a-4fd4-9e01-345b90e71e01",
            "a74f56dd-5ab4-4b7d-8ef7-3a070bbc3c0b",
            "d4e4250e-7297-49b5-b344-948dbd624819",
            "64c72154-fae8-4dc3-bccc-7865a9fa1ca5",
            "9e75a4d1-c809-452b-932e-61ed68f28322"
        )
    }
}