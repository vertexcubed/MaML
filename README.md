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

You can register builtin functions using the `registerBuiltins` method.

```kotlin
fun myFunc(arg: MValue): MValue {
    println(arg)
    return UnitValue
}

val interp = Interpreter()
interp.registerBuiltin("print", myFunc)
```

## Documentation

Documentation is available in this repository's [wiki](https://github.com/vertexcubed/MaML/wiki).

## License

MaML is open source under the MIT license. See [LICENSE](https://github.com/vertexcubed/MaML/blob/main/LICENSE) for more information.

## Reporting Issues

MaML is in active development, and bugs and issues are expected. Please report bugs on the [issue tracker](https://github.com/vertexcubed/MaML/issues).

## Roadmap

- [x] Basic expression syntax - If statements, let expressions, functions
- [x] Type checking and type inference
- [x] Recursive functions
- [x] Builtin functions
- [x] Toplevel expressions
- [x] Algebraic Data Types
- [x] Pattern Matching
- [ ] Custom infix functions
- [ ] Multi argument builtin functions
- [ ] Multi argument Type Constructors
- [ ] Type aliases
- [ ] Pattern exhaustiveness checking
- [ ] Lists
- [ ] Records
- [ ] Modules
- [ ] Exceptions

### Future Additions

- [ ] Binding Operators (let*)  
- [ ] Better Java integration
- [ ] REPL environment for command-line
- [ ] **Standard Library**
  - [ ] Either
  - [ ] Format (Pretty printing)
  - [ ] Functors (Maybe)
  - [ ] Higher order functions - map, fold
  - [ ] Lazy
  - [ ] Lists
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
- Asynchronous values
- Side effects / mutable values*
  - Lazy values *might* be the exception to this
- Classes and objects
- Polymorphic variants
- Named function arguments
- Garbage Collector
