type 'a option = Some of 'a * 'a | None



let f x =
    match x with
    | Some (a, b) -> "Meow"
    | None -> "Oh"
    end