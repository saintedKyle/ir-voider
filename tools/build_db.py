#!/usr/bin/env python3
"""Convert Flipper-IRDB TV remotes into raw IR patterns for the Android app.

Encoders are ported from Flipper Zero firmware (lib/infrared/encoder_decoder)
and IRremoteESP8266 timing constants. Output: app/src/main/assets/remotes.json
"""

import json
import os
import re
import sys
from collections import Counter, OrderedDict

ROOT = "data/irdb_all"
OUT_CATEGORIES = OrderedDict([
    ("TVs", "TV"), ("ACs", "Air Conditioner"), ("Projectors", "Projector"),
    ("Fans", "Fan"), ("LED_Lighting", "LED Light"), ("SoundBars", "Soundbar"),
    ("Audio_and_Video_Receivers", "Audio / Receiver"), ("Speakers", "Speaker"),
    ("Streaming_Devices", "Streaming Box"), ("Cable_Boxes", "Cable / Sat Box"),
    ("TV_Tuner", "TV Tuner"), ("DVB-T", "DVB-T Receiver"),
    ("DVD_Players", "DVD Player"), ("Blu-Ray", "Blu-ray Player"),
    ("VCR", "VCR"), ("CD_Players", "CD Player"), ("Cameras", "Camera"),
    ("CCTV", "CCTV"), ("Air_Purifiers", "Air Purifier"), ("Heaters", "Heater"),
    ("Humidifiers", "Humidifier"), ("Vacuum_Cleaners", "Vacuum"),
    ("Monitors", "Monitor"), ("Projectors", "Projector"),
    ("Touchscreen_Displays", "Touchscreen"), ("Whiteboards", "Whiteboard"),
    ("Converters", "Converter"), ("Car_Multimedia", "Car Audio"),
    ("Multimedia", "Multimedia"), ("Consoles", "Console"), ("Toys", "Toy"),
    ("Fireplaces", "Fireplace"), ("Picture_Frames", "Picture Frame"),
    ("Laserdisc", "Laserdisc"), ("MiniDisc", "MiniDisc"),
    ("Clocks", "Clock"), ("Digital_Signs", "Digital Sign"),
    ("Dust_Collectors", "Dust Collector"), ("Head_Units", "Head Unit"),
    ("Universal_TV_Remotes", "Universal Remote"),
    ("Videoconferencing", "Video Conferencing"),
    ("Miscellaneous", "Misc"),
])
OUT = "app/src/main/assets/remotes.json"

# ---------------------------------------------------------------- protocols


class Timings:
    def __init__(self, pm, ps, m1, s1, m0, s0, freq):
        self.pm = pm
        self.ps = ps
        self.m1 = m1
        self.s1 = s1
        self.m0 = m0
        self.s0 = s0
        self.freq = freq


NEC = Timings(9000, 4500, 560, 1690, 560, 560, 38000)
SAMSUNG = Timings(4500, 4500, 550, 1650, 550, 550, 38000)
SIRC = Timings(2400, 600, 1200, 600, 600, 600, 40000)
KASEIKYO = Timings(3456, 1728, 432, 1296, 432, 432, 38000)
RCA = Timings(4000, 4000, 500, 2000, 500, 1000, 38000)
PIONEER = Timings(8500, 4225, 500, 1500, 500, 500, 40000)
RC5_T = Timings(0, 0, 889, 0, 889, 0, 36000)
RC6_T = Timings(2666, 889, 444, 0, 444, 0, 36000)


def finish_pdwm(t, bits, nbits):
    """LSB-first pulse-distance encoding with preamble and final mark."""
    out = [t.pm, t.ps]
    for i in range(nbits):
        b = (bits >> i) & 1
        out.append(t.m1 if b else t.m0)
        out.append(t.s1 if b else t.s0)
    pwm = t.s1 == t.s0  # spaces equal -> last space is omitted
    if pwm:
        out.pop()
    else:
        out.append(t.m0)
    return out


