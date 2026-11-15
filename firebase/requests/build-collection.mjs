// Tạo collection Postman từ schema/rules Lokap. Chạy: node requests/build-collection.mjs
import { writeFileSync } from 'node:fs';

const base = '{{firestoreBaseUrl}}/projects/{{projectId}}/databases/{{databaseId}}/documents';
const root = 'projects/{{projectId}}/databases/{{databaseId}}/documents';
const auth = '{{authBaseUrl}}/accounts:';
const str = (value) => ({ stringValue: value });
const num = (value) => ({ integerValue: String(value) });
const bool = (value) => ({ booleanValue: value });
const nil = { nullValue: null };
const map = (fields = {}) => ({ mapValue: { fields } });
const arr = (...values) => ({ arrayValue: { values: values.map(str) } });
const time = (value) => ({ timestampValue: value });
const stamp = (...fields) => fields.map((fieldPath) => ({ fieldPath, setToServerValue: 'REQUEST_TIME' }));
const create = (path, fields, timestamps = ['createdAt']) => ({
  update: { name: `${root}/${path}`, fields }, currentDocument: { exists: false },
  ...(timestamps.length ? { updateTransforms: stamp(...timestamps) } : {})
});
const update = (path, fields, timestamps = [], mask = Object.keys(fields)) => ({
  update: { name: `${root}/${path}`, fields }, updateMask: { fieldPaths: mask },
  currentDocument: { exists: true },
  ...(timestamps.length ? { updateTransforms: stamp(...timestamps) } : {})
});
const filter = (fieldPath, op, value) => ({ fieldFilter: { field: { fieldPath }, op, value } });
const query = (collectionId, filters = [], order = [], limit = 50) => ({ structuredQuery: {
  from: [{ collectionId }],
  ...(filters.length ? { where: filters.length === 1 ? filters[0] : { compositeFilter: { op: 'AND', filters } } } : {}),
  ...(order.length ? { orderBy: order.map(([fieldPath, direction]) => ({ field: { fieldPath }, direction })) } : {}),
  limit
} });
const event = (listen, code) => ({ listen, script: { type: 'text/javascript', exec: code.trim().split('\n') } });
const saveSession = `
if (pm.response.code === 200) {
  const data = pm.response.json();
  pm.environment.set('idToken', data.idToken || data.id_token);
  pm.environment.set('refreshToken', data.refreshToken || data.refresh_token);
  pm.environment.set('uid', data.localId || data.user_id);
}`;
const variables = {
  projectId: '', databaseId: '(default)', apiKey: '',
  authBaseUrl: 'https://identitytoolkit.googleapis.com/v1',
  tokenBaseUrl: 'https://securetoken.googleapis.com/v1',
  firestoreBaseUrl: 'https://firestore.googleapis.com/v1',
  authEmulatorOrigin: 'http://127.0.0.1:9099',
  email: '', password: '', idToken: '', refreshToken: '', uid: '', oobCode: '',
  username: '', displayName: 'Người thử Lokap', bio: 'Thử API Lokap', birthYear: '2000',
  peerUid: '', searchPrefix: 'lokap',
  postId: '', postAuthorId: '', messageId: '', commentId: '', notificationId: '',
  imageUrl: 'https://res.cloudinary.com/demo/image/upload/sample.jpg', imagePublicId: 'sample',
  caption: 'Khoảnh khắc từ Postman', delayDays: '0', reaction: '❤️',
  message: 'Chào bạn từ Postman!', comment: 'Ảnh đẹp quá!',
  reportReason: 'Nội dung không phù hợp', reportCount: '', postUpdateTime: '',
};
const derived = ['now', 'discoverBefore', 'publicVisibleAt', 'pairId', 'memberLow', 'memberHigh', 'searchName', 'searchEnd', 'nextReportCount', 'reportStatus'];
const serializeVariables = `
// Biến *Json đã escape: tên, mật khẩu và nội dung có dấu nháy/xuống dòng vẫn là JSON hợp lệ.
${JSON.stringify([...Object.keys(variables), ...derived])}.forEach((key) => {
  const value = pm.variables.get(key);
  if (value !== undefined) pm.variables.set(key + 'Json', JSON.stringify(String(value)));
});`;
const folders = [];
let folder;
function group(name, description) { folder = { name, description, item: [] }; folders.push(folder); }
function request(name, method, url, body, { description = '', pre = '', test = '', noauth = false, form = false, status = 200 } = {}) {
  const result = {
    name, request: { method, header: [], url, description },
    event: [event('prerequest', pre + '\n' + serializeVariables), event('test', `
pm.test('HTTP ${status}', () => pm.response.to.have.status(${status}));
${test}`)]
  };
  if (noauth) result.request.auth = { type: 'noauth' };
  if (body) {
    if (form) result.request.body = { mode: 'urlencoded', urlencoded: Object.entries(body).map(([key, value]) => ({ key, value, type: 'text' })) };
    else {
      // Chỉ thay biến chiếm toàn bộ chuỗi; đường dẫn document vẫn dùng {{uid}}, {{postId}}.
      const raw = JSON.stringify(body, null, 2).replace(/"\{\{(\w+)\}\}"/g, '{{$1Json}}');
      result.request.body = { mode: 'raw', raw, options: { raw: { language: 'json' } } };
      result.request.header.push({ key: 'Content-Type', value: 'application/json' });
    }
  }
  folder.item.push(result);
}
const commit = (name, writes, options) => request(name, 'POST', base + ':commit', { writes }, options);
const fresh = (key) => `pm.environment.set('${key}', pm.variables.replaceIn('{{$guid}}'));`;
const notice = (target, id, type, sourceId, postId = null, conversationId = null) => create(
  `users/${target}/notifications/${id}`,
  { type: str(type), actorId: str('{{uid}}'), postId: postId ? str(postId) : nil,
    conversationId: conversationId ? str(conversationId) : nil, sourceId: str(sourceId), isRead: bool(false) }
);

