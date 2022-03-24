import message.Answer
import model.DnsType
import model.Opcode
import model.QR
import model.records.AAAARecord
import model.records.ARecord
import model.records.MXRecord
import model.records.TXTRecord
import zones.Zone
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.system.exitProcess


class Main(port: Int) {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Main(53).launch()
        }
    }

    private val serverSocket: DatagramSocket = DatagramSocket(port)
    private val server = Server(serverSocket)

    fun launch() {
        while (true) {
            val receivingDataBuffer = ByteArray(1024)
            val inputPacket = DatagramPacket(receivingDataBuffer, receivingDataBuffer.size)
            serverSocket.receive(inputPacket)
            val packet = server.processPacket(inputPacket)
            val keita_org = Zone(
                "keita.org",
                "10.10.10.10",
                "domain.keita.org",
                "mail.keita.org",
                "2001:0db8:85a3:0000:0000:8a2e:0370:7334"
                )
            val answer =
                if (packet.query.qName == keita_org.name && packet.header.qr == QR.Query && packet.header.opcode == Opcode.Query)
                    when (packet.query.qType) {
                        DnsType.A -> Answer(
                            packet.query.qName,
                            packet.query.qType,
                            1,
                            0,
                            4,
                            ARecord(InetAddress.getByName(keita_org.aHost))
                        )
                        DnsType.TXT -> Answer(
                            packet.query.qName,
                            packet.query.qType,
                            1,
                            0,
                            keita_org.domain.toByteArray(Charsets.US_ASCII).size,
                            TXTRecord(keita_org.domain)
                        )
                        DnsType.MX -> Answer(
                            packet.query.qName,
                            packet.query.qType,
                            1,
                            0,
                            keita_org.exchange.toByteArray(Charsets.US_ASCII).size + 2,
                            MXRecord(30, keita_org.exchange)
                        )
                        DnsType.AAAA -> Answer(
                            packet.query.qName,
                            packet.query.qType,
                            1,
                            0,
                            128,
                            AAAARecord(InetAddress.getByName(keita_org.aAAAHost))
                        )
                    }
                else continue
            packet.answer = answer
            server.sendPacket(packet, inputPacket.address, inputPacket.port)
        }
    }
}