def rev32(v):
    v &= 0xFFFFFFFF
    r = 0
    for _ in range(32):
        r = (r << 1) | (v & 1)
        v >>= 1
    return r


def rev_bits(v, n):
    r = 0
    for _ in range(n):
        r = (r << 1) | (v & 1)
        v >>= 1
    return r


def nec_pattern(address, command, ext):
    if ext:
        bits = (address & 0xFFFF) | ((command & 0xFFFF) << 16)
        return finish_pdwm(NEC, bits, 32)
    bits = ((address & 0xFF) | ((~address & 0xFF) << 8) |
            ((command & 0xFF) << 16) | ((~command & 0xFF) << 24))
    return finish_pdwm(NEC, bits, 32)


def samsung_pattern(address, command):
    bits = ((address & 0xFF) | ((address & 0xFF) << 8) |
            ((command & 0xFF) << 16) | ((~command & 0xFF) << 24))
    return finish_pdwm(SAMSUNG, bits, 32)


def sirc_pattern(address, command):
    bits = (command & 0x7F) | ((address & 0x1F) << 7)
    return finish_pdwm(SIRC, bits, 12)


def kaseikyo_pattern(address, command):
    addr = address & 0x3FFFFFF
    cmd = command & 0x3FF
    vid = (addr >> 8) & 0xFFFF
    genre1 = (addr >> 4) & 0xF
    genre2 = addr & 0xF
    ident = (addr >> 24) & 0x3
    d = [0] * 6
    d[0] = vid & 0xFF
    d[1] = vid >> 8
    vp = d[0] ^ d[1]
    vp = (vp & 0xF) ^ (vp >> 4)
    d[2] = (vp & 0xF) | (genre1 << 4)
    d[3] = (genre2 & 0xF) | ((cmd & 0xF) << 4)
    d[4] = (ident << 6) | (cmd >> 4)
    d[5] = d[2] ^ d[3] ^ d[4]
    bits = 0
    for i, byte in enumerate(d):
        bits |= byte << (8 * i)
    return finish_pdwm(KASEIKYO, bits, 48)


def rca_pattern(address, command):
    a = address & 0xF
    c = command & 0xFF
    bits = a | (c << 4) | ((~a & 0xF) << 12) | ((~c & 0xFF) << 16)
    return finish_pdwm(RCA, bits, 24)


def pioneer_pattern(address, command):
    a = address & 0xFF
    c = command & 0xFF
    d = [a, ~a & 0xFF, c, ~c & 0xFF, 0]
    bits = 0
    for i, byte in enumerate(d):
        bits |= byte << (8 * i)
    return finish_pdwm(PIONEER, bits, 33)


def rc5_pattern(address, command):
    """IRremoteESP8266 sendRC5 with toggle=0 (14 on-air bits)."""
    t = 889
    out = [t]  # leading space is skipped on first send
    out.append(t)  # mark
    # field/second start bit = 1 for RC5
    out += [t, t]  # space, mark
    data = ((address & 0x1F) << 6) | (command & 0x3F)  # 12 bits incl toggle=0
    for mask in range(1 << 11, 0, -1):
        if data & mask:
            out += [t, t]  # space then mark
        else:
            out += [t, t]  # mark then space
    # The first value is a leading space; drop it. Alternation handled below.
    return _levels_to_pattern(_rc5_levels(data, t))


def _rc5_levels(data, t):
    """Return (level,duration) list; level True=mark."""
    events = []
    # first start bit: space, mark
    events += [(False, t), (True, t)]
    # second start bit: 1 -> space, mark
    events += [(False, t), (True, t)]
    for i in range(11, -1, -1):
        if (data >> i) & 1:
            events += [(False, t), (True, t)]
        else:
            events += [(True, t), (False, t)]
    return events


