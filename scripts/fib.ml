let bottom_up_fib (n: int): int =
    if n = 0 || n = 1 then
        n
    else
        let rec go (x: int) (a: int) (b: int): int =
            print x ; print ", " ; print a ; print ", " ; println b ;
            if x = n then a + b
            else go (x + 1) (a + b) a
        in go 2 1 0


let _ = bottom_up_fib 100