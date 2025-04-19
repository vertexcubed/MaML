let f x =
    match x with
    | {age=y; ..} ->
        {x with size=(y*10)}
    | _ -> {x with size=0}
    end


let _ =
    f {name="Hi"; age=12; color="meow"}