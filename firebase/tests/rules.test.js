// Test Security Rules bằng 3 tài khoản: alice (tác giả), bob (bạn của alice), carol (người lạ).
// Chạy: cd firebase && npm install && npm test   (cần Java 21+ cho Firestore Emulator)
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { after, before, beforeEach, describe, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment
} from '@firebase/rules-unit-testing';
import {
  Timestamp,
  arrayRemove,
  collection,
  collectionGroup,
  deleteDoc,
  deleteField,
  doc,
  getDoc,
  getDocs,
  orderBy,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  where,
  writeBatch
} from 'firebase/firestore';

const DAY = 24 * 60 * 60 * 1000;
const ago = (ms) => Timestamp.fromMillis(Date.now() - ms);
const later = (ms) => Timestamp.fromMillis(Date.now() + ms);

let env;

/** Đã xác nhận email (rules yêu cầu khi ghi). */
const as = (uid, verified = true) => env.authenticatedContext(uid, { email_verified: verified }).firestore();

const profile = (username, createdAt = ago(2 * DAY)) => ({
  username,
  displayName: username.toUpperCase(),
  searchName: username,
  avatarUrl: null,
  bio: '',
  friendCount: 0,
  createdAt
});

const post = (over = {}) => ({
  authorId: 'alice',
  author: { username: 'alice', displayName: 'ALICE', avatarUrl: null },
  imageUrl: 'https://res.cloudinary.com/demo/image/upload/lokap/posts/alice/x.jpg',
  imagePublicId: 'lokap/posts/alice/x',
  caption: null,
  audience: 'friends',
  viewerIds: ['alice', 'bob'],
  recipientId: null,
  locationLevel: 'province',
  provinceCode: 'da-nang',
  provinceName: 'Đà Nẵng',
  wardCode: null,
  wardName: null,
  placeId: null,
  placeName: null,
  landmarkId: null,
  displayLat: 16.0544,
  displayLng: 108.2022,
  delayDays: 0,
  publicVisibleAt: null,
  takenAt: ago(60_000),
  createdAt: ago(60_000),
  status: 'active',
  reactions: {},
  reportCount: 0,
  ...over
});

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-lokap',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') }
  });
});

after(async () => env?.cleanup());

beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, 'users/alice'), profile('alice'));
    await setDoc(doc(db, 'users/bob'), profile('bob'));
    await setDoc(doc(db, 'users/carol'), profile('carol'));
    await setDoc(doc(db, 'users/newbie'), profile('newbie', ago(60_000)));
    await setDoc(doc(db, 'friendships/alice_bob'), {
      users: ['alice', 'bob'], requesterId: 'alice', addresseeId: 'bob', status: 'accepted', createdAt: ago(DAY)
    });
    await setDoc(doc(db, 'posts/friends'), post());
    await setDoc(doc(db, 'posts/friends/private/location'), { lat: 16.06781, lng: 108.22083, accuracyM: 12 });
    await setDoc(doc(db, 'posts/publicNow'), post({ audience: 'public', publicVisibleAt: ago(DAY) }));
    await setDoc(doc(db, 'posts/publicLater'), post({ audience: 'public', delayDays: 3, publicVisibleAt: later(2 * DAY) }));
    await setDoc(doc(db, 'posts/hidden'), post({ status: 'hidden', reportCount: 3 }));
    await setDoc(doc(db, 'posts/direct'), post({ audience: 'direct', recipientId: 'bob' }));
  });
});

const friendFeed = (db, uid) =>
  getDocs(query(collection(db, 'posts'), where('viewerIds', 'array-contains', uid), where('status', '==', 'active'), orderBy('createdAt', 'desc')));

const discover = (db, before = Date.now() - 2 * 60_000) =>
  getDocs(query(
    collection(db, 'posts'),
    where('audience', '==', 'public'),
    where('status', '==', 'active'),
    where('publicVisibleAt', '<=', Timestamp.fromMillis(before)),
    orderBy('publicVisibleAt', 'desc')
  ));

describe('Đọc bài theo luật hiển thị', () => {
  test('bạn thấy bài bạn bè, bài gửi riêng và cả bài public đang hẹn giờ', async () => {
    const snap = await assertSucceeds(friendFeed(as('bob'), 'bob'));
    const ids = snap.docs.map((d) => d.id).sort();
    assert.deepEqual(ids, ['direct', 'friends', 'publicLater', 'publicNow']);
  });

  test('người lạ không truy vấn được feed của người khác', async () => {
    await assertFails(friendFeed(as('carol'), 'bob'));
  });

  test('Khám phá chỉ có bài public đã tới giờ', async () => {
    const snap = await assertSucceeds(discover(as('carol')));
    assert.deepEqual(snap.docs.map((d) => d.id), ['publicNow']);
  });

  test('truy vấn Khám phá thiếu mốc giờ bị từ chối', async () => {
    const db = as('carol');
    await assertFails(getDocs(query(collection(db, 'posts'), where('audience', '==', 'public'), where('status', '==', 'active'))));
  });

  test('người lạ không mở được bài bạn bè / bài hẹn giờ, mở được bài public đã hiện', async () => {
    const db = as('carol');
    await assertFails(getDoc(doc(db, 'posts/friends')));
    await assertFails(getDoc(doc(db, 'posts/publicLater')));
    await assertFails(getDoc(doc(db, 'posts/direct')));
    await assertSucceeds(getDoc(doc(db, 'posts/publicNow')));
  });

  test('bài bị ẩn: chỉ tác giả còn thấy', async () => {
    await assertFails(getDoc(doc(as('bob'), 'posts/hidden')));
    await assertSucceeds(getDoc(doc(as('alice'), 'posts/hidden')));
  });

  test('toạ độ gốc chỉ tác giả đọc được', async () => {
    await assertSucceeds(getDoc(doc(as('alice'), 'posts/friends/private/location')));
    await assertFails(getDoc(doc(as('bob'), 'posts/friends/private/location')));
  });
});

