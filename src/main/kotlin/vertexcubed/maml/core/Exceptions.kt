package vertexcubed.maml.core

import vertexcubed.maml.ast.AstNode
import vertexcubed.maml.eval.MValue
import vertexcubed.maml.parse.DummyType
import vertexcubed.maml.parse.TypeVarDummy
import vertexcubed.maml.type.MType
import vertexcubed.maml.type.TypeEnv

/**
 * Thrown when parsing, usually in the case of illegal literals
 */
class ParseException(line: Int, message: String) : Exception("Error on line $line: $message")

/**
 * Thrown when trying to lookup a variable in the environment, but none was found.
 */
class UnboundVarException(val name: String) : Exception("Unbound Variable: $name")

/**
 * Thrown when trying to lookup a variable in the environment, but none was found.
 */
class UnboundExternalException(val name: String) : Exception("Java function $name not implemented.")




/**
 * Thrown when trying to lookup a variable in the environment, but none was found.
 */
class UnboundModuleException(val name: String) : Exception("Unbound Module: $name")



/**
 * Thrown when trying to lookup a type label, but none was found. For example: in 'a option, 'b is unbound.
 */
class UnboundTypeLabelException(val type: TypeVarDummy): Exception("The type of variable $type is unbound.")

/**
 * Thrown when trying to lookup a type constructor in the environment, but none was found.
 */
class UnboundTyConException(val name: String) : Exception("Unbound Type Constructor: $name")

/**
 * Runtime Exception thrown when trying to apply a non-function value.
 * Generally not thrown unless type-checking is skipped.
 * Report as a bug if this is thrown!
 */
class ApplicationException(message: String) : Exception(message)

class RecordException(message: String): Exception(message)

/**
 * Runtime Exception thrown when no pattern was able to be matched.
 * Only thrown if pattern matching is not exhaustive.
 * Report as a bug if this is thrown!
 */
class MatchException(value: MValue): Exception("Failed to match against $value")

/**
 * Runtime Exception thrown when an if condition is not a boolean.
 * Generally not thrown unless type-checking is skipped.
 * Report as a bug if this is thrown!
 */
class IfException() : Exception("Cannot use non-boolean as condition")

/**
 * General Type check failures.
 */
class TypeCheckException(val line: Int, val node: AstNode, val env: TypeEnv, val log: String): Exception("Line $line: $log)") {
        constructor(line: Int, node: AstNode, env: TypeEnv, actualType: MType, expectedType: MType) : this(line, node, env,
            "This expression has type ${actualType.asString(env)} but an expression was expected of type ${expectedType.asString(env)}")
}


/**
 * Thrown when trying to lookup a type con with the wrong amount of arguments.
 * These should generall be caught and replaced with proper TypeCheckExceptions, so you should never see them.
 * Report as a bug if this is not caught!
 */
class TypeConException(env: TypeEnv, val constr: MType, val expectedArgs: Int, val actualArgs: Int)
    : Exception("The type constructor ${constr.asString(env)} expects $expectedArgs argument(s),\n" +
        "but is here applied to $actualArgs argument(s)")

/**
 * Thrown when two types are tried to unified, but cannot be.
 * These should generally be caught and replaced with proper TypeCheckExceptions, so you should never see them.
 * Report as a bug if this is not caught!
 */
class UnifyException(val t1: MType, val t2: MType): Exception("Cannot unify type $t1 with type $t2")

class BadRecordException(val label: String): Exception("The record field label $label is defined several times")
/**
 * Thrown when trying to bind a type variable when it's already been bound.
 * Report as a bug if this is thrown!
 */
class BindException(t1: MType, boundType: MType): Exception("Type $t1 already bound to type $boundType")