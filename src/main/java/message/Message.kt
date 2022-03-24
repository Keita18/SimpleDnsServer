package message

import model.DnsHeader
import model.DnsType
import model.QClass
import model.records.RData

data class Query(
    val qName: String,
    val qType: DnsType,
    val qClass: QClass
)

class Answer(
    val name: String,
    val type: DnsType,
    val `class`: Int = 1,
    val ttl: Int = 0,
    val rdLength: Int,
    val rdata: RData
)

class Packet(
    val header: DnsHeader,
    val query: Query,
    val rawQuery: ByteArray,
    var answer: Answer?
)