describe('Tạo bài', () => {
  const create = (db, id, data, withLocation = true) => {
    const batch = writeBatch(db);
    batch.set(doc(db, `posts/${id}`), { ...data, createdAt: serverTimestamp() });
    if (withLocation) batch.set(doc(db, `posts/${id}/private/location`), { lat: 16.07, lng: 108.22, accuracyM: 20 });
    return batch.commit();
  };

  test('bài bạn bè hợp lệ', async () => {
    await assertSucceeds(create(as('alice'), 'n1', post({ createdAt: null })));
  });

  test('email chưa xác nhận thì không đăng được', async () => {
    await assertFails(create(as('alice', false), 'n2', post({ createdAt: null })));
  });

  test('không đăng bài thay người khác', async () => {
    await assertFails(create(as('carol'), 'n3', post({ createdAt: null })));
  });

  test('hẹn giờ 7 ngày nhưng giờ hiện quá sớm bị từ chối', async () => {
    const data = post({ createdAt: null, audience: 'public', delayDays: 7, publicVisibleAt: later(DAY) });
    await assertFails(create(as('alice'), 'n4', data));
  });

  test('hẹn giờ 1 ngày đúng giờ server', async () => {
    const data = post({ createdAt: null, audience: 'public', delayDays: 1, publicVisibleAt: later(DAY) });
    await assertSucceeds(create(as('alice'), 'n5', data));
  });

  test('mức "Việt Nam" không được kèm toạ độ', async () => {
    const data = post({ createdAt: null, locationLevel: 'country', provinceCode: null, provinceName: null });
    await assertFails(create(as('alice'), 'n6', data, false));
  });

  test('gửi riêng chỉ cho bạn bè', async () => {
    await assertSucceeds(create(as('alice'), 'n7', post({ createdAt: null, audience: 'direct', recipientId: 'bob', viewerIds: ['alice', 'bob'] })));
    await assertFails(create(as('alice'), 'n8', post({ createdAt: null, audience: 'direct', recipientId: 'carol', viewerIds: ['alice', 'carol'] })));
  });
});

describe('Cảm xúc, rời danh sách xem, báo cáo', () => {
  test('người xem chỉ sửa được cảm xúc của chính mình', async () => {
    const db = as('bob');
    await assertSucceeds(updateDoc(doc(db, 'posts/friends'), { 'reactions.bob': '❤️' }));
    await assertSucceeds(updateDoc(doc(db, 'posts/friends'), { 'reactions.bob': deleteField() }));
    await assertFails(updateDoc(doc(db, 'posts/friends'), { 'reactions.alice': '❤️' }));
    await assertFails(updateDoc(doc(as('carol'), 'posts/friends'), { 'reactions.carol': '❤️' }));
  });

  test('người xem chỉ tự rút tên mình khỏi viewerIds', async () => {
    await assertFails(updateDoc(doc(as('bob'), 'posts/friends'), { viewerIds: ['bob'] }));
    await assertSucceeds(updateDoc(doc(as('bob'), 'posts/friends'), { viewerIds: arrayRemove('bob') }));
  });

  const report = (db, uid, postId, count, status) => {
    const batch = writeBatch(db);
    batch.set(doc(db, `posts/${postId}/reports/${uid}`), { reason: 'spam', createdAt: serverTimestamp() });
    batch.update(doc(db, `posts/${postId}`), status ? { reportCount: count, status } : { reportCount: count });
    return batch.commit();
  };

  test('báo cáo tăng đúng 1 và kèm phiếu', async () => {
    await assertFails(report(as('carol'), 'carol', 'publicNow', 2));
    await assertSucceeds(report(as('carol'), 'carol', 'publicNow', 1));
    // Báo cáo lần 2 cùng người bị chặn
    await assertFails(report(as('carol'), 'carol', 'publicNow', 2));
  });

  test('tài khoản mới dưới 24 giờ không báo cáo được', async () => {
    await assertFails(report(as('newbie'), 'newbie', 'publicNow', 1));
  });

  test('phiếu thứ 3 phải ẩn bài', async () => {
    await env.withSecurityRulesDisabled((ctx) => updateDoc(doc(ctx.firestore(), 'posts/publicNow'), { reportCount: 2 }));
    await assertFails(report(as('carol'), 'carol', 'publicNow', 3));
    await assertSucceeds(report(as('carol'), 'carol', 'publicNow', 3, 'hidden'));
  });
});

