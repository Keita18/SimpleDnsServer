import message.Packet
import message.Query
import model.*
import model.records.AAAARecord
import model.records.ARecord
import model.records.MXRecord
import model.records.TXTRecord
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class Server(private val serverSocket: DatagramSocket) {

    fun processPacket(inputPacket: DatagramPacket): Packet {
        val buffer = inputPacket.data

//      ======================= Id Section ===========================

        val id = buffer.slice(0..1).toInt()

//      ===================== Flags section ==========================

        val qr = when (buffer[2].toInt() ushr 7) {
            0 -> QR.Query
            1 -> QR.Response
            else -> throw IllegalStateException("Wrong QR")
        }
        val opcode = when ((buffer[2].toInt() ushr 3) and 0b00001111) {
            0 -> Opcode.Query
            1 -> Opcode.IQuery
            2 -> Opcode.Status
            else -> throw IllegalStateException("Wrong opcode")
        }
        val aa = (buffer[2].toInt() ushr 2) and 1 == 1
        val tc = (buffer[2].toInt() ushr 1) and 1 == 1
        val rd = (buffer[2].toInt()) and 1 == 1
        val ra = (buffer[3].toInt() ushr 7) and 1 == 1
        val rCode = when ((buffer[3].toInt()) and 0b00001111) {
            0 -> RCode.Null
            1 -> RCode.FormatError
            2 -> RCode.ServerFailure
            3 -> RCode.NameError
            4 -> RCode.NotImplemented
            5 -> RCode.Refused
            else -> throw IllegalStateException("Wrong RCODE")
        }

//      ===================== Count Section ===========================

        val qdCount = buffer.slice(4..5).toInt()
        val anCount = buffer.slice(6..7).toInt()
        val nsCount = buffer.slice(8..9).toInt()
        val arCount = buffer.slice(10..11).toInt()

//      ===================== Query Section ===========================

        var byte = 12

        val sb = StringBuilder()

        while (true) {
            val num = buffer[byte].toInt()
            if (num == 0) {
                sb.deleteAt(sb.length - 1)
                break
            }
            val string = buffer.slice(byte + 1..byte + num).toByteArray().toString(Charsets.US_ASCII)
            sb.append(string)
            sb.append(".")
            byte += num + 1
        }

        byte++

        val qname = sb.toString()

        val qtype = when (buffer.slice(byte..byte + 1).toInt()) {
            1 -> DnsType.A
            15 -> DnsType.MX
            16 -> DnsType.TXT
            28 -> DnsType.AAAA
            else -> throw IllegalStateException("Wrong QTYPE")
        }

        val qclass = QClass.ALL

        val header = DnsHeader(id, qr, opcode, aa, tc, rd, ra, rCode, qdCount, anCount, nsCount, arCount)

        val query = Query(qname, qtype, qclass)

        return Packet(
            header,
            query,
            buffer.sliceArray(12 until inputPacket.length),
            null
        )
    }

    fun sendPacket(packet: Packet, inetAddress: InetAddress, port: Int) {
//        val nameBytes = packet.rawDomain!!
        val nameBytes = byteArrayOf(0b11000000.toByte(), 0b00001100.toByte()) // 11000000 00001100
        val typeBytes = ByteArray(1) + packet.answer!!.type.code.toByte()
        val classBytes = ByteArray(1) + packet.answer!!.`class`.toByte()
        val ttlBytes = ByteArray(3) + byteArrayOf(100.toByte())
        val rDataBytes = when (packet.answer!!.type) {
            DnsType.A -> {
                val a = packet.answer!!.rdata as ARecord
                a.inetAddress.address
            }
            DnsType.TXT -> {
                val txt = packet.answer!!.rdata as TXTRecord
                val bytes = txt.string.toByteArray(Charsets.US_ASCII)
                byteArrayOf(bytes.size.toByte()) + bytes
            }
            DnsType.MX -> {
                val mx = packet.answer!!.rdata as MXRecord
                val exchangeBytes = mutableListOf<Byte>()
                for (sub in mx.exchange.split('.')) {
                    exchangeBytes.add(sub.length.toByte())
                    exchangeBytes.addAll(sub.toByteArray(Charsets.US_ASCII).toList())
                }
                exchangeBytes.add(0.toByte())
                ByteArray(1) + mx.preference.toByte() + exchangeBytes
            }
            DnsType.AAAA -> {
                val aaaa = packet.answer!!.rdata as AAAARecord
                aaaa.inetAddress.address
            }
        }
        val rdLengthBytes = ByteArray(1) + rDataBytes.size.toByte()

        val header = ByteArray(2 - packet.header.id.toByteArray().size) + packet.header.id.toByteArray() +
                ((1 shl 7) + (packet.header.opcode.code shl 3) + ((if (packet.header.aa) 1 else 0) shl 2) + ((if (packet.header.tc) 1 else 0) shl 1) + (if (packet.header.rd) 1 else 0)).toByteArray() +
                ByteArray(1 - (if (packet.header.ra) 1 else 0 shl 7).toByteArray().size) + (if (packet.header.ra) 1 else 0 shl 7).toByteArray() +
                ByteArray(2 - packet.header.qdCount.toByteArray().size) + packet.header.qdCount.toByteArray() +
                byteArrayOf(0.toByte(), 1.toByte()) +
                ByteArray(2 - packet.header.nsCount.toByteArray().size) + packet.header.nsCount.toByteArray() +
                ByteArray(2 - packet.header.arCount.toByteArray().size) + packet.header.arCount.toByteArray()

        val buffer =
            header + packet.rawQuery + nameBytes + typeBytes + classBytes + ttlBytes + rdLengthBytes + rDataBytes

        val datagramPacket = DatagramPacket(buffer, buffer.size, inetAddress, port)
        serverSocket.send(datagramPacket)
    }

}