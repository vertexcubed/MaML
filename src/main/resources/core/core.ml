

(* Exceptions *)

type exn = ..

external raise: exn -> 'a = "maml_core_raise"

(** Raised when none of the cases in a match statement are matched. Formatted as file_name, line# *)
exception Match_failure of (string * int)

(** Raised when an assertion fails. In the format file_name, line# *)
exception Assert_failure of (string * int)


(** Thrown when this function's arguments don't make sense. *)
exception Invalid_argument of string

(** Thrown in general failures. *)
exception Failure of string

(** Thrown when some element can't be found in a search function. *)
exception Not_found

(** Thrown when out of memory. *)
exception Out_of_memory

(** Thrown when the stack is too large. Generally indicates infinite recursion problems *)
exception Stack_overflow


(** Raised when trying to divide by zero *)
exception Division_by_zero

let failwith s = raise (Failure s)
let invalid_arg s = raise (Invalid_argument s)



(* Basic Operators *)

external (+): int -> int -> int = "maml_core_add"
infix 5 (+)
external (-): int -> int -> int = "maml_core_sub"
infix 5 (-)
external ( * ): int -> int -> int = "maml_core_mul"
infix 6 ( * )
external (/): int -> int -> int = "maml_core_div"
infix 6 (/)
external (%): int -> int -> int = "maml_core_mod"
infix 6 (%)

external (+.): int -> int -> int = "maml_core_addf"
infix 5 (+)
external (-.): int -> int -> int = "maml_core_subf"
infix 5 (-.)
external ( *.): int -> int -> int = "maml_core_mulf"
infix 6 ( *.)
external (/.): int -> int -> int = "maml_core_divf"
infix 6 (/.)
external (%.): int -> int -> int = "maml_core_modf"
infix 6 (%.)

external (=): 'a -> 'a -> bool = "maml_core_eq"
infix 2 (=)
external (!=): 'a -> 'a -> bool = "maml_core_neq"
infix 2 (!=)
external (<): 'a -> 'a -> bool = "maml_core_lt"
infix 2 (<)
external (<=): 'a -> 'a -> bool = "maml_core_lte"
infix 2 (<=)
external (>): 'a -> 'a -> bool = "maml_core_gt"
infix 2 (>)
external (>=): 'a -> 'a -> bool = "maml_core_gte"
infix 2 (>=)

external (!): bool -> bool = "maml_core_not"
external (&&): bool -> bool = "maml_core_and"
infixr 1 (&&)
external (||): bool -> bool = "maml_core_or"
infixr 0 (||)

external (~-): int -> int = "maml_core_negate"
external (~-.): int -> int = "maml_core_negatef"







type 'a option = Some of 'a | None
type ('a, 'b) result = Ok of 'a | Error of 'b


(* Integer arithmetic *)
external int_of_float: float -> int = "maml_core_int_of_float"

let sign (x: int): int =
  if x < 0 then -1 else 1


(* Floating point arithmetic *)

external float_of_int: int -> float = "maml_core_float_of_int"

external sqrt: float -> float = "maml_core_sqrt"
external ( ** ): float -> float -> float = "maml_core_pow"
infixr 7 ( ** )
external log: float -> float = "maml_core_log"
external log10: float -> float = "maml_core_log10"

external sin: float -> float = "maml_core_sin"
external cos: float -> float = "maml_core_cos"
external tan: float -> float = "maml_core_tan"
external asin: float -> float = "maml_core_asin"
external acos: float -> float = "maml_core_acos"
external atan: float -> float = "maml_core_atan"
external atan2: float -> float -> float = "maml_core_atan2"

external floor: float -> float = "maml_core_floor"
external ceil: float -> float = "maml_core_ceil"
external round: float -> float = "maml_core_round"

let signf (x: float): float =
  if x < 0.0 then -1.0 else 1.0


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
  | _ -> invalid_arg s
  end

let bool_of_string_opt s =
  match s with
  | "true" -> Some true
  | "false" -> Some false
  | _ -> None
  end

external string_of_int: int -> string = "maml_core_string_of_int"

external int_of_string: string -> int = "maml_core_int_of_string"

let int_of_string_opt s =
  try Some (int_of_string s) with
  | Invalid_argument _ -> None
  end


external string_of_float: float -> string = "maml_core_string_of_float"

external float_of_string: string -> int = "maml_core_float_of_string"

let float_of_string_opt s =
  try Some (float_of_string s) with
  | Invalid_argument _ -> None
  end



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
external print: 'a -> unit = "maml_core_print"
external println: 'a -> unit = "maml_core_println"


(* List *)
type 'a list = [] | (::) of 'a * 'a list
external (::): 'a -> 'a list -> 'a list = "maml_list_cons"
infixr 4 (::)

let rec (@) l1 l2 =
  match l1 with
  | [] -> l2
  | h1 :: [] -> h1 :: l2
  | h1 :: h2 :: [] -> h1 :: h2 :: l2
  | h1 :: h2 :: h3 :: tl -> (h1 :: h2 :: h3 :: ((@) tl l2))
  end
infixr 3 (@)