describe('Bạn bè và chặn', () => {
  const request = (db, from, to) => setDoc(doc(db, `friendships/${[from, to].sort().join('_')}`), {
    users: [from, to].sort(), requesterId: from, addresseeId: to, status: 'pending', createdAt: serverTimestamp()
  });

  test('mời kết bạn rồi người được mời chấp nhận', async () => {
    await assertSucceeds(request(as('carol'), 'carol', 'bob'));
    await assertFails(updateDoc(doc(as('carol'), 'friendships/bob_carol'), { status: 'accepted', acceptedAt: serverTimestamp() }));
    await assertSucceeds(updateDoc(doc(as('bob'), 'friendships/bob_carol'), { status: 'accepted', acceptedAt: serverTimestamp() }));
  });

  test('bị chặn thì không mời được', async () => {
    await assertSucceeds(setDoc(doc(as('bob'), 'blocks/bob_carol'), { blockerId: 'bob', blockedId: 'carol', createdAt: serverTimestamp() }));
    await assertFails(request(as('carol'), 'carol', 'bob'));
  });

  test('chỉ người chặn đọc được danh sách chặn của mình', async () => {
    await assertSucceeds(setDoc(doc(as('bob'), 'blocks/bob_carol'), { blockerId: 'bob', blockedId: 'carol', createdAt: serverTimestamp() }));
    await assertFails(getDoc(doc(as('carol'), 'blocks/bob_carol')));
    await assertSucceeds(getDocs(query(collection(as('bob'), 'blocks'), where('blockerId', '==', 'bob'))));
  });

  test('người ngoài không xoá được quan hệ của người khác', async () => {
    await assertFails(deleteDoc(doc(as('carol'), 'friendships/alice_bob')));
    await assertSucceeds(deleteDoc(doc(as('bob'), 'friendships/alice_bob')));
  });
});

describe('Hồ sơ và username', () => {
  const createProfile = (db, uid, username) => {
    const batch = writeBatch(db);
    batch.set(doc(db, `usernames/${username}`), { uid });
    batch.set(doc(db, `users/${uid}`), { ...profile(username), createdAt: serverTimestamp() });
    return batch.commit();
  };

  test('tạo hồ sơ giữ username', async () => {
    await assertSucceeds(createProfile(as('dave'), 'dave', 'dave.nguyen'));
  });

  test('không lấy được username của người khác', async () => {
    await env.withSecurityRulesDisabled((ctx) => setDoc(doc(ctx.firestore(), 'usernames/taken'), { uid: 'carol' }));
    await assertFails(createProfile(as('dave'), 'dave', 'taken'));
  });

  test('thông tin riêng chỉ chủ đọc được, dưới 13 tuổi bị từ chối', async () => {
    const year = new Date().getFullYear();
    await assertSucceeds(setDoc(doc(as('alice'), 'users/alice/private/profile'), { birthYear: year - 20 }));
    await assertFails(setDoc(doc(as('alice'), 'users/alice/private/profile'), { birthYear: year - 5 }));
    await assertFails(getDoc(doc(as('bob'), 'users/alice/private/profile')));
  });
});


describe('Đúng các truy vấn app đang dùng', () => {
  test('đọc cặp bạn chưa tồn tại (transaction mời kết bạn), người ngoài thì không', async () => {
    await assertSucceeds(getDoc(doc(as('carol'), 'friendships/bob_carol')));
    await assertFails(getDoc(doc(as('alice'), 'friendships/bob_carol')));
  });

  test('danh sách bạn và danh sách chặn của mình', async () => {
    await assertSucceeds(getDocs(query(collection(as('bob'), 'friendships'), where('users', 'array-contains', 'bob'))));
    await assertFails(getDocs(query(collection(as('carol'), 'friendships'), where('users', 'array-contains', 'bob'))));
  });

  test('trang hồ sơ: bạn xem bài có tên mình, người lạ chỉ xem bài public đã hiện', async () => {
    const asBob = await assertSucceeds(getDocs(query(
      collection(as('bob'), 'posts'),
      where('authorId', '==', 'alice'),
      where('viewerIds', 'array-contains', 'bob'),
      where('status', '==', 'active'),
      orderBy('createdAt', 'desc')
    )));
    assert.equal(asBob.size, 4);
    const asCarol = await assertSucceeds(getDocs(query(
      collection(as('carol'), 'posts'),
      where('authorId', '==', 'alice'),
      where('audience', '==', 'public'),
      where('status', '==', 'active'),
      where('publicVisibleAt', '<=', Timestamp.fromMillis(Date.now() - 120_000)),
      orderBy('publicVisibleAt', 'desc')
    )));
    assert.deepEqual(asCarol.docs.map((d) => d.id), ['publicNow']);
    await assertFails(getDocs(query(collection(as('carol'), 'posts'), where('authorId', '==', 'alice'))));
  });

  test('tác giả đọc hết bài của mình (kể cả bài bị ẩn) để cập nhật viewerIds', async () => {
    const mine = await assertSucceeds(getDocs(query(collection(as('alice'), 'posts'), where('authorId', '==', 'alice'))));
    assert.equal(mine.size, 5);
    await assertSucceeds(updateDoc(doc(as('alice'), 'posts/friends'), { viewerIds: ['alice', 'bob', 'carol'] }));
    await assertFails(updateDoc(doc(as('alice'), 'posts/friends'), { viewerIds: ['bob'] }));
  });

  test('hỏi bài không tồn tại trả về "không có" thay vì lỗi quyền', async () => {
    const snap = await assertSucceeds(getDoc(doc(as('carol'), 'posts/khong-co')));
    assert.equal(snap.exists(), false);
  });

  test('tìm người theo tiền tố username', async () => {
    const found = await assertSucceeds(getDocs(query(
      collection(as('carol'), 'users'), orderBy('username'), where('username', '>=', 'al'), where('username', '<=', 'al\uf8ff')
    )));
    assert.deepEqual(found.docs.map((d) => d.id), ['alice']);
  });
});

