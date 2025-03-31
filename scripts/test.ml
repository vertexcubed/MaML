let firstFun (x: int): int = x + 2 in

let secondFun (y: int): int = firstFun y in

let builtinTest: unit = print "Hello World!" in


secondFun 3