let add_func x y = x + y in
let apply_func func x y =
    func x y
in
apply_func (add_func) 1 4