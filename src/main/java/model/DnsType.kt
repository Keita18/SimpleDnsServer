package model

enum class DnsType(val code: Int) {
    A(1),
    MX(15),
    TXT(16),
    AAAA(28)
}