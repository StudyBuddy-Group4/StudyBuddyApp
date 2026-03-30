package com.example.studybuddyapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.time.LocalDateTime;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class MeetingRoomActivityTest {

    @Test
    public void onMainViewChanged_remoteUser_updatesSpeakerLabel() {
        MeetingRoomActivity activity = buildActivity();

        activity.onMainViewChanged(42);

        TextView label = activity.findViewById(R.id.tvSpeakerId);
        assertEquals("ID: 42", label.getText().toString());
        assertEquals(android.view.View.VISIBLE, label.getVisibility());
    }

    @Test
    public void onMainViewChanged_localUser_hidesSpeakerLabel() {
        MeetingRoomActivity activity = buildActivity();

        activity.onMainViewChanged(0);

        TextView label = activity.findViewById(R.id.tvSpeakerId);
        assertEquals("You", label.getText().toString());
        assertEquals(android.view.View.GONE, label.getVisibility());
    }

    @Test
    public void flagButton_startsFlagParticipantActivityWithSnapshot() throws Exception {
        MeetingRoomActivity activity = buildActivity();
        MeetingRoomVideoCallManager manager = mock(MeetingRoomVideoCallManager.class);
        ArrayList<Integer> remoteUids = new ArrayList<>();
        remoteUids.add(11);
        remoteUids.add(22);
        when(manager.getRemoteUidsSnapshot()).thenReturn(remoteUids);

        setField(activity, "channelName", "room-30");
        setField(activity, "videoCallManager", manager);

        Method wireControlButtons = MeetingRoomActivity.class.getDeclaredMethod("wireControlButtons");
        wireControlButtons.setAccessible(true);
        wireControlButtons.invoke(activity);

        activity.findViewById(R.id.btnFlag).performClick();

        Intent nextIntent = shadowOf(activity).getNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals(FlagParticipantActivity.class.getName(), nextIntent.getComponent().getClassName());
        assertEquals("room-30", nextIntent.getStringExtra(MeetingRoomActivity.EXTRA_CHANNEL_NAME));
        assertEquals(remoteUids, nextIntent.getIntegerArrayListExtra("remote_uids"));
        assertTrue((Boolean) getField(activity, "isNavigatingToChild"));
    }

    @Test
    public void getRemovalMessage_returnsPermanentBanMessage() throws Exception {
        MeetingRoomActivity activity = buildActivity();
        com.example.studybuddyapp.api.dto.UserProfileResponse profile =
                new com.example.studybuddyapp.api.dto.UserProfileResponse();
        setField(profile, "isBanned", true);

        Method method = MeetingRoomActivity.class
                .getDeclaredMethod("getRemovalMessage",
                        com.example.studybuddyapp.api.dto.UserProfileResponse.class);
        method.setAccessible(true);
        String message = (String) method.invoke(activity, profile);

        assertTrue(message.contains("Banned Permanently"));
    }

    @Test
    public void getRemovalMessage_returnsThreeDayBanMessageForLongRestriction() throws Exception {
        MeetingRoomActivity activity = buildActivity();
        com.example.studybuddyapp.api.dto.UserProfileResponse profile =
                new com.example.studybuddyapp.api.dto.UserProfileResponse();
        setField(profile, "bannedUntil", LocalDateTime.now().plusDays(3).toString());

        Method method = MeetingRoomActivity.class
                .getDeclaredMethod("getRemovalMessage",
                        com.example.studybuddyapp.api.dto.UserProfileResponse.class);
        method.setAccessible(true);
        String message = (String) method.invoke(activity, profile);

        assertTrue(message.contains("Banned For 3 Days"));
    }

    @Test
    public void isLongTermBan_returnsFalseForInvalidTimestamp() throws Exception {
        MeetingRoomActivity activity = buildActivity();
        Method method = MeetingRoomActivity.class.getDeclaredMethod("isLongTermBan", String.class);
        method.setAccessible(true);

        boolean result = (Boolean) method.invoke(activity, "not-a-date");

        assertFalse(result);
    }

    private static MeetingRoomActivity buildActivity() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.putExtra(MeetingRoomActivity.EXTRA_FOCUS_DURATION, 30);
        intent.putExtra(MeetingRoomActivity.EXTRA_CHANNEL_NAME, "room-30");
        return Robolectric.buildActivity(MeetingRoomActivity.class, intent).setup().get();
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object getField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
