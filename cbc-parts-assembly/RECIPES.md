# Рецепты последовательной сборки

Файл создан `tools/generate_data.py`, руками не править.

Рецепт есть только у деталей, которые делаются исключительно в мире (литьё и сверление),
и только если этот процесс в мире работает. Рецепт добавлен рядом с литьём, ничего не заменено.

Ресурсы те же, что у литья. Металл ×3, остальное в прежнем количестве:
форма держится в деплоере и не тратится (при литье она тоже возвращается).
Старт: один слиток материала. В каждом цикле часть металла идёт слитками (деплоер),
часть расплавом того же металла (дозатор), затем механический пресс и механическая пила.

Деталям, которым в мире после сверления нужен предмет (экстрактор, пружина), сборка даёт
incomplete-деталь, как после сверления. Затем деплоер ставит в неё этот предмет (один раз).

## Create Big Cannons

| Деталь | Литьё, слитков | Рецепт, слитков (слитками + расплавом) | Шаги одного цикла | Циклов | Потом деплоер |
|---|---|---|---|---|---|
| `createbigcannons:cast_iron_autocannon_barrel` | 3 | 9 (4 + 450 mB) | деплоер: `createbigcannons:autocannon_barrel_cast_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → дозатор: 450 mB `#c:molten_cast_iron` → пресс → пила | 1 | — |
| `createbigcannons:cast_iron_autocannon_breech` | 4 | 12 (5 + 630 mB) | деплоер: `createbigcannons:autocannon_breech_cast_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → дозатор: 315 mB `#c:molten_cast_iron` → пресс → пила | 2 | `createbigcannons:incomplete_cast_iron_autocannon_breech` + `createbigcannons:cast_iron_autocannon_breech_extractor` |
| `createbigcannons:cast_iron_autocannon_recoil_spring` | 4 | 12 (5 + 630 mB) | деплоер: `createbigcannons:autocannon_recoil_spring_cast_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → дозатор: 315 mB `#c:molten_cast_iron` → пресс → пила | 2 | `createbigcannons:incomplete_cast_iron_autocannon_recoil_spring` + `createbigcannons:recoil_spring` |
| `createbigcannons:bronze_autocannon_barrel` | 3 | 9 (4 + 450 mB) | деплоер: `createbigcannons:autocannon_barrel_cast_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → дозатор: 450 mB `#c:molten_bronze` → пресс → пила | 1 | — |
| `createbigcannons:bronze_autocannon_breech` | 4 | 12 (5 + 630 mB) | деплоер: `createbigcannons:autocannon_breech_cast_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → дозатор: 315 mB `#c:molten_bronze` → пресс → пила | 2 | `createbigcannons:incomplete_bronze_autocannon_breech` + `createbigcannons:bronze_autocannon_breech_extractor` |
| `createbigcannons:bronze_autocannon_recoil_spring` | 4 | 12 (5 + 630 mB) | деплоер: `createbigcannons:autocannon_recoil_spring_cast_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → дозатор: 315 mB `#c:molten_bronze` → пресс → пила | 2 | `createbigcannons:incomplete_bronze_autocannon_recoil_spring` + `createbigcannons:recoil_spring` |
| `createbigcannons:steel_autocannon_barrel` | 3 | 9 (4 + 450 mB) | деплоер: `createbigcannons:autocannon_barrel_cast_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → дозатор: 450 mB `#c:molten_steel` → пресс → пила | 1 | — |
| `createbigcannons:steel_autocannon_breech` | 4 | 12 (5 + 630 mB) | деплоер: `createbigcannons:autocannon_breech_cast_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → дозатор: 315 mB `#c:molten_steel` → пресс → пила | 2 | `createbigcannons:incomplete_steel_autocannon_breech` + `createbigcannons:steel_autocannon_breech_extractor` |
| `createbigcannons:steel_autocannon_recoil_spring` | 4 | 12 (5 + 630 mB) | деплоер: `createbigcannons:autocannon_recoil_spring_cast_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → дозатор: 315 mB `#c:molten_steel` → пресс → пила | 2 | `createbigcannons:incomplete_steel_autocannon_recoil_spring` + `createbigcannons:recoil_spring` |

