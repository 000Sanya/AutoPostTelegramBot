## Что бот может?

Если кратко, назначением бота является автоматизация и нормализация создания и
публикации постов (совокупность сообщений) в каком-либо чате/канале. Публикация
происходит из чата/канала, выбранного как `sourceChat` путем выставления
соответствующего `sourceChatId`.

На данный момент готов тот минимум, который не позволит превратить место с
записями для постинга в помойку и при этом вполне себе постить записи по
расписанию. Итак, возможности:

* Постинг по таймауту
* Возможность выбирать различные фильтры для постинга
    * Обычный фильтр - сначала лучшие
    * Обычный рандом - из лучших выбирается не самый старый, а случайный пост
    * "Умный фильтр", настраиваемый по периодам времени. В каждый период
    времени действует одна совокупность настроек:
        * Минимальный рейтинг, может быть не установлен, тогда считается, что
        минимального порога нет
        * Максимальный рейтинг, может быть не установлен, тогда считается, что
        максимального порога нет
        * Массив времени, по сути, пары "от"-"до"
        * Режим сортировки или "что выбрать?"
            * Восходящая - публикуются посты с минимальным рейтингом из
            выбранных
            * Нисходящая - публикуются посты с максимальным рейтингом из
            выбранных
            * Рандом - публикуются случайные посты из выбранных
* Наличие простейшей системы плагинов
* Поддержка форматов:
    * Изображения
    * Видео
    * Аудио
    * Голос
    * Текст
    * Медиагруппы (ВСЕГДА публикуются одним постом, но могут быть включены в
    другие посты)
    * Контакты
    * Геолокация
    * Документы

На данный момент существует только один плагин - сборщик мусора, который

* Отслеживает изменения лайков
* Раз в какое-то время может проверять наличие постов с рейтингом ниже
установленного и удалять их.

## Что планируется?

Планов много и честно признаюсь - я очень стараюсь себя сдерживать, чтобы не
наворотить делов и не писать лишнего, стараясь оставить это всё стабильным и
работающим, по пути отлаживая уже существующий код.

Итак, что будет?

* Больше плагинов
    * Публикация по рейтингу (на данный момент есть еще выделенный функционал
    триггеров постинга, но, скорее всего, он будет упразднен до плагина)
    * Сборщик мусора, который будет способен фильтровать записи в диапазоне или
    вне диапазона
* Больше фильтров постов
    * Добавление возможности постить не один пост за раз (точнее возможность
    есть, но фильтры её не используют)
    * Улучшение структуры фильтров
* Больше поддерживаемых форматов данных
* Возможность указания отдельной группы/канала для ведения логов бота

На данный момент это всё, что сходу приходит в голову. Со временем список будет
пополняться, кроме того, сюда не включены исправления багов, рефакторирг и
прочее, но это вполне очевидно. Так или иначе, жду отзывы и предложения в
[личке](https://t.me/insanusmokrassar).
