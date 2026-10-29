# Рецепты последовательной сборки

Файл создан `tools/generate_data.py`, руками не править.

Рецепт есть только у деталей, которые делаются исключительно в мире (литьё и сверление),
и только если этот процесс в мире работает. Рецепт добавлен рядом с литьём, ничего не заменено.

Ресурсы те же, что у литья. Металл ×3, остальное в прежнем количестве:
форма держится в деплоере и не тратится (при литье она тоже возвращается),
предмет для incomplete-блока (экстрактор, пружина) тратится один раз.
Старт: блок материала (9 слитков).

## Create Big Cannons

| Деталь | Литьё, слитков | Рецепт, слитков | Шаги одного цикла | Циклов |
|---|---|---|---|---|
| `createbigcannons:cast_iron_autocannon_barrel` | 3 | 9 | деплоер: `createbigcannons:autocannon_barrel_cast_mould` (остаётся) → пресс → пила | 1 |
| `createbigcannons:cast_iron_autocannon_breech` | 4 | 12 | деплоер: `createbigcannons:autocannon_breech_cast_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → пресс → пила → деплоер: `createbigcannons:cast_iron_autocannon_breech_extractor` | 1 |
| `createbigcannons:cast_iron_autocannon_recoil_spring` | 4 | 12 | деплоер: `createbigcannons:autocannon_recoil_spring_cast_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → пресс → пила → деплоер: `createbigcannons:recoil_spring` | 1 |
| `createbigcannons:bronze_autocannon_barrel` | 3 | 9 | деплоер: `createbigcannons:autocannon_barrel_cast_mould` (остаётся) → пресс → пила | 1 |
| `createbigcannons:bronze_autocannon_breech` | 4 | 12 | деплоер: `createbigcannons:autocannon_breech_cast_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → пресс → пила → деплоер: `createbigcannons:bronze_autocannon_breech_extractor` | 1 |
| `createbigcannons:bronze_autocannon_recoil_spring` | 4 | 12 | деплоер: `createbigcannons:autocannon_recoil_spring_cast_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → пресс → пила → деплоер: `createbigcannons:recoil_spring` | 1 |
| `createbigcannons:steel_autocannon_barrel` | 3 | 9 | деплоер: `createbigcannons:autocannon_barrel_cast_mould` (остаётся) → пресс → пила | 1 |
| `createbigcannons:steel_autocannon_breech` | 4 | 12 | деплоер: `createbigcannons:autocannon_breech_cast_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → пресс → пила → деплоер: `createbigcannons:steel_autocannon_breech_extractor` | 1 |
| `createbigcannons:steel_autocannon_recoil_spring` | 4 | 12 | деплоер: `createbigcannons:autocannon_recoil_spring_cast_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → пресс → пила → деплоер: `createbigcannons:recoil_spring` | 1 |

## CBC: Advanced Technologies

