# Рецепты последовательной сборки

Файл создан `tools/generate_data.py`, руками не править.

Каждая строка: стартовый предмет, затем шаги одного цикла, число циклов и итоговый расход слитков.
Форма (cast mould) держится в деплоере и не тратится.

## Create Big Cannons

| Деталь | Старт | Шаги одного цикла | Циклов | Слитков |
|---|---|---|---|---|
| `createbigcannons:cast_iron_autocannon_barrel` | `#c:ingots/cast_iron` | деплоер: `createbigcannons:autocannon_barrel_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/cast_iron` → пресс → пила | 2 | 3 |
| `createbigcannons:cast_iron_autocannon_breech` | `createbigcannons:cast_iron_autocannon_breech_extractor` | деплоер: `createbigcannons:autocannon_breech_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/cast_iron` → пресс | 3 | 3 + механизм |
| `createbigcannons:cast_iron_autocannon_recoil_spring` | `#c:ingots/cast_iron` | деплоер: `createbigcannons:autocannon_recoil_spring_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/cast_iron` → пресс → деплоер: `createbigcannons:recoil_spring` | 3 | 4 |
| `createbigcannons:bronze_autocannon_barrel` | `#c:ingots/bronze` | деплоер: `createbigcannons:autocannon_barrel_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → пресс → пила | 2 | 3 |
| `createbigcannons:bronze_autocannon_breech` | `createbigcannons:bronze_autocannon_breech_extractor` | деплоер: `createbigcannons:autocannon_breech_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → пресс | 3 | 3 + механизм |
| `createbigcannons:bronze_autocannon_recoil_spring` | `#c:ingots/bronze` | деплоер: `createbigcannons:autocannon_recoil_spring_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → пресс → деплоер: `createbigcannons:recoil_spring` | 3 | 4 |
| `createbigcannons:steel_autocannon_barrel` | `#c:ingots/steel` | деплоер: `createbigcannons:autocannon_barrel_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → пресс → пила | 2 | 3 |
| `createbigcannons:steel_autocannon_breech` | `createbigcannons:steel_autocannon_breech_extractor` | деплоер: `createbigcannons:autocannon_breech_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → пресс | 3 | 3 + механизм |
| `createbigcannons:steel_autocannon_recoil_spring` | `#c:ingots/steel` | деплоер: `createbigcannons:autocannon_recoil_spring_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → пресс → деплоер: `createbigcannons:recoil_spring` | 3 | 4 |

## CBC: Advanced Technologies