def rc6_pattern(address, command):
    t = 444
    events = [(True, 2666), (False, 889)]
    events += [(True, t), (False, t)]  # start bit 1
    # 20 data bits, MSB first: mode(3)=0, toggle=0, address(8), command(8)
    data = ((address & 0xFF) << 8) | (command & 0xFF)
    for i in range(19, -1, -1):
        bit_time = 2 * t if i == 16 else t  # 4th sent bit = toggle
        if (data >> i) & 1:
            events += [(True, bit_time), (False, bit_time)]
        else:
            events += [(False, bit_time), (True, bit_time)]
    return _levels_to_pattern(events)


def _levels_to_pattern(events):
    """Merge consecutive same-level events, drop leading space, add gap."""
    merged = []
    for lvl, dur in events:
        if merged and merged[-1][0] == lvl:
            merged[-1] = (lvl, merged[-1][1] + dur)
        else:
            merged.append((lvl, dur))
    if merged and not merged[0][0]:
        merged.pop(0)
    pat = [int(dur) for _, dur in merged]
    if merged and not merged[-1][0]:
        pat[-1] += 30000  # extend trailing off into the gap
    else:
        pat.append(30000)
    return pat


PROTOCOLS = {
    "NEC": lambda a, c: nec_pattern(a, c, False),
    "NECext": lambda a, c: nec_pattern(a, c, True),
    "NEC42": lambda a, c: None,
    "NEC42ext": lambda a, c: None,
    "Samsung32": samsung_pattern,
    "SIRC": sirc_pattern,
    "SIRC15": sirc_pattern,
    "SIRC20": sirc_pattern,
    "Kaseikyo": kaseikyo_pattern,
    "RCA": rca_pattern,
    "Pioneer": pioneer_pattern,
    "RC5": rc5_pattern,
    "RC6": rc6_pattern,
}

# ---------------------------------------------------------------- .ir parsing


def parse_ir(path):
    entries = []
    cur = None
    with open(path, "r", encoding="utf-8", errors="replace") as f:
        for line in f:
            s = line.strip()
            if not s or s.startswith("#"):
                continue
            if s.startswith("name:"):
                if cur:
                    entries.append(cur)
                cur = {"name": s.split(":", 1)[1].strip()}
                continue
            if ":" in s and cur is not None:
                k, v = s.split(":", 1)
                k = k.strip().lower()
                v = v.strip()
                if k == "data":
                    cur[k] = [int(x) for x in v.split() if re.fullmatch(r"-?\d+", x)]
                else:
                    cur[k] = v
    if cur:
        entries.append(cur)
    return entries


def hexval(s):
    if s is None:
        return 0
    parts = s.replace("-", " ").split()
    try:
        raw = bytes(int(p, 16) for p in parts)
    except ValueError:
        return 0
    return int.from_bytes(raw, "little")


