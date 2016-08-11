/*
 * #%L
 * NetRelay-Controller
 * %%
 * Copyright (C) 2015 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package de.braintags.netrelay.unit;

import de.braintags.io.vertx.pojomapper.IDataStore;
import de.braintags.io.vertx.pojomapper.dataaccess.query.IQuery;
import de.braintags.io.vertx.pojomapper.testdatastore.DatastoreBaseTest;
import de.braintags.io.vertx.pojomapper.testdatastore.ResultContainer;
import de.braintags.netrelay.controller.BodyController;
import de.braintags.netrelay.controller.CurrentMemberController;
import de.braintags.netrelay.controller.ThymeleafTemplateController;
import de.braintags.netrelay.controller.api.MailController;
import de.braintags.netrelay.controller.authentication.AuthenticationController;
import de.braintags.netrelay.controller.authentication.PasswordLostController;
import de.braintags.netrelay.controller.authentication.RegisterController;
import de.braintags.netrelay.controller.persistence.PersistenceController;
import de.braintags.netrelay.init.Settings;
import de.braintags.netrelay.model.Member;
import de.braintags.netrelay.routing.RouterDefinition;
import io.vertx.ext.unit.TestContext;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class NetRelayBaseConnectorTest extends NetRelayBaseTest {

  /**
   * 
   */
  public NetRelayBaseConnectorTest() {
  }

  /**
   * This method is modifying the {@link Settings} which are used to init NetRelay. Here it defines the
   * template directory as "testTemplates"
   * 
   * @param settings
   */
  @Override
  public void modifySettings(TestContext context, Settings settings) {
    super.modifySettings(context, settings);
    settings.getRouterDefinitions().add(MailController.createDefaultRouterDefinition());
    settings.getRouterDefinitions().add(AuthenticationController.createDefaultRouterDefinition());
    settings.getRouterDefinitions().add(RegisterController.createDefaultRouterDefinition());
    settings.getRouterDefinitions().add(PasswordLostController.createDefaultRouterDefinition());
    settings.getRouterDefinitions().add(CurrentMemberController.createDefaultRouterDefinition());
    settings.getRouterDefinitions().addAfter(BodyController.class.getSimpleName(),
        PersistenceController.createDefaultRouterDefinition());

    RouterDefinition tct = ThymeleafTemplateController.createDefaultRouterDefinition();
    tct.getHandlerProperties().setProperty(ThymeleafTemplateController.TEMPLATE_DIRECTORY_PROPERTY, "testTemplates");
    settings.getRouterDefinitions().add(tct);

  }

  /**
   * Searches in the database, wether a member with the given username / password exists.
   * If not, it is created. After the found or created member is returned
   * 
   * @param context
   * @param datastore
   * @param member
   * @return
   */
  public static final Member createOrFindMember(TestContext context, IDataStore datastore, Member member) {
    IQuery<Member> query = datastore.createQuery(Member.class);
    query.field("userName").is(member.getUserName()).field("password").is(member.getPassword());
    Member returnMember = (Member) DatastoreBaseTest.findFirst(context, query);
    if (returnMember == null) {
      ResultContainer cont = DatastoreBaseTest.saveRecord(context, member);
      returnMember = member;
    }
    return returnMember;
  }

}
