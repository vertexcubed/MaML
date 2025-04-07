let rec factorial n =
    match n with
    | 0 | 1 -> 1
    | _ -> n * factorial (n - 1)
    end

let _ = println (factorial 5)