BUTTON_ALIASES = {
    "power": "power", "power_toggle": "power", "power_on": "power",
    "on": "power", "turn_on": "power", "off": "power_off",
    "power_off": "power_off", "turn_off": "power_off",
    "vol_up": "vol_up", "volume_up": "vol_up", "vol_+": "vol_up", "vol+": "vol_up",
    "vol_dn": "vol_down", "vol_down": "vol_down", "volume_down": "vol_down",
    "vol_-": "vol_down", "vol-": "vol_down",
    "mute": "mute", "mute_toggle": "mute",
    "ch_next": "ch_up", "ch_up": "ch_up", "channel_up": "ch_up", "ch+": "ch_up",
    "ch_prev": "ch_down", "ch_down": "ch_down", "channel_down": "ch_down", "ch-": "ch_down",
    "input": "input", "source": "input", "tv_source": "input", "input_source": "input",
    "up": "up", "arrow_up": "up", "down": "down", "arrow_down": "down",
    "left": "left", "arrow_left": "left", "right": "right", "arrow_right": "right",
    "ok": "ok", "enter": "ok", "select": "ok", "center": "ok",
    "back": "back", "return": "back", "exit": "exit", "close": "exit",
    "home": "home", "smart_hub": "home", "menu_home": "home",
    "menu": "menu", "settings": "menu",
    "info": "info", "information": "info", "guide": "guide", "epg": "guide",
    "play": "play_pause", "pause": "play_pause", "play_pause": "play_pause",
    "stop": "stop", "rewind": "rewind", "rw": "rewind",
    "fast_forward": "forward", "ff": "forward", "forward": "forward",
    "prev": "prev", "previous": "prev", "next": "next", "record": "record",
    "subtitle": "subtitle", "subtitles": "subtitle", "cc": "subtitle",
    "audio": "audio", "audio_track": "audio", "language": "audio",
    "aspect": "aspect", "ratio": "aspect", "picture_size": "aspect",
    "last": "last", "last_channel": "last", "channel_return": "last",
    "fav": "fav", "favorite": "fav", "favourite": "fav",
    "apps": "apps", "netflix": "netflix",
    "temp_up": "temp_up", "temp_plus": "temp_up", "temp+": "temp_up",
    "temp_increase": "temp_up", "increase_temp": "temp_up",
    "temp_down": "temp_down", "temp_minus": "temp_down", "temp-": "temp_down",
    "temp_decrease": "temp_down", "decrease_temp": "temp_down",
    "mode": "mode", "operation_mode": "mode",
    "fan": "fan", "fan_speed": "speed", "fan_speed_up": "speed",
    "swing": "swing", "swing_v": "swing", "swing_h": "swing",
    "swing_toggle": "swing", "vertical_swing": "swing", "horizontal_swing": "swing",
    "speed": "speed", "speed_up": "speed",
    "light": "light", "lamp": "light", "display": "light", "led": "light",
    "timer": "timer", "sleep_timer": "timer", "off_timer": "timer", "on_timer": "timer",
    "sleep": "sleep",
    "brightness_up": "brightness_up", "bright_up": "brightness_up",
    "brightness+": "brightness_up",
    "brightness_down": "brightness_down", "bright_down": "brightness_down",
    "brightness-": "brightness_down",
    "color": "color", "colour": "color", "color_temp": "color",
    "red": "red", "green": "green", "yellow": "yellow", "blue": "blue",
    "zoom_in": "zoom_in", "zoom+": "zoom_in",
    "zoom_out": "zoom_out", "zoom-": "zoom_out",
    "dash": "dash", "-": "dash", "dot": "dot", ".": "dot", "period": "dot",
}

for _d in range(10):
    BUTTON_ALIASES[str(_d)] = "num_%d" % _d
    BUTTON_ALIASES["num_%d" % _d] = "num_%d" % _d
    BUTTON_ALIASES["digit_%d" % _d] = "num_%d" % _d
    BUTTON_ALIASES["number_%d" % _d] = "num_%d" % _d

# Device-prefixed and abbreviated power labels carried by the converted
# third-party dumps. Without these a remote is dropped for having no power key.
for _p in ("tv", "vcr", "cd", "dvd", "amp", "av", "main", "zone2"):
    BUTTON_ALIASES["%s_power" % _p] = "power"
    BUTTON_ALIASES["%s-power" % _p] = "power"
    BUTTON_ALIASES["%s_on" % _p] = "power"
    BUTTON_ALIASES["%s_off" % _p] = "power_off"
BUTTON_ALIASES.update({
    "pwr": "power", "pwr_on": "power", "power_on": "power",
    "poweron": "power", "poweroff": "power_off", "pwr_off": "power_off",
    "power_on_off": "power", "power_onoff": "power", "on_off": "power",
    "onoff": "power", "off_on": "power", "turnon": "power", "turnoff": "power_off",
    "power-on": "power", "power-off": "power_off", "set_top_power": "power",
    "power_toggle_on_off": "power", "main_power": "power", "standby": "power",
    "power_on_assert": "power", "power_off_assert": "power_off",
    "toggle_power": "power", "power_button": "power", "system_standby": "power",
})

