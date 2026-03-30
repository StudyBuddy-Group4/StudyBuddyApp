package com.example.studybuddyapp;

import android.content.Context;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.studybuddyapp.api.dto.UserProfileResponse;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class MeetingRoomActivityInstrumentedTest {

    // test that long temporary bans are treated as long-term restrictions
    @Test
    public void isLongTermBan_returnsTrueForFutureRestriction() throws Exception {
        MeetingRoomActivity activity = createActivityOnMainThread();
        Method method = MeetingRoomActivity.class
                .getDeclaredMethod("isLongTermBan", String.class);
        method.setAccessible(true);

        String futureBan = LocalDateTime.now().plusDays(2).toString();

        boolean result = (boolean) method.invoke(activity, futureBan);

        assertTrue(result);
    }

    // test that invalid or short ban values are not treated as long-term restrictions
    @Test
    public void isLongTermBan_returnsFalseForInvalidValue() throws Exception {
        MeetingRoomActivity activity = createActivityOnMainThread();
        Method method = MeetingRoomActivity.class
                .getDeclaredMethod("isLongTermBan", String.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(activity, "not-a-date");

        assertFalse(result);
    }

    // test that a permanently banned user gets the permanent removal message
    @Test
    public void getRemovalMessage_returnsPermanentBanMessage() throws Exception {
        MeetingRoomActivity activity = createActivityOnMainThread();
        Method method = MeetingRoomActivity.class
                .getDeclaredMethod("getRemovalMessage", UserProfileResponse.class);
        method.setAccessible(true);

        UserProfileResponse profile = new UserProfileResponse();
        setField(profile, "isBanned", true);

        String message = (String) method.invoke(activity, profile);

        assertTrue(message.contains("Banned Permanently"));
    }

    // test that the timer label uses the expected mm:ss format with the line break
    @Test
    public void updateTimerText_formatsTimerLabel() throws Exception {
        MeetingRoomActivity activity = createActivityOnMainThread();
        Context context = ApplicationProvider.getApplicationContext();
        TextView timerView = new TextView(context);
        setField(activity, "tvTimer", timerView);

        Method method = MeetingRoomActivity.class
                .getDeclaredMethod("updateTimerText", long.class);
        method.setAccessible(true);
        method.invoke(activity, 125000L);

        assertEquals("02:05\nLeft", timerView.getText().toString());
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private MeetingRoomActivity createActivityOnMainThread() {
        AtomicReference<MeetingRoomActivity> reference = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> reference.set(new MeetingRoomActivity()));
        return reference.get();
    }
}
