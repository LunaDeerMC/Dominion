"""Minimal localhost-only protocol client for the issue #298 potion probe.

Run two instances, with CLIENT_NAME=Issue298Visitor and Issue298Target.
The server probe equips/positions players and sends E298_USE:<case>. This client
responds with a real main-hand use-item packet, using the received rotation.
MC_PORT defaults to 25598; MC_PROTOCOL supports 776 (26.2) and 777 (26.3).
An E298_DONE message ends the connection; test assertions remain server-side.
"""

import collections
import json
import os
import re
import socket
import struct
import time
import uuid
import zlib


# IDs checked against Paper 26.2 build 132's ordered Login/Configuration/Game
# protocol declarations. Only packets required by this non-rendering client.
IDS = {
    "login_serverbound": {"hello": 0, "login_acknowledged": 3},
    "login_clientbound": {
        "disconnect": 0, "hello": 1, "login_finished": 2,
        "login_compression": 3,
    },
    "configuration_serverbound": {
        "finish_configuration": 3, "keep_alive": 4, "pong": 5,
        "select_known_packs": 7, "accept_code_of_conduct": 9,
    },
    "configuration_clientbound": {
        "disconnect": 2, "finish_configuration": 3, "keep_alive": 4,
        "ping": 5, "select_known_packs": 14, "code_of_conduct": 19,
    },
    "game_serverbound": {
        "accept_teleportation": 0, "chunk_batch_received": 11,
        "keep_alive": 28, "move_player_pos_rot": 31,
        "player_loaded": 44, "pong": 45, "use_item": 67,
    },
    "game_clientbound": {
        "chunk_batch_finished": 11, "disconnect": 32, "keep_alive": 44,
        "ping": 61, "player_position": 72, "system_chat": 121,
    },
}


def varint(value):
    result = bytearray()
    value &= 0xffffffff
    while value > 127:
        result.append((value & 127) | 128)
        value >>= 7
    result.append(value)
    return bytes(result)


def read_varint(data, offset=0):
    value = 0
    for i in range(5):
        byte = data[offset + i]
        value |= (byte & 127) << (7 * i)
        if byte < 128:
            return value, offset + i + 1
    raise ValueError("oversized VarInt")


def string(value):
    data = value.encode("utf-8")
    return varint(len(data)) + data


def main():
    protocol = int(os.environ.get("MC_PROTOCOL", "776"))
    if protocol not in (776, 777):
        raise ValueError("MC_PROTOCOL must be 776 (26.2) or 777 (26.3)")
    if protocol == 777:
        IDS["configuration_clientbound"].update(
            select_known_packs=15, code_of_conduct=20)
        IDS["game_clientbound"].update(
            keep_alive=45, ping=62, player_position=73, system_chat=124)
    incoming = {
        phase: {packet_id: name for name, packet_id in ids.items()}
        for phase, ids in IDS.items() if phase.endswith("_clientbound")
    }
    player_name = os.environ.get("CLIENT_NAME", "Issue298Visitor")
    port = int(os.environ.get("MC_PORT", "25598"))
    deadline = time.monotonic() + int(os.environ.get("MC_TIMEOUT", "1200"))
    phase = "login"
    compression = None
    position = [0.0] * 5
    sequence = 0
    loaded = False
    counts = collections.Counter()
    buffer = b""

    with socket.create_connection(("127.0.0.1", port), timeout=10) as connection:
        connection.settimeout(0.5)

        def send_id(packet_id, payload=b""):
            data = varint(packet_id) + payload
            if compression is not None:
                if len(data) >= compression:
                    data = varint(len(data)) + zlib.compress(data)
                else:
                    data = varint(0) + data
            connection.sendall(varint(len(data)) + data)

        def send(name, payload=b""):
            send_id(IDS[phase + "_serverbound"][name], payload)

        send_id(0, varint(protocol) + string("localhost")
                + struct.pack(">H", port) + varint(2))
        send("hello", string(player_name) + uuid.uuid3(
            uuid.NAMESPACE_DNS, player_name).bytes)
        try:
            while time.monotonic() < deadline:
                try:
                    data = connection.recv(1048576)
                except socket.timeout:
                    continue
                if not data:
                    raise RuntimeError("Server closed connection before E298_DONE")
                buffer += data
                while buffer:
                    try:
                        length, start = read_varint(buffer)
                    except IndexError:
                        break
                    if len(buffer) < start + length:
                        break
                    packet = buffer[start:start + length]
                    buffer = buffer[start + length:]
                    if compression is not None:
                        uncompressed, offset = read_varint(packet)
                        packet = (zlib.decompress(packet[offset:]) if uncompressed
                                  else packet[offset:])
                    packet_id, offset = read_varint(packet)
                    payload = packet[offset:]
                    name = incoming[phase + "_clientbound"].get(
                        packet_id, str(packet_id))
                    counts[phase + ":" + name] += 1
                    if name == "disconnect":
                        raise RuntimeError("Disconnected: " + repr(payload))
                    if phase == "login":
                        if name == "hello":
                            raise RuntimeError("Probe requires online-mode=false")
                        if name == "login_compression":
                            compression, _ = read_varint(payload)
                        elif name == "login_finished":
                            send("login_acknowledged")
                            phase = "configuration"
                    elif phase == "configuration":
                        if name == "select_known_packs":
                            send("select_known_packs", varint(0))
                        elif name == "code_of_conduct":
                            send("accept_code_of_conduct")
                        elif name == "finish_configuration":
                            send("finish_configuration")
                            phase = "game"
                            print("PLAY", player_name, "protocol", protocol, flush=True)
                        elif name == "keep_alive":
                            send("keep_alive", payload)
                        elif name == "ping":
                            send("pong", payload)
                    elif name == "keep_alive":
                        send("keep_alive", payload)
                    elif name == "ping":
                        send("pong", payload)
                    elif name == "player_position":
                        teleport, offset = read_varint(payload)
                        values = (struct.unpack_from(">ddd", payload, offset)
                                  + struct.unpack_from(">ff", payload, offset + 48))
                        relatives, = struct.unpack_from(">i", payload, offset + 56)
                        position = [
                            value + (position[i] if relatives & (1 << i) else 0)
                            for i, value in enumerate(values)
                        ]
                        confirmation = varint(teleport)
                        if protocol == 777:
                            confirmation += struct.pack(">dddff", *position)
                        send("accept_teleportation", confirmation)
                        if protocol == 776:
                            send("move_player_pos_rot",
                                 struct.pack(">dddffB", *position, 0))
                        if not loaded:
                            send("player_loaded")
                            loaded = True
                    elif name == "chunk_batch_finished":
                        send("chunk_batch_received", struct.pack(">f", 20.0))
                    elif name == "system_chat":
                        case = re.search(rb"E298_USE:([A-Za-z0-9_.-]+)", payload)
                        if case:
                            sequence += 1
                            send("use_item", varint(0) + varint(sequence)
                                 + struct.pack(">ff", *position[3:]))
                            print("USE", case.group(1).decode(), "sequence", sequence,
                                  "rotation", position[3:], flush=True)
                        if b"E298_DONE" in payload:
                            print("DONE", player_name, "uses", sequence, flush=True)
                            return
            raise TimeoutError("Timed out waiting for E298_DONE")
        finally:
            print("PACKETS", player_name, json.dumps(counts, sort_keys=True), flush=True)


if __name__ == "__main__":
    main()
