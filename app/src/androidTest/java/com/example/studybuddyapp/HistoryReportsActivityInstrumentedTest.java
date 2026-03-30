package com.example.studybuddyapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.studybuddyapp.api.dto.ReportResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class HistoryReportsActivityInstrumentedTest {

    private HistoryReportsActivity activity;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, HistoryReportsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        activity = (HistoryReportsActivity) InstrumentationRegistry.getInstrumentation()
                .startActivitySync(intent);
    }

    @After
    public void tearDown() {
        if (activity != null) {
            activity.finish();
        }
    }

    // test that an empty report list shows the empty state message
    @Test
    public void populateReportsList_showsEmptyStateWhenListIsEmpty() throws Exception {
        invokePopulateReportsList(Collections.emptyList());

        LinearLayout reportsContainer = activity.findViewById(R.id.reportsContainer);
        TextView emptyView = (TextView) reportsContainer.getChildAt(0);

        assertEquals(1, reportsContainer.getChildCount());
        assertEquals("No reports found.", emptyView.getText().toString());
    }

    // test that a dismissed report uses the invalid label and dismissed icon
    @Test
    public void populateReportsList_rendersDismissedReportState() throws Exception {
        ReportResponse report = createReportResponse(15L, "DISMISSED", "2026-03-30T12:45:00");

        invokePopulateReportsList(Collections.singletonList(report));

        LinearLayout reportsContainer = activity.findViewById(R.id.reportsContainer);
        View row = reportsContainer.getChildAt(0);
        TextView userView = row.findViewById(R.id.tvReportUser);
        TextView actionView = row.findViewById(R.id.tvReportAction);
        TextView dateView = row.findViewById(R.id.tvReportDate);
        ImageView iconView = row.findViewById(R.id.ivReportIcon);

        assertEquals("Flag : 15", userView.getText().toString());
        assertEquals("Invalid", actionView.getText().toString());
        assertEquals("Mar 30 - 2026", dateView.getText().toString());
        assertTrue(drawablesMatch(iconView.getDrawable(), R.drawable.ic_close_circle));
    }

    // test that missing fields fall back to a pending label and unknown user id
    @Test
    public void populateReportsList_usesFallbacksForMissingStatusAndUserId() throws Exception {
        ReportResponse report = createReportResponse(null, null, "bad-value");

        invokePopulateReportsList(Collections.singletonList(report));

        LinearLayout reportsContainer = activity.findViewById(R.id.reportsContainer);
        View row = reportsContainer.getChildAt(0);
        TextView userView = row.findViewById(R.id.tvReportUser);
        TextView actionView = row.findViewById(R.id.tvReportAction);
        TextView dateView = row.findViewById(R.id.tvReportDate);

        assertEquals("Flag : ?", userView.getText().toString());
        assertEquals("Pending review", actionView.getText().toString());
        assertEquals("", dateView.getText().toString());
    }

    // test that multiple reports insert a divider between rendered rows
    @Test
    public void populateReportsList_addsDividerBetweenRows() throws Exception {
        ReportResponse first = createReportResponse(1L, "ACTIONED", "2026-03-28T08:00:00");
        ReportResponse second = createReportResponse(2L, "PENDING", "2026-03-29T09:00:00");

        invokePopulateReportsList(Arrays.asList(first, second));

        LinearLayout reportsContainer = activity.findViewById(R.id.reportsContainer);

        assertEquals(4, reportsContainer.getChildCount());
        assertEquals(View.class, reportsContainer.getChildAt(1).getClass());
        assertEquals(View.class, reportsContainer.getChildAt(3).getClass());
    }

    private void invokePopulateReportsList(java.util.List<ReportResponse> reports) throws Exception {
        Method method = HistoryReportsActivity.class
                .getDeclaredMethod("populateReportsList", java.util.List.class);
        method.setAccessible(true);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try {
                method.invoke(activity, reports);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private ReportResponse createReportResponse(Long userId, String status, String timestamp)
            throws Exception {
        ReportResponse report = new ReportResponse();
        setField(report, "reportedUserId", userId);
        setField(report, "status", status);
        setField(report, "timestamp", timestamp);
        return report;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private boolean drawablesMatch(Drawable actual, int expectedResId) {
        Drawable expected = activity.getDrawable(expectedResId);
        return actual != null
                && expected != null
                && drawableToBitmap(actual).sameAs(drawableToBitmap(expected));
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        int width = Math.max(1, drawable.getIntrinsicWidth());
        int height = Math.max(1, drawable.getIntrinsicHeight());

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
