// Run in a directory with minecraft-protocol 1.66.2 installed; localhost offline test server only.
const mc = require('minecraft-protocol');
const client = mc.createClient({host: '127.0.0.1', port: Number(process.env.MC_PORT || 25588),
  username: 'Issue288Visitor', auth: 'offline', version: '1.21.11'});
let sequence = 0;
client.on('position', packet => {
  client.write('teleport_confirm', {teleportId: packet.teleportId});
  client.write('position_look', {x: packet.x, y: packet.y, z: packet.z,
    yaw: packet.yaw, pitch: packet.pitch, flags: {onGround: true, hasHorizontalCollision: false}});
});
client.on('system_chat', packet => {
  if (JSON.stringify(packet).includes('E288_FIRE')) {
    client.write('use_item', {hand: 0, sequence: ++sequence, rotation: {x: 0, y: 0}});
    console.log('Fired the server-prepared crossbow');
  }
});
client.on('entity_status', packet => {
  if (packet.entityStatus === 17) console.log('Received firework visual explosion packet');
});
client.on('login', () => console.log('Test client connected'));
client.on('kick_disconnect', packet => console.error('Disconnected:', packet));
client.on('error', error => {console.error(error); process.exitCode = 1;});
client.on('end', () => process.exit());