group('01 · Authentication', 'Đăng ký → gửi link xác minh → xác minh → đăng nhập lại để token có email_verified=true.');
request('01.1 · Đăng ký email', 'POST', auth + 'signUp?key={{apiKey}}', { email: '{{email}}', password: '{{password}}', returnSecureToken: true }, { noauth: true, test: saveSession });
request('01.2 · Đăng nhập', 'POST', auth + 'signInWithPassword?key={{apiKey}}', { email: '{{email}}', password: '{{password}}', returnSecureToken: true }, { noauth: true, test: saveSession });
request('01.3 · Gửi link xác minh email', 'POST', auth + 'sendOobCode?key={{apiKey}}', { requestType: 'VERIFY_EMAIL', idToken: '{{idToken}}' }, { noauth: true });
request('01.4 · Xác minh bằng oobCode', 'POST', auth + 'update?key={{apiKey}}', { oobCode: '{{oobCode}}' }, { noauth: true, description: 'Lấy oobCode từ link email hoặc request 08.1 trên Emulator. Sau đó chạy 01.2 hoặc 01.5 để làm mới token.' });
request('01.5 · Làm mới ID token', 'POST', '{{tokenBaseUrl}}/token?key={{apiKey}}', { grant_type: 'refresh_token', refresh_token: '{{refreshToken}}' }, { noauth: true, form: true, test: saveSession });
request('01.6 · Xem tài khoản Auth', 'POST', auth + 'lookup?key={{apiKey}}', { idToken: '{{idToken}}' }, { noauth: true, test: `if (pm.response.code === 200) pm.test('Đúng UID đang đăng nhập', () => pm.expect(pm.response.json().users[0].localId).to.eql(pm.environment.get('uid')));` });
request('01.7 · Gửi email đặt lại mật khẩu', 'POST', auth + 'sendOobCode?key={{apiKey}}', { requestType: 'PASSWORD_RESET', email: '{{email}}' }, { noauth: true, description: 'Chạy riêng khi cần. Firebase thật sẽ gửi email tới địa chỉ đã điền.' });

