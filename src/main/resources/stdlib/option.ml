

let get_or_else o f =
  match o with
  | Some v -> v
  | None -> f ()
  end


let get_default o v =
  match o with
  | Some v -> v
  | None -> v
  end

let get o =
  match o with
  | Some v -> v
  | None -> invalid_arg "get"
  end


(* Monad operations *)


let bind o f =
  match o with
  | Some v -> f v
  | None -> None
  end


let left l r =
  match l with
  | Some _ ->
    match r with
    | Some _ -> l
    | None -> None
    end
  | None -> None
  end


let right l r =
  match l with
  | Some _ -> r
  | None -> None
  end

let map o f =
  match o with
  | Some v -> Some (f v)
  | None -> None
  end

let is_some o =
  match o with
  | Some _ -> true
  | None -> false
  end

let is_none o = !(is_some o)


let compare f l r =
  match l with
  | Some lv ->
    match r with
    | Some rv -> f lv rv
    | None -> 1
    end
  | None ->
    match r with
    | Some _ -> -1
    | None -> 0
    end
  end
