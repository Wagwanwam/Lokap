// Điền URL Firebase trực tiếp vào các request YAML đang mở trong Postman.
// Chạy: node Lokap/firebase/requests/bind-firebase.mjs (không gọi API).
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';
const require = createRequire(import.meta.url);
const YAML = require('yaml');
const appRoot = fileURLToPath(new URL('../../', import.meta.url));
const workspace = path.resolve(appRoot, '..');
const requestsDir = fileURLToPath(new URL('./', import.meta.url));
const collectionName = 'Lokap · Firebase Auth & Firestore';
const liveCollection = path.join(workspace, 'postman/collections', collectionName);
const sourceCollection = path.join(requestsDir, collectionName);
const emulatorCollection = path.join(workspace, 'postman/collections/Lokap · Firebase Emulator');

const services = JSON.parse(fs.readFileSync(path.join(appRoot, 'app/google-services.json'), 'utf8'));
const client = services.client.find(c => c.client_info?.android_client_info?.package_name === 'vn.nhom2.lokap');
if (!client) throw new Error('Không tìm thấy cấu hình Android vn.nhom2.lokap.');
const dotenv = {};
const dotenvPath = path.join(appRoot, '.env');
if (fs.existsSync(dotenvPath)) {
  for (const raw of fs.readFileSync(dotenvPath, 'utf8').split('\n')) {
    const line = raw.trim();
    if (!line || line.startsWith('#') || !line.includes('=')) continue;
    const at = line.indexOf('=');
    dotenv[line.slice(0, at).trim()] = line.slice(at + 1).trim().replace(/^"(.*)"$/, '$1');
  }
}
const firebase = {
  projectId: dotenv.FIREBASE_PROJECT_ID || services.project_info.project_id,
  apiKey: dotenv.FIREBASE_API_KEY || client.api_key[0].current_key,
  databaseId: '(default)',
  authBaseUrl: 'https://identitytoolkit.googleapis.com/v1',
  tokenBaseUrl: 'https://securetoken.googleapis.com/v1',
  firestoreBaseUrl: 'https://firestore.googleapis.com/v1',
  authEmulatorOrigin: 'http://127.0.0.1:9099'
};
const emulator = {
  projectId: 'demo-lokap', apiKey: 'fake-api-key', databaseId: '(default)',
  authBaseUrl: 'http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1',
  tokenBaseUrl: 'http://127.0.0.1:9099/securetoken.googleapis.com/v1',
  firestoreBaseUrl: 'http://127.0.0.1:8080/v1',
  authEmulatorOrigin: 'http://127.0.0.1:9099'
};
if (Object.values(firebase).some(value => !value)) throw new Error('Thiếu cấu hình Firebase của app.');

function walk(folder) {
  return fs.readdirSync(folder, { withFileTypes: true }).flatMap(entry => {
    const file = path.join(folder, entry.name);
    return entry.isDirectory() ? walk(file) : [file];
  });
}

// Giữ collection Emulator riêng trước khi điền URL production vào collection chính.
if (!fs.existsSync(emulatorCollection)) fs.cpSync(liveCollection, emulatorCollection, { recursive: true });
for (const [folder, config, isEmulator] of [
  [liveCollection, firebase, false], [sourceCollection, firebase, false], [emulatorCollection, emulator, true]
]) {
  const definitionFile = path.join(folder, '.resources/definition.yaml');
  const doc = YAML.parseDocument(fs.readFileSync(definitionFile, 'utf8'));
  const code = doc.getIn(['scripts', 0, 'code']);
  const previousConfigMatch = code.match(/const firebaseConfig = (\{[\s\S]*?\n\});/);
  const previousConfig = previousConfigMatch ? JSON.parse(previousConfigMatch[1]) : null;
  const requestFiles = walk(folder).filter(file => file.endsWith('.request.yaml'));
  for (const file of requestFiles) {
    let content = fs.readFileSync(file, 'utf8');
    // Cho phép chạy lại khi project đổi hoặc tạo lại bản Emulator từ collection đã điền URL.
    if (previousConfig) {
      for (const key of ['authBaseUrl', 'tokenBaseUrl', 'firestoreBaseUrl', 'authEmulatorOrigin']) {
        content = content.replaceAll(previousConfig[key] + '/', `{{${key}}}/`);
      }
      content = content.replaceAll(
        `projects/${previousConfig.projectId}/databases/${previousConfig.databaseId}`,
        'projects/{{projectId}}/databases/{{databaseId}}'
      );
      content = content.replaceAll(previousConfig.apiKey, '{{apiKey}}');
    }
    const emulatorHelper = path.basename(path.dirname(file)).startsWith('08 ');
    const requestConfig = emulatorHelper ? emulator : config;
    for (const [key, value] of Object.entries(requestConfig)) {
      content = content.replaceAll(`{{${key}Json}}`, JSON.stringify(value)).replaceAll(`{{${key}}}`, value);
    }
    if (emulatorHelper) {
      const doc = YAML.parseDocument(content);
      doc.set('description', isEmulator
        ? 'Lấy mã xác minh email từ Auth Emulator local. Chạy 01.3 trước, sau đó dùng 01.4 để xác minh.'
        : 'Request hỗ trợ Auth Emulator. Mở collection Lokap · Firebase Emulator và chọn environment Emulator để chạy request này.');
      const scripts = doc.get('scripts');
      for (let i = 0; i < scripts.items.length; i++) {
        if (scripts.items[i].get('type') === 'beforeRequest') {
          doc.setIn(['scripts', i, 'code'], isEmulator
            ? "if (pm.variables.get('projectId') !== 'demo-lokap') throw new Error('Request này chỉ dùng cho Auth Emulator local.');"
            : "throw new Error('Mở collection Lokap · Firebase Emulator để chạy request 08.1.');");
        }
      }
      content = doc.toString();
    }
    fs.writeFileSync(file, content);
  }

  const at = code.indexOf('const now = Date.now();');
  if (at < 0) throw new Error('Không tìm thấy script khởi tạo của collection.');
  const modeName = isEmulator ? 'Lokap · Emulator · A' : 'Lokap · Firebase · A';
  const bootstrap = [
    '// URL và document path đã được điền trực tiếp; các biến này phục vụ script/body.',
    `const firebaseConfig = ${JSON.stringify(config, null, 2)};`,
    "const selectedProject = pm.environment.get('projectId');",
    `if (selectedProject && selectedProject !== firebaseConfig.projectId) throw new Error('Chọn environment ${modeName} cho collection này.');`,
    'for (const [key, value] of Object.entries(firebaseConfig)) pm.variables.set(key, value);',
    code.slice(at)
  ].join('\n');
  doc.setIn(['scripts', 0, 'code'], bootstrap);
  doc.set('description', isEmulator
    ? '51 request dùng Firebase Emulator local (demo-lokap). Chọn environment Lokap · Emulator · A. URL đã điền sẵn; gửi từng request theo luồng trong README.'
    : '51 request dùng Firebase thật của app Lokap. URL, project, database và API key đã điền trực tiếp. Chọn environment Lokap · Firebase · A, nhập email/mật khẩu và dữ liệu của thao tác. Request 08.1 chỉ dùng ở collection Emulator. Gửi từng request theo luồng trong README.');
  fs.writeFileSync(definitionFile, doc.toString());
  console.log(`${path.relative(workspace, folder)}: đã điền URL cho ${requestFiles.length} request.`);
}
