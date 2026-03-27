package com.example.studybuddyapp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AgoraConfigTest {

    @Test
    public void channelNameForDuration_returnsChannelNameForPresetDuration() {
        assertEquals("study_15", AgoraConfig.channelNameForDuration(15));
    }

    @Test
    public void channelNameForDuration_returnsChannelNameForAnotherPresetDuration() {
        assertEquals("study_30", AgoraConfig.channelNameForDuration(30));
    }

    @Test
    public void channelNameForDuration_returnsChannelNameForCustomDuration() {
        assertEquals("study_45", AgoraConfig.channelNameForDuration(45));
    }
}