KEEP = ["power", "power_off", "vol_up", "vol_down", "mute", "ch_up", "ch_down",
        "input", "up", "down", "left", "right", "ok", "back", "home", "menu",
        "exit", "info", "guide", "play_pause", "stop", "rewind", "forward",
        "prev", "next", "record", "temp_up", "temp_down", "mode", "fan", "swing",
        "speed", "light", "timer", "sleep", "brightness_up", "brightness_down",
        "color", "red", "green", "yellow", "blue", "zoom_in", "zoom_out",
        "subtitle", "audio", "aspect", "last", "fav", "apps", "netflix",
        "num_0", "num_1", "num_2", "num_3", "num_4", "num_5", "num_6", "num_7",
        "num_8", "num_9", "dash", "dot"]


def normalize(name):
    key = re.sub(r"[^a-z0-9_+\-]", "", name.strip().lower().replace(" ", "_"))
    return BUTTON_ALIASES.get(key)


MIN_FREQ = 30000      # Android's ConsumerIrManager rejects carriers below this
MAX_FREQ = 60000
# Air-conditioner frames are whole state packets: Daikin and Mitsubishi power codes
# run to ~586 events. A HAL that refuses a long pattern raises, and the app reports
# the refusal, so the cost of shipping one is one honest error message. The cost of
# dropping one is a brand nobody can reach.
MAX_EVENTS = 1024
FRAME_GAP = 20000     # a trailing off-period this long ends a repeated frame


def _similar(a, b):
    if len(a) != len(b):
        return False
    same = sum(1 for x, y in zip(a, b) if abs(x - y) <= max(200, 0.15 * max(x, y)))
    return same >= len(a) * 0.9


def _trim_repeats(pat):
    """Cut trailing copies of the first frame.

    Only trims when the rest of the capture is a near-duplicate: aircon
    commands are sent as two different frames and must stay whole.
    """
    for i in range(1, len(pat), 2):
        if pat[i] >= FRAME_GAP and i + 1 < len(pat):
            head, tail = pat[:i + 1], pat[i + 1:]
            if _similar(head, tail[:len(head)]):
                return head
            return pat
    return pat


def sanitize(freq, pat):
    """Keep only patterns a phone can physically emit."""
    if not (MIN_FREQ <= freq <= MAX_FREQ):
        return None
    if len(pat) > MAX_EVENTS:
        pat = _trim_repeats(pat)
    if len(pat) > MAX_EVENTS or len(pat) % 2 == 1:
        return None
    return freq, pat


def to_pattern(entry):
    if entry.get("type") == "raw":
        data = entry.get("data") or []
        if len(data) < 4:
            return None
        freq = int(float(entry.get("frequency", 38000)))
        pat = [int(x) for x in data if x > 0]
        if not pat or max(pat) > 400000:
            return None
        if len(pat) % 2 == 1:
            pat.append(30000)
        else:
            pat[-1] = max(pat[-1], 30000)
        return freq, pat
    proto = entry.get("protocol")
    fn = PROTOCOLS.get(proto)
    if not fn:
        return None
    addr = hexval(entry.get("address"))
    cmd = hexval(entry.get("command"))
    try:
        pat = fn(addr, cmd)
    except Exception:
        return None
    if not pat:
        return None
    freq = {
        "RC5": 36000, "RC6": 36000, "SIRC": 40000, "SIRC15": 40000,
        "SIRC20": 40000, "Pioneer": 40000,
    }.get(proto, 38000)
    pat = list(pat)
    if len(pat) % 2 == 1:  # ends on a mark -> add trailing off gap
        pat.append(30000)
    return freq, pat


