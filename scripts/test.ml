type ('a, 'b, 'c) triple = First of 'a | Second of 'b | Third of 'c

type ('a, 'b) other_test = ('b, bool, 'a) triple

let f (x: (int, string) other_test) = x

let _ = f (First "true")

type test = int

let _ = 5