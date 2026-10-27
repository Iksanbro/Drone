-- radiolink RX: приёмник (ставится на удалённом сублевеле / дроне)
-- Принимает состояние от tx.lua и выдаёт те же сигналы (0-15) в редстоун.
-- Если связь пропала - все выходы сбрасываются в безопасное значение.

---------------------------------------------------------------------
-- НАСТРОЙКИ (CHANNEL, NETWORK, SECRET должны совпадать с tx.lua)
---------------------------------------------------------------------
local CHANNEL  = 4210
local NETWORK  = "drone1"
local SECRET   = "change-me"

local FAILSAFE = 1.0   -- сек без пакетов -> выходы в SAFE (0 = отключить)

-- Выходы: имя сигнала (как в tx.lua) -> куда выдавать.
--   side  = сторона компьютера
--   relay = имя редстоун-реле (peripheral), если выходов больше 6
--   safe  = значение при потере связи (по умолчанию 0)
local OUTPUTS = {
  forward = { side = "front" },
  back    = { side = "back"  },
  left    = { side = "left"  },
  right   = { side = "right" },
  up      = { side = "top"   },
  down    = { side = "bottom"},
  -- boost = { relay = "redstone_relay_0", side = "front" },
  -- fire  = { relay = "redstone_relay_0", side = "back", safe = 0 },
}
---------------------------------------------------------------------

local PROTO = "radiolink/1"

local modem = peripheral.find("modem", function(_, m) return m.isWireless() end)
if not modem then error("Нет беспроводного/эндер-модема рядом с компьютером", 0) end
modem.open(CHANNEL)

local writers = {}
for name, dst in pairs(OUTPUTS) do
  if dst.relay then
    local r = peripheral.wrap(dst.relay)
    if not r then error("Реле не найдено: " .. dst.relay, 0) end
    writers[name] = function(v) r.setAnalogOutput(dst.side, v) end
  else
    writers[name] = function(v) redstone.setAnalogOutput(dst.side, v) end
  end
end

local current = {}
local function set(name, v)
  v = math.max(0, math.min(15, math.floor(tonumber(v) or 0)))
  if current[name] ~= v then
    current[name] = v
    pcall(writers[name], v)
  end
end

local function applySafe()
  for name, dst in pairs(OUTPUTS) do set(name, dst.safe or 0) end
end

local function apply(state)
  -- сигналы, которых нет в пакете, считаются выключенными (0)
  for name in pairs(OUTPUTS) do set(name, state[name] or 0) end
end

applySafe()

term.clear(); term.setCursorPos(1, 1)
print(("radiolink RX  сеть=%s  канал=%d"):format(NETWORK, CHANNEL))
print("Ctrl+T - остановить")

local session, lastSeq = nil, 0
local linked = false
local lastRx = os.clock()

local function status(text)
  term.setCursorPos(1, 4); term.clearLine(); write(text)
end
status("ожидание передатчика...")

local timer = os.startTimer(0.1)
while true do
  local ev, a, b, c, msg = os.pullEvent()

  if ev == "modem_message" and b == CHANNEL and type(msg) == "table"
     and msg.proto == PROTO and msg.net == NETWORK and msg.key == SECRET
     and type(msg.state) == "table" and type(msg.seq) == "number" then
    -- новая сессия (передатчик перезагрузился) -> начинаем счёт заново
    if msg.session ~= session then session, lastSeq = msg.session, 0 end
    -- старые/повторные пакеты отбрасываем, чтобы не "откатить" состояние
    if msg.seq > lastSeq then
      lastSeq = msg.seq
      lastRx = os.clock()
      apply(msg.state)
      if not linked then linked = true end
      local parts = {}
      for k, v in pairs(current) do if v > 0 then parts[#parts + 1] = k .. "=" .. v end end
      table.sort(parts)
      status("связь OK  seq " .. lastSeq .. "  " .. table.concat(parts, " "))
    end

  elseif ev == "timer" and a == timer then
    timer = os.startTimer(0.1)
    if linked and FAILSAFE > 0 and os.clock() - lastRx > FAILSAFE then
      linked = false
      applySafe()
      status("СВЯЗЬ ПОТЕРЯНА - выходы сброшены")
    end
  end
end
