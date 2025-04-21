package vertexcubed.maml.eval

import vertexcubed.maml.core.MIdentifier

fun longOf(value: MValue): Long? {
    return when(value) {
        is IntegerValue -> value.value
        else -> null
    }
}

fun longOrThrow(value: MValue): Long {
    return when(value) {
        is IntegerValue -> value.value
        else -> throw IllegalArgumentException("Value $value is not an integer!")
    }
}

fun boolOf(value: MValue): Boolean? {
    return when(value) {
        is BooleanValue -> value.value
        else -> null
    }
}

fun stringOf(value: MValue): String? {
    return when(value) {
        is StringValue -> value.value
        else -> null
    }
}

fun charOf(value: MValue): Char? {
    return when(value) {
        is CharValue -> value.value
        else -> null
    }
}

fun floatOf(value: MValue): Float? {
    return when(value) {
        is FloatValue -> value.value
        else -> null
    }
}

fun unitOf(value: MValue): Unit? {
    return when(value) {
        is UnitValue -> Unit
        else -> null
    }
}

fun pairOf(value: MValue): Pair<MValue, MValue>? {
    return when(value) {
        is TupleValue ->
            if(value.values.size != 2) null else value.values[0] to value.values[1]
        else -> null
    }
}

fun tripleOf(value: MValue): Triple<MValue, MValue, MValue>? {
    return when(value) {
        is TupleValue ->
            if(value.values.size != 3) null else Triple(value.values[0], value.values[1], value.values[2])
        else -> null
    }
}

fun tupleToList(value: MValue): List<MValue>? {
    return when(value) {
        is TupleValue -> value.values
        else -> null
    }
}

fun toFunction(value: MValue): ((MValue) -> MValue)? {
    return when(value) {
        is FunctionValue -> {
            {
                val newEnv = value.env.copy()
                newEnv.addBinding(value.arg to it)
                value.expr.eval(newEnv)
            }
        }
        is RecursiveFunctionValue -> {
            {
                val newEnv = value.func.env.copy()
                newEnv.addBinding(value.func.arg to it)
                newEnv.addBinding(value.name to value)
                value.func.expr.eval(newEnv)
            }
        }
        else -> null
    }
}

fun recordToMap(value: MValue): Map<String, MValue>? {
    return when(value) {
        is RecordValue -> value.values
        else -> null
    }
}

fun conToPair(value: MValue): Pair<MIdentifier, MValue?>? {
    return when(value) {
        is ConValue -> value.name to (if(value.value.isPresent) value.value.get() else null)
        else -> null
    }
}