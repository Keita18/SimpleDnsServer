package model.records

import java.net.InetAddress

open class RData

class ARecord(
    val inetAddress: InetAddress
) : RData()

class MXRecord(
    val preference: Int,
    val exchange: String
) : RData()

class TXTRecord(
    val string: String
) : RData()

class AAAARecord(
    val inetAddress: InetAddress
) : RData()