| Деталь | Литьё, слитков | Рецепт, слитков | Шаги одного цикла | Циклов |
|---|---|---|---|---|
| `cbc_at:cast_iron_autocannon_silencer` | 3 | 9 | деплоер: `cbc_at:autocannon_silencer_mould` (остаётся) → пресс → пила | 1 |
| `cbc_at:cast_iron_autocannon_muzzle_brake` | 3 | 9 | деплоер: `cbc_at:autocannon_muzzle_brake_mould` (остаётся) → пресс → пила | 1 |
| `cbc_at:cast_iron_twin_autocannon_recoil_spring` | 6 | 18 | деплоер: `cbc_at:twin_autocannon_recoil_spring_mould` (остаётся) → деплоер: `#c:storage_blocks/cast_iron` → пресс → пила | 1 |
| `cbc_at:cast_iron_twin_autocannon_breech` | 6 | 18 | деплоер: `cbc_at:twin_autocannon_breech_mould` (остаётся) → деплоер: `#c:storage_blocks/cast_iron` → пресс → пила | 1 |
| `cbc_at:cast_iron_twin_autocannon_silencer` | 6 | 18 | деплоер: `cbc_at:twin_autocannon_silencer_mould` (остаётся) → деплоер: `#c:storage_blocks/cast_iron` → пресс → пила | 1 |
| `cbc_at:cast_iron_twin_autocannon_muzzle_brake` | 6 | 18 | деплоер: `cbc_at:twin_autocannon_muzzle_brake_mould` (остаётся) → деплоер: `#c:storage_blocks/cast_iron` → пресс → пила | 1 |
| `cbc_at:cast_iron_vert_twin_autocannon_barrel` | 6 | 18 | деплоер: `cbc_at:vert_twin_autocannon_barrel_mould` (остаётся) → деплоер: `#c:storage_blocks/cast_iron` → пресс → пила | 1 |
| `cbc_at:cast_iron_vert_twin_autocannon_recoil_spring` | 8 | 24 | деплоер: `cbc_at:vert_twin_autocannon_recoil_spring_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → пресс → пила | 5 |
| `cbc_at:cast_iron_vert_twin_autocannon_breech` | 8 | 24 | деплоер: `cbc_at:vert_twin_autocannon_breech_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → пресс → пила | 5 |
| `cbc_at:cast_iron_vert_twin_autocannon_silencer` | 8 | 24 | деплоер: `cbc_at:vert_twin_autocannon_silencer_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → пресс → пила | 5 |
| `cbc_at:cast_iron_vert_twin_autocannon_muzzle_brake` | 6 | 18 | деплоер: `cbc_at:vert_twin_autocannon_muzzle_brake_mould` (остаётся) → деплоер: `#c:storage_blocks/cast_iron` → пресс → пила | 1 |
| `cbc_at:cast_iron_heavy_autocannon_barrel` | 6 | 18 | деплоер: `cbc_at:heavy_autocannon_barrel_mould` (остаётся) → деплоер: `#c:storage_blocks/cast_iron` → пресс → пила | 1 |
| `cbc_at:cast_iron_heavy_autocannon_muzzle_brake` | 6 | 18 | деплоер: `cbc_at:heavy_autocannon_muzzle_brake_mould` (остаётся) → деплоер: `#c:storage_blocks/cast_iron` → пресс → пила | 1 |
| `cbc_at:cast_iron_heavy_autocannon_silencer` | 6 | 18 | деплоер: `cbc_at:heavy_autocannon_silencer_mould` (остаётся) → деплоер: `#c:storage_blocks/cast_iron` → пресс → пила | 1 |
| `cbc_at:cast_iron_heavy_autocannon_recoil_spring` | 6 | 18 | деплоер: `cbc_at:heavy_autocannon_recoil_spring_mould` (остаётся) → деплоер: `#c:storage_blocks/cast_iron` → пресс → пила | 1 |
| `cbc_at:cast_iron_heavy_autocannon_breech` | 6 | 18 | деплоер: `cbc_at:heavy_autocannon_breech_mould` (остаётся) → деплоер: `#c:storage_blocks/cast_iron` → пресс → пила | 1 |
| `cbc_at:cast_iron_heavy_autocannon_qfbreech` | 6 | 18 | деплоер: `cbc_at:heavy_autocannon_qfbreech_mould` (остаётся) → деплоер: `#c:storage_blocks/cast_iron` → пресс → пила | 1 |
| `cbc_at:bronze_autocannon_silencer` | 3 | 9 | деплоер: `cbc_at:autocannon_silencer_mould` (остаётся) → пресс → пила | 1 |
| `cbc_at:bronze_autocannon_muzzle_brake` | 3 | 9 | деплоер: `cbc_at:autocannon_muzzle_brake_mould` (остаётся) → пресс → пила | 1 |
| `cbc_at:bronze_twin_autocannon_recoil_spring` | 6 | 18 | деплоер: `cbc_at:twin_autocannon_recoil_spring_mould` (остаётся) → деплоер: `#c:storage_blocks/bronze` → пресс → пила | 1 |
| `cbc_at:bronze_twin_autocannon_breech` | 6 | 18 | деплоер: `cbc_at:twin_autocannon_breech_mould` (остаётся) → деплоер: `#c:storage_blocks/bronze` → пресс → пила | 1 |
| `cbc_at:bronze_twin_autocannon_silencer` | 6 | 18 | деплоер: `cbc_at:twin_autocannon_silencer_mould` (остаётся) → деплоер: `#c:storage_blocks/bronze` → пресс → пила | 1 |
| `cbc_at:bronze_twin_autocannon_muzzle_brake` | 6 | 18 | деплоер: `cbc_at:twin_autocannon_muzzle_brake_mould` (остаётся) → деплоер: `#c:storage_blocks/bronze` → пресс → пила | 1 |
| `cbc_at:bronze_vert_twin_autocannon_barrel` | 6 | 18 | деплоер: `cbc_at:vert_twin_autocannon_barrel_mould` (остаётся) → деплоер: `#c:storage_blocks/bronze` → пресс → пила | 1 |
| `cbc_at:bronze_vert_twin_autocannon_recoil_spring` | 8 | 24 | деплоер: `cbc_at:vert_twin_autocannon_recoil_spring_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → пресс → пила | 5 |
| `cbc_at:bronze_vert_twin_autocannon_breech` | 8 | 24 | деплоер: `cbc_at:vert_twin_autocannon_breech_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → пресс → пила | 5 |
| `cbc_at:bronze_vert_twin_autocannon_silencer` | 8 | 24 | деплоер: `cbc_at:vert_twin_autocannon_silencer_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → пресс → пила | 5 |
| `cbc_at:bronze_vert_twin_autocannon_muzzle_brake` | 6 | 18 | деплоер: `cbc_at:vert_twin_autocannon_muzzle_brake_mould` (остаётся) → деплоер: `#c:storage_blocks/bronze` → пресс → пила | 1 |
| `cbc_at:bronze_heavy_autocannon_barrel` | 6 | 18 | деплоер: `cbc_at:heavy_autocannon_barrel_mould` (остаётся) → деплоер: `#c:storage_blocks/bronze` → пресс → пила | 1 |
| `cbc_at:bronze_heavy_autocannon_muzzle_brake` | 6 | 18 | деплоер: `cbc_at:heavy_autocannon_muzzle_brake_mould` (остаётся) → деплоер: `#c:storage_blocks/bronze` → пресс → пила | 1 |
| `cbc_at:bronze_heavy_autocannon_silencer` | 6 | 18 | деплоер: `cbc_at:heavy_autocannon_silencer_mould` (остаётся) → деплоер: `#c:storage_blocks/bronze` → пресс → пила | 1 |
| `cbc_at:bronze_heavy_autocannon_recoil_spring` | 6 | 18 | деплоер: `cbc_at:heavy_autocannon_recoil_spring_mould` (остаётся) → деплоер: `#c:storage_blocks/bronze` → пресс → пила | 1 |
| `cbc_at:bronze_heavy_autocannon_breech` | 6 | 18 | деплоер: `cbc_at:heavy_autocannon_breech_mould` (остаётся) → деплоер: `#c:storage_blocks/bronze` → пресс → пила | 1 |
| `cbc_at:bronze_heavy_autocannon_qfbreech` | 6 | 18 | деплоер: `cbc_at:heavy_autocannon_qfbreech_mould` (остаётся) → деплоер: `#c:storage_blocks/bronze` → пресс → пила | 1 |
| `cbc_at:steel_autocannon_silencer` | 3 | 9 | деплоер: `cbc_at:autocannon_silencer_mould` (остаётся) → пресс → пила | 1 |
| `cbc_at:steel_autocannon_muzzle_brake` | 3 | 9 | деплоер: `cbc_at:autocannon_muzzle_brake_mould` (остаётся) → пресс → пила | 1 |
| `cbc_at:steel_twin_autocannon_recoil_spring` | 6 | 18 | деплоер: `cbc_at:twin_autocannon_recoil_spring_mould` (остаётся) → деплоер: `#c:storage_blocks/steel` → пресс → пила | 1 |
| `cbc_at:steel_twin_autocannon_breech` | 6 | 18 | деплоер: `cbc_at:twin_autocannon_breech_mould` (остаётся) → деплоер: `#c:storage_blocks/steel` → пресс → пила | 1 |
| `cbc_at:steel_twin_autocannon_silencer` | 6 | 18 | деплоер: `cbc_at:twin_autocannon_silencer_mould` (остаётся) → деплоер: `#c:storage_blocks/steel` → пресс → пила | 1 |
| `cbc_at:steel_twin_autocannon_muzzle_brake` | 6 | 18 | деплоер: `cbc_at:twin_autocannon_muzzle_brake_mould` (остаётся) → деплоер: `#c:storage_blocks/steel` → пресс → пила | 1 |
| `cbc_at:steel_vert_twin_autocannon_barrel` | 6 | 18 | деплоер: `cbc_at:vert_twin_autocannon_barrel_mould` (остаётся) → деплоер: `#c:storage_blocks/steel` → пресс → пила | 1 |
| `cbc_at:steel_vert_twin_autocannon_recoil_spring` | 8 | 24 | деплоер: `cbc_at:vert_twin_autocannon_recoil_spring_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → пресс → пила | 5 |
| `cbc_at:steel_vert_twin_autocannon_breech` | 8 | 24 | деплоер: `cbc_at:vert_twin_autocannon_breech_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → пресс → пила | 5 |
| `cbc_at:steel_vert_twin_autocannon_silencer` | 8 | 24 | деплоер: `cbc_at:vert_twin_autocannon_silencer_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → пресс → пила | 5 |
| `cbc_at:steel_vert_twin_autocannon_muzzle_brake` | 6 | 18 | деплоер: `cbc_at:vert_twin_autocannon_muzzle_brake_mould` (остаётся) → деплоер: `#c:storage_blocks/steel` → пресс → пила | 1 |
| `cbc_at:steel_heavy_autocannon_barrel` | 6 | 18 | деплоер: `cbc_at:heavy_autocannon_barrel_mould` (остаётся) → деплоер: `#c:storage_blocks/steel` → пресс → пила | 1 |
| `cbc_at:steel_heavy_autocannon_muzzle_brake` | 6 | 18 | деплоер: `cbc_at:heavy_autocannon_muzzle_brake_mould` (остаётся) → деплоер: `#c:storage_blocks/steel` → пресс → пила | 1 |
| `cbc_at:steel_heavy_autocannon_silencer` | 6 | 18 | деплоер: `cbc_at:heavy_autocannon_silencer_mould` (остаётся) → деплоер: `#c:storage_blocks/steel` → пресс → пила | 1 |
| `cbc_at:steel_heavy_autocannon_recoil_spring` | 6 | 18 | деплоер: `cbc_at:heavy_autocannon_recoil_spring_mould` (остаётся) → деплоер: `#c:storage_blocks/steel` → пресс → пила | 1 |
| `cbc_at:steel_heavy_autocannon_breech` | 6 | 18 | деплоер: `cbc_at:heavy_autocannon_breech_mould` (остаётся) → деплоер: `#c:storage_blocks/steel` → пресс → пила | 1 |
| `cbc_at:steel_heavy_autocannon_qfbreech` | 6 | 18 | деплоер: `cbc_at:heavy_autocannon_qfbreech_mould` (остаётся) → деплоер: `#c:storage_blocks/steel` → пресс → пила | 1 |

