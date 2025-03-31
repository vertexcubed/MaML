package vertexcubed.maml.type

sealed class MType() {}

data object Int: MType()
data object Float: MType()
data object Bool: MType()
data object String: MType()
data object Char: MType()
data class Function(val arg: MType, val ret: MType): MType()