| Деталь | Старт | Шаги одного цикла | Циклов | Слитков |
|---|---|---|---|---|
| `cbc_at:cast_iron_autocannon_silencer` | `#c:ingots/cast_iron` | деплоер: `cbc_at:autocannon_silencer_mould` (остаётся в деплоере) → деплоер: `#c:ingots/cast_iron` → пресс → пила | 2 | 3 |
| `cbc_at:cast_iron_autocannon_muzzle_brake` | `#c:ingots/cast_iron` | деплоер: `cbc_at:autocannon_muzzle_brake_mould` (остаётся в деплоере) → деплоер: `#c:ingots/cast_iron` → пресс → пила | 2 | 3 |
| `cbc_at:cast_iron_twin_autocannon_barrel` | `#c:ingots/cast_iron` | деплоер: `cbc_at:twin_autocannon_barrel_mould` (остаётся в деплоере) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → пресс → пила | 2 | 5 |
| `cbc_at:cast_iron_twin_autocannon_recoil_spring` | `#c:ingots/cast_iron` | деплоер: `cbc_at:twin_autocannon_recoil_spring_mould` (остаётся в деплоере) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → пресс → деплоер: `createbigcannons:recoil_spring` | 2 | 5 |
| `cbc_at:cast_iron_twin_autocannon_breech` | `createbigcannons:cast_iron_autocannon_breech_extractor` | деплоер: `cbc_at:twin_autocannon_breech_mould` (остаётся в деплоере) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → пресс | 2 | 4 + механизм |
| `cbc_at:cast_iron_twin_autocannon_silencer` | `#c:ingots/cast_iron` | деплоер: `cbc_at:twin_autocannon_silencer_mould` (остаётся в деплоере) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → пресс → пила | 2 | 5 |
| `cbc_at:cast_iron_twin_autocannon_muzzle_brake` | `#c:ingots/cast_iron` | деплоер: `cbc_at:twin_autocannon_muzzle_brake_mould` (остаётся в деплоере) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → пресс → пила | 2 | 5 |
| `cbc_at:cast_iron_vert_twin_autocannon_barrel` | `#c:ingots/cast_iron` | деплоер: `cbc_at:vert_twin_autocannon_barrel_mould` (остаётся в деплоере) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → пресс → пила | 2 | 5 |
| `cbc_at:cast_iron_vert_twin_autocannon_recoil_spring` | `#c:ingots/cast_iron` | деплоер: `cbc_at:vert_twin_autocannon_recoil_spring_mould` (остаётся в деплоере) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → пресс → деплоер: `createbigcannons:recoil_spring` | 3 | 7 |
| `cbc_at:cast_iron_vert_twin_autocannon_breech` | `createbigcannons:cast_iron_autocannon_breech_extractor` | деплоер: `cbc_at:vert_twin_autocannon_breech_mould` (остаётся в деплоере) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → пресс | 3 | 6 + механизм |
| `cbc_at:cast_iron_vert_twin_autocannon_silencer` | `#c:ingots/cast_iron` | деплоер: `cbc_at:vert_twin_autocannon_silencer_mould` (остаётся в деплоере) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → пресс → пила | 3 | 7 |
| `cbc_at:cast_iron_vert_twin_autocannon_muzzle_brake` | `#c:ingots/cast_iron` | деплоер: `cbc_at:vert_twin_autocannon_muzzle_brake_mould` (остаётся в деплоере) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → пресс → пила | 2 | 5 |
| `cbc_at:cast_iron_heavy_autocannon_barrel` | `#c:ingots/cast_iron` | деплоер: `cbc_at:heavy_autocannon_barrel_mould` (остаётся в деплоере) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → пресс → пила | 2 | 5 |
| `cbc_at:cast_iron_heavy_autocannon_muzzle_brake` | `#c:ingots/cast_iron` | деплоер: `cbc_at:heavy_autocannon_muzzle_brake_mould` (остаётся в деплоере) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → пресс → пила | 2 | 5 |
| `cbc_at:cast_iron_heavy_autocannon_silencer` | `#c:ingots/cast_iron` | деплоер: `cbc_at:heavy_autocannon_silencer_mould` (остаётся в деплоере) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → пресс → пила | 2 | 5 |
| `cbc_at:cast_iron_heavy_autocannon_recoil_spring` | `#c:ingots/cast_iron` | деплоер: `cbc_at:heavy_autocannon_recoil_spring_mould` (остаётся в деплоере) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → пресс → деплоер: `createbigcannons:recoil_spring` | 2 | 5 |
| `cbc_at:cast_iron_heavy_autocannon_breech` | `createbigcannons:cast_iron_autocannon_breech_extractor` | деплоер: `cbc_at:heavy_autocannon_breech_mould` (остаётся в деплоере) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → пресс | 2 | 4 + механизм |
| `cbc_at:cast_iron_heavy_autocannon_qfbreech` | `createbigcannons:cast_iron_autocannon_breech_extractor` | деплоер: `cbc_at:heavy_autocannon_qfbreech_mould` (остаётся в деплоере) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → пресс | 2 | 4 + механизм |
| `cbc_at:cast_iron_rocket_pod_rail` | `#c:ingots/cast_iron` | деплоер: `cbc_at:rocket_pod_rail_mould` (остаётся в деплоере) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → пресс → пила | 4 | 9 |
| `cbc_at:cast_iron_rocket_pod_breech` | `#c:ingots/cast_iron` | деплоер: `cbc_at:rocket_pod_breech_mould` (остаётся в деплоере) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → пресс | 4 | 9 |
| `cbc_at:bronze_autocannon_silencer` | `#c:ingots/bronze` | деплоер: `cbc_at:autocannon_silencer_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → пресс → пила | 2 | 3 |
| `cbc_at:bronze_autocannon_muzzle_brake` | `#c:ingots/bronze` | деплоер: `cbc_at:autocannon_muzzle_brake_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → пресс → пила | 2 | 3 |
| `cbc_at:bronze_twin_autocannon_barrel` | `#c:ingots/bronze` | деплоер: `cbc_at:twin_autocannon_barrel_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → пресс → пила | 2 | 5 |
| `cbc_at:bronze_twin_autocannon_recoil_spring` | `#c:ingots/bronze` | деплоер: `cbc_at:twin_autocannon_recoil_spring_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → пресс → деплоер: `createbigcannons:recoil_spring` | 2 | 5 |
| `cbc_at:bronze_twin_autocannon_breech` | `createbigcannons:bronze_autocannon_breech_extractor` | деплоер: `cbc_at:twin_autocannon_breech_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → пресс | 2 | 4 + механизм |
| `cbc_at:bronze_twin_autocannon_silencer` | `#c:ingots/bronze` | деплоер: `cbc_at:twin_autocannon_silencer_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → пресс → пила | 2 | 5 |
| `cbc_at:bronze_twin_autocannon_muzzle_brake` | `#c:ingots/bronze` | деплоер: `cbc_at:twin_autocannon_muzzle_brake_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → пресс → пила | 2 | 5 |
| `cbc_at:bronze_vert_twin_autocannon_barrel` | `#c:ingots/bronze` | деплоер: `cbc_at:vert_twin_autocannon_barrel_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → пресс → пила | 2 | 5 |
| `cbc_at:bronze_vert_twin_autocannon_recoil_spring` | `#c:ingots/bronze` | деплоер: `cbc_at:vert_twin_autocannon_recoil_spring_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → пресс → деплоер: `createbigcannons:recoil_spring` | 3 | 7 |
| `cbc_at:bronze_vert_twin_autocannon_breech` | `createbigcannons:bronze_autocannon_breech_extractor` | деплоер: `cbc_at:vert_twin_autocannon_breech_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → пресс | 3 | 6 + механизм |
| `cbc_at:bronze_vert_twin_autocannon_silencer` | `#c:ingots/bronze` | деплоер: `cbc_at:vert_twin_autocannon_silencer_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → пресс → пила | 3 | 7 |
| `cbc_at:bronze_vert_twin_autocannon_muzzle_brake` | `#c:ingots/bronze` | деплоер: `cbc_at:vert_twin_autocannon_muzzle_brake_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → пресс → пила | 2 | 5 |
| `cbc_at:bronze_heavy_autocannon_barrel` | `#c:ingots/bronze` | деплоер: `cbc_at:heavy_autocannon_barrel_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → пресс → пила | 2 | 5 |
| `cbc_at:bronze_heavy_autocannon_muzzle_brake` | `#c:ingots/bronze` | деплоер: `cbc_at:heavy_autocannon_muzzle_brake_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → пресс → пила | 2 | 5 |
| `cbc_at:bronze_heavy_autocannon_silencer` | `#c:ingots/bronze` | деплоер: `cbc_at:heavy_autocannon_silencer_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → пресс → пила | 2 | 5 |
| `cbc_at:bronze_heavy_autocannon_recoil_spring` | `#c:ingots/bronze` | деплоер: `cbc_at:heavy_autocannon_recoil_spring_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → пресс → деплоер: `createbigcannons:recoil_spring` | 2 | 5 |
| `cbc_at:bronze_heavy_autocannon_breech` | `createbigcannons:bronze_autocannon_breech_extractor` | деплоер: `cbc_at:heavy_autocannon_breech_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → пресс | 2 | 4 + механизм |
| `cbc_at:bronze_heavy_autocannon_qfbreech` | `createbigcannons:bronze_autocannon_breech_extractor` | деплоер: `cbc_at:heavy_autocannon_qfbreech_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → пресс | 2 | 4 + механизм |
| `cbc_at:bronze_rocket_pod_rail` | `#c:ingots/bronze` | деплоер: `cbc_at:rocket_pod_rail_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → пресс → пила | 4 | 9 |
| `cbc_at:bronze_rocket_pod_breech` | `#c:ingots/bronze` | деплоер: `cbc_at:rocket_pod_breech_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → пресс | 4 | 9 |
| `cbc_at:steel_autocannon_silencer` | `#c:ingots/steel` | деплоер: `cbc_at:autocannon_silencer_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → пресс → пила | 2 | 3 |
| `cbc_at:steel_autocannon_muzzle_brake` | `#c:ingots/steel` | деплоер: `cbc_at:autocannon_muzzle_brake_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → пресс → пила | 2 | 3 |
| `cbc_at:steel_twin_autocannon_barrel` | `#c:ingots/steel` | деплоер: `cbc_at:twin_autocannon_barrel_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → пресс → пила | 2 | 5 |
| `cbc_at:steel_twin_autocannon_recoil_spring` | `#c:ingots/steel` | деплоер: `cbc_at:twin_autocannon_recoil_spring_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → пресс → деплоер: `createbigcannons:recoil_spring` | 2 | 5 |
| `cbc_at:steel_twin_autocannon_breech` | `createbigcannons:steel_autocannon_breech_extractor` | деплоер: `cbc_at:twin_autocannon_breech_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → пресс | 2 | 4 + механизм |
| `cbc_at:steel_twin_autocannon_silencer` | `#c:ingots/steel` | деплоер: `cbc_at:twin_autocannon_silencer_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → пресс → пила | 2 | 5 |
| `cbc_at:steel_twin_autocannon_muzzle_brake` | `#c:ingots/steel` | деплоер: `cbc_at:twin_autocannon_muzzle_brake_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → пресс → пила | 2 | 5 |
| `cbc_at:steel_vert_twin_autocannon_barrel` | `#c:ingots/steel` | деплоер: `cbc_at:vert_twin_autocannon_barrel_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → пресс → пила | 2 | 5 |
| `cbc_at:steel_vert_twin_autocannon_recoil_spring` | `#c:ingots/steel` | деплоер: `cbc_at:vert_twin_autocannon_recoil_spring_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → пресс → деплоер: `createbigcannons:recoil_spring` | 3 | 7 |
| `cbc_at:steel_vert_twin_autocannon_breech` | `createbigcannons:steel_autocannon_breech_extractor` | деплоер: `cbc_at:vert_twin_autocannon_breech_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → пресс | 3 | 6 + механизм |
| `cbc_at:steel_vert_twin_autocannon_silencer` | `#c:ingots/steel` | деплоер: `cbc_at:vert_twin_autocannon_silencer_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → пресс → пила | 3 | 7 |
| `cbc_at:steel_vert_twin_autocannon_muzzle_brake` | `#c:ingots/steel` | деплоер: `cbc_at:vert_twin_autocannon_muzzle_brake_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → пресс → пила | 2 | 5 |
| `cbc_at:steel_heavy_autocannon_barrel` | `#c:ingots/steel` | деплоер: `cbc_at:heavy_autocannon_barrel_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → пресс → пила | 2 | 5 |
| `cbc_at:steel_heavy_autocannon_muzzle_brake` | `#c:ingots/steel` | деплоер: `cbc_at:heavy_autocannon_muzzle_brake_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → пресс → пила | 2 | 5 |
| `cbc_at:steel_heavy_autocannon_silencer` | `#c:ingots/steel` | деплоер: `cbc_at:heavy_autocannon_silencer_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → пресс → пила | 2 | 5 |
| `cbc_at:steel_heavy_autocannon_recoil_spring` | `#c:ingots/steel` | деплоер: `cbc_at:heavy_autocannon_recoil_spring_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → пресс → деплоер: `createbigcannons:recoil_spring` | 2 | 5 |
| `cbc_at:steel_heavy_autocannon_breech` | `createbigcannons:steel_autocannon_breech_extractor` | деплоер: `cbc_at:heavy_autocannon_breech_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → пресс | 2 | 4 + механизм |
| `cbc_at:steel_heavy_autocannon_qfbreech` | `createbigcannons:steel_autocannon_breech_extractor` | деплоер: `cbc_at:heavy_autocannon_qfbreech_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → пресс | 2 | 4 + механизм |
| `cbc_at:steel_rocket_pod_rail` | `#c:ingots/steel` | деплоер: `cbc_at:rocket_pod_rail_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → пресс → пила | 4 | 9 |
| `cbc_at:steel_rocket_pod_breech` | `#c:ingots/steel` | деплоер: `cbc_at:rocket_pod_breech_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → пресс | 4 | 9 |

