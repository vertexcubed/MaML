package vertexcubed.maml.eval


class ParseException(line: Int, message: String) : Exception("Error on line $line: $message")
class BinaryOpException(message: String) : Exception(message)
class UnaryOpException(message: String) : Exception(message)
class UnboundVarException(name: String) : Exception("Unbound Variable: $name")
class ApplicationException(message: String) : Exception(message)
class IfException() : Exception("Cannot use non-boolean as condition")