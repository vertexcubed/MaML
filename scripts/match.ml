type 'a option = Some of 'a | None

let myFunc x =
    match x with
    | Some a -> a
    | None -> "Failure"
    end

let _ = println (myFunc (None))