# --------------------------------------------------- converted third-party dumps
#
# Flipper-IRDB also ships an `_Converted_` tree generated from the public
# probonopd/irdb CSV collection. Its README states those files are untested and
# may carry the wrong protocol, so every remote taken from that tree is tagged
# src=1 and the app labels it "unverified source" instead of presenting it as a
# known-good code.

CONVERTED = os.path.join(ROOT, "_Converted_")
CONVERTED_SOURCES = ("CSV", "Pronto", "IR_Plus")

# Ordered device-type rules, first match wins. Needles are compared as whole
# words against the lower-cased, non-alphanumeric-stripped folder/file names.
CATEGORY_RULES = [
    ("projector|video projector|dlp|laser tv projector", "Projector"),
    ("air conditioner|aircondition|aircon|hvac|mini split|heat pump|"
     "split system|air handler|climatizador", "Air Conditioner"),
    ("fan|fans|ceiling fan", "Fan"),
    ("bluray|blue ray|sacd", "Blu-ray Player"),
    ("dvd|hd dvd|dvd recorder|dvd changer", "DVD Player"),
    ("vcr|videocassette|cassette", "VCR"),
    ("laser disc|laserdisc", "Laserdisc"),
    ("mini disc|minidisc", "MiniDisc"),
    ("cd|cd r|cd player|cd changer|cd jukebox|jukebox|cdrom|cd rom", "CD Player"),
    ("soundbar|sound base|soundbase", "Soundbar"),
    ("speaker|speakers|subwoofer|woofer", "Speaker"),
    ("receiver|amplifier|pre amp|preamp|surround|dsp|processor|avr|zone|tuner|"
     "power amp|integrated amp", "Audio / Receiver"),
    ("satellite|satelite|sat|dss|stb|set top|settop|ird|dvb|dtv|cable|hddvr|"
     "dvr|pvr|recorder|tivo", "Cable / Sat Box"),
    ("light|lighting|lights|lamp|lamps|dmx|dimmer|led strip", "LED Light"),
    ("camera|cameras|ptz|cctv|camcorder", "Camera"),
    ("monitor|monitors", "Monitor"),
    ("whiteboard|interactive board|smart board", "Whiteboard"),
    ("touchscreen", "Touchscreen"),
    ("vacuum|robot cleaner|roomba|dust collector", "Vacuum"),
    ("purifier", "Air Purifier"),
    ("heater|fireplace|fire place", "Heater"),
    ("humidifier", "Humidifier"),
    ("head unit|car audio|car stereo|car cd", "Head Unit"),
    ("switcher|matrix|switch|hub|kvm|splitter|converter|selector", "Converter"),
    ("console|game|xbox|playstation|nintendo", "Console"),
    ("toy|drone", "Toy"),
    ("clock|digital sign", "Clock"),
    ("tv|tvs|television|plasma|lcd|oled|hdtv|btv", "TV"),
    ("radio|stereo|mini system|music center|mp3|ipod|portable|media player|"
     "lifestyle|karaoke|audio", "Multimedia"),
]


def _norm(text):
    return re.sub(r"[^a-z0-9]+", " ", text.lower()).strip()


def classify(*hints):
    """Map device-type and model-name fragments onto an app category name."""
    norm = " %s " % _norm(" ".join(h for h in hints if h))
    for needles, display in CATEGORY_RULES:
        for needle in needles.split("|"):
            if " %s " % needle in norm:
                return display
    return "Misc"


def pick_display(variants):
    """Prefer a mixed-case spelling of a brand over an ALL-CAPS duplicate."""
    mixed = sorted(v for v in variants if v != v.upper())
    return (mixed or sorted(variants))[0]