group('02 · Hồ sơ và cài đặt', 'Tạo username + hồ sơ công khai + hồ sơ riêng tư trong cùng commit để thỏa getAfter() trong rules.');
request('02.1 · Kiểm tra username', 'GET', base + '/usernames/{{username}}', null, { test: '', status: 404, description: '404 = tên chưa dùng; 200 = đã có. Request này cố ý mong đợi 404 trước bước tạo hồ sơ.' });
commit('02.2 · Tạo hồ sơ', [
  create('usernames/{{username}}', { uid: str('{{uid}}') }, []),
  create('users/{{uid}}', { username: str('{{username}}'), displayName: str('{{displayName}}'), searchName: str('{{searchName}}'), avatarUrl: nil, bio: str('{{bio}}'), friendCount: num(0) }),
  create('users/{{uid}}/private/profile', { email: str('{{email}}'), birthYear: num('{{birthYear}}'), homeProvinceCode: nil, defaultLocationLevel: str('PROVINCE') }, [])
], { description: 'Chỉ chạy một lần cho tài khoản mới. username phải có 3–20 ký tự [a-z0-9._]; email đã xác minh; tên 2–30; bio ≤60; đủ 13 tuổi.' });
request('02.3 · Đọc hồ sơ của mình', 'GET', base + '/users/{{uid}}');
request('02.4 · Đọc hồ sơ riêng tư', 'GET', base + '/users/{{uid}}/private/profile');
commit('02.5 · Sửa tên và giới thiệu', [update('users/{{uid}}', { displayName: str('{{displayName}}'), searchName: str('{{searchName}}'), bio: str('{{bio}}') })]);
commit('02.6 · Cập nhật cài đặt', [update('users/{{uid}}/private/profile', { defaultLocationLevel: str('PROVINCE'), notifyNewPhotos: bool(true), notifyReactions: bool(true), notifyMessages: bool(true), notifyFriendRequests: bool(true) })]);
request('02.7 · Tìm người theo username', 'POST', base + ':runQuery', query('users', [filter('username', 'GREATER_THAN_OR_EQUAL', str('{{searchPrefix}}')), filter('username', 'LESS_THAN_OR_EQUAL', str('{{searchEnd}}'))], [['username', 'ASCENDING']], 20));

group('03 · Bạn bè', 'Dùng hai environment A/B. peerUid là UID của người kia. pairId tự sắp xếp hai UID.');
request('03.1 · Danh sách quan hệ của mình', 'POST', base + ':runQuery', query('friendships', [filter('users', 'ARRAY_CONTAINS', str('{{uid}}'))]));
commit('03.2 · Gửi lời mời và thông báo', [
  create('friendships/{{pairId}}', { users: arr('{{memberLow}}', '{{memberHigh}}'), requesterId: str('{{uid}}'), addresseeId: str('{{peerUid}}'), status: str('pending') }),
  notice('{{peerUid}}', '{{notificationId}}', 'FRIEND_REQUEST', '{{pairId}}')
], { pre: fresh('notificationId'), description: 'A gửi cho B. Cả hai phải có hồ sơ.' });
commit('03.3 · Chấp nhận và thông báo', [
  update('friendships/{{pairId}}', { status: str('accepted') }, ['acceptedAt']),
  notice('{{peerUid}}', '{{notificationId}}', 'FRIEND_ACCEPTED', '{{pairId}}')
], { pre: fresh('notificationId'), description: 'Chuyển sang B, peerUid=A. Chỉ người nhận lời mời mới chấp nhận được.' });
request('03.4 · Đọc quan hệ theo cặp', 'GET', base + '/friendships/{{pairId}}');

