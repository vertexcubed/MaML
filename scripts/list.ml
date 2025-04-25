open Core

(* let _ = 1 + 2 + 3 + 4 + 5 + 6 + 7 *)



module Test = struct
  let (++) x y = x + y + y

  infix 5 (++)

end