package model

enum class QR(val code: Int) {
    Query(0),
    Response(1)
}

enum class Opcode(val code: Int) {
    Query(0),
    IQuery(1),
    Status(2)
}

enum class RCode(val code: Int) {
    Null(0),
    FormatError(1),
    ServerFailure(2),
    NameError(3),
    NotImplemented(4),
    Refused(5)
}

enum class QClass(val code: Int) {
    ALL(255)
}