def classify_buttons(keys):
    """Fallback when a converted file carries no device-type folder.

    The button vocabulary is the only evidence left: an aircon remote has temp
    keys, a TV has channel keys, an amplifier has volume but no channels.
    """
    has = set(keys)
    if has & {"temp_up", "temp_down", "swing"}:
        return "Air Conditioner"
    if has & {"ch_up", "ch_down"}:
        return "TV"
    if has & {"vol_up", "vol_down", "mute"}:
        return "Audio / Receiver"
    if has & {"play_pause", "stop", "forward", "rewind"}:
        return "Multimedia"
    return "Misc"


def brand_key(name):
    # Underscore and spacing spellings are one brand: "Hampton_Bay" joins "Hampton Bay".
    return re.sub(r"[\s_]+", " ", name.strip().strip("()")).lower()


def buttons_of(path, skipped):
    buttons = {}
    for e in parse_ir(path):
        key = normalize(e.get("name", ""))
        if not key or key in buttons:
            continue
        res = to_pattern(e)
        if not res:
            skipped[e.get("protocol", "raw")] += 1
            continue
        res = sanitize(*res)
        if not res:
            skipped["unsendable"] += 1
            continue
        buttons[key] = res
    if "power" not in buttons:
        return None
    return {k: [buttons[k][0], buttons[k][1]] for k in KEEP if k in buttons}


def converted_items():
    """Yield (device-type folder, brand dir, model name, path) from the tree."""
    for dirpath, dirnames, filenames in os.walk(CONVERTED):
        irs = sorted(f for f in filenames if f.endswith(".ir"))
        if not irs:
            continue
        rel = os.path.relpath(dirpath, CONVERTED).split(os.sep)
        top = rel[0] if rel != ["."] else ""
        if top not in CONVERTED_SOURCES or len(rel) < 3:
            continue  # skips the multi-device Power_off.ir and oddball folders
        brand = rel[2]
        dtype = rel[3] if top == "CSV" and len(rel) > 3 else None
        for fn in irs:
            yield dtype, brand, fn[:-3], os.path.join(dirpath, fn)


def make_tree():
    """Fold both corpora into {category: {brand_key: entry}}."""
    skipped = Counter()
    tree = {}
    stats = Counter()

    def place(cat_name, brand_dir, remote):
        entry = tree.setdefault(cat_name, {})
        key = brand_key(brand_dir)
        node = entry.get(key)
        if node is None:
            node = {"variants": Counter(), "remotes": [], "seen": set()}
            entry[key] = node
        node["variants"][brand_dir] += 1
        freq, pattern = remote["buttons"]["power"]
        sig = (remote["name"].lower(), freq, tuple(pattern))
        if sig in node["seen"]:
            stats["duplicate"] += 1
            return False
        node["seen"].add(sig)
        node["remotes"].append(remote)
        return True

    for folder, display in OUT_CATEGORIES.items():
        cpath = os.path.join(ROOT, folder)
        if not os.path.isdir(cpath):
            continue
        for brand in sorted(os.listdir(cpath)):
            bpath = os.path.join(cpath, brand)
            if not os.path.isdir(bpath):
                continue
            for fn in sorted(os.listdir(bpath)):
                if not fn.endswith(".ir"):
                    continue
                btns = buttons_of(os.path.join(bpath, fn), skipped)
                if btns is None:
                    stats["no_power_main"] += 1
                    continue
                if place(display, brand, {"name": fn[:-3], "buttons": btns}):
                    stats["main"] += 1

    for dtype, brand, name, path in converted_items():
        btns = buttons_of(path, skipped)
        if btns is None:
            stats["no_power_converted"] += 1
            continue
        cat_name = classify(dtype or "", name)
        if cat_name == "Misc" and dtype is None:
            cat_name = classify_buttons(btns)
            if cat_name != "Misc":
                stats["filed_by_buttons"] += 1
        if place(cat_name, brand, {"name": name, "buttons": btns, "src": 1}):
            stats["converted"] += 1

    return tree, skipped, stats


SHARD_LIMIT = 3_000_000  # keep one asset read small enough to parse on a phone


