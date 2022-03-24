package model

fun List<Byte>.toInt(): Int {
    var result = 0
    var shift = 0
    this.reversed().forEach {
        result += it.toUByte().toInt() shl shift
        shift += 8
    }
    return result
}

fun Int.toByteArray(): ByteArray {
    val bytes = mutableListOf<Byte>()

    var shift = 0

    var limit = this.countBits() / 8

    if (this.countBits() % 8 != 0) limit++

    for (i in 0 until limit) {
        bytes.add((this shr shift).toByte())
        shift += 8
    }

    return bytes.reversed().toByteArray()
}

fun Int.countBits(): Int {
    var n = this
    var count = 0
    while (n != 0) {
        count++
        n = n shr 1
    }
    return count
}