const chat = () => ({members:['alice','bob'],createdAt:serverTimestamp(),readAt:{}});
const msg = () => ({senderId:'alice',body:'hello',createdAt:serverTimestamp(),updatedAt:serverTimestamp()});
const comment = () => ({authorId:'bob',content:'hello',parentId:null,createdAt:serverTimestamp(),updatedAt:serverTimestamp(),votes:{}});
const notice = (type,actorId,sourceId,extra={}) => ({type,actorId,sourceId,postId:null,conversationId:null,createdAt:serverTimestamp(),isRead:false,...extra});
async function sendMessage(a) {const b=writeBatch(a);b.set(doc(a,'conversations/alice_bob/messages/m1'),msg());b.set(doc(a,'users/bob/notifications/message_m1'),notice('NEW_MESSAGE','alice','m1',{conversationId:'alice_bob'}));await b.commit();}
describe('Live social content',()=>{
 test('chat CRUD, outsider denial and read receipt ownership',async()=>{
  const a=as('alice'),b=as('bob');await assertSucceeds(setDoc(doc(a,'conversations/alice_bob'),chat()));await assertFails(getDoc(doc(as('carol'),'conversations/alice_bob')));await assertSucceeds(getDocs(query(collection(a,'conversations'),where('members','array-contains','alice'))));
  await assertFails(setDoc(doc(as('alice',false),'conversations/alice_bob/messages/no'),msg()));await assertSucceeds(sendMessage(a));await assertSucceeds(getDocs(query(collection(b,'conversations/alice_bob/messages'),orderBy('createdAt'))));
  await assertFails(updateDoc(doc(b,'conversations/alice_bob/messages/m1'),{body:'forged',updatedAt:serverTimestamp()}));await assertSucceeds(updateDoc(doc(a,'conversations/alice_bob/messages/m1'),{body:'edited',updatedAt:serverTimestamp()}));await assertFails(updateDoc(doc(b,'conversations/alice_bob'),{'readAt.alice':serverTimestamp()}));await assertSucceeds(updateDoc(doc(b,'conversations/alice_bob'),{'readAt.bob':serverTimestamp()}));await assertFails(deleteDoc(doc(b,'conversations/alice_bob/messages/m1')));await assertSucceeds(deleteDoc(doc(a,'conversations/alice_bob/messages/m1')));
 });
 test('tin nhắn trả lời ảnh: bản chụp phải khớp bài gốc người gửi xem được, ảnh của 1 trong 2 người',async()=>{
  const a=as('alice'),b=as('bob');await setDoc(doc(a,'conversations/alice_bob'),chat());
  const url=post().imageUrl;
  const reply=(over={})=>({...msg(),senderId:'bob',replyTo:{postId:'friends',authorId:'alice',imageUrl:url,caption:null,takenAt:ago(60_000),...over}});
  await assertSucceeds(setDoc(doc(b,'conversations/alice_bob/messages/r1'),reply()));
  await assertSucceeds(setDoc(doc(b,'conversations/alice_bob/messages/r2'),reply({postId:'direct',caption:'chào'})));
  await assertFails(setDoc(doc(b,'conversations/alice_bob/messages/r3'),reply({imageUrl:'https://evil.example/x.jpg'})));
  await assertFails(setDoc(doc(b,'conversations/alice_bob/messages/r4'),reply({authorId:'carol'})));
  await assertFails(setDoc(doc(b,'conversations/alice_bob/messages/r5'),reply({postId:'hidden'})));
  await assertFails(setDoc(doc(b,'conversations/alice_bob/messages/r6'),reply({postId:'khong-co'})));
  await assertFails(setDoc(doc(b,'conversations/alice_bob/messages/r7'),reply({extra:true})));
  await assertFails(updateDoc(doc(b,'conversations/alice_bob/messages/r1'),{'replyTo.postId':'direct',updatedAt:serverTimestamp()}));
 });
 test('blocked and unfriended accounts cannot send or edit',async()=>{
  const a=as('alice');await setDoc(doc(a,'conversations/alice_bob'),chat());await sendMessage(a);await setDoc(doc(as('bob'),'blocks/bob_alice'),{blockerId:'bob',blockedId:'alice',createdAt:serverTimestamp()});await assertFails(setDoc(doc(a,'conversations/alice_bob/messages/m2'),msg()));await assertFails(updateDoc(doc(a,'conversations/alice_bob/messages/m1'),{body:'bad',updatedAt:serverTimestamp()}));await deleteDoc(doc(as('bob'),'blocks/bob_alice'));await deleteDoc(doc(a,'friendships/alice_bob'));await assertFails(setDoc(doc(a,'conversations/alice_bob/messages/m2'),msg()));
 });
 test('comment CRUD, replies and votes check author and parent access',async()=>{
  const b=as('bob'),batch=writeBatch(b);batch.set(doc(b,'posts/friends/comments/c1'),comment());batch.set(doc(b,'users/alice/notifications/comment_c1'),notice('COMMENT','bob','c1',{postId:'friends'}));await assertSucceeds(batch.commit());await assertSucceeds(getDocs(query(collection(b,'posts/friends/comments'),orderBy('createdAt'))));await assertFails(getDocs(collection(as('carol'),'posts/friends/comments')));await assertFails(setDoc(doc(b,'posts/friends/comments/fake'),{...comment(),authorId:'alice'}));await assertFails(setDoc(doc(b,'posts/friends/comments/bad'),{...comment(),parentId:'missing'}));await assertSucceeds(setDoc(doc(b,'posts/friends/comments/reply'),{...comment(),parentId:'c1'}));await assertSucceeds(updateDoc(doc(b,'posts/friends/comments/c1'),{content:'edited',updatedAt:serverTimestamp()}));await assertFails(updateDoc(doc(as('alice'),'posts/friends/comments/c1'),{content:'forged',updatedAt:serverTimestamp()}));await assertSucceeds(updateDoc(doc(as('alice'),'posts/friends/comments/c1'),{'votes.alice':1}));await assertFails(updateDoc(doc(as('alice'),'posts/friends/comments/c1'),{'votes.bob':1}));await assertFails(updateDoc(doc(as('alice'),'posts/friends/comments/c1'),{'votes.alice':100}));await assertSucceeds(deleteDoc(doc(b,'posts/friends/comments/c1')));await assertSucceeds(deleteDoc(doc(b,'posts/friends/comments/reply')));
 });
 test('notifications reject forged sources, outsider reads and source replay',async()=>{
  const a=as('alice'),b=as('bob');await setDoc(doc(a,'conversations/alice_bob'),chat());await assertFails(setDoc(doc(a,'users/bob/notifications/message_fake'),notice('NEW_MESSAGE','alice','fake',{conversationId:'alice_bob'})));await sendMessage(a);const ref=doc(b,'users/bob/notifications/message_m1');await assertFails(getDoc(doc(a,'users/bob/notifications/message_m1')));await assertFails(updateDoc(ref,{actorId:'carol'}));await assertSucceeds(updateDoc(ref,{isRead:true}));await assertSucceeds(updateDoc(ref,{isRead:false}));await assertSucceeds(deleteDoc(ref));await assertFails(setDoc(doc(a,'users/bob/notifications/message_m1'),notice('NEW_MESSAGE','alice','m1',{conversationId:'alice_bob'})));
 });
 test('real friendship transitions create notices atomically',async()=>{
  const a=as('alice'),c=as('carol'),b=writeBatch(a);b.set(doc(a,'friendships/alice_carol'),{users:['alice','carol'],requesterId:'alice',addresseeId:'carol',status:'pending',createdAt:serverTimestamp()});b.set(doc(a,'users/carol/notifications/request1'),notice('FRIEND_REQUEST','alice','alice_carol'));await assertSucceeds(b.commit());const accepted=writeBatch(c);accepted.update(doc(c,'friendships/alice_carol'),{status:'accepted',acceptedAt:serverTimestamp()});accepted.set(doc(c,'users/alice/notifications/accept1'),notice('FRIEND_ACCEPTED','carol','alice_carol'));await assertSucceeds(accepted.commit());
 });
 test('reaction notice requires a real change and respects blocks',async()=>{
  const b=as('bob'),batch=writeBatch(b);batch.update(doc(b,'posts/friends'),{'reactions.bob':'❤️'});batch.set(doc(b,'users/alice/notifications/reaction1'),notice('REACTION','bob','bob',{postId:'friends'}));await assertSucceeds(batch.commit());await assertFails(setDoc(doc(b,'users/alice/notifications/fake'),notice('REACTION','bob','bob',{postId:'friends'})));await setDoc(doc(as('alice'),'blocks/alice_bob'),{blockerId:'alice',blockedId:'bob',createdAt:serverTimestamp()});const blocked=writeBatch(b);blocked.update(doc(b,'posts/friends'),{'reactions.bob':'🔥'});blocked.set(doc(b,'users/alice/notifications/reaction2'),notice('REACTION','bob','bob',{postId:'friends'}));await assertFails(blocked.commit());
 });
});

