/*
 * Copyright (c) 2015, salesforce.com, inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.salesforce.dataloader.action.visitor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sforce.async.AsyncApiException;
import com.sforce.async.BulkConnection;
import com.sforce.ws.ConnectorConfig;

public class BulkV1Connection extends BulkConnection {
    private static final String SFORCE_CALL_OPTIONS_HEADER = "Sforce-Call-Options";
    private static Logger logger = LogManager.getLogger(BulkV1Connection.class);

    public BulkV1Connection(ConnectorConfig config) throws AsyncApiException {
        super(config);
        
        // This is needed to set the correct client name in Bulk V1 calls
        addHeader(SFORCE_CALL_OPTIONS_HEADER, config.getRequestHeader("Sforce-Call-Options"));
    }
    
    public void addHeader(String headerName, String headerValue) {
        super.addHeader(headerName, headerValue);
        if ("Sforce-Call-Options".equalsIgnoreCase(headerName)) {
            logger.debug("Sforce-Call-Options : " + headerValue);
        }
    }
}