group('04 · Bài viết và feed', 'Ảnh đã được upload Cloudinary. Mẫu dùng ảnh demo công khai; thay imageUrl/imagePublicId khi thử dữ liệu riêng.');
function postFields(audience, province) {
  return {
    authorId: str('{{uid}}'), author: map({ username: str('{{username}}'), displayName: str('{{displayName}}'), avatarUrl: nil }),
    imageUrl: str('{{imageUrl}}'), imagePublicId: str('{{imagePublicId}}'), caption: str('{{caption}}'), audience: str(audience),
    viewerIds: audience === 'public' ? arr('{{uid}}') : arr('{{uid}}', '{{peerUid}}'), recipientId: audience === 'direct' ? str('{{peerUid}}') : nil,
    locationLevel: str(province ? 'province' : 'country'), provinceCode: province ? str('da-nang') : nil, provinceName: province ? str('Đà Nẵng') : nil,
    wardCode: nil, wardName: nil, placeId: nil, placeName: nil, landmarkId: nil,
    displayLat: province ? { doubleValue: 16.0544 } : nil, displayLng: province ? { doubleValue: 108.2022 } : nil,
    delayDays: num(audience === 'public' ? '{{delayDays}}' : 0), publicVisibleAt: audience === 'public' ? time('{{publicVisibleAt}}') : nil,
    takenAt: time('{{now}}'), status: str('active'), reactions: map(), reportCount: num(0)
  };
}
for (const [suffix, audience, province] of [['1 · Tạo bài public', 'public', false], ['2 · Tạo bài bạn bè và GPS riêng tư', 'friends', true], ['3 · Tạo bài gửi riêng', 'direct', false]]) {
  const writes = [create('posts/{{postId}}', postFields(audience, province))];
  if (province) writes.push(create('posts/{{postId}}/private/location', { lat: { doubleValue: 16.06781 }, lng: { doubleValue: 108.22083 }, accuracyM: { doubleValue: 12 } }, []));
  commit('04.' + suffix, writes, { pre: fresh('postId'), test: `if (pm.response.code === 200) pm.environment.set('postAuthorId', pm.environment.get('uid'));`, description: 'Mỗi Send tạo postId mới. Chép postId và postAuthorId sang environment B để thử đọc/bình luận. Bài gửi riêng yêu cầu đã là bạn bè. Mức country không ghi GPS. Mẫu friends chỉ có mình + peerUid.' });
}
request('04.4 · Đọc bài theo ID', 'GET', base + '/posts/{{postId}}', null, { test: `
if (pm.response.code === 200) {
  const d = pm.response.json();
  pm.environment.set('postAuthorId', d.fields.authorId.stringValue);
  pm.environment.set('reportCount', d.fields.reportCount.integerValue);
  pm.environment.set('postUpdateTime', d.updateTime);
}` });
request('04.5 · Feed bạn bè', 'POST', base + ':runQuery', query('posts', [filter('viewerIds', 'ARRAY_CONTAINS', str('{{uid}}')), filter('status', 'EQUAL', str('active'))], [['createdAt', 'DESCENDING']]));
request('04.6 · Feed khám phá', 'POST', base + ':runQuery', query('posts', [filter('audience', 'EQUAL', str('public')), filter('status', 'EQUAL', str('active')), filter('publicVisibleAt', 'LESS_THAN_OR_EQUAL', time('{{discoverBefore}}'))], [['publicVisibleAt', 'DESCENDING']]), { description: 'Mốc giờ lùi 2 phút như app; bài public vừa tạo có thể cần hơn 2 phút mới xuất hiện ở đây.' });
request('04.7 · Bài của mình', 'POST', base + ':runQuery', query('posts', [filter('authorId', 'EQUAL', str('{{uid}}'))], [['createdAt', 'DESCENDING']]));
commit('04.8 · Sửa caption', [update('posts/{{postId}}', { caption: str('{{caption}}') })]);
commit('04.9 · Thả cảm xúc', [update('posts/{{postId}}', { reactions: map({ '{{uid}}': str('{{reaction}}') }) }, [], ['reactions.`{{uid}}`'])], { description: 'Chỉ đổi ô của mình, giữ cảm xúc người khác. Request dữ liệu cơ bản; chưa kèm thông báo REACTION.' });
request('04.10 · Đọc GPS gốc (chủ bài)', 'GET', base + '/posts/{{postId}}/private/location');
const reportWrite = update('posts/{{postId}}', { reportCount: num('{{nextReportCount}}'), status: str('{{reportStatus}}') });
reportWrite.currentDocument = { updateTime: '{{postUpdateTime}}' };
commit('04.11 · Báo cáo bài', [reportWrite, create('posts/{{postId}}/reports/{{uid}}', { reason: str('{{reportReason}}') })], {
  pre: `
const count = Number(pm.environment.get('reportCount'));
if (pm.environment.get('reportCount') === '' || !Number.isInteger(count) || count < 0 || !pm.environment.get('postUpdateTime')) throw new Error('Chạy 04.4 ngay trước khi báo cáo.');
pm.variables.set('nextReportCount', String(count + 1));
pm.variables.set('reportStatus', count + 1 >= 3 ? 'hidden' : 'active');`,
  description: 'Người báo cáo khác tác giả, hồ sơ đã tạo >24 giờ. Chạy 04.4 trước. Nếu document đã đổi, đọc lại rồi thử lại; updateTime ngăn ghi đè khi có báo cáo đồng thời. Phiếu thứ 3 tự ẩn bài.'
});

