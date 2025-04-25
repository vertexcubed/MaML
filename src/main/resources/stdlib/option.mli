

(** Gets the value in this option, or run function otherwise *)
val get_or_else: 'a option -> (unit -> 'a) -> 'a

(** Gets the value in this option, or default otherwise *)
val get_default: 'a option -> 'a -> 'a

(** Gets the value in this option. Throws Invalid_argument if None *)
val get: 'a option -> 'a

(** Monadic option bind. *)
val bind: 'a option -> ('a -> 'b option) -> 'b option

(** Monadic left composition. Returns first value if both values are Some *)
val left: 'a option -> 'b option -> 'a option

(** Monadic right composition. Returns second value if both values are Some *)
val right: 'a option -> 'b option -> 'b option

(** Monadic mapping function. Runs mapping function if value is Some, or returns None *)
val map: 'a option -> ('a -> 'b) -> 'b option

(** Returns true if option is Some *)
val is_some: 'a option -> bool

(** Returns true if option is None *)
val is_none: 'a option -> bool

(** Performs compare function comparison on optional values. None is always less than any Some *)
val compare: ('a -> 'a -> int) -> 'a option -> 'a option -> int