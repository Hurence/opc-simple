/*
 *  Copyright (C) 2018 Hurence (support@hurence.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.hurence.opc;

import java.util.Objects;
import java.util.Optional;

/**
 * The status of an operation.
 *
 * @author amarziali
 */
public class OperationStatus {

    /**
     * The importance of a status.
     */
    public enum Level {
        INFO,
        WARNING,
        ERROR
    }

    /**
     * The status level.
     */
    private final Level level;
    /**
     * The full code.
     */
    private final long code;
    /**
     * The code meaning (the message).
     */
    private final Optional<String> messageDetail;


    /**
     * Construct a new instance.
     *
     * @param level         the {@link Level}
     * @param code          the full status code.
     * @param messageDetail the status code human readable meaning.
     */
    public OperationStatus(Level level, long code, Optional<String> messageDetail) {
        this.level = level;
        this.code = code;
        this.messageDetail = messageDetail;
    }

    /**
     * The status level.
     *
     * @return a {@link Level}
     */
    public Level getLevel() {
        return level;
    }

    /**
     * The full code.
     *
     * @return the original error code.
     */
    public long getCode() {
        return code;
    }

    /**
     * The code meaning. Optional but never null.
     *
     * @return the {@link Optional} code description.
     */
    public Optional<String> getMessageDetail() {
        return messageDetail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OperationStatus that = (OperationStatus) o;
        return code == that.code &&
                level == that.level;
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, code);
    }

    @Override
    public String toString() {
        return "OperationStatus{" +
                "level=" + level +
                ", code=" + code +
                ", messageDetail='" + messageDetail + '\'' +
                '}';
    }
}
