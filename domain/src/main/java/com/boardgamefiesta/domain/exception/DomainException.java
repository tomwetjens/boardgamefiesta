/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.boardgamefiesta.domain.exception;

import lombok.Getter;

public abstract class DomainException extends RuntimeException {

    protected DomainException(String errorCode) {
        this(errorCode, errorCode);
    }

    protected DomainException(String errorCode, String message) {
        super(message);

        this.errorCode = errorCode;
    }

    protected DomainException(String errorCode, Throwable cause) {
        this(errorCode, errorCode, cause);
    }

    protected DomainException(String errorCode, String message, Throwable cause) {
        super(message, cause);

        this.errorCode = errorCode;
    }

    @Getter
    private final String errorCode;

}