describe('Đăng lại', () => {
  const repost = (uid, over = {}) => ({ uid, createdAt: serverTimestamp(), ...over });

  test('ai cũng đăng lại được ảnh public đã hiện của người khác, mỗi người 1 lần', async () => {
    await assertSucceeds(setDoc(doc(as('bob'), 'posts/publicNow/reposts/bob'), repost('bob')));
    await assertSucceeds(setDoc(doc(as('carol'), 'posts/publicNow/reposts/carol'), repost('carol')));
    // Đã có thì không ghi lại (createdAt mới = update, rules không cho)
    await assertFails(setDoc(doc(as('bob'), 'posts/publicNow/reposts/bob'), repost('bob')));
  });

  test('không đăng lại ảnh bạn bè, ảnh đang hẹn giờ, bài bị ẩn, bài của mình', async () => {
    const b = as('bob');
    await assertFails(setDoc(doc(b, 'posts/friends/reposts/bob'), repost('bob')));
    await assertFails(setDoc(doc(b, 'posts/direct/reposts/bob'), repost('bob')));
    await assertFails(setDoc(doc(b, 'posts/publicLater/reposts/bob'), repost('bob')));
    await assertFails(setDoc(doc(b, 'posts/hidden/reposts/bob'), repost('bob')));
    await assertFails(setDoc(doc(b, 'posts/khong-co/reposts/bob'), repost('bob')));
    await assertFails(setDoc(doc(as('alice'), 'posts/publicNow/reposts/alice'), repost('alice')));
  });

  test('không đăng lại thay người khác, sai dữ liệu, chưa xác nhận email, bị chặn', async () => {
    const b = as('bob');
    await assertFails(setDoc(doc(b, 'posts/publicNow/reposts/carol'), repost('carol')));
    await assertFails(setDoc(doc(b, 'posts/publicNow/reposts/bob'), repost('carol')));
    await assertFails(setDoc(doc(b, 'posts/publicNow/reposts/bob'), repost('bob', { extra: 1 })));
    await assertFails(setDoc(doc(b, 'posts/publicNow/reposts/bob'), repost('bob', { createdAt: ago(DAY) })));
    await assertFails(setDoc(doc(as('bob', false), 'posts/publicNow/reposts/bob'), repost('bob')));
    await setDoc(doc(as('alice'), 'blocks/alice_carol'), { blockerId: 'alice', blockedId: 'carol', createdAt: serverTimestamp() });
    await assertFails(setDoc(doc(as('carol'), 'posts/publicNow/reposts/carol'), repost('carol')));
  });

  test('đếm lượt và truy vấn nhóm theo người đăng lại; chưa đăng nhập thì không', async () => {
    await setDoc(doc(as('bob'), 'posts/publicNow/reposts/bob'), repost('bob'));
    const counted = await assertSucceeds(getDocs(collection(as('carol'), 'posts/publicNow/reposts')));
    assert.equal(counted.size, 1);
    const feed = await assertSucceeds(getDocs(query(
      collectionGroup(as('alice'), 'reposts'), where('uid', 'in', ['bob', 'carol']), orderBy('createdAt', 'desc')
    )));
    assert.deepEqual(feed.docs.map((d) => d.ref.parent.parent.id), ['publicNow']);
    await assertSucceeds(getDocs(query(collectionGroup(as('carol'), 'reposts'), where('uid', '==', 'bob'), orderBy('createdAt', 'desc'))));
    await assertFails(getDocs(collection(env.unauthenticatedContext().firestore(), 'posts/publicNow/reposts')));
  });

  test('bỏ đăng lại: chính mình hoặc tác giả bài (khi xoá bài); người khác thì không', async () => {
    await setDoc(doc(as('bob'), 'posts/publicNow/reposts/bob'), repost('bob'));
    await setDoc(doc(as('carol'), 'posts/publicNow/reposts/carol'), repost('carol'));
    await assertFails(deleteDoc(doc(as('carol'), 'posts/publicNow/reposts/bob')));
    await assertSucceeds(deleteDoc(doc(as('bob'), 'posts/publicNow/reposts/bob')));
    await assertSucceeds(deleteDoc(doc(as('alice'), 'posts/publicNow/reposts/carol')));
  });
});

