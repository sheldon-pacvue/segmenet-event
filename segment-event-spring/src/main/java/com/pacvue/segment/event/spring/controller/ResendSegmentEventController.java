package com.pacvue.segment.event.spring.controller;

import com.pacvue.segment.event.core.SegmentIO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResendSegmentEventController {
    @Autowired
    private SegmentIO segmentIO;

    @GetMapping("/segment/resend/start")
    public Boolean resendStart() {
        return segmentIO.startResend();
    }

    @GetMapping("/segment/resend/stop")
    public Boolean stopStart() {
        segmentIO.stopResend();
        return true;
    }
}
