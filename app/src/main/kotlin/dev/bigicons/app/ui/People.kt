package dev.bigicons.app.ui

/** Участник проекта. [url] пустой, если ссылки нет. */
data class Person(val name: String, val role: String, val url: String = "")

/**
 * Списки для страницы «Инфо». Чтобы добавить человека, допишите строку в нужный список.
 */
object People {
    val developers = listOf(
        Person("vipa-ch-cloud", "Автор и разработчик", "https://github.com/vipa-ch-cloud"),
        Person("Claude Code (Anthropic)", "Ассистент по коду"),
    )

    /** Пока пусто: добавляйте сюда тех, кто поддержал проект. */
    val supporters: List<Person> = emptyList()
}
