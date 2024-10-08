## Идея

У нас есть две никак не связанные ячейки `X` и `Y`. 

Если мы получим значение `X` , а затем `Y`, то в общем случае мы не знаем что на момент получения `Y` было в `X`.

Поэтому было решено наложить ограничения на `X`. Самое очевидное - это придать `X` свойство монотонности. 
Получив когда-то `X` мы знаем, что далее все полученные значения `X` будут не меньше данного.

Базовая идея: X > 0, разобъем все значения `Y` на части

- `Y` будет увеличиваться от `-X` до 0 - все значения отвечаются пользователю
- Затем, когда `Y` становится неотрицательным мы пытаемся увеличить его до `X`
- и когда `Y = X`, делаем `X++; Y = -X`

Возникает проблема, как это сделать неблокирующе. Если в одном потоке сделать `X++`, 
то второй поток может сделать `Y = X; X++; Y = -X` и тогда в силу монотонности в данном случае мы потеряем одно значение `X`.
А в общем случае можем потерять произвольное количество значений `X`.

Такое не годится, усложняем алгоритм. Тут нам пригодятся отрицательные значения X.

Отныне `X` не положительный и монотонный, а монотонный относительно **абсолютного значения**. 
То есть если `X = -7 или 7`, то при следующих прочтениях `X not in [-6:6]`.

Теперь будет происходить следующее
- когда `Y > 0 && Y = X`, мы делаем `X.compareAndSet(X, -X)` после этого `Y` изменяться не может, пока мы не изменим `X`,
так как `Y` больше или равен всех возможных значений `X` которые мы получили до этого
- когда `Y > 0 && X = -Y`, мы делаем `Y.compareAndSet(Y, -Y)`
- когда `Y < 0 && X = Y`, мы делаем `X.compareAndSet(X, -X + 1)`, 
после чего опять можем начинать инкрементить `Y` и возвращать значения

Таким образом в моменты когда `|Y| = |X|` все потоки пытаются помочь сделать 
- `X = -X`
- `Y = X`
- `X = -X + 1`

Причем во время этой операции старые потоки со старыми `X` и `Y` не могут нам навредить, так как 
- Если `Y outdated, то мы не сможем сделать Y.compareAndSet`, иначе
- Если `|X| < |Y|` мы уйдем заново брать новые значения `X` и `Y`
- Если `|X| == |Y|` мы будем помогать обновлять
- `|X| > |Y|` быть не может по алгоритму

У алгоритма есть одна проблема: в случае когда мы заинкрементили Y, мы не можем быть уверены, 
что прочитанный Х правильный, и нам надо заново его проверить. В таком случае на каждом витке может теряться до `thread` значений `Y`.
То есть на каждом витке в худшем случае Y может принимать (X - thread) значений

Итоговое количество значений `2^31 * \sum_x=2^MAX_INT(x - thread) = 2^31 * (2^30 - thread)`

В лучшем случае: `2^31 * 2^30`

## Тестирование

Эквивалентные версии алгоритма
- RealTest - версия алгоритма, которая проверяется нагрузочным тестированием
- ConsequentTest - версия алгоритма, в которой мы случайно выбираем поток, исполнияем его до следующей операции чтения/записи, и повторяем это в цикле.

## PS 

Я знаю что описание идеи не является доказательством, но алгоритм слишком сложный чтобы это сделать.

По-хорошему, тут все можно формально доказать в каком-нибудь coq.

А для тестовой работы, наверное, хватит двух видов тестов.