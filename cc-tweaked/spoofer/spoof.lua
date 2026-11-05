-- spoof.lua — генератор ложных целей для чужого радара (CC:Tweaked)
-- Внутриигровое РЭБ: шлёт правдоподобные контакты на канал, который слушает
-- радар. Цели дрейфуют, появляются и исчезают, бывают разных типов —
-- в т.ч. фейковые сущности пушек CBC, игроки и сублевелы Sable.
--
-- Это НЕ флуд-краш. Частота отправки ограничена, чтобы завалить мусором
-- радар, а не сервер. Не задирайте RATE до абсурда: заляжет весь сервер
-- и прилетит бан. Используйте только там, где такое PvP разрешено.

---------------------------------------------------------------------
-- НАСТРОЙКИ
---------------------------------------------------------------------
-- Каналы, которые слушает радар. Можно несколько.
local CHANNELS = { 1 }          -- напр. { 65533, 65534 }

-- Схема сообщения ДОЛЖНА совпадать с тем, что ждёт радар друга.
-- Ниже — типовой вид. Если у него поля называются иначе — поправьте
-- makeContact() под его формат (посмотрите его приёмный скрипт).
-- Общий вид поля: {name=, type=, x=, y=, z=, dim=, dist=}

local RATE       = 5            -- пакетов в секунду (держите 2..10)
local TARGETS    = 14           -- сколько ложных целей держать «в эфире»
local CHURN      = 0.08         -- шанс за тик, что цель исчезнет/сменится
local CENTER     = { x = 0, y = 80, z = 0 } -- вокруг чего раскидывать цели
local SPREAD     = 1200         -- разброс по горизонтали, блоков
local SEND_BATCH = false        -- true = слать весь список одним пакетом,
                                --        false = по одной цели (как «живой» радар)

local DIM = "minecraft:overworld"
---------------------------------------------------------------------

local modem = peripheral.find("modem", function(_, m) return m.isWireless() end)
if not modem then error("Нет беспроводного/эндер-модема рядом с компьютером", 0) end

math.randomseed(os.epoch("utc"))

-- типы ложных целей и как они двигаются
local KINDS = {
  { type = "player",  names = { "Steve","Alex","xX_Dr4g0n_Xx","Notch","Herobrine","mike_","qwerty777" }, speed = 4,  y = {64, 90} },
  { type = "sublevel", names = { "sable:ship","sable:contraption","drone_a","drone_b","carrier" },        speed = 9,  y = {70,160} },
  { type = "cbc_projectile", names = { "createbigcannons:shell","createbigcannons:ap_shell","createbigcannons:he_shell" }, speed = 55, y = {60,140} },
  { type = "cbc_cannon", names = { "createbigcannons:cannon_mount","createbigcannons:autocannon" },        speed = 2,  y = {64, 96} },
  { type = "entity",  names = { "minecraft:creeper","minecraft:zombie","minecraft:arrow" },               speed = 3,  y = {60, 80} },
}

local function pick(t) return t[math.random(#t)] end

local function newTarget()
  local k = pick(KINDS)
  local a = math.random() * 2 * math.pi
  return {
    kind  = k,
    name  = pick(k.names),
    type  = k.type,
    x     = CENTER.x + (math.random() * 2 - 1) * SPREAD,
    y     = math.random(k.y[1], k.y[2]),
    z     = CENTER.z + (math.random() * 2 - 1) * SPREAD,
    vx    = math.cos(a) * k.speed,
    vz    = math.sin(a) * k.speed,
    vy    = (math.random() * 2 - 1) * (k.speed / 6),
  }
end

local function step(t, dt)
  t.x = t.x + t.vx * dt
  t.y = t.y + t.vy * dt
  t.z = t.z + t.vz * dt
  -- лёгкое рыскание, чтобы траектория не была идеально прямой
  if math.random() < 0.15 then
    local a = math.random() * 2 * math.pi
    t.vx = math.cos(a) * t.kind.speed
    t.vz = math.sin(a) * t.kind.speed
  end
end

-- превратить цель в сообщение нужного радару формата.
-- ПОДГОНИТЕ ПОД ЕГО СХЕМУ, если поля называются иначе.
local function makeContact(t)
  local dx, dy, dz = t.x - CENTER.x, t.y - CENTER.y, t.z - CENTER.z
  return {
    name = t.name,
    type = t.type,
    x = math.floor(t.x + 0.5),
    y = math.floor(t.y + 0.5),
    z = math.floor(t.z + 0.5),
    dim = DIM,
    dist = math.floor(math.sqrt(dx*dx + dy*dy + dz*dz) + 0.5),
  }
end

-- заполнить начальный список
local targets = {}
for i = 1, TARGETS do targets[i] = newTarget() end

term.clear(); term.setCursorPos(1, 1)
print(("spoof  каналы=%s  целей=%d  темп=%d/с")
  :format(table.concat(CHANNELS, ","), TARGETS, RATE))
print("Ctrl+T — стоп")

local interval = 1 / RATE
local dt = interval * (SEND_BATCH and 1 or #targets) -- за цикл проходит ~1 «кадр»
local rr = 1 -- round-robin по целям при поштучной отправке

while true do
  -- обновить сцену
  for i, t in ipairs(targets) do
    step(t, interval)
    if math.random() < CHURN then targets[i] = newTarget() end
  end

  if SEND_BATCH then
    local list = {}
    for i, t in ipairs(targets) do list[i] = makeContact(t) end
    for _, ch in ipairs(CHANNELS) do
      modem.transmit(ch, ch, { contacts = list })
    end
    sleep(interval)
  else
    -- слать по одной цели — так шумит естественнее
    local t = targets[rr]; rr = rr % #targets + 1
    local msg = makeContact(t)
    for _, ch in ipairs(CHANNELS) do
      modem.transmit(ch, ch, msg)
    end
    sleep(interval)
  end
end
