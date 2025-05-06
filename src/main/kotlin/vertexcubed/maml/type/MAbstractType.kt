package vertexcubed.maml.type

data class MAbstractType(override val id: Int, override val args: List<MType>): MTypeCon(id, args) {

    override fun substitute(from: MType, to: MType): MType {
        if(from is MAbstractType && isSame(from)) {
            return to
        }
        return MAbstractType(id, args.map { it.substitute(from, to) })
    }

    override fun toString(): String {
        return "Abstr($id, $args)"
    }


}