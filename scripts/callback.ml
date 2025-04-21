module Callback = struct

  external register: string -> 'a -> unit = "maml_register_named_value"

end



let sum x y z =
  x + y + z

let rec factorial n =
  if n <= 1 then 1
  else n * factorial (n - 1)


let _ = Callback.register "factorial" factorial
let _ = Callback.register "sum" sum