package no.nav.helse.utbetalingslinjer

import java.nio.ByteBuffer
import java.util.UUID
import org.apache.commons.codec.binary.Base32

private const val pad = '='
private const val padByte = pad.code.toByte()

fun genererUtbetalingsreferanse(uuid: UUID): String {
    return uuid.base32Encode()
}

fun decodeUtbetalingsreferanse(utbetalingsreferanse: String): UUID {
    return Base32(padByte).decode(utbetalingsreferanse).let {
        ByteBuffer.wrap(it).asLongBuffer()
    }.let {
        UUID(it.get(), it.get())
    }
}

private fun UUID.base32Encode(): String {
    return Base32(padByte)
        .encodeAsString(this.byteArray())
        .replace(pad.toString(), "")
}

private fun UUID.byteArray() = ByteBuffer.allocate(Long.SIZE_BYTES * 2).apply {
    putLong(this@byteArray.mostSignificantBits)
    putLong(this@byteArray.leastSignificantBits)
}.array()