test('new post notice uses one-time receipt; deletion cannot cause replay, blocks are retained',async()=>{
 const a=as('alice'),b=as('bob');const batch=writeBatch(a);batch.set(doc(a,'posts/friends/notificationReceipts/bob'),{createdAt:serverTimestamp()});batch.set(doc(a,'users/bob/notifications/post_friends'),notice('NEW_POST','alice','friends',{postId:'friends'}));await assertSucceeds(batch.commit());await assertFails(deleteDoc(doc(a,'posts/friends/notificationReceipts/bob')));await deleteDoc(doc(b,'users/bob/notifications/post_friends'));await assertFails(setDoc(doc(a,'users/bob/notifications/post_friends'),notice('NEW_POST','alice','friends',{postId:'friends'})));
 await setDoc(doc(b,'blocks/bob_alice'),{blockerId:'bob',blockedId:'alice',createdAt:serverTimestamp()});const blocked=writeBatch(a);blocked.set(doc(a,'posts/direct/notificationReceipts/bob'),{createdAt:serverTimestamp()});blocked.set(doc(a,'users/bob/notifications/post_direct'),notice('NEW_POST','alice','direct',{postId:'direct'}));await assertFails(blocked.commit());
});

// ------------------------------------------------------------------ địa danh

const landmark = (over = {}) => ({
  name: 'Cầu Rồng',
  searchName: 'cau rong',
  address: 'Nguyễn Văn Linh, Hải Châu, Đà Nẵng',
  description: null,
  provinceCode: 'da-nang',
  provinceName: 'Đà Nẵng',
  wardCode: null,
  wardName: null,
  lat: 16.0611,
  lng: 108.2275,
  vietmapRef: 'vm:cau-rong',
  createdBy: 'carol',
  createdAt: ago(DAY),
  status: 'active',
  reportCount: 0,
  ...over
});

