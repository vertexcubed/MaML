package vertexcubed.maml.type

data class ForAll(val typeVars: List<MGeneralTypeVar>, val type: MType) {

    companion object {
        fun generalize(type: MType, types: TypeSystem): ForAll {

            val typeList = ArrayList<MGeneralTypeVar>()

            val freeVars = recursiveFind(type).toList()
            if(freeVars.isEmpty()) {
                return ForAll(typeList, type)
            }
            typeList.add(types.newGeneralType())
            var retType = type.substitute(freeVars[0], typeList[0])
            for(i in 1 until freeVars.size) {
                typeList.add(types.newGeneralType())
                retType = retType.substitute(freeVars[i], typeList[i])
            }
            return ForAll(typeList, retType)
        }

        fun empty(type: MType): ForAll {
            return ForAll(emptyList(), type)
        }


        private fun recursiveFind(type: MType): Set<MTypeVar> {
            return when(val real = type.find()) {
                is MFunction -> {
                    val ret = HashSet<MTypeVar>()
                    ret.addAll(recursiveFind(real.arg))
                    ret.addAll(recursiveFind(real.ret))
                    ret
                }
                is MGeneralTypeVar -> emptySet()
                is MBaseTypeVar -> emptySet()
                is MTuple -> {
                    val ret = HashSet<MTypeVar>()
                    for(t in real.types) {
                        val vars = recursiveFind(t)
                        ret.addAll(vars)
                    }
                    ret
                }
                is MTypeVar -> setOf(real)

                MBool, MChar, MFloat, MInt, MString, MUnit, MEmptyRow -> emptySet()

                is MConstr -> {
                    val ret = HashSet<MTypeVar>()
                    ret.addAll(recursiveFind(real.type))
                    if(real.argType.isPresent) {
                        ret.addAll(recursiveFind(real.argType.get()))
                    }
                    ret
                }
                is MRecord -> {
                    val ret = HashSet<MTypeVar>()
                    for(v in real.fields.values) {
                        ret.addAll(recursiveFind(v))
                    }
                    ret.addAll(recursiveFind(real.rest))
                    ret
                }
                is MAlias -> {
                    val ret = HashSet<MTypeVar>()
                    for(a in real.realArgs) {
                        ret.addAll(recursiveFind(a))
                    }
                    for(a in real.args) {
                        ret.addAll(recursiveFind(a))
                    }
                    ret
                }
                is MTypeCon -> {
                    val ret = HashSet<MTypeVar>()
                    for(a in real.args) {
                        ret.addAll(recursiveFind(a))
                    }
                    ret
                }
            }
        }
    }


    fun instantiate(types: TypeSystem): MType {
        var ret = type

        for(i in typeVars.indices) {
            ret = ret.substitute(typeVars[i], types.newTypeVar())
        }
        return ret
    }

    fun instantiate(args: List<MType>): MType {
        if(args.size != typeVars.size) {
            throw IllegalArgumentException("Cannot instantiate types with different amount of args than forall!")
        }
        var ret = type
        for(i in typeVars.indices) {
            ret = ret.substitute(typeVars[i], args[i])
        }
        return ret
    }

    fun instantiateBase(types: TypeSystem): MType {
        var ret = type
        for(i in typeVars.indices) {
            ret = ret.substitute(typeVars[i], types.newBaseType())
        }
        return ret
    }
}