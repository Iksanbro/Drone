-- radiolink TX: передатчик (ставится рядом с печатной машинкой)
-- Читает редстоун-сигналы (0-15) и шлёт их по беспроводному/эндер-модему.
-- Пара к нему: rx.lua на удалённом сублевеле.

---------------------------------------------------------------------
-- НАСТРОЙКИ (должны совпадать с rx.lua)
---------------------------------------------------------------------
local CHANNEL   = 4210          -- канал модема (0..65535)
local NETWORK   = "drone1"      -- имя сети: разные дроны = разные имена
local SECRET    = "change-me"   -- общий пароль, чтобы чужие не могли рулить

local POLL      = 0.05          -- как часто опрашивать входы (сек), 0.05 = 1 тик
local HEARTBEAT = 0.25          -- полное состояние шлётся не реже чем раз в N сек

-- Входы: имя сигнала -> откуда читать.
--   side  = "left"/"right"/"top"/"bottom"/"front"/"back"
--   relay = имя редстоун-реле (peripheral), если входов больше 6
-- Имя сигнала - это его "смысл"; на приёмнике то же имя выводится наружу.
local INPUTS = {
  forward = { side = "front" },
  back    = { side = "back"  },
  left    = { side = "left"  },
  right   = { side = "right" },
  up      = { side = "top"   },
  down    = { side = "bottom"},
  -- примеры через реле:
  -- boost = { relay = "redstone_relay_0", side = "front" },
  -- fire  = { relay = "redstone_relay_0", side = "back"  },
}
---------------------------------------------------------------------

local PROTO = "radiolink/1"

-- беспроводной или эндер-модем (эндер-модем: бесконечная дальность, лучше его)
local modem = peripheral.find("modem", function(_, m) return m.isWireless() end)
if not modem then error("Нет беспроводного/эндер-модема рядом с компьютером", 0) end

-- подготовить источники
local readers = {}
for name, src in pairs(INPUTS) do
  if src.relay then
    local r = peripheral.wrap(src.relay)
    if not r then error("Реле не найдено: " .. src.relay, 0) end
    readers[name] = function() return r.getAnalogInput(src.side) end
  else
    readers[name] = function() return redstone.getAnalogInput(src.side) end
  end
end

local function readAll()
  local s = {}
  for name, read in pairs(readers) do
    local ok, v = pcall(read)
    s[name] = ok and v or 0
  end
  return s
end

local function differs(a, b)
  for k, v in pairs(a) do if b[k] ~= v then return true end end
  for k in pairs(b) do if a[k] == nil then return true end end
  return false
end

-- уникальный id сессии: приёмник поймёт, что передатчик перезагрузился
local session = tostring(os.getComputerID()) .. "-" .. tostring(os.epoch("utc"))
local seq = 0

local function send(state)
  seq = seq + 1
  modem.transmit(CHANNEL, CHANNEL, {
    proto   = PROTO,
    net     = NETWORK,
    key     = SECRET,
    session = session,
    seq     = seq,
    state   = state,
  })
end

local last = readAll()
send(last)
local lastSend = os.clock()

term.clear(); term.setCursorPos(1, 1)
print(("radiolink TX  сеть=%s  канал=%d"):format(NETWORK, CHANNEL))
print("Ctrl+T - остановить")

local timer = os.startTimer(POLL)
while true do
  local ev, id = os.pullEvent()
  if ev == "redstone" or (ev == "timer" and id == timer) then
    if ev == "timer" then timer = os.startTimer(POLL) end
    local now = readAll()
    if differs(now, last) or os.clock() - lastSend >= HEARTBEAT then
      send(now)
      last, lastSend = now, os.clock()
      -- короткий статус
      local x, y = 1, 4
      term.setCursorPos(x, y); term.clearLine()
      local parts = {}
      for k, v in pairs(now) do if v > 0 then parts[#parts + 1] = k .. "=" .. v end end
      table.sort(parts)
      write("seq " .. seq .. "  " .. table.concat(parts, " "))
    end
  end
end
