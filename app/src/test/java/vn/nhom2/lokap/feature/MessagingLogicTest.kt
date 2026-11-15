package vn.nhom2.lokap.feature
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import vn.nhom2.lokap.data.fake.*
import vn.nhom2.lokap.domain.model.*
import vn.nhom2.lokap.domain.repository.*
import vn.nhom2.lokap.feature.chat.*
import vn.nhom2.lokap.feature.notifications.*
import java.time.Instant
@OptIn(ExperimentalCoroutinesApi::class)
class MessagingLogicTest {
 private val dispatcher = UnconfinedTestDispatcher()
 private val content = ContentTestRepository()
 private val conv = SampleData.conversations.first()
 @Before fun setup() { Dispatchers.setMain(dispatcher); SampleSocialData.resetAccountForDemo(); content.chat.value=Result.success(ChatData(conv, emptyList())) }
 @After fun cleanup() { Dispatchers.resetMain() }
 private fun vm() = ChatViewModel(SavedStateHandle(mapOf("conversationId" to conv.id)),content,FakeAuthRepository(),FakeSocialRepository())
 @Test fun `send failure retains draft and never invents a response`() = runTest(dispatcher) {
  content.failure=AppException("offline");val vm=vm();vm.onAction(ChatAction.InputChanged("hello"));vm.onAction(ChatAction.SendMessage)
  assertEquals(1,content.sendCount);assertEquals("hello",vm.uiState.value.inputText);assertEquals("offline",vm.uiState.value.errorMessage)
  advanceTimeBy(10_000);assertTrue(vm.uiState.value.timelineItems.isEmpty());assertFalse(vm.uiState.value.isFriendTyping)
 }
 @Test fun `successful send clears draft but content only appears from listener`() = runTest(dispatcher) {
  val vm=vm();vm.onAction(ChatAction.InputChanged("hello"));vm.onAction(ChatAction.SendMessage);assertEquals("",vm.uiState.value.inputText);assertTrue(vm.uiState.value.timelineItems.isEmpty())
 }
 @Test fun `unfriend disables sending and keeps input`() {
  SampleSocialData.unfriend(conv.friend.id);val vm=vm();vm.onAction(ChatAction.InputChanged("hello"));vm.onAction(ChatAction.SendMessage);assertEquals(0,content.sendCount);assertFalse(vm.uiState.value.canSend)
 }
 @Test fun `failed edit leaves edit dialog open`() {
  val vm=vm();content.failure=AppException("denied");val msg=Message("m",conv.id,SampleData.currentUser.id,"old",null,null,Instant.now(),PostSendState.SENT,false)
  vm.onAction(ChatAction.EditMessage(msg));vm.onAction(ChatAction.EditTextChanged("new"));vm.onAction(ChatAction.SaveEdit);assertEquals(msg,vm.uiState.value.editingMessage);assertEquals("denied",vm.uiState.value.errorMessage)
 }
 @Test fun `notification failure does not navigate or mark read`() = runTest(dispatcher) {
  val n=AppNotification("n",NotificationType.NEW_MESSAGE,conv.friend,null,"message",Instant.now(),false,conv.id);content.notices.value=Result.success(listOf(n));content.failure=AppException("offline")
  val vm=NotificationsViewModel(content,FakeSocialRepository());vm.onAction(NotificationsAction.ClickNotification(n));assertNull(withTimeoutOrNull(100){vm.effects.first()});assertFalse(content.notices.value.getOrThrow().single().isRead)
 }
 @Test fun `notification click uses stored conversation id and marks read`() = runTest(dispatcher) {
  val n=AppNotification("n",NotificationType.NEW_MESSAGE,conv.friend,null,"message",Instant.now(),false,conv.id);content.notices.value=Result.success(listOf(n))
  val vm=NotificationsViewModel(content,FakeSocialRepository());vm.onAction(NotificationsAction.ClickNotification(n));assertEquals(NotificationsEffect.NavigateToChat(conv.id),vm.effects.first());assertTrue(content.notices.value.getOrThrow().single().isRead)
 }
 @Test fun `friend request from someone already a friend shows as accepted, not pending`() {
  val friend=AppNotification("f",NotificationType.FRIEND_REQUEST,conv.friend,null,"lời mời",Instant.now(),false,null)
  val stranger=User(id="u_la",username="nguoila",displayName="Người lạ",avatarUrl=null)
  val pending=AppNotification("p",NotificationType.FRIEND_REQUEST,stranger,null,"lời mời",Instant.now(),false,null)
  content.notices.value=Result.success(listOf(friend,pending))
  val state=NotificationsViewModel(content,FakeSocialRepository()).uiState.value
  assertEquals(true,state.friendRequestDecision(friend))
  assertNull(state.friendRequestDecision(pending))
 }
 @Test fun `timeline orders messages newest first and labels today`() {
  val m=Message("m",conv.id,"me","hello",null,null,Instant.now(),PostSendState.SENT,false);val timeline=ChatViewModel.buildReverseTimeline(listOf(m),"me",Instant.now());assertEquals("m",(timeline.first() as ChatTimelineItem.MessageNode).message.id);assertEquals("Hôm nay",(timeline.last() as ChatTimelineItem.DateHeader).label)
 }
}
