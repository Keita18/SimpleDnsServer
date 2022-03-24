package model


data class DnsHeader(
    val id: Int,
    val qr: QR,
    val opcode: Opcode,
    val aa: Boolean,
    val tc: Boolean,
    val rd: Boolean,
    val ra: Boolean,
    val rCode: RCode,
    val qdCount: Int,
    val anCount: Int,
    val nsCount: Int,
    val arCount: Int
)