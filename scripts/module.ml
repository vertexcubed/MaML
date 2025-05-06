module type SIG =
  sig
    type 'a t

    val f: 'a -> 'a t


  end

module Mod: SIG =
  struct

    type 'a t = 'a option

    let f x = Some x

  end
