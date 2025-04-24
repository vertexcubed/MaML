external print: 'a -> unit = "maml_core_print"
external println: 'a -> unit = "maml_core_println"

type 'a list = [] | (::) of 'a * 'a list
external (::): 'a -> 'a list -> 'a list = "maml_list_cons"


(* let _ = 1 + 2 + 3 + 4 + 5 + 6 + 7 *)




let f x =
  match x with
  | [] -> 5
  | x :: xs -> 1
  end


let _ = f [1; 2; 3; 4; 5;]
