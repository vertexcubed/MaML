module A = struct


    module B = struct

        let meow = 3

    end

    let bark = 2

end

module C = struct
    let longMeow = A.B.meow * 2

end


let _ = C.longMeow + A.bark