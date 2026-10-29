# Weather Physics Sable Fix

Мини-мод для **NeoForge 1.21.1**. Исправляет краш сервера у **Weather Physics** (`tornadophysics` 1.0.3) вместе с **Sable**:

```
java.lang.RuntimeException: Body has been removed
    at dev.ryanhcode.sable...RapierPhysicsPipeline.assertBodyValid
    at dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle.getLinearVelocity
    at net.killey.tornadophysics.event.TornadoEvent.processPhysics
```

## Причина

`TornadoEvent.processPhysics` берёт физическое тело подуровня Sable и спрашивает его скорость, не проверив, что тело ещё существует. Если подуровень удалён в этом же тике (развалился, слился, уничтожен), Sable бросает исключение и сервер падает. В соседнем классе `WindPhysics` тот же мод проверяет `RigidBodyHandle.isValid()`, а здесь проверки нет.

## Фикс

Mixin добавляет в начало `processPhysics` ту же проверку: если тело удалено, подуровень пропускается на этот тик. Больше ничего не меняется. Без Weather Physics мод ничего не делает.

Положить jar в `mods/` (на сервер; в одиночной игре — в клиент).

## Проверка

```bash
./gradlew runGameTestServer
```

Тест поднимает сервер с Sable, Weather2 и Weather Physics, создаёт подуровень Sable и удаляет его. Затем проверяет:

- прямой запрос скорости у удалённого тела бросает `Body has been removed` (краш воспроизводится);
- `TornadoEvent.processPhysics` с настоящим штормом Weather2 на этом теле больше не падает.

Без mixin тот же тест падает с `RuntimeException: Body has been removed`, как в краш-репорте.
