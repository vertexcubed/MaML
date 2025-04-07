type 'a 'b option = Some of 'a | None | Other of 'b

(* TODO: Fix this not parsing *)
let my_func (x: int int option) = x