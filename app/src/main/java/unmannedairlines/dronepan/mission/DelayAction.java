package unmannedairlines.dronepan.mission;

import android.os.Handler;
import android.support.annotation.Nullable;

import dji.common.error.DJIError;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.timeline.TimelineElement;
import dji.sdk.mission.timeline.TimelineElementFeedback;


public class DelayAction extends TimelineElement {

    Handler handler;
    TimelineElementFeedback feedback;
    int delayInMilliseconds;
    boolean cancelled;

    public DelayAction(int delayInMilliseconds) {
        this.delayInMilliseconds = delayInMilliseconds;
        this.handler = new Handler();
        this.feedback = MissionControl.getInstance();
    }

    @Override
    public void run() {
        this.cancelled = false;

        final DelayAction self = this;
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (self.cancelled) {
                    return;
                }

                feedback.onFinishWithError(self, null);
            }
        };

        handler.postDelayed(r, this.delayInMilliseconds);
    }

    @Override
    public boolean isPausable() {
        return false;
    }

    @Override
    public void stop() {
        this.cancelled = true;
        this.feedback.onStopWithError(this, null);
    }

    @Override
    public DJIError checkValidity() {
        return null;
    }
    @Override
    public void finishRun(@Nullable DJIError var1) {return;}


}
