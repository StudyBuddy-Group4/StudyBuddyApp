package com.example.studybuddyapp;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class FlagParticipantActivityInstrumentedTest {

    private FlagParticipantActivity activity;

    @After
    public void tearDown() {
        if (activity != null) {
            activity.finish();
        }
    }

    // test that an empty participant list shows the empty state and hides the grid
    @Test
    public void setupParticipantSlots_showsEmptyStateWhenNoRemoteUsersExist() {
        launchWithRemoteUids(new ArrayList<>());

        TextView emptyView = activity.findViewById(R.id.tvNoParticipants);
        View participantGrid = activity.findViewById(R.id.participantGrid);

        assertEquals(View.VISIBLE, emptyView.getVisibility());
        assertEquals(View.GONE, participantGrid.getVisibility());
    }

    // test that a single remote user fills the first slot and keeps the second row hidden
    @Test
    public void setupParticipantSlots_showsFirstSlotForSingleParticipant() {
        launchWithRemoteUids(new ArrayList<>(Arrays.asList(42)));

        View slot1 = activity.findViewById(R.id.slot1);
        View row2 = activity.findViewById(R.id.row2);
        TextView uidLabel = activity.findViewById(R.id.tvUid1);

        assertEquals(View.VISIBLE, slot1.getVisibility());
        assertEquals("User ID: 42", uidLabel.getText().toString());
        assertEquals(View.GONE, row2.getVisibility());
    }

    // test that four remote users reveal the second row and the last slot label
    @Test
    public void setupParticipantSlots_showsSecondRowWhenMoreThanTwoParticipantsExist() {
        launchWithRemoteUids(new ArrayList<>(Arrays.asList(11, 22, 33, 44)));

        View row2 = activity.findViewById(R.id.row2);
        View slot4 = activity.findViewById(R.id.slot4);
        TextView uidLabel4 = activity.findViewById(R.id.tvUid4);

        assertEquals(View.VISIBLE, row2.getVisibility());
        assertEquals(View.VISIBLE, slot4.getVisibility());
        assertEquals("User ID: 44", uidLabel4.getText().toString());
    }

    // test that tapping a flag button stores the selected reported user id
    @Test
    public void flagButtonClick_updatesSelectedReportedUserId() throws Exception {
        launchWithRemoteUids(new ArrayList<>(Arrays.asList(77)));

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                activity.findViewById(R.id.flagBtn1).performClick());
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertEquals(77L, getSelectedReportedUserId());
    }

    private void launchWithRemoteUids(ArrayList<Integer> remoteUids) {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, FlagParticipantActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(MeetingRoomActivity.EXTRA_CHANNEL_NAME, "room-1");
        intent.putIntegerArrayListExtra("remote_uids", remoteUids);

        activity = (FlagParticipantActivity) InstrumentationRegistry.getInstrumentation()
                .startActivitySync(intent);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private long getSelectedReportedUserId() throws Exception {
        Field field = FlagParticipantActivity.class.getDeclaredField("selectedReportedUserId");
        field.setAccessible(true);
        return field.getLong(activity);
    }
}
