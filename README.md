# Krepost
The open source library with DSL syntax for creation of a robust repository layer for Android applications.
Библиотека с открытым сходным кодом для создания слоя Репозиторий в приложениях. В данный момент поддерживается только Android.

## Название
Krepost - производное от Kotlin REPOSiTory. А так же от слова Крепость.
Крепость символизирует надёжную, стабильную, но в тоже время понятную конструкцию с простым и понятным взаимодействием.

**Примечание:** Проект находится в ранней стадии разработки

## Установка

Скопировать исходники себе в проект :-)


## Пример реализации запроса в репозитории через Крепость:
```kotlin

val krepost = Krepost.initialize()

suspend fun fetchData1(): RequestResult<Int> =
        krepost.fetchDataMapped<String, Int> {
            action { getData() }
            cache {
                name = "fetchString"
                strategy = CacheStrategy.IfExist
                arguments("string", 1)
            }
            mapper { data -> data.toInt() }
        }
```

## Возможности
- DSL синтаксис
- Полностью на Kotlin
- Простая реализация Repository layer
- Null safety
- RequestResult для результата сетевого (или какого ещё либо suspend) запроса
- Результат или в suspend функции или через Flow (В разработке)
- Маппинг результата из DTO в Domain модели
- Маппинг ошибок выполнения запроса
- Кеширование
  - Разные стратегии использования кэша
    - IfExist - Если результат есть в эше - возвращать его, иначе выполнять запрос и кэшировать
    - IfNotAvailable - Выполнять запрос, если не удалось - возаращать результат из кэша
	- CachedThenLoad - Возвращать запрос из кэша, затем выполнять запрос и возвращать данные из него (только при использовании Flow)
  - Кэширование смапленных моделей
  - Любые способы храниения кэша
  
## RequestResult

Расширенная альтарнативная версия класса Result.
Позволяет обращаться к запросам из ViewModel или Interactor без использования блоков try catch и проверок на null для определения результатов запроса.

Может быть в 3 состояниях:
- Success (содержит данные)
- Cached (содержит данные, дату сохранения в кэш. Наследуется от Success)
- Failure (содержит выброшенное исключение, а так же данные ошибки, если используются).


```kotlin
val info = repository.getInfo(id)
when {
    info.isSuccess() -> {
		Log.d(TAG, "Receive info successfully. Load from (${if (info.isCached()) "cache" else "api"})")
        setNormalState(info.data)
    }
    info.isFailure() -> {
        Log.d(TAG, "Error loading data")
        val fail = info.asFailure()
        setErrorState(fail.message)
    }
}
```
