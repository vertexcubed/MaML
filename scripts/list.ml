(* let _ = 1 + 2 + 3 + 4 + 5 + 6 + 7 *)


let a = [1; 2; 3; 4; 5]
let _ = List.fold_left (op +) 0 a

let _ = List.nth_opt a 2