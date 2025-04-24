type 'a option = Some of 'a | None


module type BRIAN =
  sig
    type 'a t
    val amazing: int -> int
    val test: 'a t -> 'a t
    val other_test: int t -> int t
    val final: 'a -> 'a
  end

module Brian : BRIAN =
  struct
    type 'a t = Meow of 'a

    let amazing x = x
    let test a = a
    let other_test b =
      match b with
      | Meow (b') -> Meow (b')
      end
    let final x = x + 3

  end

