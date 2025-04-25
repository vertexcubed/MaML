(** List operations.

*)


(** Returns the length of the given list. *)
val length: 'a list -> int

(** Reverses the given list. *)
val rev: 'a list -> 'a list

(** Returns true if and only if the list is empty *)
val is_empty: 'a list -> bool

(** Concats a list of lists. *)
val concat: 'a list list -> 'a list

(** Maps all values in a list using the given mapping function *)
val map: ('a -> 'b) -> 'a list -> 'b list

(** fold_left f init [a1; a2; ... aN] is equivalent to f (... f ( (f init a1) a2) ...) aN  *)
val fold_left: ('acc -> 'a -> 'acc) -> 'acc -> 'a list -> 'acc

(** fold_right f [a1; a2; ... aN] init is equivalent to f a1 (f a2 (... (f aN init) ...))  *)
val fold_right: ('a -> 'acc -> 'acc) -> 'a list -> 'acc -> 'acc

(** Filters a list using the given predicate. Elements are kept if the predicate is true *)
val filter: ('a -> bool) -> 'a list -> 'a list

(** Filters an option list. Elements are kept if they are Some e *)
val filter_opt: 'a option list -> 'b list

(** Returns the nth element in a list *)
val nth: 'a list -> int -> 'a

(** Returns the nth element in a list, or none if index is greater than length *)
val nth_opt: 'a list -> int -> 'a option

(** Returns true if the given element is a member of the given list *)
val mem: 'a -> 'a list -> bool