## CBC Neo Warfare

| Деталь | Старт | Шаги одного цикла | Циклов | Слитков |
|---|---|---|---|---|
| `cbcmodernwarfare:cast_iron_mediumcannon_barrel` | `#c:ingots/cast_iron` | деплоер: `cbcmodernwarfare:mediumcannon_barrel_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/cast_iron` → пресс → пила | 2 | 3 |
| `cbcmodernwarfare:cast_iron_mediumcannon_breech` | `cbcmodernwarfare:cast_iron_mediumcannon_falling_breech` | деплоер: `cbcmodernwarfare:mediumcannon_breech_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/cast_iron` → пресс | 3 | 3 + механизм |
| `cbcmodernwarfare:cast_iron_mediumcannon_recoil_barrel` | `#c:ingots/cast_iron` | деплоер: `cbcmodernwarfare:mediumcannon_recoil_barrel_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/cast_iron` → пресс → деплоер: `cbcmodernwarfare:heavy_recoil_spring` | 3 | 4 |
| `cbcmodernwarfare:bronze_mediumcannon_barrel` | `#c:ingots/bronze` | деплоер: `cbcmodernwarfare:mediumcannon_barrel_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → пресс → пила | 2 | 3 |
| `cbcmodernwarfare:bronze_mediumcannon_breech` | `cbcmodernwarfare:bronze_mediumcannon_falling_breech` | деплоер: `cbcmodernwarfare:mediumcannon_breech_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → пресс | 3 | 3 + механизм |
| `cbcmodernwarfare:bronze_mediumcannon_recoil_barrel` | `#c:ingots/bronze` | деплоер: `cbcmodernwarfare:mediumcannon_recoil_barrel_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → пресс → деплоер: `cbcmodernwarfare:heavy_recoil_spring` | 3 | 4 |
| `cbcmodernwarfare:steel_mediumcannon_barrel` | `#c:ingots/steel` | деплоер: `cbcmodernwarfare:mediumcannon_barrel_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → пресс → пила | 2 | 3 |
| `cbcmodernwarfare:steel_mediumcannon_breech` | `cbcmodernwarfare:steel_mediumcannon_falling_breech` | деплоер: `cbcmodernwarfare:mediumcannon_breech_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → пресс | 3 | 3 + механизм |
| `cbcmodernwarfare:steel_mediumcannon_recoil_barrel` | `#c:ingots/steel` | деплоер: `cbcmodernwarfare:mediumcannon_recoil_barrel_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → пресс → деплоер: `cbcmodernwarfare:heavy_recoil_spring` | 3 | 4 |
| `cbcmodernwarfare:nethersteel_mediumcannon_barrel` | `#c:ingots/nethersteel` | деплоер: `cbcmodernwarfare:mediumcannon_barrel_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/nethersteel` → пресс → пила | 2 | 3 |
| `cbcmodernwarfare:nethersteel_mediumcannon_breech` | `cbcmodernwarfare:nethersteel_mediumcannon_falling_breech` | деплоер: `cbcmodernwarfare:mediumcannon_breech_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/nethersteel` → пресс | 3 | 3 + механизм |
| `cbcmodernwarfare:nethersteel_mediumcannon_recoil_barrel` | `#c:ingots/nethersteel` | деплоер: `cbcmodernwarfare:mediumcannon_recoil_barrel_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/nethersteel` → пресс → деплоер: `cbcmodernwarfare:heavy_recoil_spring` | 3 | 4 |
| `cbcmodernwarfare:bronze_rotarycannon_barrel` | `#c:ingots/bronze` | деплоер: `cbcmodernwarfare:rotarycannon_barrel_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → пресс → пила | 2 | 3 |
| `cbcmodernwarfare:bronze_rotarycannon_breech` | `cbcmodernwarfare:bronze_rotarycannon_breech_extractor` | деплоер: `cbcmodernwarfare:rotarycannon_breech_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → пресс | 3 | 3 + механизм |
| `cbcmodernwarfare:bronze_rotarycannon_bearing` | `#c:ingots/bronze` | деплоер: `cbcmodernwarfare:rotarycannon_bearing_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/bronze` → пресс | 3 | 4 |
| `cbcmodernwarfare:steel_rotarycannon_barrel` | `#c:ingots/steel` | деплоер: `cbcmodernwarfare:rotarycannon_barrel_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → пресс → пила | 2 | 3 |
| `cbcmodernwarfare:steel_rotarycannon_breech` | `cbcmodernwarfare:steel_rotaryannon_breech_extractor` | деплоер: `cbcmodernwarfare:rotarycannon_breech_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → пресс | 3 | 3 + механизм |
| `cbcmodernwarfare:steel_rotarycannon_bearing` | `#c:ingots/steel` | деплоер: `cbcmodernwarfare:rotarycannon_bearing_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/steel` → пресс | 3 | 4 |
| `cbcmodernwarfare:nethersteel_rotarycannon_barrel` | `#c:ingots/nethersteel` | деплоер: `cbcmodernwarfare:rotarycannon_barrel_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/nethersteel` → пресс → пила | 2 | 3 |
| `cbcmodernwarfare:nethersteel_rotarycannon_breech` | `cbcmodernwarfare:nethersteel_rotarycannon_breech_extractor` | деплоер: `cbcmodernwarfare:rotarycannon_breech_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/nethersteel` → пресс | 3 | 3 + механизм |
| `cbcmodernwarfare:nethersteel_rotarycannon_bearing` | `#c:ingots/nethersteel` | деплоер: `cbcmodernwarfare:rotarycannon_bearing_cast_mould` (остаётся в деплоере) → деплоер: `#c:ingots/nethersteel` → пресс | 3 | 4 |