/** Vị trí của bài khi gắn vào địa danh vm_caurong: điểm công khai của địa danh. */
const atLandmark = (id = 'vm_caurong') => ({
  landmarkId: id,
  locationLevel: 'place',
  provinceCode: 'da-nang',
  provinceName: 'Đà Nẵng',
  wardCode: null,
  wardName: null,
  placeId: null,
  placeName: 'Cầu Rồng',
  displayLat: 16.0611,
  displayLng: 108.2275
});

describe('Địa danh', () => {
  beforeEach(async () => {
    await env.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore();
      await setDoc(doc(db, 'landmarks/vm_caurong'), landmark());
      await setDoc(doc(db, 'landmarks/vm_hidden'), landmark({ name: 'Rác', status: 'hidden', reportCount: 3 }));
      await setDoc(doc(db, 'posts/lmPublic'), post({
        authorId: 'carol', author: { username: 'carol', displayName: 'CAROL', avatarUrl: null }, viewerIds: ['carol'],
        audience: 'public', publicVisibleAt: ago(DAY), reactions: { alice: '❤️', bob: '🔥' }, ...atLandmark()
      }));
      await setDoc(doc(db, 'posts/lmLater'), post({
        authorId: 'carol', author: { username: 'carol', displayName: 'CAROL', avatarUrl: null }, viewerIds: ['carol'],
        audience: 'public', delayDays: 3, publicVisibleAt: later(2 * DAY), ...atLandmark()
      }));
    });
  });

  const createLandmark = (db, id, data) => setDoc(doc(db, `landmarks/${id}`), { ...data, createdAt: serverTimestamp() });

  test('người đã xác nhận email thêm được địa danh của chính mình', async () => {
    await assertSucceeds(createLandmark(as('alice'), 'vm_moi', landmark({ createdBy: 'alice', name: 'Chợ Hàn' })));
  });

  test('thêm địa danh sai dữ liệu bị từ chối', async () => {
    await assertFails(createLandmark(as('alice', false), 'vm_a1', landmark({ createdBy: 'alice' })));
    await assertFails(createLandmark(as('alice'), 'vm_a2', landmark({ createdBy: 'bob' })));
    await assertFails(createLandmark(as('alice'), 'vm a3', landmark({ createdBy: 'alice' })));
    await assertFails(createLandmark(as('alice'), 'vm_a4', landmark({ createdBy: 'alice', lat: 35.68, lng: 139.76 })));
    await assertFails(createLandmark(as('alice'), 'vm_a5', landmark({ createdBy: 'alice', name: 'A' })));
    await assertFails(createLandmark(as('alice'), 'vm_a6', landmark({ createdBy: 'alice', status: 'hidden' })));
    await assertFails(createLandmark(as('alice'), 'vm_a7', landmark({ createdBy: 'alice', reportCount: 0, coverUrl: 'x' })));
  });

  test('không ghi đè, sửa hay xoá địa danh đã có', async () => {
    await assertFails(createLandmark(as('alice'), 'vm_caurong', landmark({ createdBy: 'alice' })));
    await assertFails(updateDoc(doc(as('carol'), 'landmarks/vm_caurong'), { name: 'Cầu Rồng mới' }));
    await assertFails(deleteDoc(doc(as('carol'), 'landmarks/vm_caurong')));
  });

  test('ai đăng nhập cũng xem được địa danh đang hoạt động; bị ẩn thì chỉ người tạo thấy', async () => {
    const list = await assertSucceeds(getDocs(query(collection(as('bob'), 'landmarks'), where('status', '==', 'active'))));
    assert.deepEqual(list.docs.map((d) => d.id), ['vm_caurong']);
    await assertFails(getDocs(collection(as('bob'), 'landmarks')));
    await assertFails(getDoc(doc(as('bob'), 'landmarks/vm_hidden')));
    await assertSucceeds(getDoc(doc(as('carol'), 'landmarks/vm_hidden')));
    const missing = await assertSucceeds(getDoc(doc(as('bob'), 'landmarks/vm_khong_co')));
    assert.equal(missing.exists(), false);
    await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(), 'landmarks/vm_caurong')));
  });

  const reportLandmark = (db, uid, count, status) => {
    const batch = writeBatch(db);
    batch.set(doc(db, `landmarks/vm_caurong/reports/${uid}`), { reason: 'Spam', createdAt: serverTimestamp() });
    batch.update(doc(db, 'landmarks/vm_caurong'), status ? { reportCount: count, status } : { reportCount: count });
    return batch.commit();
  };

  test('báo cáo địa danh: đúng +1 kèm phiếu, người tạo không tự báo cáo, phiếu thứ 3 phải ẩn', async () => {
    await assertFails(reportLandmark(as('carol'), 'carol', 1));
    await assertFails(reportLandmark(as('alice'), 'alice', 2));
    await assertSucceeds(reportLandmark(as('alice'), 'alice', 1));
    await assertFails(reportLandmark(as('alice'), 'alice', 2));
    await assertFails(reportLandmark(as('newbie'), 'newbie', 2));
    await env.withSecurityRulesDisabled((ctx) => updateDoc(doc(ctx.firestore(), 'landmarks/vm_caurong'), { reportCount: 2 }));
    await assertFails(reportLandmark(as('bob'), 'bob', 3));
    await assertSucceeds(reportLandmark(as('bob'), 'bob', 3, 'hidden'));
  });

  const createPost = (db, id, data) => setDoc(doc(db, `posts/${id}`), { ...data, createdAt: serverTimestamp() });
  const publicNow = (over = {}) => post({ createdAt: null, audience: 'public', publicVisibleAt: Timestamp.now(), ...over });

  test('đăng ảnh lên địa danh: bài public, đúng điểm và tỉnh của địa danh', async () => {
    await assertSucceeds(createPost(as('alice'), 'lm1', publicNow(atLandmark())));
    await assertFails(createPost(as('alice'), 'lm2', post({ createdAt: null, ...atLandmark() })));
    await assertFails(createPost(as('alice'), 'lm3', publicNow({ ...atLandmark(), displayLat: 16.0612 })));
    await assertFails(createPost(as('alice'), 'lm4', publicNow({ ...atLandmark(), provinceCode: 'hue' })));
    await assertFails(createPost(as('alice'), 'lm5', publicNow({ ...atLandmark(), locationLevel: 'province' })));
    await assertFails(createPost(as('alice'), 'lm6', publicNow(atLandmark('vm_khong_co'))));
    await assertFails(createPost(as('alice'), 'lm7', publicNow({ ...atLandmark('vm_hidden') })));
  });

  test('tác giả gắn bài public đã đăng vào địa danh và gỡ ra; người khác thì không', async () => {
    await assertFails(updateDoc(doc(as('bob'), 'posts/publicNow'), atLandmark()));
    await assertFails(updateDoc(doc(as('alice'), 'posts/friends'), atLandmark()));
    await assertFails(updateDoc(doc(as('alice'), 'posts/publicNow'), { ...atLandmark(), displayLat: 10.0 }));
    await assertFails(updateDoc(doc(as('alice'), 'posts/publicNow'), { ...atLandmark(), caption: 'đổi cùng lúc' }));
    await assertSucceeds(updateDoc(doc(as('alice'), 'posts/publicNow'), atLandmark()));
    await assertFails(updateDoc(doc(as('alice'), 'posts/publicNow'), { landmarkId: null, displayLat: 1 }));
    await assertSucceeds(updateDoc(doc(as('alice'), 'posts/publicNow'), { landmarkId: null }));
  });

  test('lưới ảnh địa danh: ai cũng truy vấn được bài public đã tới giờ, kèm cảm xúc để chọn ảnh bìa', async () => {
    const snap = await assertSucceeds(getDocs(query(
      collection(as('bob'), 'posts'),
      where('landmarkId', '==', 'vm_caurong'),
      where('audience', '==', 'public'),
      where('status', '==', 'active'),
      where('publicVisibleAt', '<=', Timestamp.fromMillis(Date.now() - 120_000)),
      orderBy('publicVisibleAt', 'desc')
    )));
    assert.deepEqual(snap.docs.map((d) => d.id), ['lmPublic']);
    assert.equal(Object.keys(snap.docs[0].data().reactions).length, 2);
    await assertFails(getDocs(query(collection(as('bob'), 'posts'), where('landmarkId', '==', 'vm_caurong'))));
  });
});
