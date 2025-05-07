
let length l =
  let rec go subl acc =
    match subl with
    | [] -> acc
    | _ :: xs -> go xs (acc + 1)
    end
  in
  go l 0


let rev l =
  let rec go subl acc =
    match subl with
    | [] -> acc
    | x :: xs -> go xs (x :: acc)
    end
  in
  go l []

let is_empty l =
  match l with
  | [] -> true
  | _ -> false
  end

let rec concat (l: 'a list list): 'a list =
  match l with
  | [] -> []
  | x :: xs -> x @ (concat xs)
  end

let rec map (f: 'a -> 'b) (l: 'a list): 'b list =
  match l with
  | [] -> []
  | x :: [] -> [(f x)]
  | x :: y :: xs ->
    (f x) :: (f y) :: (map f xs)
  end

let rec fold_left f acc l =
  match l with
  | [] -> acc
  | x :: xs -> fold_left f (f acc x) xs
  end

let rec fold_right f l acc =
  match l with
  | [] -> acc
  | x :: xs -> f x (fold_right f l acc)
  end

let rec filter (p: 'a -> bool) l =
  match l with
  | [] -> []
  | x :: xs ->
    if p x then x :: filter p xs else filter p xs
  end

let rec filter_opt l =
  match l with
  | [] -> []
  | x :: xs ->
    match x with
    | Some x -> x :: filter_opt xs
    | None -> filter_opt xs
    end
  end


let nth l n =
  let rec go subl acc =
    match subl with
(*    | [] -> failwith "nth" *)
    | x :: xs -> if acc = 0 then x else go xs (acc - 1)
    end
  in
  go l n

let nth_opt l n =
  let rec go subl acc =
    match subl with
    | [] -> None
    | x :: xs -> if acc = 0 then Some x else go xs (acc - 1)
    end
  in
  go l n

let mem p l =
  let rec go subl =
    match subl with
    | [] -> false
    | x :: xs -> if p = x then true else go xs
    end
  in
  go l
