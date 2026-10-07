"""Minimal 26.3 protocol probe; not a rendering client. Local disposable server only."""
import collections, json, os, re, socket, struct, sys, time, uuid, zlib
from pathlib import Path
IDS = json.loads(Path(__file__).with_name('packet-ids.json').read_text())
def vi(n):
    out = bytearray(); n &= 0xffffffff
    while n > 127: out.append((n & 127) | 128); n >>= 7
    out.append(n); return bytes(out)
def readvi(data, offset=0):
    value = 0
    for i in range(5):
        b = data[offset+i]; value |= (b & 127) << (7*i)
        if b < 128: return value, offset+i+1
    raise ValueError('oversized VarInt')
def string(s):
    b=s.encode(); return vi(len(b))+b
name=os.environ.get('PROBE_NAME','Probe263')
ready=os.environ.get('ISSUE287_READY_FILE')
run=str(uuid.uuid4()); seen=set(); pending=None; started=False; sequence=0
s=socket.create_connection(('127.0.0.1',int(os.environ.get('PROBE_PORT','25583'))));s.settimeout(0.2)
compression=False; phase='login'; counts=collections.Counter(); deadline=time.monotonic()+(480 if ready else 300)
buffer=b''; position=[0.0]*5
def send_id(i,payload=b''):
    data=vi(i)+payload
    if compression: data=vi(0)+data
    s.sendall(vi(len(data))+data)
def send(name,payload=b''):send_id(IDS[phase+'_serverbound'][name],payload)
send_id(0,vi(777)+string('localhost')+struct.pack('>H',25583)+vi(2))
send('hello',string(name)+uuid.UUID(int=263).bytes)
try:
    while time.monotonic()<deadline:
        if phase=='game' and ready:
            if not started:
                send('chat_command',string('issue287 '+run));started=True
            try: state=json.loads(Path(ready).read_text())
            except (FileNotFoundError,json.JSONDecodeError): state={}
            if state.get('run')==run:
                if state.get('done'):
                    print('RESULT',json.dumps(state),flush=True)
                    sys.exit(0 if state.get('failures')==0 and state.get('scenarios')==16 and state.get('checks',0)>0 else 1)
                if state.get('case') and state['case'] not in seen:
                    seen.add(state['case']);pending=(time.monotonic()+1.2,state)
            if pending and time.monotonic()>=pending[0]:
                state=pending[1];pending=None;x,y,z=state['x'],state['y'],state['z'];sequence+=1
                packed=((x&0x3ffffff)<<38)|((z&0x3ffffff)<<12)|(y&0xfff)
                send('use_item_on',vi(0)+struct.pack('>Q',packed)+vi(1)+struct.pack('>fff',0.5,1.,0.5)+b'\x00\x00'+vi(sequence))
                print('RIGHT_CLICK',state['case'],flush=True)
        try: data=s.recv(1048576)
        except socket.timeout: continue
        if not data: break
        buffer+=data
        while buffer:
            try: length,start=readvi(buffer)
            except IndexError: break
            if len(buffer)<start+length: break
            packet=buffer[start:start+length];buffer=buffer[start+length:]
            if compression:
                uncompressed,off=readvi(packet);packet=zlib.decompress(packet[off:]) if uncompressed else packet[off:]
            pid,off=readvi(packet);payload=packet[off:]
            name=next((k for k,v in IDS[phase+'_clientbound'].items() if v==pid),str(pid))
            counts[phase+':'+name]+=1
            if 'disconnect' in name: print('DISCONNECT',repr(payload),flush=True)
            if phase=='login':
                if name=='login_compression':compression=True
                elif name=='login_finished':send('login_acknowledged');phase='configuration'
            elif phase=='configuration':
                if name=='select_known_packs':send('select_known_packs',vi(0))
                elif name=='finish_configuration':send('finish_configuration');phase='game';print('PLAY',flush=True)
                elif name=='keep_alive':send('keep_alive',payload)
                elif name=='ping':send('pong',payload)
            else:
                if name=='keep_alive':send('keep_alive',payload)
                elif name=='ping':send('pong',payload)
                elif name=='player_position':
                    teleport,pos=readvi(payload)
                    values=struct.unpack('>ddd',payload[pos:pos+24])+struct.unpack('>ff',payload[pos+48:pos+56])
                    relative=struct.unpack('>i',payload[pos+56:pos+60])[0]
                    position=[v+(position[i] if relative & (1<<i) else 0) for i,v in enumerate(values)]
                    send('accept_teleportation',vi(teleport)+struct.pack('>dddff',*position))
                elif name=='chunk_batch_finished':send('chunk_batch_received',struct.pack('>f',20.0))
                elif name=='system_chat':
                    if b'E288_FIRE' in payload:
                        sequence+=1;send('use_item',vi(0)+vi(sequence)+struct.pack('>ff',0.,0.))
                        print('CROSSBOW fired',flush=True)
                    m=re.search(rb'PROBE263_BREAK:(\d+):(\d+):(\d+)',payload)
                    if m:
                        x,y,z=map(int,m.groups()); packed=((x&0x3ffffff)<<38)|((z&0x3ffffff)<<12)|(y&0xfff)
                        send('player_action',vi(0)+struct.pack('>Q',packed)+b'\x01'+vi(1))
                        print('BREAK request',x,y,z,flush=True)
                elif name=='entity_event' and payload[-1:]==b'\x11':
                    print('FIREWORK visual',flush=True)
                elif name=='show_dialog':
                    # The probe dialog has one callback. Extract its NBT string and return it twice
                    # to exercise both delivery and single-use replay protection.
                    m=re.search(b'__dominion_token\x00\x16([A-Za-z0-9_-]{22})',payload)
                    print('DIALOG',len(payload),'callback=',bool(m),flush=True)
                    if m:
                        key=b'__dominion_token'; token=m.group(1)
                        nbt=b'\x0a\x08'+struct.pack('>H',len(key))+key+struct.pack('>H',len(token))+token+b'\x00'
                        click=string('dominion:dialog_callback')+vi(len(nbt))+nbt
                        send('custom_click_action',click);send('custom_click_action',click)
finally:
    s.close(); print('PACKETS',json.dumps(counts,sort_keys=True),flush=True)
