package vertexcubed.maml.parse.ast

enum class Bop(val dataType: DataType) {
    ADD(DataType.INT),
    SUB(DataType.INT),
    MUL(DataType.INT),
    DIV(DataType.INT),
    MOD(DataType.INT),
    LT(DataType.INT),
    LTE(DataType.INT),
    GT(DataType.INT),
    GTE(DataType.INT),
    EQ(DataType.INT),
    NEQ(DataType.INT),
    AND(DataType.BOOL),
    OR(DataType.BOOL),

    ;


    enum class DataType {
        INT,
        BOOL
    }
}