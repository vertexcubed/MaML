package vertexcubed.maml.type

sealed class MType() {}

data object MInt: MType() {
    override fun toString(): String {
        return "int"
    }
}
data object MFloat: MType() {
    override fun toString(): String {
        return "float"
    }
}
data object MBool: MType() {
    override fun toString(): String {
        return "bool"
    }
}
data object MString: MType() {
    override fun toString(): String {
        return "string"
    }
}
data object MChar: MType() {
    override fun toString(): String {
        return "int"
    }
}
data object MUnit: MType() {
    override fun toString(): String {
        return "unit"
    }
}
data class MFunction(val arg: MType, val ret: MType): MType() {
    override fun toString(): String {
        return "$arg -> $ret"
    }
}