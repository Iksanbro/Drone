-- sniff.lua — сниффер модемного эфира (CC:Tweaked)
-- Пассивная разведка: слушает каналы и показывает, что по ним летает —
-- канал, обратный канал, расстояние до отправителя и само сообщение.
-- Ничего не передаёт. Нужен, чтобы узнать канал и формат связи противника,
-- прежде чем что-то с этим делать.
--
-- Модем держит открытыми максимум 128 каналов разом, а всего каналов 65536,
-- поэтому для полного охвата эфир сканируется развёрткой: окно из 128 каналов
-- сдвигается по очереди. Периодические передачи так ловятся за пару минут.

---------------------------------------------------------------------
-- НАСТРОЙКИ
---------------------------------------------------------------------
-- MODE "sweep"  — пройти все 65536 каналов развёрткой (ищем, где вообще связь)
-- MODE "watch"  — слушать только каналы из WATCH (уже знаем, где смотреть)
local MODE  = "sweep"
local WATCH = { 1 }          -- для MODE="watch"

local BATCH = 128            -- каналов в окне (лимит модема)
local DWELL = 0.4            -- сколько секунд держать окно перед сдвигом
local LOG   = "sniff.log"    -- файл с полным дампом
---------------------------------------------------------------------

local modem = peripheral.find("modem", function(_, m) return m.isWireless() end)
if not modem then error("Нужен беспроводной/эндер-модем рядом с компьютером", 0) end

-- закрыть всё, с чем мог остаться прошлый запуск
modem.closeAll()

local log = fs.open(LOG, "a")
local function record(line)
  log.writeLine(line); log.flush()
end

local function openRange(from)
  for c = from, math.min(from + BATCH - 1, 65535) do modem.open(c) end
end
local function closeRange(from)
  for c = from, math.min(from + BATCH - 1, 65535) do modem.close(c) end
end

-- краткая строка о сообщении
local function describe(ch, reply, dist, msg)
  local body
  if type(msg) == "table" then
    body = textutils.serialize(msg, { compact = true })
  else
    body = tostring(msg)
  end
  if #body > 120 then body = body:sub(1, 117) .. "..." end
  local d = dist and ("%.0f"):format(dist) or "?"
  return ("[ch %5d <-%5d  %4sм]  %s"):format(ch, reply or -1, d, body)
end

term.clear(); term.setCursorPos(1, 1)
print(("sniff  режим=%s  лог=%s"):format(MODE, LOG))
print("Ctrl+T — стоп")

local seen = {}          -- какие каналы хоть раз были активны
local hits = 0

-- поток-приёмник: ловит сообщения, пока основной крутит развёртку
local function receiver()
  while true do
    local _, _, ch, reply, msg, dist = os.pullEvent("modem_message")
    hits = hits + 1
    if not seen[ch] then seen[ch] = true end
    local line = describe(ch, reply, dist, msg)
    record(os.date("%H:%M:%S ") .. line)
    -- на экране — компактный журнал
    local _, h = term.getSize()
    term.scroll(1); term.setCursorPos(1, h); write(line)
  end
end

local function scanner()
  if MODE == "watch" then
    for _, c in ipairs(WATCH) do modem.open(c) end
    term.setCursorPos(1, 3)
    write("слушаю каналы: " .. table.concat(WATCH, ", "))
    while true do os.sleep(1) end
  else
    while true do
      for from = 0, 65535, BATCH do
        openRange(from)
        local active = {}
        for c in pairs(seen) do active[#active + 1] = c end
        table.sort(active)
        local status = ("окно %5d-%5d  найдено каналов: %d  сообщений: %d")
          :format(from, math.min(from + BATCH - 1, 65535), #active, hits)
        term.setCursorPos(1, 3); term.clearLine(); write(status)
        if #active > 0 then
          term.setCursorPos(1, 4); term.clearLine()
          write("активны: " .. table.concat(active, ", "):sub(1, 180))
        end
        os.sleep(DWELL)
        closeRange(from)
      end
    end
  end
end

parallel.waitForAny(receiver, scanner)
