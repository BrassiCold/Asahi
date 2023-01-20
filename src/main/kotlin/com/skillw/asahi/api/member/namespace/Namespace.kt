package com.skillw.asahi.api.member.namespace

import com.skillw.asahi.api.AsahiManager
import com.skillw.asahi.api.member.AsahiRegistrable
import com.skillw.asahi.api.member.parser.infix.namespacing.BaseInfix
import com.skillw.asahi.api.member.parser.prefix.namespacing.BasePrefix

open class Namespace(override val key: String, val shared: Boolean = false) : AsahiRegistrable<String> {
    /** Prefix 前缀解释器容器， Token -> 前缀解释器 */
    internal val prefixMap = HashMap<String, BasePrefix<*>>()

    /** Infix 中缀解释器容器， 类型 -> 中缀解释器 */
    internal val infixMap = java.util.HashMap<Class<*>, BaseInfix<*>>()

    internal val allInfixTokens = HashSet<String>()

    fun hasPrefix(key: String): Boolean {
        return prefixMap.containsKey(key)
    }

    fun getPrefix(key: String): BasePrefix<*>? {
        return prefixMap[key]
    }

    fun registerPrefix(func: BasePrefix<*>) {
        val keys = listOf(func.key, *func.alias)
        keys.forEach { key ->
            prefixMap[key] = func
        }
    }

    fun <T : Any> getAction(type: Class<T>): BaseInfix<T> {
        return infixMap[type] as? BaseInfix<T>? ?: kotlin.run {
            val newAction = BaseInfix.createInfix(type)
            infixMap.entries.sortedWith { a, b ->
                if (a.key.isAssignableFrom(b.key)) -1 else 1
            }.forEach {
                newAction.putAll(it.value)
            }
            newAction.apply { register() }
        }
    }

    fun registerInfix(action: BaseInfix<*>) {
        val type = action.key
        if (infixMap.containsKey(type)) {
            infixMap[type]?.putAll(action)
            return
        }
        infixMap[type] = action
        action.actions.keys.forEach(allInfixTokens::add)
    }

    fun hasInfix(action: String?): Boolean {
        return allInfixTokens.contains(action)
    }

    fun hasInfix(type: Class<*>): Boolean {
        return infixMap.containsKey(type) || infixMap.keys.any { it.isAssignableFrom(type) }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> infixOf(any: T): BaseInfix<T> {
        return getAction(any::class.java as Class<T>)
    }

    override fun register() {
        AsahiManager.namespaces[key] = this
    }

}
