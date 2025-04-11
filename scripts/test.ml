type 'a option = Some of 'a | None

type ('a, 'b) either = First of 'a | Second of 'b



let a = First (5)
let b = Second (7)

let f x =
    match x with
    | First f -> "First!"
    | Second g -> "Second!"
    end

let _ = f a
let _ = f b