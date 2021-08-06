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

package com.boardgamefiesta.server.donation;

import com.paypal.base.rest.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class TransactionHistory extends PayPalResource {
    List<TransactionDetail> transactionDetails;

    public static TransactionHistory list(APIContext apiContext, Map<String, String> params) throws PayPalRESTException {
        var pattern = "v1/reporting/transactions?start_date={start_date}&end_date={end_date}";
        var resourcePath = RESTUtil.formatURIPath(pattern, params);
        var payload = "";
        var transactionHistory = configureAndExecute(apiContext, HttpMethod.GET, resourcePath, payload, TransactionHistory.class);
        apiContext.setRequestId(null);
        return transactionHistory;
    }
}
