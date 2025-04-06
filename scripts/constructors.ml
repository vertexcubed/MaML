type myType = A of int * int | B of myType

let test = B (A (5, 2))