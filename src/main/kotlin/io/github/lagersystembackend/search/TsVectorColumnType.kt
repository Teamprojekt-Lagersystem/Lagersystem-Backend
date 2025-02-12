import org.jetbrains.exposed.sql.ColumnType
import org.postgresql.util.PGobject

private const val TS_VECTOR_SQL_TYPE = "tsvector"

class TsVectorColumnType : ColumnType<String>(nullable = true) {
    override fun sqlType(): String = TS_VECTOR_SQL_TYPE

    override fun valueFromDB(value: Any): String? {
        return value.toString()
    }

    override fun valueToDB(value: String?): Any? {
        return PGobject().apply {
            type = TS_VECTOR_SQL_TYPE
            this.value = value
        }.value
    }


    override fun notNullValueToDB(value: String): Any {
        return PGobject().apply {
            type = TS_VECTOR_SQL_TYPE
            this.value = value
        }
    }

    override fun nonNullValueToString(value: String): String {
        return "'$value'"
    }
}