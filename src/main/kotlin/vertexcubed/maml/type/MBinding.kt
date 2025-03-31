package vertexcubed.maml.type

data class MBinding(val binding: String, val type: MType) {
    override fun toString(): String {
        return "Binding($binding : $type)"
    }
}