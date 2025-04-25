# MaML

MaML is a simple ML dialect inspired by OCaml and Standard ML, implemented in Kotlin. 
It is designed to be embedded into Java/Kotlin applications as a lightweight scripting language.

## Installation

TODO: Create maven repository

## Getting Started

To get started, create a new `Interpreter` object, and call `run` to execute code.

```kotlin
val code = "let rec factorial n = \n" +
            "  if n <= 1 then 1 \n" +
            "  else n * factorial (n - 1)"

val interp = Interpreter()
interp.run(code)
```

You can register external functions using the `registerBuiltins` method.

```ocaml
    external print_endline: string -> unit = "maml_println"

```

```kotlin
fun myFunc(arg: MValue): MValue {
    println(arg)
    return UnitValue
}

val interp = Interpreter()
interp.registerExternal("maml_println", ::myFunc)
```

## Documentation

Documentation is available in this repository's [wiki](https://github.com/vertexcubed/MaML/wiki).

## License

MaML is open source under the MIT license. See [LICENSE](https://github.com/vertexcubed/MaML/blob/main/LICENSE) for more information.

## Reporting Issues

MaML is in active development, and bugs and issues are expected. Please report bugs on the [issue tracker](https://github.com/vertexcubed/MaML/issues).

## Roadmap

### Core Language

- [x] Basic expression syntax - If statements, let expressions, functions
- [x] Type checking and type inference
- [x] Recursive functions
- [x] Builtin functions
- [x] Toplevel expressions
- [x] Algebraic Data Types
- [x] Pattern Matching
- [x] Custom infix functions
- [x] Multi argument builtin functions
- [x] Multi argument Type Constructors
- [x] Type aliases
- [ ] Pattern exhaustiveness checking
- [x] List Sugar (`[]`, `x :: xs`, etc.)
- [x] Records
- [x] Modules
  - [x] Module Signatures / Interfaces
  - [ ] Module Functors
- [x] Exceptions

### Additional Features

- [ ] Asynchronous values?
- [x] Callbacks
- [x] REPL environment for command-line
- [ ] **Standard Library**
  - [ ] Either
  - [ ] Format (Pretty printing)
  - [ ] Functors (Maybe)
  - [ ] Higher order functions - map, fold
  - [ ] Lazy
  - [x] Lists
  - [ ] Maps
  - [ ] Monads
  - [ ] Options
  - [ ] Result
  - [ ] Sets
  - [ ] Stacks
  - [ ] Streams
  - [ ] Queues
- [ ] **Compiler**
  - [ ] IR Bytecode
  - [ ] Virtual stack instead of JVM Call Stack
  - [ ] Tail Call Optimization

### Not Planned

- Iteration
- Side effects / mutable values*
  - Lazy values *might* be the exception to this
- Classes and objects
- Polymorphic variants*
  - I might change my mind on this depending on how hard it is to implement.
- Named function arguments
- Garbage Collector
  - Out of scope for this project - let the JVM GC handle it.
