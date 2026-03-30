package com.example.studybuddyapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class MeetingRoomVideoCallManagerTest {

    @Test
    public void getRemoteUidsSnapshot_returnsDefensiveCopy() throws Exception {
        MeetingRoomVideoCallManager manager = buildManager();
        List<Integer> remoteUids = getRemoteUids(manager);
        remoteUids.add(7);
        remoteUids.add(9);

        java.util.ArrayList<Integer> snapshot = manager.getRemoteUidsSnapshot();
        snapshot.add(99);

        assertEquals(2, getRemoteUids(manager).size());
        assertEquals(2, manager.getRemoteUidsSnapshot().size());
    }

    @Test
    public void setCameraOff_updatesLocalSurfaceVisibility() throws Exception {
        MeetingRoomVideoCallManager manager = buildManager();
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        SurfaceView surface = new SurfaceView(activity);
        setField(manager, "localSurface", surface);

        manager.setCameraOff(true);
        assertEquals(android.view.View.GONE, surface.getVisibility());

        manager.setCameraOff(false);
        assertEquals(android.view.View.VISIBLE, surface.getVisibility());
    }

    @Test
    public void leaveAndCleanup_clearsTrackedStateWithoutRtcEngine() throws Exception {
        MeetingRoomVideoCallManager manager = buildManager();
        getRemoteUids(manager).add(5);
        getRemoteSurfaces(manager).put(5, new SurfaceView(
                Robolectric.buildActivity(AppCompatActivity.class).setup().get()));
        setField(manager, "isInChannel", true);
        setField(manager, "localSurface", new SurfaceView(
                Robolectric.buildActivity(AppCompatActivity.class).setup().get()));

        manager.leaveAndCleanup();

        assertTrue(getRemoteUids(manager).isEmpty());
        assertTrue(getRemoteSurfaces(manager).isEmpty());
        assertNull(getField(manager, "localSurface"));
        assertEquals(false, getField(manager, "isInChannel"));
    }

    @Test
    public void rebindContainers_rebuildsLocalThumbnailWhenRemoteOwnsMainView() throws Exception {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        FrameLayout oldMain = new FrameLayout(activity);
        LinearLayout oldThumbs = new LinearLayout(activity);
        MeetingRoomVideoCallManager manager =
                new MeetingRoomVideoCallManager(activity, oldMain, oldThumbs, new NoOpCallbacks());

        setField(manager, "localSurface", new SurfaceView(activity));
        setField(manager, "mainViewUid", 99);

        FrameLayout newMain = new FrameLayout(activity);
        LinearLayout newThumbs = new LinearLayout(activity);
        manager.rebindContainers(newMain, newThumbs);

        assertEquals(1, newThumbs.getChildCount());
        assertEquals("local", newThumbs.getChildAt(0).getTag());
    }

    @Test
    public void addLabelToFrame_addsVisibleTextLabel() throws Exception {
        MeetingRoomVideoCallManager manager = buildManager();
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        FrameLayout frame = new FrameLayout(activity);

        java.lang.reflect.Method method = MeetingRoomVideoCallManager.class
                .getDeclaredMethod("addLabelToFrame", FrameLayout.class, String.class);
        method.setAccessible(true);
        method.invoke(manager, frame, "You");

        TextView label = findTextView(frame, "You");
        assertEquals("You", label.getText().toString());
    }

    private static MeetingRoomVideoCallManager buildManager() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        FrameLayout main = new FrameLayout(activity);
        LinearLayout thumbnails = new LinearLayout(activity);
        return new MeetingRoomVideoCallManager(activity, main, thumbnails, new NoOpCallbacks());
    }

    @SuppressWarnings("unchecked")
    private static List<Integer> getRemoteUids(MeetingRoomVideoCallManager manager) throws Exception {
        return (List<Integer>) getField(manager, "remoteUids");
    }

    @SuppressWarnings("unchecked")
    private static Map<Integer, SurfaceView> getRemoteSurfaces(MeetingRoomVideoCallManager manager) throws Exception {
        return (Map<Integer, SurfaceView>) getField(manager, "remoteSurfaces");
    }

    private static Object getField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static TextView findTextView(View root, String text) {
        if (root instanceof TextView && text.equals(((TextView) root).getText().toString())) {
            return (TextView) root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = findTextView(group.getChildAt(i), text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static final class NoOpCallbacks implements MeetingRoomVideoCallManager.Callbacks {
        @Override
        public void onChannelJoined() {
        }

        @Override
        public void onMainViewChanged(int uid) {
        }
    }
}
