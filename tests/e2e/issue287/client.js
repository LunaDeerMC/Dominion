'use strict';

const { randomUUID } = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const mineflayer = require('mineflayer');
const { Vec3 } = require('vec3');

const readyFile = process.argv[2] || process.env.ISSUE287_READY_FILE;
const host = process.env.ISSUE287_HOST || '127.0.0.1';
const port = Number(process.env.ISSUE287_PORT || 25587);
const version = process.env.ISSUE287_VERSION || '26.1';

if (!readyFile || !Number.isInteger(port) || port < 1 || port > 65535) {
  console.error('Usage: node client.js /path/to/server/plugins/Issue287/ready.json');
  console.error('Alternatively set ISSUE287_READY_FILE. ISSUE287_PORT must be a valid TCP port.');
  process.exit(2);
}

const run = randomUUID();
const seen = new Set();
let finished = false;
let polling = false;
let started = false;
let startTimer;
let pollTimer;
let timeoutTimer;
let bot;

function finish(code, message) {
  if (finished) return;
  finished = true;
  clearTimeout(startTimer);
  clearInterval(pollTimer);
  clearTimeout(timeoutTimer);
  (code === 0 ? console.log : console.error)(message);
  process.exitCode = code;
  if (bot) bot.quit();
  // A broken connection must not leave the test process running indefinitely.
  setTimeout(() => process.exit(code), 1000).unref();
}

async function pollReadyFile() {
  if (!started || finished || polling) return;
  polling = true;
  try {
    let data;
    try {
      data = JSON.parse(await fs.readFile(readyFile, 'utf8'));
    } catch (error) {
      // The harness may not have created the file yet, or may be replacing it.
      if (error.code === 'ENOENT' || error instanceof SyntaxError) return;
      throw error;
    }
    if (!data || data.run !== run || finished) return;

    if (data.done === true) {
      if (!Number.isInteger(data.failures) || data.failures < 0) {
        throw new Error('Harness result has no valid failures count');
      }
      if (data.scenarios !== 16 || !Number.isInteger(data.checks) || data.checks <= 0) {
        throw new Error(`Incomplete harness result: ${JSON.stringify(data)}`);
      }
      finish(data.failures === 0 ? 0 : 1,
        `RESULT ${JSON.stringify({ run, failures: data.failures, checks: data.checks, scenarios: data.scenarios })}`);
      return;
    }

    if (!data.case || seen.has(data.case)) return;
    if (![data.x, data.y, data.z].every(Number.isInteger)) {
      throw new Error(`Invalid block coordinates for case ${data.case}`);
    }
    seen.add(data.case);
    // Allow the world change and target chunks to arrive before clicking.
    await new Promise(resolve => setTimeout(resolve, 1200));
    if (finished) return;
    const block = bot.blockAt(new Vec3(data.x, data.y, data.z));
    if (!block || block.name === 'air') {
      throw new Error(`Target block unavailable for case ${data.case} at ${data.x},${data.y},${data.z}`);
    }
    console.log(`ACTIVATE ${JSON.stringify({ run, case: data.case, world: data.world, block: block.name, position: block.position })}`);
    await bot.activateBlock(block);
    console.log(`SENT_RIGHT_CLICK ${data.case}`);
  } catch (error) {
    finish(1, `CLIENT_ERROR ${error.stack || error}`);
  } finally {
    polling = false;
  }
}

console.log(`CONNECT ${JSON.stringify({ host, port, version, username: 'Issue287Bot', run, readyFile: path.resolve(readyFile) })}`);
try {
  bot = mineflayer.createBot({ host, port, version, username: 'Issue287Bot', auth: 'offline' });
  bot.once('spawn', () => {
    console.log(`CONNECTED ${bot.version}`);
    startTimer = setTimeout(() => {
      try {
        bot.chat(`/issue287 ${run}`);
        started = true;
        console.log(`STARTED ${run}`);
      } catch (error) {
        finish(1, `START_ERROR ${error.stack || error}`);
      }
    }, 3000);
  });
  bot.on('message', message => console.log(`CHAT ${message.toString()}`));
  bot.on('error', error => finish(1, `CONNECTION_ERROR ${error.stack || error}`));
  bot.on('kicked', reason => finish(1, `KICKED ${JSON.stringify(reason)}`));
  bot.on('end', reason => finish(1, `CONNECTION_ENDED ${reason}`));
  pollTimer = setInterval(pollReadyFile, 200);
  timeoutTimer = setTimeout(() => finish(1, `TIMEOUT run=${run}: no final result within 8 minutes`), 8 * 60 * 1000);
} catch (error) {
  finish(1, `CONNECTION_ERROR ${error.stack || error}`);
}

process.once('SIGINT', () => finish(130, 'Interrupted'));
process.once('SIGTERM', () => finish(143, 'Terminated'));
