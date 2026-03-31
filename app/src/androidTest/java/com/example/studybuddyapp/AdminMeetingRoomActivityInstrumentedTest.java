package com.example.studybuddyapp;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.widget.LinearLayout;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.lifecycle.Lifecycle;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AdminMeetingRoomActivityInstrumentedTest {

    private AdminMeetingRoomActivity activity;
    private ActivityScenario<AdminMeetingRoomActivity> scenario;

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
        if (activity != null) {
            activity.finish();
        }
    }

    @Test
    public void onCreate_withoutChannel_finishesActivity() {
        Intent intent = buildIntent(null, -1L, -1L);

        scenario = ActivityScenario.launch(intent);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertTrue(scenario.getState() == Lifecycle.State.DESTROYED);
    }

    @Test
    public void onCreate_withChannel_bindsParticipantGridAndBackFinishes() {
        launchActivity("admin-room", 5L, 9L);

        LinearLayout participantGrid = activity.findViewById(R.id.participantGrid);
        assertNotNull(participantGrid);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                activity.findViewById(R.id.ivBack).performClick());
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertTrue(activity.isFinishing() || activity.isDestroyed());
    }

    private void launchActivity(String channelName, long reportId, long reportedUserId) {
        Intent intent = buildIntent(channelName, reportId, reportedUserId);
        activity = (AdminMeetingRoomActivity) InstrumentationRegistry.getInstrumentation()
                .startActivitySync(intent);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private Intent buildIntent(String channelName, long reportId, long reportedUserId) {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, AdminMeetingRoomActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (channelName != null) {
            intent.putExtra("channel_name", channelName);
        }
        intent.putExtra("report_id", reportId);
        intent.putExtra("reported_user_id", reportedUserId);
        return intent;
    }
}
