# Снаряды средних пушек

Сгенерировано `tools/generate_medium_shells.py` из параметров больших снарядов (`tools/medium_shells/big/`), руками не править.

Взрыв: одно число, или «по существам / по блокам». Масса — `durability_mass`. Баллистика у всех как у снарядов средних пушек Neo Warfare: гравитация −0.04, сопротивление воздуха 0.01.

| Снаряд | ID снаряда (патрон: `_cartridge`) | Урон | Взрыв | Пробитие | Прочность | Масса | Отдача | Особое |
|---|---|---|---|---|---|---|---|---|
| Шрапнельный снаряд | `cbcparts:medium_shrapnel_shell_round` | 15 | 1.5 / 1 | 0.5 | 0.5 | 1 | 1 | шрапнель: 25 шт., разлёт 0.6 |
| Жидкостный снаряд | `cbcparts:medium_fluid_shell_round` | 15 | 1.5 / 1 | 0.5 | 0.5 | 1 | 1 | жидкость до 1000 мВ, капли по 500 мВ |
| Тяжёлый снаряд | `cbcparts:medium_solid_shot_round` | 15 | — | 0.55 | 1 | 1.5 | 1 | без взрыва, без взрывателя |
| Миномётный снаряд | `cbcparts:medium_drop_mortar_shell_round` | 10 | 3 / 1 | 0.5 | 0.5 | 1 | 1 |  |
| Миномётный камень | `cbcparts:medium_mortar_stone_round` | 25 | 2 | 0 | 0 | 2 | 1 | взрыв при ударе о блок, без взрывателя |
| Фугасный снаряд Mk.I | `cbcparts:medium_he_shell_mk1_round` | 45 | 10 / 8 | 0.5 | 0.5 | 1 | 1 |  |
| Фугасный снаряд Mk.II | `cbcparts:medium_he_shell_mk2_round` | 75 | 12 / 10 | 0.5 | 0.5 | 1 | 1 |  |
| Фугасный снаряд Mk.III | `cbcparts:medium_he_shell_mk3_round` | 150 | 14 / 12 | 0.5 | 0.5 | 1 | 1 |  |
| Фугасный снаряд Mk.IV | `cbcparts:medium_he_shell_mk4_round` | 225 | 16 / 14 | 0.5 | 0.5 | 1 | 1 |  |
| Фугасный снаряд Mk.V | `cbcparts:medium_he_shell_mk5_round` | 300 | 18 / 16 | 0.5 | 0.5 | 1 | 1 |  |
| Бронебойно-фугасный снаряд Mk.I | `cbcparts:medium_ap_shell_mk1_round` | 50 | 4 / 3.5 | 2 | 0.5 | 9 | 1 | взрыв после пробития |
| Бронебойно-фугасный снаряд Mk.II | `cbcparts:medium_ap_shell_mk2_round` | 100 | 4.5 / 4 | 4 | 1 | 13 | 1 | взрыв после пробития |
| Бронебойно-фугасный снаряд Mk.III | `cbcparts:medium_ap_shell_mk3_round` | 250 | 5 / 4.5 | 8 | 2 | 17.5 | 1 | взрыв после пробития |
| Бронебойно-фугасный снаряд Mk.IV | `cbcparts:medium_ap_shell_mk4_round` | 375 | 5.5 / 5 | 16 | 4 | 20.5 | 1 | взрыв после пробития |
| Бронебойно-фугасный снаряд Mk.V | `cbcparts:medium_ap_shell_mk5_round` | 500 | 6 / 5.5 | 32 | 8 | 25.25 | 1 | взрыв после пробития |
| Бронебойный снаряд Mk.I | `cbcparts:medium_ap_shot_mk1_round` | 75 | — | 2 | 1 | 9 | 1 | без взрыва, без взрывателя |
| Бронебойный снаряд Mk.II | `cbcparts:medium_ap_shot_mk2_round` | 125 | — | 4 | 2 | 13 | 1 | без взрыва, без взрывателя |
| Бронебойный снаряд Mk.III | `cbcparts:medium_ap_shot_mk3_round` | 250 | — | 8 | 4 | 17.5 | 1 | без взрыва, без взрывателя |
| Бронебойный снаряд Mk.IV | `cbcparts:medium_ap_shot_mk4_round` | 375 | — | 16 | 8 | 20.5 | 1 | без взрыва, без взрывателя |
| Бронебойный снаряд Mk.V | `cbcparts:medium_ap_shot_mk5_round` | 500 | — | 32 | 16 | 25.25 | 1 | без взрыва, без взрывателя |
| Тяжёлый снаряд Mk.I | `cbcparts:medium_solid_shot_mk1_round` | 45 | — | 0.55 | 1 | 4.5 | 1 | без взрыва, без взрывателя |
| Тяжёлый снаряд Mk.II | `cbcparts:medium_solid_shot_mk2_round` | 75 | — | 0.55 | 1 | 9 | 1 | без взрыва, без взрывателя |
| Тяжёлый снаряд Mk.III | `cbcparts:medium_solid_shot_mk3_round` | 150 | — | 0.55 | 1 | 12 | 1 | без взрыва, без взрывателя |
| Тяжёлый снаряд Mk.IV | `cbcparts:medium_solid_shot_mk4_round` | 225 | — | 0.55 | 1 | 15 | 1 | без взрыва, без взрывателя |
| Тяжёлый снаряд Mk.V | `cbcparts:medium_solid_shot_mk5_round` | 300 | — | 0.55 | 1 | 18 | 1 | без взрыва, без взрывателя |
| Шрапнельный снаряд Mk.I | `cbcparts:medium_shrapnel_shell_mk1_round` | 45 | 1.5 | 0.5 | 0.5 | 1 | 1 | шрапнель: 75 шт., разлёт 0.7 |
| Шрапнельный снаряд Mk.II | `cbcparts:medium_shrapnel_shell_mk2_round` | 75 | 2 | 0.5 | 0.5 | 1 | 1 | шрапнель: 125 шт., разлёт 0.8 |
| Шрапнельный снаряд Mk.III | `cbcparts:medium_shrapnel_shell_mk3_round` | 150 | 3 | 0.5 | 0.5 | 1 | 1 | шрапнель: 250 шт., разлёт 0.9 |
| Шрапнельный снаряд Mk.IV | `cbcparts:medium_shrapnel_shell_mk4_round` | 225 | 3 | 0.5 | 0.5 | 1 | 1 | шрапнель: 375 шт., разлёт 1 |
| Шрапнельный снаряд Mk.V | `cbcparts:medium_shrapnel_shell_mk5_round` | 300 | 3 | 0.5 | 0.5 | 1 | 1 | шрапнель: 500 шт., разлёт 1.2 |
| Дымовой снаряд Mk.I | `cbcparts:medium_smoke_shell_mk1_round` | 15 | — | 0.5 | 0.5 | 1 | 1 | дым: размер 10, 300 тиков |
| Дымовой снаряд Mk.II | `cbcparts:medium_smoke_shell_mk2_round` | 15 | — | 0.5 | 0.5 | 1 | 1 | дым: размер 10, 450 тиков |
| Дымовой снаряд Mk.III | `cbcparts:medium_smoke_shell_mk3_round` | 15 | — | 0.5 | 0.5 | 1 | 1 | дым: размер 10, 600 тиков |
| Дымовой снаряд Mk.IV | `cbcparts:medium_smoke_shell_mk4_round` | 15 | — | 0.5 | 0.5 | 1 | 1 | дым: размер 10, 750 тиков |
| Дымовой снаряд Mk.V | `cbcparts:medium_smoke_shell_mk5_round` | 15 | — | 0.5 | 0.5 | 1 | 1 | дым: размер 10, 900 тиков |
| Миномётный снаряд Mk.I | `cbcparts:medium_drop_mortar_shell_mk1_round` | 30 | 4.5 / 1.5 | 0.5 | 0.5 | 1 | 1 |  |
| Миномётный снаряд Mk.II | `cbcparts:medium_drop_mortar_shell_mk2_round` | 50 | 6 / 2 | 0.5 | 0.5 | 1 | 1 |  |
| Миномётный снаряд Mk.III | `cbcparts:medium_drop_mortar_shell_mk3_round` | 100 | 9 / 3 | 0.5 | 0.5 | 1 | 1 |  |
| Миномётный снаряд Mk.IV | `cbcparts:medium_drop_mortar_shell_mk4_round` | 150 | 12 / 4 | 0.5 | 0.5 | 1 | 1 |  |
| Миномётный снаряд Mk.V | `cbcparts:medium_drop_mortar_shell_mk5_round` | 200 | 14 / 5 | 0.5 | 0.5 | 1 | 1 |  |
| Мешок с картечью Mk.I | `cbcparts:medium_bag_of_grapeshot_mk1_round` | 0 | — | 0 | 0 | 0.5 | 1 | картечь у ствола: 38 шт., разлёт 0.15 |
| Мешок с картечью Mk.II | `cbcparts:medium_bag_of_grapeshot_mk2_round` | 0 | — | 0 | 0 | 0.5 | 1 | картечь у ствола: 63 шт., разлёт 0.15 |
| Мешок с картечью Mk.III | `cbcparts:medium_bag_of_grapeshot_mk3_round` | 0 | — | 0 | 0 | 0.5 | 1 | картечь у ствола: 125 шт., разлёт 0.15 |
| Мешок с картечью Mk.IV | `cbcparts:medium_bag_of_grapeshot_mk4_round` | 0 | — | 0 | 0 | 0.5 | 1 | картечь у ствола: 188 шт., разлёт 0.15 |
| Мешок с картечью Mk.V | `cbcparts:medium_bag_of_grapeshot_mk5_round` | 0 | — | 0 | 0 | 0.5 | 1 | картечь у ствола: 250 шт., разлёт 0.15 |
| Миномётный камень Mk.I | `cbcparts:medium_mortar_stone_mk1_round` | 75 | 3 | 0 | 0 | 6 | 1 | взрыв при ударе о блок, без взрывателя |
| Миномётный камень Mk.II | `cbcparts:medium_mortar_stone_mk2_round` | 125 | 4 | 0 | 0 | 10 | 1 | взрыв при ударе о блок, без взрывателя |
| Миномётный камень Mk.III | `cbcparts:medium_mortar_stone_mk3_round` | 250 | 6 | 0 | 0 | 20 | 1 | взрыв при ударе о блок, без взрывателя |
| Миномётный камень Mk.IV | `cbcparts:medium_mortar_stone_mk4_round` | 375 | 6 | 0 | 0 | 30 | 1 | взрыв при ударе о блок, без взрывателя |
| Миномётный камень Mk.V | `cbcparts:medium_mortar_stone_mk5_round` | 500 | 6 | 0 | 0 | 40 | 1 | взрыв при ударе о блок, без взрывателя |
| Кассетный снаряд Mk.I | `cbcparts:medium_cluster_shell_mk1_round` | 35 | 5 | 1 | 1 | 2 | 1 | делится на 3 фугасных Mk.I (средних), конус 24° |
| Кассетный снаряд Mk.II | `cbcparts:medium_cluster_shell_mk2_round` | 70 | 7 | 2 | 2 | 4 | 1 | делится на 5 фугасных Mk.I (средних), конус 28° |
| Кассетный снаряд Mk.III | `cbcparts:medium_cluster_shell_mk3_round` | 120 | 10 | 4 | 4 | 6 | 1 | делится на 6 фугасных Mk.II (средних), конус 32° |
| Кассетный снаряд Mk.IV | `cbcparts:medium_cluster_shell_mk4_round` | 190 | 13 | 7 | 7 | 8 | 1 | делится на 8 фугасных Mk.III (средних), конус 36° |
| Кассетный снаряд Mk.V | `cbcparts:medium_cluster_shell_mk5_round` | 280 | 16 | 10 | 10 | 10 | 1 | делится на 10 фугасных Mk.IV (средних), конус 40° |
| Бетонобойный снаряд Mk.I | `cbcparts:medium_bunker_buster_shell_mk1_round` | 160 | 9 | 20 | 20 | 12.5 | 1.5 | сверлит 5 блоков, потом взрыв |
| Бетонобойный снаряд Mk.II | `cbcparts:medium_bunker_buster_shell_mk2_round` | 275 | 12 | 27.5 | 27.5 | 17.5 | 1.5 | сверлит 9 блоков, потом взрыв |
| Бетонобойный снаряд Mk.III | `cbcparts:medium_bunker_buster_shell_mk3_round` | 450 | 16 | 37.5 | 37.5 | 22.5 | 1.5 | сверлит 14 блоков, потом взрыв |
| Бетонобойный снаряд Mk.IV | `cbcparts:medium_bunker_buster_shell_mk4_round` | 700 | 21 | 50 | 50 | 27.5 | 1.5 | сверлит 20 блоков, потом взрыв |
| Бетонобойный снаряд Mk.V | `cbcparts:medium_bunker_buster_shell_mk5_round` | 1000 | 27 | 65 | 65 | 32.5 | 1.5 | сверлит 28 блоков, потом взрыв |