group('05 · Bình luận', 'postAuthorId lấy từ 04.4. Request thêm bình luận dưới đây dùng khi người bình luận khác tác giả.');
request('05.1 · Danh sách bình luận', 'POST', base + '/posts/{{postId}}:runQuery', query('comments', [], [['createdAt', 'ASCENDING']]));
commit('05.2 · Bình luận và thông báo', [
  create('posts/{{postId}}/comments/{{commentId}}', { authorId: str('{{uid}}'), content: str('{{comment}}'), parentId: nil, votes: map() }, ['createdAt', 'updatedAt']),
  notice('{{postAuthorId}}', 'comment_{{commentId}}', 'COMMENT', '{{commentId}}', '{{postId}}')
], { pre: fresh('commentId') + `\nif (pm.environment.get('postAuthorId') === pm.environment.get('uid')) throw new Error('Dùng tài khoản khác tác giả; tác giả dùng 05.3.');` });
commit('05.3 · Tác giả tự bình luận', [create('posts/{{postId}}/comments/{{commentId}}', { authorId: str('{{uid}}'), content: str('{{comment}}'), parentId: nil, votes: map() }, ['createdAt', 'updatedAt'])], { pre: fresh('commentId') });
commit('05.4 · Sửa bình luận', [update('posts/{{postId}}/comments/{{commentId}}', { content: str('{{comment}}') }, ['updatedAt'])]);
commit('05.5 · Upvote bình luận', [update('posts/{{postId}}/comments/{{commentId}}', { votes: map({ '{{uid}}': num(1) }) }, [], ['votes.`{{uid}}`'])]);

