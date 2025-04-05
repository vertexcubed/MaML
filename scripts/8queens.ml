let n = 8

(*
    fun print_dots (i: int): void =
        if i > 0 then (print ". "; print_dots (i-1)) else ()
*)
let rec print_dots (i : int) : unit =
  if i > 0 then (
    print(". ");
    print_dots(i - 1)
    )
else ()

(*
    fun print_row (i: int): void =
    (
        print_dots (i); print "Q "; print_dots (N-i-1); print "\n";
    )
*)
let print_row (i : int) : unit =
  print_dots(i);
  print("Q ");
  print_dots(n - i - 1);
  print("\n")

(*
    fun board_get
        (bd: int8, i: int): int =
        if i = 0 then bd.0
        else if i = 1 then bd.1
        else if i = 2 then bd.2
        else if i = 3 then bd.3
        else if i = 4 then bd.4
        else if i = 5 then bd.5
        else if i = 6 then bd.6
        else if i = 7 then bd.7
        else ~1
*)
let board_get bd (i : int) : int =
  if i = 0 then fst (bd)
  else if i = 1 then fst (snd (bd))
  else if i = 2 then fst (snd (snd (bd)))
  else if i = 3 then fst (snd (snd (snd (bd))))
  else if i = 4 then fst (snd (snd (snd (snd (bd)))))
  else if i = 5 then fst (snd (snd (snd (snd (snd (bd))))))
  else if i = 6 then fst (snd (snd (snd (snd (snd (snd (bd)))))))
  else if i = 7 then snd (snd (snd (snd (snd (snd (snd (bd)))))))
  else -1

(*
    fun print_board (bd: int8): void =
    (
        print_row (bd.0); print_row (bd.1); print_row (bd.2); print_row (bd.3);
        print_row (bd.4); print_row (bd.5); print_row (bd.6); print_row (bd.7);
        print_newline ()
    )
*)
let print_board (bd) : unit =
  print_row (board_get bd 0);
  print_row (board_get bd 1);
  print_row (board_get bd 2);
  print_row (board_get bd 3);
  print_row (board_get bd 4);
  print_row (board_get bd 5);
  print_row (board_get bd 6);
  print_row (board_get bd 7)

(*
    fun board_set
        (bd: int8, i: int, j:int): int8 = let
        val (x0, x1, x2, x3, x4, x5, x6, x7) = bd
    in
        if i = 0 then let
                val x0 = j in (x0, x1, x2, x3, x4, x5, x6, x7)
        end else if i = 1 then let
                val x1 = j in (x0, x1, x2, x3, x4, x5, x6, x7)
        end else if i = 2 then let
                val x2 = j in (x0, x1, x2, x3, x4, x5, x6, x7)
        end else if i = 3 then let
                val x3 = j in (x0, x1, x2, x3, x4, x5, x6, x7)
        end else if i = 4 then let
                val x4 = j in (x0, x1, x2, x3, x4, x5, x6, x7)
        end else if i = 5 then let
                val x5 = j in (x0, x1, x2, x3, x4, x5, x6, x7)
        end else if i = 6 then let
                val x6 = j in (x0, x1, x2, x3, x4, x5, x6, x7)
        end else if i = 7 then let
                val x7 = j in (x0, x1, x2, x3, x4, x5, x6, x7)
        end else bd // end of [if]
    end
*)
let board_set bd (i : int) (j : int) =
  let x0 = board_get bd 0 in
  let x1 = board_get bd 1 in
  let x2 = board_get bd 2 in
  let x3 = board_get bd 3 in
  let x4 = board_get bd 4 in
  let x5 = board_get bd 5 in
  let x6 = board_get bd 6 in
  let x7 = board_get bd 7 in
  if i = 0 then
    let x0 = j in
    (x0, (x1, (x2, (x3, (x4, (x5, (x6, x7)))))))
  else if i = 1 then
    let x1 = j in
    (x0, (x1, (x2, (x3, (x4, (x5, (x6, x7)))))))
  else if i = 2 then
    let x2 = j in
    (x0, (x1, (x2, (x3, (x4, (x5, (x6, x7)))))))
  else if i = 3 then
    let x3 = j in
    (x0, (x1, (x2, (x3, (x4, (x5, (x6, x7)))))))
  else if i = 4 then
    let x4 = j in
    (x0, (x1, (x2, (x3, (x4, (x5, (x6, x7)))))))
  else if i = 5 then
    let x5 = j in
    (x0, (x1, (x2, (x3, (x4, (x5, (x6, x7)))))))
  else if i = 6 then
    let x6 = j in
    (x0, (x1, (x2, (x3, (x4, (x5, (x6, x7)))))))
  else if i = 7 then
    let x7 = j in
    (x0, (x1, (x2, (x3, (x4, (x5, (x6, x7)))))))
  else bd

(*
    fun safety_test1
    (
        i0: int, j0: int, i: int, j: int
    ) : bool =
        j0 != j andalso abs (i0 - i) != abs (j0 - j)
*)
let abs (n : int) : int = if n < 0 then -n else n

let safety_test1 (i0 : int) (j0 : int) (i : int) (j : int) : bool =
  j0 != j && abs (i0 - i) != abs (j0 - j)

(*
    fun safety_test2
    (
        i0: int, j0: int, bd: int8, i: int
    ) : bool =
        if i >= 0 then
            if safety_test1 (i0, j0, i, board_get (bd, i))
                then safety_test2 (i0, j0, bd, i-1) else false
        else true
*)
let rec safety_test2 (i0 : int) (j0 : int) bd (i : int) : bool =
  if i >= 0 then
    if safety_test1 i0 j0 i (board_get bd i) then safety_test2 i0 j0 bd (i - 1)
    else false
  else true

(*
    fun search
    (
        bd: int8, i: int, j: int, nsol: int
    ) : int = (
    if (j < N)
    then let
        val test = safety_test2 (i, j, bd, i-1)
    in
        if test
            then let
                val bd1 = board_set (bd, i, j)
            in
                if i+1 = N
                    then let
                        val () = print! ("Solution #", nsol+1, ":\n\n")
                        val () = print_board (bd1)
                    in
                        search (bd, i, j+1, nsol+1)
            end
            else (
                search (bd1, i+1, 0, nsol) // positioning next piece
            )
        end
        else search (bd, i, j+1, nsol)
    end
    else (
        if i > 0
            then search (bd, i-1, board_get (bd, i-1) + 1, nsol) else nsol
    )
    )
*)
let rec search bd (i : int) (j : int) (nsol : int) : int =
  if j < n then
    let test = safety_test2 i j bd (i - 1) in
    if test then
      let bd1 = board_set bd i j in
      if i + 1 = n then (
        print ("Solution #");
        print (nsol + 1);
        print (":\n\n");
        print_board (bd1);
        search bd i (j + 1) (nsol + 1) )
      else
        search bd1 (i + 1) 0 nsol
    else
        search bd i (j + 1) nsol
  else
    if i > 0 then search bd (i - 1) ((board_get bd (i - 1)) + 1) nsol else nsol



let _ = println "Hi!"

let _ = println (search (0, (0, (0, (0, (0, (0, (0, 0))))))) 0 0 0)