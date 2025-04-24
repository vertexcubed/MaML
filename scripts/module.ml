module A = struct
  module B = struct
    type 'a t = Meow of int
  end

  let f (x: 'a B.t) = x
end
open A


let e = A.f 2