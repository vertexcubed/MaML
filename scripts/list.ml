type 'a list = Nil | Cons of 'a * 'a list

let rev l =
    let rec go sub_l acc =
        match sub_l with
        | Nil -> acc
        | Cons (x, xs) -> go xs (Cons (x, acc))
        end
    in
    go l Nil

let _ = println (rev (Cons (1, Cons (2, Cons (3, Cons (4, Cons (5, Nil)))))))
