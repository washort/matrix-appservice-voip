/*
 * matrix-appservice-voip - Matrix Bridge to VoIP/SMS
 * Copyright (C) 2018 Kamax Sarl
 *
 * https://www.kamax.io/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.matrix.bridge.voip;

import io.kamax.matrix.bridge.voip.matrix.MatrixEndpoint;
import io.kamax.matrix.bridge.voip.remote.RemoteEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class Call {

    private final Logger log = LoggerFactory.getLogger(Call.class);

    private String id;
    private MatrixEndpoint local;
    private RemoteEndpoint remote;

    public Call(String id, MatrixEndpoint local, RemoteEndpoint remote) {
        this.id = id;
        this.local = local;
        this.remote = remote;

        this.local.addListener(new CallListener() {

            @Override
            public void onInvite(String destination, CallInviteEvent ev) {
                log.info("Call {}: Matrix: invite for {}", id, destination);
                remote.handle(destination, ev);
            }

            @Override
            public void onCandidates(CallCandidatesEvent ev) {
                log.info("Call {}: Matrix: candidates", id);
            }

            @Override
            public void onAnswer(CallAnswerEvent ev) {
                log.info("Call {}: Matrix: answer", id);
            }

            @Override
            public void onHangup(CallHangupEvent ev) {
                log.info("Call {}: Matrix: hangup", id);
                terminate(ev.getReason());
            }

            @Override
            public void onClose() {
                log.info("Call {}: Matrix: close", id);
                terminate();
            }

        });

        this.remote.addListener(new CallListener() {

            @Override
            public void onInvite(String from, CallInviteEvent ev) {
                log.info("Call {}: Remote: invite from {}", id, from);
                local.handle(ev);
            }

            @Override
            public void onCandidates(CallCandidatesEvent ev) {
                log.info("Call {}: Remote: candidates", id);
            }

            @Override
            public void onAnswer(CallAnswerEvent ev) {
                log.info("Call {}: Remote: answer", id);
            }

            @Override
            public void onHangup(CallHangupEvent ev) {
                log.info("Call {}: Remote: hangup", id);
                terminate(ev.getReason());
            }

            @Override
            public void onClose() {
                log.info("Call {}: Remote: close", id);
                terminate();
            }
        });
    }

    public synchronized void terminate(String reason) {
        if (Objects.isNull(local) || Objects.isNull(remote)) {
            return;
        }

        log.info("Call {}: terminating", id);
        local.handle(CallHangupEvent.forCall(id, reason));
        remote.close();

        local = null;
        remote = null;
    }

    public void terminate() {
        terminate(null);
    }

}