group('06 · Chat', 'Hai tài khoản phải là bạn bè. pairId tự tính từ uid/peerUid.');
commit('06.1 · Tạo cuộc trò chuyện', [create('conversations/{{pairId}}', { members: arr('{{memberLow}}', '{{memberHigh}}'), readAt: map() })], { description: 'Chạy một lần; nếu đã có thì dùng 06.2/06.4. Preconditions tránh ghi đè readAt.' });
request('06.2 · Danh sách cuộc trò chuyện', 'POST', base + ':runQuery', query('conversations', [filter('members', 'ARRAY_CONTAINS', str('{{uid}}'))]));
commit('06.3 · Gửi tin và thông báo', [
  create('conversations/{{pairId}}/messages/{{messageId}}', { senderId: str('{{uid}}'), body: str('{{message}}') }, ['createdAt', 'updatedAt']),
  notice('{{peerUid}}', 'message_{{messageId}}', 'NEW_MESSAGE', '{{messageId}}', null, '{{pairId}}')
], { pre: fresh('messageId') });
request('06.4 · Đọc tin nhắn', 'POST', base + '/conversations/{{pairId}}:runQuery', query('messages', [], [['createdAt', 'ASCENDING']]));
commit('06.5 · Sửa tin nhắn', [update('conversations/{{pairId}}/messages/{{messageId}}', { body: str('{{message}}') }, ['updatedAt'])]);
commit('06.6 · Đánh dấu đã xem', [update('conversations/{{pairId}}', {}, ['readAt.`{{uid}}`'], [])]);

group('07 · Thông báo', 'Chỉ đọc và đánh dấu thông báo trong users/{uid}/notifications của mình.');
request('07.1 · Danh sách thông báo', 'POST', base + '/users/{{uid}}:runQuery', query('notifications', [], [['createdAt', 'DESCENDING']]), { test: `
if (pm.response.code === 200) {
  const row = pm.response.json().find((r) => r.document);
  if (row) pm.environment.set('notificationId', row.document.name.split('/').pop());
}` });
commit('07.2 · Đánh dấu đã đọc', [update('users/{{uid}}/notifications/{{notificationId}}', { isRead: bool(true) })]);

group('08 · Hỗ trợ Emulator', 'Chỉ dùng localhost; không có endpoint này trên Firebase thật.');
request('08.1 · Lấy mã xác minh email', 'GET', '{{authEmulatorOrigin}}/emulator/v1/projects/{{projectId}}/oobCodes', null, {
  noauth: true,
  pre: String.raw`if (!String(pm.environment.get('projectId')).startsWith('demo-') || !/^http:\/\/(127\.0\.0\.1|localhost):\d+$/.test(pm.environment.get('authEmulatorOrigin'))) throw new Error('Request này chỉ dành cho Emulator local với project demo-.');`,
  test: `
if (pm.response.code === 200) {
  const codes = pm.response.json().oobCodes || [];
  const code = codes.filter((c) => c.email === pm.environment.get('email') && c.requestType === 'VERIFY_EMAIL').pop();
  pm.test('Có mã xác minh cho email đang dùng', () => pm.expect(Boolean(code)).to.eql(true));
  if (code) pm.environment.set('oobCode', code.oobCode);
}`
});

group('09 · Kiểm tra quyền (mong đợi 403)', 'Dùng B, peerUid=A, postId trỏ bài của A có GPS. Token của B đã xác minh email.');
request('09.1 · Không đọc hồ sơ riêng tư người khác', 'GET', base + '/users/{{peerUid}}/private/profile', null, { status: 403 });
request('09.2 · Không đọc GPS của người khác', 'GET', base + '/posts/{{postId}}/private/location', null, { status: 403 });
request('09.3 · Không query toàn bộ posts', 'POST', base + ':runQuery', query('posts'), { status: 403 });
commit('09.4 · Không sửa caption người khác', [update('posts/{{postId}}', { caption: str('Sửa trái phép') })], { status: 403 });

