type 'a option = Some of 'a | None
type ('a, 'b) result = Ok of 'a | Error of 'b
type 'a list = Nil | Cons of 'a * 'a list
(* type exn = .. *)

(* Temporary. Replace with exception later *)
let invalid_arg s = s



(* String conversion functions *)

let string_of_bool b =
  match b with
  | true -> "true"
  | false -> "false"
  end

let bool_of_string s =
  match s with
  | "true" -> true
  | "false" -> false
  | _ -> invalid_arg false
  end

let bool_of_string_opt s =
  | match s with
  | "true" -> Some true
  | "false" -> Some false
  | _ -> None

external string_of_int: int -> string = "maml_string_of_int"
external string_of_float: float -> string = "maml_string_of_float"

(* Pairs *)

let fst (pair: 'a * 'b): 'a =
  match pair with
  | (x, _) -> x
  end

let snd (pair: 'a * 'b): 'b =
  match pair with
  | (_, y) -> y
  end


(* Printing *)

external print: string -> unit = "maml_print"
external println: string -> unit = "maml_println"