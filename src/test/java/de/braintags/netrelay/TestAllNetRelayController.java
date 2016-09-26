/*
 * #%L
 * netrelay
 * %%
 * Copyright (C) 2015 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package de.braintags.netrelay;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.braintags.netrelay.unit.TAuthenticationDatastore;
import de.braintags.netrelay.unit.TAuthenticationDatastoreEncrypted;
import de.braintags.netrelay.unit.TAuthenticationMongo;
import de.braintags.netrelay.unit.TAuthorization;
import de.braintags.netrelay.unit.TCaptureParameters;
import de.braintags.netrelay.unit.TDataTablesController;
import de.braintags.netrelay.unit.TFailureController;
import de.braintags.netrelay.unit.TMailController;
import de.braintags.netrelay.unit.TMailProcessor;
import de.braintags.netrelay.unit.TPasswordLost;
import de.braintags.netrelay.unit.TProtocolController;
import de.braintags.netrelay.unit.TRegistration;
import de.braintags.netrelay.unit.TTemplateController;
import de.braintags.netrelay.unit.TVirtualHostController;
import de.braintags.netrelay.unit.elfinder.ElFinderTestSuite;
import de.braintags.netrelay.unit.persistence.TPersistenceSuite;

/**
 * LET TSettings the last class
 * 
 * @author Michael Remme
 * 
 */
@RunWith(Suite.class)
@SuiteClasses({ TestAllNetRelay.class, TFailureController.class, TTemplateController.class, TCaptureParameters.class,
    TMailProcessor.class, TPersistenceSuite.class, TAuthenticationMongo.class, TAuthenticationDatastore.class,
    TAuthenticationDatastoreEncrypted.class, TAuthorization.class, TRegistration.class, TPasswordLost.class,
    TDataTablesController.class, TMailController.class, TVirtualHostController.class, TProtocolController.class,
    ElFinderTestSuite.class })

public class TestAllNetRelayController {
  // -DBlockedThreadCheckInterval=10000000 -DWarningExceptionTime=10000000 -DtestTimeout=5
  // -Djava.util.logging.config.file=src/main/resources/logging.properties

  // -DBlockedThreadCheckInterval=10000000 -DWarningExceptionTime=10000000 -DtestTimeout=500
  // -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.Log4jLogDelegateFactory
  // -DmailClientUserName=dev-test@braintags.net -DmailClientPassword=

}