## CBC: Advanced Technologies

| Деталь | Литьё, слитков | Рецепт, слитков (слитками + расплавом) | Шаги одного цикла | Циклов | Потом деплоер |
|---|---|---|---|---|---|
| `cbc_at:cast_iron_autocannon_silencer` | 3 | 9 (4 + 450 mB) | деплоер: `cbc_at:autocannon_silencer_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → дозатор: 450 mB `#c:molten_cast_iron` → пресс → пила | 1 | — |
| `cbc_at:cast_iron_autocannon_muzzle_brake` | 3 | 9 (4 + 450 mB) | деплоер: `cbc_at:autocannon_muzzle_brake_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → дозатор: 450 mB `#c:molten_cast_iron` → пресс → пила | 1 | — |
| `cbc_at:cast_iron_twin_autocannon_recoil_spring` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:twin_autocannon_recoil_spring_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → дозатор: 330 mB `#c:molten_cast_iron` → пресс → пила | 3 | — |
| `cbc_at:cast_iron_twin_autocannon_breech` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:twin_autocannon_breech_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → дозатор: 330 mB `#c:molten_cast_iron` → пресс → пила | 3 | — |
| `cbc_at:cast_iron_twin_autocannon_silencer` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:twin_autocannon_silencer_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → дозатор: 330 mB `#c:molten_cast_iron` → пресс → пила | 3 | — |
| `cbc_at:cast_iron_twin_autocannon_muzzle_brake` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:twin_autocannon_muzzle_brake_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → дозатор: 330 mB `#c:molten_cast_iron` → пресс → пила | 3 | — |
| `cbc_at:cast_iron_vert_twin_autocannon_barrel` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:vert_twin_autocannon_barrel_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → дозатор: 330 mB `#c:molten_cast_iron` → пресс → пила | 3 | — |
| `cbc_at:cast_iron_vert_twin_autocannon_recoil_spring` | 8 | 24 (10 + 1260 mB) | деплоер: `cbc_at:vert_twin_autocannon_recoil_spring_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → дозатор: 420 mB `#c:molten_cast_iron` → пресс → пила | 3 | — |
| `cbc_at:cast_iron_vert_twin_autocannon_breech` | 8 | 24 (10 + 1260 mB) | деплоер: `cbc_at:vert_twin_autocannon_breech_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → дозатор: 420 mB `#c:molten_cast_iron` → пресс → пила | 3 | — |
| `cbc_at:cast_iron_vert_twin_autocannon_silencer` | 8 | 24 (10 + 1260 mB) | деплоер: `cbc_at:vert_twin_autocannon_silencer_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → дозатор: 420 mB `#c:molten_cast_iron` → пресс → пила | 3 | — |
| `cbc_at:cast_iron_vert_twin_autocannon_muzzle_brake` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:vert_twin_autocannon_muzzle_brake_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → дозатор: 330 mB `#c:molten_cast_iron` → пресс → пила | 3 | — |
| `cbc_at:cast_iron_heavy_autocannon_barrel` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:heavy_autocannon_barrel_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → дозатор: 330 mB `#c:molten_cast_iron` → пресс → пила | 3 | — |
| `cbc_at:cast_iron_heavy_autocannon_muzzle_brake` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:heavy_autocannon_muzzle_brake_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → дозатор: 330 mB `#c:molten_cast_iron` → пресс → пила | 3 | — |
| `cbc_at:cast_iron_heavy_autocannon_silencer` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:heavy_autocannon_silencer_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → дозатор: 330 mB `#c:molten_cast_iron` → пресс → пила | 3 | — |
| `cbc_at:cast_iron_heavy_autocannon_recoil_spring` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:heavy_autocannon_recoil_spring_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → дозатор: 330 mB `#c:molten_cast_iron` → пресс → пила | 3 | — |
| `cbc_at:cast_iron_heavy_autocannon_breech` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:heavy_autocannon_breech_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → дозатор: 330 mB `#c:molten_cast_iron` → пресс → пила | 3 | — |
| `cbc_at:cast_iron_heavy_autocannon_qfbreech` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:heavy_autocannon_qfbreech_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → дозатор: 330 mB `#c:molten_cast_iron` → пресс → пила | 3 | — |
| `cbc_at:bronze_autocannon_silencer` | 3 | 9 (4 + 450 mB) | деплоер: `cbc_at:autocannon_silencer_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → дозатор: 450 mB `#c:molten_bronze` → пресс → пила | 1 | — |
| `cbc_at:bronze_autocannon_muzzle_brake` | 3 | 9 (4 + 450 mB) | деплоер: `cbc_at:autocannon_muzzle_brake_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → дозатор: 450 mB `#c:molten_bronze` → пресс → пила | 1 | — |
| `cbc_at:bronze_twin_autocannon_recoil_spring` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:twin_autocannon_recoil_spring_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → дозатор: 330 mB `#c:molten_bronze` → пресс → пила | 3 | — |
| `cbc_at:bronze_twin_autocannon_breech` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:twin_autocannon_breech_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → дозатор: 330 mB `#c:molten_bronze` → пресс → пила | 3 | — |
| `cbc_at:bronze_twin_autocannon_silencer` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:twin_autocannon_silencer_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → дозатор: 330 mB `#c:molten_bronze` → пресс → пила | 3 | — |
| `cbc_at:bronze_twin_autocannon_muzzle_brake` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:twin_autocannon_muzzle_brake_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → дозатор: 330 mB `#c:molten_bronze` → пресс → пила | 3 | — |
| `cbc_at:bronze_vert_twin_autocannon_barrel` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:vert_twin_autocannon_barrel_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → дозатор: 330 mB `#c:molten_bronze` → пресс → пила | 3 | — |
| `cbc_at:bronze_vert_twin_autocannon_recoil_spring` | 8 | 24 (10 + 1260 mB) | деплоер: `cbc_at:vert_twin_autocannon_recoil_spring_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → дозатор: 420 mB `#c:molten_bronze` → пресс → пила | 3 | — |
| `cbc_at:bronze_vert_twin_autocannon_breech` | 8 | 24 (10 + 1260 mB) | деплоер: `cbc_at:vert_twin_autocannon_breech_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → дозатор: 420 mB `#c:molten_bronze` → пресс → пила | 3 | — |
| `cbc_at:bronze_vert_twin_autocannon_silencer` | 8 | 24 (10 + 1260 mB) | деплоер: `cbc_at:vert_twin_autocannon_silencer_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → дозатор: 420 mB `#c:molten_bronze` → пресс → пила | 3 | — |
| `cbc_at:bronze_vert_twin_autocannon_muzzle_brake` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:vert_twin_autocannon_muzzle_brake_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → дозатор: 330 mB `#c:molten_bronze` → пресс → пила | 3 | — |
| `cbc_at:bronze_heavy_autocannon_barrel` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:heavy_autocannon_barrel_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → дозатор: 330 mB `#c:molten_bronze` → пресс → пила | 3 | — |
| `cbc_at:bronze_heavy_autocannon_muzzle_brake` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:heavy_autocannon_muzzle_brake_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → дозатор: 330 mB `#c:molten_bronze` → пресс → пила | 3 | — |
| `cbc_at:bronze_heavy_autocannon_silencer` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:heavy_autocannon_silencer_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → дозатор: 330 mB `#c:molten_bronze` → пресс → пила | 3 | — |
| `cbc_at:bronze_heavy_autocannon_recoil_spring` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:heavy_autocannon_recoil_spring_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → дозатор: 330 mB `#c:molten_bronze` → пресс → пила | 3 | — |
| `cbc_at:bronze_heavy_autocannon_breech` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:heavy_autocannon_breech_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → дозатор: 330 mB `#c:molten_bronze` → пресс → пила | 3 | — |
| `cbc_at:bronze_heavy_autocannon_qfbreech` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:heavy_autocannon_qfbreech_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → дозатор: 330 mB `#c:molten_bronze` → пресс → пила | 3 | — |
| `cbc_at:steel_autocannon_silencer` | 3 | 9 (4 + 450 mB) | деплоер: `cbc_at:autocannon_silencer_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → дозатор: 450 mB `#c:molten_steel` → пресс → пила | 1 | — |
| `cbc_at:steel_autocannon_muzzle_brake` | 3 | 9 (4 + 450 mB) | деплоер: `cbc_at:autocannon_muzzle_brake_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → дозатор: 450 mB `#c:molten_steel` → пресс → пила | 1 | — |
| `cbc_at:steel_twin_autocannon_recoil_spring` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:twin_autocannon_recoil_spring_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → дозатор: 330 mB `#c:molten_steel` → пресс → пила | 3 | — |
| `cbc_at:steel_twin_autocannon_breech` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:twin_autocannon_breech_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → дозатор: 330 mB `#c:molten_steel` → пресс → пила | 3 | — |
| `cbc_at:steel_twin_autocannon_silencer` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:twin_autocannon_silencer_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → дозатор: 330 mB `#c:molten_steel` → пресс → пила | 3 | — |
| `cbc_at:steel_twin_autocannon_muzzle_brake` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:twin_autocannon_muzzle_brake_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → дозатор: 330 mB `#c:molten_steel` → пресс → пила | 3 | — |
| `cbc_at:steel_vert_twin_autocannon_barrel` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:vert_twin_autocannon_barrel_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → дозатор: 330 mB `#c:molten_steel` → пресс → пила | 3 | — |
| `cbc_at:steel_vert_twin_autocannon_recoil_spring` | 8 | 24 (10 + 1260 mB) | деплоер: `cbc_at:vert_twin_autocannon_recoil_spring_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → дозатор: 420 mB `#c:molten_steel` → пресс → пила | 3 | — |
| `cbc_at:steel_vert_twin_autocannon_breech` | 8 | 24 (10 + 1260 mB) | деплоер: `cbc_at:vert_twin_autocannon_breech_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → дозатор: 420 mB `#c:molten_steel` → пресс → пила | 3 | — |
| `cbc_at:steel_vert_twin_autocannon_silencer` | 8 | 24 (10 + 1260 mB) | деплоер: `cbc_at:vert_twin_autocannon_silencer_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → дозатор: 420 mB `#c:molten_steel` → пресс → пила | 3 | — |
| `cbc_at:steel_vert_twin_autocannon_muzzle_brake` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:vert_twin_autocannon_muzzle_brake_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → дозатор: 330 mB `#c:molten_steel` → пресс → пила | 3 | — |
| `cbc_at:steel_heavy_autocannon_barrel` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:heavy_autocannon_barrel_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → дозатор: 330 mB `#c:molten_steel` → пресс → пила | 3 | — |
| `cbc_at:steel_heavy_autocannon_muzzle_brake` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:heavy_autocannon_muzzle_brake_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → дозатор: 330 mB `#c:molten_steel` → пресс → пила | 3 | — |
| `cbc_at:steel_heavy_autocannon_silencer` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:heavy_autocannon_silencer_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → дозатор: 330 mB `#c:molten_steel` → пресс → пила | 3 | — |
| `cbc_at:steel_heavy_autocannon_recoil_spring` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:heavy_autocannon_recoil_spring_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → дозатор: 330 mB `#c:molten_steel` → пресс → пила | 3 | — |
| `cbc_at:steel_heavy_autocannon_breech` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:heavy_autocannon_breech_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → дозатор: 330 mB `#c:molten_steel` → пресс → пила | 3 | — |
| `cbc_at:steel_heavy_autocannon_qfbreech` | 6 | 18 (7 + 990 mB) | деплоер: `cbc_at:heavy_autocannon_qfbreech_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → дозатор: 330 mB `#c:molten_steel` → пресс → пила | 3 | — |

