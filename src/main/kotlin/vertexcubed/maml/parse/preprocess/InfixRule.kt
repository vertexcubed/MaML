package vertexcubed.maml.parse.preprocess

class InfixRule(val name: String, val precedence: Int, val assoc: Associativity) {




}
enum class Associativity {
    LEFT,
    RIGHT,
    NONE
}