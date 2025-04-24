type 'a option = Some of 'a | None


module type BRIAN =
  sig
    type 'a t
    val amazing: int -> int

  end

module type BRIAN_OPT =
  sig
    include BRIAN
    val other: t -> t option
  end

module Brian : BRIAN_OPT =
  struct
    type 'a t = Meow of 'a
    let amazing x = x
    let other x = Some x
    let priv = 6
  end


let _ = Brian.amazing 5