## CBC Neo Warfare (нужен CBCMW recipe fix)

| Деталь | Литьё, слитков | Рецепт, слитков (слитками + расплавом) | Шаги одного цикла | Циклов | Потом деплоер |
|---|---|---|---|---|---|
| `cbcmodernwarfare:cast_iron_mediumcannon_barrel` | 3 | 9 (4 + 450 mB) | деплоер: `cbcmodernwarfare:mediumcannon_barrel_cast_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → дозатор: 450 mB `#c:molten_cast_iron` → пресс → пила | 1 | — |
| `cbcmodernwarfare:cast_iron_mediumcannon_breech` | 4 | 12 (5 + 630 mB) | деплоер: `cbcmodernwarfare:mediumcannon_breech_cast_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → дозатор: 315 mB `#c:molten_cast_iron` → пресс → пила | 2 | `cbcmodernwarfare:incomplete_cast_iron_mediumcannon_breech` + `cbcmodernwarfare:cast_iron_mediumcannon_falling_breech` |
| `cbcmodernwarfare:cast_iron_mediumcannon_recoil_barrel` | 4 | 12 (5 + 630 mB) | деплоер: `cbcmodernwarfare:mediumcannon_recoil_barrel_cast_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → дозатор: 315 mB `#c:molten_cast_iron` → пресс → пила | 2 | `cbcmodernwarfare:incomplete_cast_iron_mediumcannon_recoil_barrel` + `cbcmodernwarfare:heavy_recoil_spring` |
| `cbcmodernwarfare:bronze_mediumcannon_barrel` | 3 | 9 (4 + 450 mB) | деплоер: `cbcmodernwarfare:mediumcannon_barrel_cast_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → дозатор: 450 mB `#c:molten_bronze` → пресс → пила | 1 | — |
| `cbcmodernwarfare:bronze_mediumcannon_breech` | 4 | 12 (5 + 630 mB) | деплоер: `cbcmodernwarfare:mediumcannon_breech_cast_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → дозатор: 315 mB `#c:molten_bronze` → пресс → пила | 2 | `cbcmodernwarfare:incomplete_bronze_mediumcannon_breech` + `cbcmodernwarfare:bronze_mediumcannon_falling_breech` |
| `cbcmodernwarfare:bronze_mediumcannon_recoil_barrel` | 4 | 12 (5 + 630 mB) | деплоер: `cbcmodernwarfare:mediumcannon_recoil_barrel_cast_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → дозатор: 315 mB `#c:molten_bronze` → пресс → пила | 2 | `cbcmodernwarfare:incomplete_bronze_mediumcannon_recoil_barrel` + `cbcmodernwarfare:heavy_recoil_spring` |
| `cbcmodernwarfare:steel_mediumcannon_barrel` | 3 | 9 (4 + 450 mB) | деплоер: `cbcmodernwarfare:mediumcannon_barrel_cast_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → дозатор: 450 mB `#c:molten_steel` → пресс → пила | 1 | — |
| `cbcmodernwarfare:steel_mediumcannon_breech` | 4 | 12 (5 + 630 mB) | деплоер: `cbcmodernwarfare:mediumcannon_breech_cast_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → дозатор: 315 mB `#c:molten_steel` → пресс → пила | 2 | `cbcmodernwarfare:incomplete_steel_mediumcannon_breech` + `cbcmodernwarfare:steel_mediumcannon_falling_breech` |
| `cbcmodernwarfare:steel_mediumcannon_recoil_barrel` | 4 | 12 (5 + 630 mB) | деплоер: `cbcmodernwarfare:mediumcannon_recoil_barrel_cast_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → дозатор: 315 mB `#c:molten_steel` → пресс → пила | 2 | `cbcmodernwarfare:incomplete_steel_mediumcannon_recoil_barrel` + `cbcmodernwarfare:heavy_recoil_spring` |
| `cbcmodernwarfare:nethersteel_mediumcannon_barrel` | 3 | 9 (4 + 450 mB) | деплоер: `cbcmodernwarfare:mediumcannon_barrel_cast_mould` (остаётся) → деплоер: `#c:ingots/nethersteel` → деплоер: `#c:ingots/nethersteel` → деплоер: `#c:ingots/nethersteel` → дозатор: 450 mB `#c:molten_nethersteel` → пресс → пила | 1 | — |
| `cbcmodernwarfare:nethersteel_mediumcannon_breech` | 4 | 12 (5 + 630 mB) | деплоер: `cbcmodernwarfare:mediumcannon_breech_cast_mould` (остаётся) → деплоер: `#c:ingots/nethersteel` → деплоер: `#c:ingots/nethersteel` → дозатор: 315 mB `#c:molten_nethersteel` → пресс → пила | 2 | `cbcmodernwarfare:incomplete_nethersteel_mediumcannon_breech` + `cbcmodernwarfare:nethersteel_mediumcannon_falling_breech` |
| `cbcmodernwarfare:nethersteel_mediumcannon_recoil_barrel` | 4 | 12 (5 + 630 mB) | деплоер: `cbcmodernwarfare:mediumcannon_recoil_barrel_cast_mould` (остаётся) → деплоер: `#c:ingots/nethersteel` → деплоер: `#c:ingots/nethersteel` → дозатор: 315 mB `#c:molten_nethersteel` → пресс → пила | 2 | `cbcmodernwarfare:incomplete_nethersteel_mediumcannon_recoil_barrel` + `cbcmodernwarfare:heavy_recoil_spring` |

