let f x =
    let s = x.age * 10 in
    {x with size=s}




let b = f {name="Hi"; age=12}