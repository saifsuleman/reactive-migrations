package net.saifs.reactivemigrations.extension

import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.ResolvableType
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

private var internal: AnnotationConfigApplicationContext? = null

var context: AnnotationConfigApplicationContext
    get() = internal ?: error("Attempted to access context before initialization")
    set(value) {
        if (internal != null) error("Attempted to set context multiple times")
        internal = value
    }

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T : Any> inject(): Lazy<T> =
    lazy(LazyThreadSafetyMode.PUBLICATION) {
        val type = typeOf<T>()
        if (type.classifier == List::class) {
            val parameter = type.arguments.first().type ?: error("Attempted to inject star-projected collection")
            return@lazy context.getBeanNamesForType(ResolvableType.forType(parameter.javaType))
                .map(context::getBean)
                .filterIsInstance<T>() as T
        }
        context.getBeanProvider<T>(ResolvableType.forType(type.javaType)).getObject()
    }