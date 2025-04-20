module Core = struct

    external print: string -> unit = "maml_core_print"
    external println: string -> unit = "maml_core_println"

end
let _ = Core.println "Hello World!"