## Без рецепта

Процесс в мире сломан или отсутствует, поэтому мод ничего не добавляет.

| Деталь | Причина |
|---|---|
| `cbc_at:cast_iron_twin_autocannon_barrel` | рецепт формы `cbc_at:twin_autocannon_barrel_mould` в CBC:AT 0.1.4c не загружается |
| `cbc_at:cast_iron_rocket_pod_rail` | рецепт формы `cbc_at:rocket_pod_rail_mould` в CBC:AT 0.1.4c не загружается |
| `cbc_at:cast_iron_rocket_pod_breech` | рецепт формы `cbc_at:rocket_pod_breech_mould` в CBC:AT 0.1.4c не загружается |
| `cbc_at:bronze_twin_autocannon_barrel` | рецепт формы `cbc_at:twin_autocannon_barrel_mould` в CBC:AT 0.1.4c не загружается |
| `cbc_at:bronze_rocket_pod_rail` | рецепт формы `cbc_at:rocket_pod_rail_mould` в CBC:AT 0.1.4c не загружается |
| `cbc_at:bronze_rocket_pod_breech` | рецепт формы `cbc_at:rocket_pod_breech_mould` в CBC:AT 0.1.4c не загружается |
| `cbc_at:steel_twin_autocannon_barrel` | рецепт формы `cbc_at:twin_autocannon_barrel_mould` в CBC:AT 0.1.4c не загружается |
| `cbc_at:steel_rocket_pod_rail` | рецепт формы `cbc_at:rocket_pod_rail_mould` в CBC:AT 0.1.4c не загружается |
| `cbc_at:steel_rocket_pod_breech` | рецепт формы `cbc_at:rocket_pod_breech_mould` в CBC:AT 0.1.4c не загружается |
| `cbcmodernwarfare:bronze_rotarycannon_barrel` | в Neo Warfare 1.21.1 нет литья, форм и экстрактора; CBCMW recipe fix их не добавляет |
| `cbcmodernwarfare:bronze_rotarycannon_breech` | в Neo Warfare 1.21.1 нет литья, форм и экстрактора; CBCMW recipe fix их не добавляет |
| `cbcmodernwarfare:bronze_rotarycannon_bearing` | в Neo Warfare 1.21.1 нет литья, форм и экстрактора; CBCMW recipe fix их не добавляет |
| `cbcmodernwarfare:steel_rotarycannon_barrel` | в Neo Warfare 1.21.1 нет литья, форм и экстрактора; CBCMW recipe fix их не добавляет |
| `cbcmodernwarfare:steel_rotarycannon_breech` | в Neo Warfare 1.21.1 нет литья, форм и экстрактора; CBCMW recipe fix их не добавляет |
| `cbcmodernwarfare:steel_rotarycannon_bearing` | в Neo Warfare 1.21.1 нет литья, форм и экстрактора; CBCMW recipe fix их не добавляет |
| `cbcmodernwarfare:nethersteel_rotarycannon_barrel` | в Neo Warfare 1.21.1 нет литья, форм и экстрактора; CBCMW recipe fix их не добавляет |
| `cbcmodernwarfare:nethersteel_rotarycannon_breech` | в Neo Warfare 1.21.1 нет литья, форм и экстрактора; CBCMW recipe fix их не добавляет |
| `cbcmodernwarfare:nethersteel_rotarycannon_bearing` | в Neo Warfare 1.21.1 нет литья, форм и экстрактора; CBCMW recipe fix их не добавляет |