def slug(name):
    return re.sub(r"[^a-z0-9]+", "_", name.lower()).strip("_")


def write_db(categories, totals):
    """Write per-category shard files plus a small index the app reads at boot."""
    base = os.path.dirname(OUT)
    dbdir = os.path.join(base, "db")
    if os.path.isdir(dbdir):
        for fn in os.listdir(dbdir):
            os.remove(os.path.join(dbdir, fn))
    else:
        os.makedirs(dbdir, exist_ok=True)

    index = []
    for cat in categories:
        name = cat["name"]
        s = slug(name)
        files = []
        bucket = []
        size = 0

        def flush(suffix):
            fn = "db/%s%s.json" % (s, suffix)
            payload = {"name": name, "brands": bucket[:]}
            blob = json.dumps(payload, separators=(",", ":"))
            with open(os.path.join(base, fn), "w", encoding="utf-8") as fh:
                fh.write(blob)
            files.append(fn)

        for brand in cat["brands"]:
            blob = json.dumps(brand, separators=(",", ":"))
            if bucket and size + len(blob) > SHARD_LIMIT:
                flush("" if not files else "-%d" % (len(files) + 1))
                bucket = []
                size = 0
            bucket.append(brand)
            size += len(blob)
        if bucket:
            flush("" if not files else "-%d" % (len(files) + 1))
        entry = {"name": name, "files": files, "brands": len(cat["brands"]),
                 "remotes": cat["remotes"], "codes": cat["codes"],
                 "unverified": cat["unverified"]}
        index.append(entry)
    data = {
        "source": "Flipper-IRDB (https://github.com/Lucaslhm/Flipper-IRDB)",
        "format": 2,
        "totals": dict(totals),
        "categories": index,
    }
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(data, f, separators=(",", ":"))
    shards = sum(len(e["files"]) for e in index)
    return shards, os.path.getsize(OUT)


def serialize(tree):
    known = list(OrderedDict((d, 1) for _, d in OUT_CATEGORIES.items()))
    names = known + sorted(c for c in tree if c not in known)
    categories = []
    totals = Counter()
    for cat_name in names:
        entry = tree.get(cat_name)
        if not entry:
            continue
        brands = []
        remotes_total = codes_total = unver_total = 0
        for key in sorted(entry):
            node = entry[key]
            remotes = sorted(node["remotes"], key=lambda r: r["name"].lower())
            if not remotes:
                continue
            labelled = Counter()
            for r in remotes:
                labelled[r["name"]] += 1
                if labelled[r["name"]] > 1:
                    # the model spinner must never show two identical names
                    r["name"] = "%s (%d)" % (r["name"], labelled[r["name"]])
            brands.append({"name": pick_display(node["variants"]), "remotes": remotes})
            remotes_total += len(remotes)
            codes_total += sum(len(r["buttons"]) for r in remotes)
            unver_total += sum(1 for r in remotes if r.get("src"))
        if not brands:
            continue
        totals["remotes"] += remotes_total
        totals["codes"] += codes_total
        totals["unverified"] += unver_total
        totals["brands"] += len(brands)
        categories.append({"name": cat_name, "brands": brands,
                           "remotes": remotes_total, "codes": codes_total,
                           "unverified": unver_total})
        print(f"{cat_name:<20} brands={len(brands):<5} remotes={remotes_total:<6} "
              f"codes={codes_total}")
    return categories, totals


def main():
    tree, skipped, stats = make_tree()
    categories, totals = serialize(tree)
    shard_count, index_bytes = write_db(categories, totals)
    print(f"categories={len(categories)} shards={shard_count} "
          f"remotes={totals['remotes']} codes={totals['codes']} "
          f"unverified={totals['unverified']} index={index_bytes/1e3:.1f}KB")
    print("ingested:", dict(stats))
    print("skipped protocols:", skipped.most_common(8))


if __name__ == "__main__":
    main()