## CBC Neo Warfare (нужен CBCMW recipe fix)

| Деталь | Литьё, слитков | Рецепт, слитков | Шаги одного цикла | Циклов |
|---|---|---|---|---|
| `cbcmodernwarfare:cast_iron_mediumcannon_barrel` | 3 | 9 | деплоер: `cbcmodernwarfare:mediumcannon_barrel_cast_mould` (остаётся) → пресс → пила | 1 |
| `cbcmodernwarfare:cast_iron_mediumcannon_breech` | 4 | 12 | деплоер: `cbcmodernwarfare:mediumcannon_breech_cast_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → пресс → пила → деплоер: `cbcmodernwarfare:cast_iron_mediumcannon_falling_breech` | 1 |
| `cbcmodernwarfare:cast_iron_mediumcannon_recoil_barrel` | 4 | 12 | деплоер: `cbcmodernwarfare:mediumcannon_recoil_barrel_cast_mould` (остаётся) → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → деплоер: `#c:ingots/cast_iron` → пресс → пила → деплоер: `cbcmodernwarfare:heavy_recoil_spring` | 1 |
| `cbcmodernwarfare:bronze_mediumcannon_barrel` | 3 | 9 | деплоер: `cbcmodernwarfare:mediumcannon_barrel_cast_mould` (остаётся) → пресс → пила | 1 |
| `cbcmodernwarfare:bronze_mediumcannon_breech` | 4 | 12 | деплоер: `cbcmodernwarfare:mediumcannon_breech_cast_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → пресс → пила → деплоер: `cbcmodernwarfare:bronze_mediumcannon_falling_breech` | 1 |
| `cbcmodernwarfare:bronze_mediumcannon_recoil_barrel` | 4 | 12 | деплоер: `cbcmodernwarfare:mediumcannon_recoil_barrel_cast_mould` (остаётся) → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → деплоер: `#c:ingots/bronze` → пресс → пила → деплоер: `cbcmodernwarfare:heavy_recoil_spring` | 1 |
| `cbcmodernwarfare:steel_mediumcannon_barrel` | 3 | 9 | деплоер: `cbcmodernwarfare:mediumcannon_barrel_cast_mould` (остаётся) → пресс → пила | 1 |
| `cbcmodernwarfare:steel_mediumcannon_breech` | 4 | 12 | деплоер: `cbcmodernwarfare:mediumcannon_breech_cast_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → пресс → пила → деплоер: `cbcmodernwarfare:steel_mediumcannon_falling_breech` | 1 |
| `cbcmodernwarfare:steel_mediumcannon_recoil_barrel` | 4 | 12 | деплоер: `cbcmodernwarfare:mediumcannon_recoil_barrel_cast_mould` (остаётся) → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → деплоер: `#c:ingots/steel` → пресс → пила → деплоер: `cbcmodernwarfare:heavy_recoil_spring` | 1 |
| `cbcmodernwarfare:nethersteel_mediumcannon_barrel` | 3 | 9 | деплоер: `cbcmodernwarfare:mediumcannon_barrel_cast_mould` (остаётся) → пресс → пила | 1 |
| `cbcmodernwarfare:nethersteel_mediumcannon_breech` | 4 | 12 | деплоер: `cbcmodernwarfare:mediumcannon_breech_cast_mould` (остаётся) → деплоер: `#c:ingots/nethersteel` → деплоер: `#c:ingots/nethersteel` → деплоер: `#c:ingots/nethersteel` → пресс → пила → деплоер: `cbcmodernwarfare:nethersteel_mediumcannon_falling_breech` | 1 |
| `cbcmodernwarfare:nethersteel_mediumcannon_recoil_barrel` | 4 | 12 | деплоер: `cbcmodernwarfare:mediumcannon_recoil_barrel_cast_mould` (остаётся) → деплоер: `#c:ingots/nethersteel` → деплоер: `#c:ingots/nethersteel` → деплоер: `#c:ingots/nethersteel` → пресс → пила → деплоер: `cbcmodernwarfare:heavy_recoil_spring` | 1 |

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
