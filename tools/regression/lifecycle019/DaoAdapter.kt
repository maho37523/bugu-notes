package com.dspro.android.data.notes

import androidx.room.Entity
import androidx.room.Query
import androidx.room.Insert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.lang.reflect.*
import java.sql.Connection
import java.sql.ResultSet

/** Uses the actual DAO annotations and entity constructors; the repository and SQL under test are production sources. */
fun <T> daoAdapter(type:Class<T>, connection:Connection):T = type.cast(Proxy.newProxyInstance(type.classLoader,arrayOf(type)) { _, method, raw ->
    if (method.name == "toString") return@newProxyInstance "SQLite DAO ${type.simpleName}"
    val args=raw?.toList().orEmpty()
    val insert=method.getAnnotation(Insert::class.java)
    val query=method.getAnnotation(Query::class.java)
    when {
        insert != null -> {
            val entity=args.first()!!
            val table=entity.javaClass.getAnnotation(Entity::class.java).tableName
            val fields=entity.javaClass.declaredFields.filter { !Modifier.isStatic(it.modifiers) && !it.isSynthetic }
            val sql="INSERT OR REPLACE INTO `$table` (${fields.joinToString(",") { "`${it.name}`" }}) VALUES (${fields.joinToString(",") { "?" }})"
            connection.prepareStatement(sql).use { stmt -> fields.forEachIndexed { i,f -> f.isAccessible=true;val v=f.get(entity);stmt.setObject(i+1,if(v is Boolean) if(v) 1 else 0 else v) };stmt.executeUpdate() }
            Unit
        }
        query != null -> {
            val bindings=method.parameters.mapIndexed { i,p -> p.name to args.getOrNull(i) }.toMap()
            val ordered=mutableListOf<Any?>()
            val sql=Regex(":([A-Za-z][A-Za-z0-9_]*)").replace(query.value) { match ->
                val key=match.groupValues[1];check(bindings.containsKey(key)) { "No argument $key for ${method.name}" };ordered.add(bindings[key]);"?"
            }
            fun queryRows(target:Type):Any? = connection.prepareStatement(sql).use { stmt ->
                ordered.forEachIndexed { i,v -> stmt.setObject(i+1,v) }
                if (!sql.trimStart().startsWith("SELECT",true) && !sql.trimStart().startsWith("PRAGMA",true)) {
                    val count=stmt.executeUpdate()
                    if (unwrap(target).typeName in listOf("int","java.lang.Integer")) count else Unit
                } else stmt.executeQuery().use { result -> readResult(result,unwrap(target)) }
            }
            val target=if (method.parameterTypes.lastOrNull()?.name == "kotlin.coroutines.Continuation")
                unwrap((method.genericParameterTypes.last() as ParameterizedType).actualTypeArguments.single()) else method.genericReturnType
            if (target is ParameterizedType && target.rawType == Flow::class.java) flow { emit(queryRows(target.actualTypeArguments.single())) }
            else queryRows(target)
        }
        else -> error("Unrecognized DAO method ${method.name}")
    }
})
private fun unwrap(type:Type):Type = if(type is WildcardType) unwrap(type.lowerBounds.firstOrNull() ?: type.upperBounds.first()) else type
private fun readResult(rs:ResultSet,target:Type):Any? {
    if(target is ParameterizedType && (target.rawType as? Class<*>)?.isAssignableFrom(List::class.java)==true) {
        val rows=mutableListOf<Any?>();while(rs.next()) rows.add(readRow(rs,unwrap(target.actualTypeArguments.single())));return rows
    }
    return if(rs.next()) readRow(rs,target) else null
}
private fun readRow(rs:ResultSet,target:Type):Any? {
    val cls=target as Class<*>
    if (cls==String::class.java) return rs.getString(1)
    if(cls==Int::class.java || cls==Integer::class.java) return rs.getObject(1)?.let { (it as Number).toInt() }
    if(cls==Long::class.java || cls==java.lang.Long::class.java) return rs.getObject(1)?.let { (it as Number).toLong() }
    val constructor=cls.declaredConstructors.first { it.parameterCount>0 && it.parameterTypes.last().name!="kotlin.jvm.internal.DefaultConstructorMarker" }
    val columns=(1..rs.metaData.columnCount).map { rs.metaData.getColumnLabel(it) }.toSet()
    val values=constructor.parameters.map { p ->
        check(p.name in columns) { "Missing column ${p.name} for ${cls.simpleName}" }
        val value=rs.getObject(p.name)
        if(value==null) null else when(p.type) {
            Boolean::class.java, java.lang.Boolean::class.java -> (value as Number).toInt()!=0
            Long::class.java, java.lang.Long::class.java -> (value as Number).toLong()
            Int::class.java, Integer::class.java -> (value as Number).toInt()
            else -> value.toString()
        }
    }.toTypedArray()
    return constructor.newInstance(*values)
}
