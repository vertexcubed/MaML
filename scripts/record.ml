let f x =
    {x with size=10}


let _ =
    let b = f {name="Hi"; age=12; color="meow"} in
    b