group('10 · Xóa dữ liệu thử (chạy riêng)', 'Chạy từng request khi cần. Không Run toàn bộ collection vì có luồng thay tài khoản, email, và xóa.');
request('10.1 · Xóa bình luận', 'DELETE', base + '/posts/{{postId}}/comments/{{commentId}}');
request('10.2 · Xóa tin nhắn', 'DELETE', base + '/conversations/{{pairId}}/messages/{{messageId}}');
request('10.3 · Xóa thông báo', 'DELETE', base + '/users/{{uid}}/notifications/{{notificationId}}');
commit('10.4 · Xóa bài và GPS', [
  { delete: root + '/posts/{{postId}}/private/location' }, { delete: root + '/posts/{{postId}}' }
], { description: 'Chỉ chủ bài. Xóa bình luận/notificationReceipts của bài trước (cùng commit với bài đối với receipts). Không xóa đệ quy subcollection, phiếu reports hay ảnh Cloudinary. Mẫu bài tạo bởi collection không có receipts.' });

const collection = {
  info: { name: 'Lokap · Firebase Auth & Firestore', schema: 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json', description: 'Request REST theo firebase/firestore.rules của Lokap. Xem README.md cùng thư mục. Chọn environment trước, gửi từng request theo luồng; không chạy toàn bộ collection. Firebase Auth REST: https://firebase.google.com/docs/reference/rest/auth ; Firestore writes: https://firebase.google.com/docs/firestore/reference/rest/v1/Write' },
  auth: { type: 'bearer', bearer: [{ key: 'token', value: '{{idToken}}', type: 'string' }] },
  event: [event('prerequest', `
const required = ['projectId', 'apiKey', 'databaseId', 'authBaseUrl', 'tokenBaseUrl', 'firestoreBaseUrl'];
for (const key of required) {
  const value = pm.environment.get(key) || pm.collectionVariables.get(key) || pm.globals.get(key);
  if (!value) throw new Error('Thiếu cấu hình dùng chung: ' + key);
  pm.variables.set(key, value);
}
const now = Date.now();
pm.variables.set('now', new Date(now).toISOString());
pm.variables.set('discoverBefore', new Date(now - 120000).toISOString());
const delay = Number(pm.environment.get('delayDays') || 0);
if (![0, 1, 3, 7].includes(delay)) throw new Error('delayDays phải là 0, 1, 3 hoặc 7.');
pm.variables.set('publicVisibleAt', new Date(now + delay * 86400000).toISOString());
const members = [pm.environment.get('uid'), pm.environment.get('peerUid')].sort();
pm.variables.set('memberLow', members[0] || '');
pm.variables.set('memberHigh', members[1] || '');
pm.variables.set('pairId', members.join('_'));
pm.variables.set('searchName', String(pm.environment.get('displayName') || '').normalize('NFD').replace(/[\\u0300-\\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase().trim());
pm.variables.set('searchEnd', String(pm.environment.get('searchPrefix') || '') + '\\uf8ff');
`)], item: folders
};
function write(name, value) { writeFileSync(new URL(name, import.meta.url), JSON.stringify(value, null, 2) + '\n'); }
write('Lokap.postman_collection.json', collection);
for (const emulator of [true, false]) {
  const values = { ...variables, ...(emulator ? {
    projectId: 'demo-lokap', apiKey: 'fake-api-key',
    authBaseUrl: 'http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1',
    tokenBaseUrl: 'http://127.0.0.1:9099/securetoken.googleapis.com/v1',
    firestoreBaseUrl: 'http://127.0.0.1:8080/v1',
    email: 'lokap.a@example.test', password: 'Lokap-local-123!', username: 'lokap.a'
  } : {}) };
  write(emulator ? 'Emulator.postman_environment.json' : 'Firebase.postman_environment.json', {
    name: emulator ? 'Lokap · Emulator · A' : 'Lokap · Firebase · A',
    values: Object.entries(values).map(([key, value]) => ({ key, value, enabled: true, type: ['apiKey', 'password', 'newPassword', 'idToken', 'refreshToken', 'oobCode'].includes(key) ? 'secret' : 'default' })),
    _postman_variable_scope: 'environment'
  });
}
console.log(`Đã tạo ${folders.reduce((n, f) => n + f.item.length, 0)} request và 2 environment.`);