## Компоненты Neo Warfare (в 1.21.1 у них нет рецептов)

| Предмет | Старт | Шаги одного цикла | Циклов |
|---|---|---|---|
| `cbcmodernwarfare:bronze_rotarycannon_breech_extractor` | `#c:ingots/bronze` | деплоер: `#c:plates/iron` → пила | 2 |
| `cbcmodernwarfare:steel_rotaryannon_breech_extractor` | `#c:ingots/steel` | деплоер: `#c:plates/iron` → пила | 2 |
| `cbcmodernwarfare:nethersteel_rotarycannon_breech_extractor` | `#c:ingots/nethersteel` | деплоер: `#c:plates/iron` → пила | 2 |
| `cbcmodernwarfare:cast_iron_mediumcannon_falling_breech` | `#c:ingots/cast_iron` | деплоер: `create:andesite_alloy` → пресс → пила | 2 |
| `cbcmodernwarfare:bronze_mediumcannon_falling_breech` | `#c:ingots/bronze` | деплоер: `create:andesite_alloy` → пресс → пила | 2 |
| `cbcmodernwarfare:steel_mediumcannon_falling_breech` | `#c:ingots/steel` | деплоер: `create:andesite_alloy` → пресс → пила | 2 |
| `cbcmodernwarfare:nethersteel_mediumcannon_falling_breech` | `#c:ingots/nethersteel` | деплоер: `create:andesite_alloy` → пресс → пила | 2 |
| `cbcmodernwarfare:heavy_recoil_spring` | `createbigcannons:recoil_spring` | деплоер: `#c:plates/iron` → пресс | 2 |

## Формы

Распил любого бревна (`#minecraft:logs`) механической пилой:

- `cbcmodernwarfare:mediumcannon_barrel_cast_mould`
- `cbcmodernwarfare:mediumcannon_breech_cast_mould`
- `cbcmodernwarfare:mediumcannon_recoil_barrel_cast_mould`
- `cbcmodernwarfare:rotarycannon_barrel_cast_mould`
- `cbcmodernwarfare:rotarycannon_breech_cast_mould`
- `cbcmodernwarfare:rotarycannon_bearing_cast_mould`
- `cbc_at:twin_autocannon_barrel_mould` (рецепт CBC:AT 0.1.4c не загружается)
- `cbc_at:rocket_pod_rail_mould` (рецепт CBC:AT 0.1.4c не загружается)
- `cbc_at:rocket_pod_breech_mould` (рецепт CBC:AT 0.1.4c не загружается)
